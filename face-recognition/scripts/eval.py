"""人脸识别评估工具（对应任务书"识别测试"交付物）

模式一：数据集评估
    python scripts/eval.py --dataset "C:/Users/18022/Downloads/样本集"
    目录结构（两种写法都支持）：
        样本集/<姓名>/              该对象的测试照片
        样本集/<姓名>——注册集/      该对象人工挑选的注册照（可缺省）
        样本集/负样本/              未注册人员照片（__negative__ 亦可）
    注册集不足 enroll-count 张时，从 <姓名>/ 里按质量自动补足，
    被选中的照片随即从测试集中移除，避免注册集与测试集重叠。

输出（全部写入 --out 目录）：
    summary.md      统计表与混淆矩阵（报告直接可用）
    per_image.csv   逐张记录（运行记录）
    config.json     本次评测使用的全部参数（运行记录）

指标口径（关键：被拒的样本计入分母，不静默丢弃）：
    漏检率   连人脸都没检出（关掉质量门控也检不出）占全部样本的比例
    拒识率   已注册对象中"未能正确识别"的比例 = 漏检 + 被门控拒 + 相似度不足
    误识率   已注册对象被认成了别人
    准确率   仅在"通过检出并接受"的样本上统计，用于和拒识率对照看
    FAR      未注册对象被当成已注册人员放行的比例

模式二：视频日志对比（验证多帧投票与跳帧改进点）
    python scripts/demo.py --source clip.mp4 --no-voting --log raw.csv
    python scripts/demo.py --source clip.mp4 --log voting.csv
    python scripts/eval.py --compare-logs raw.csv voting.csv
"""

import argparse
import csv
import json
import sys
import time
from collections import Counter
from datetime import datetime
from pathlib import Path

import cv2
import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from face_engine.config import load_config
from face_engine.detector import FaceDetector
from face_engine.engine import FaceEngine

IMAGE_EXTS = {".jpg", ".jpeg", ".png", ".bmp"}
ENROLL_SUFFIX = "——注册集"
NEGATIVE_DIRS = {"负样本", "__negative__"}
REJECT_LABEL = "拒识"


# ---------------------------------------------------------------- 数据读取

def _list_images(d: Path):
    if not d.is_dir():
        return []
    return sorted(p for p in d.iterdir() if p.suffix.lower() in IMAGE_EXTS)


def _read_image(path: Path):
    """读取图像（自动校正手机照片的 EXIF 方向，避免竖拍图片被横读）"""
    try:
        from PIL import Image, ImageOps

        rgb = ImageOps.exif_transpose(Image.open(path).convert("RGB"))
        return cv2.cvtColor(np.array(rgb), cv2.COLOR_RGB2BGR)
    except Exception:
        return cv2.imread(str(path))


def load_dataset(root: Path):
    """扫描数据集，返回 (每人照片表, 负样本路径表)

    每人照片表：{姓名: {"enroll": [...], "test": [...]}}
    """
    people, negatives = {}, []
    for entry in sorted(root.iterdir()):
        if not entry.is_dir():
            continue
        if entry.name in NEGATIVE_DIRS:
            negatives.extend(_list_images(entry))
        elif entry.name.endswith(ENROLL_SUFFIX):
            owner = entry.name[: -len(ENROLL_SUFFIX)]
            people.setdefault(owner, {"enroll": [], "test": []})["enroll"].extend(
                _list_images(entry)
            )
        else:
            people.setdefault(entry.name, {"enroll": [], "test": []})["test"].extend(
                _list_images(entry)
            )
    return people, negatives


# ---------------------------------------------------------------- 注册

def _score_photos(detector: FaceDetector, paths):
    """给候选注册照打分：检测置信度 × √人脸占比

    既要检出可靠，也要人脸足够大——注册照决定后续比对的基准，不能随便挑。
    返回按分数降序的 [(分数, 路径)]，检不出人脸的图片不参与。
    """
    scored = []
    for p in paths:
        img = _read_image(p)
        if img is None:
            continue
        h, w = img.shape[:2]
        faces = detector.detect(img)
        if not faces:
            continue
        f = faces[0]
        ratio = (f.bbox[2] * f.bbox[3]) / (w * h)
        scored.append((f.score * float(np.sqrt(ratio)), p))
    scored.sort(key=lambda t: t[0], reverse=True)
    return scored


def pick_enrollment(detector: FaceDetector, enroll_paths, test_paths, count: int):
    """挑出用于注册的照片

    优先使用人工挑选的注册集（相信人的判断），数量不足 count 时，
    再从测试集里按检测质量补足。补进来的照片会从测试集中移除。
    返回 (选中的路径, 评分明细)。
    """
    chosen = [p for _, p in _score_photos(detector, enroll_paths)][:count]
    rest = [p for p in enroll_paths if p not in set(chosen)]
    if len(chosen) < count:
        for _, p in _score_photos(detector, test_paths):
            if len(chosen) >= count:
                break
            chosen.append(p)
        rest += [p for p in test_paths if p not in set(chosen)]
    return chosen, rest


def register_people(engine: FaceEngine, people, enroll_count: int, manifest=None):
    """注册全部对象；返回 ({姓名: 注册向量数}, {姓名: 注册照片路径})

    manifest 给定时直接沿用其中的注册照，不再重新挑选。消融实验必须这么做：
    否则每个变体挑出不同的注册照、测试集随之变化，各变体的指标就不可比了。
    """
    info, chosen_map = {}, {}
    for name, files in people.items():
        if manifest and name in manifest:
            picked = [Path(p) for p in manifest[name]]
        else:
            picked, _ = pick_enrollment(
                engine.detector, files["enroll"], files["test"], enroll_count
            )
        chosen_map[name] = picked
        # 被选去注册的照片要从测试集里剔除，避免注册集与测试集重叠
        files["test"] = [p for p in files["test"] if p not in set(picked)]
        frames = [f for f in (_read_image(p) for p in picked) if f is not None]
        info[name] = engine.register(name, name, frames)
    return info, chosen_map


# ---------------------------------------------------------------- 单张判定

def classify(engine, img, detector_nogate: FaceDetector, threshold_rule: str, timing=None):
    """判定单张图片，返回记录字典

    threshold_rule: "global" 用全局拒绝阈值；"adaptive" 用每人自适应阈值
    timing: 传入则记录各阶段耗时（报告用的分阶段时延）
    """
    rec = {"outcome": None, "user_id": None, "sim": 0.0, "threshold": 0.0,
           "detected": False, "gated_out": False, "quality": {}, "ratio": 0.0}
    t0 = time.perf_counter()
    faces = engine.detector.detect(img)
    if timing is not None:
        timing["检测"].append(time.perf_counter() - t0)
    if not faces:
        # 区分"根本检不出人脸"和"检出了但被质量门控拒掉"；
        # 后者记录关掉门控时的人脸占比，供分档表定位它在哪一档被拦下
        loose = detector_nogate.detect(img)
        rec["outcome"] = "no_face"
        rec["gated_out"] = bool(loose)
        if loose:
            h, w = img.shape[:2]
            rec["ratio"] = (loose[0].bbox[2] * loose[0].bbox[3]) / (w * h)
        return rec
    rec["detected"] = True
    face = faces[0]
    h, w = img.shape[:2]
    rec["ratio"] = (face.bbox[2] * face.bbox[3]) / (w * h)

    t0 = time.perf_counter()
    emb = engine.embedder.embed(face.image)
    if timing is not None:
        timing["特征提取"].append(time.perf_counter() - t0)

    t0 = time.perf_counter()
    match = engine.gallery.match(emb)
    if timing is not None:
        timing["库比对"].append(time.perf_counter() - t0)

    rec.update(user_id=match.user_id, sim=match.similarity,
               threshold=match.threshold, quality=face.quality)

    thr = (engine.recognizer.reject_threshold if threshold_rule == "global"
           else max(match.threshold, engine.recognizer.reject_threshold))
    rec["threshold"] = thr
    if match.user_id is None or match.similarity < thr:
        rec["outcome"] = "rejected"
    else:
        rec["outcome"] = "accepted"
    return rec


# ---------------------------------------------------------------- 指标

def summarize(pos_records, neg_records, total_pos, total_neg):
    """把逐张记录汇总成指标

    分母一律用"全部样本"，不做筛选——被拒绝的样本也是结果，必须计入。
    """
    no_face = sum(1 for r in pos_records if r["outcome"] == "no_face")
    gated = sum(1 for r in pos_records if r["outcome"] == "no_face" and r["gated_out"])
    reject = sum(1 for r in pos_records if r["outcome"] == "rejected")
    wrong = sum(1 for r in pos_records if r["outcome"] == "accepted" and r["user_id"] != r["true"])
    right = sum(1 for r in pos_records if r["outcome"] == "accepted" and r["user_id"] == r["true"])
    accepted = right + wrong

    far = sum(1 for r in neg_records if r["outcome"] == "accepted")
    neg_face = sum(1 for r in neg_records if r["outcome"] != "no_face")

    p = max(1, total_pos)
    return {
        "n_pos": total_pos,
        "n_neg": total_neg,
        "no_face": no_face,
        "gated_out": gated,
        "miss_rate": no_face / p,
        "reject_only": reject / p,
        "frr": (no_face + wrong + reject) / p,
        "misid_rate": wrong / p,
        "acc_total": right / p,
        "acc_accepted": (right / accepted) if accepted else 0.0,
        "coverage": accepted / p,
        "neg_detected": neg_face,
        "far": (far / total_neg) if total_neg else 0.0,
        "n_right": right,
        "n_wrong": wrong,
        "n_accepted": accepted,
    }


def confusion(pos_records, neg_records, names):
    """行=真实身份（含"未注册"），列=判定结果（含"拒识"）"""
    cols = names + [REJECT_LABEL]
    rows = names + ["未注册"]
    m = {r: {c: 0 for c in cols} for r in rows}
    for rec in pos_records:
        true = rec["true"]
        if rec["outcome"] == "accepted":
            m[true][rec["user_id"]] += 1
        else:
            m[true][REJECT_LABEL] += 1
    for rec in neg_records:
        if rec["outcome"] == "accepted":
            m["未注册"][rec["user_id"]] += 1
        else:
            m["未注册"][REJECT_LABEL] += 1
    return m, rows, cols


def ratio_buckets(pos_records, names):
    """按人脸占比分档——直接回答"人站到多远系统就不认了"

    被门控拒掉的样本也按它真实的人脸占比归入对应档位（占比取自关闭门控的检测），
    这样这张表才能回答"阈值卡在哪一档、卡掉多少人"。
    """
    edges = [(0.0, 0.02, "<2%"), (0.02, 0.05, "2–5%"),
             (0.05, 0.10, "5–10%"), (0.10, 1.01, ">10%")]
    out = []
    for lo, hi, label in edges:
        bucket = [r for r in pos_records if lo <= r.get("ratio", 0) < hi]
        if bucket:
            ok = sum(1 for r in bucket if r["outcome"] == "accepted" and r["user_id"] == r["true"])
            out.append((label, len(bucket), ok / len(bucket), 1 - ok / len(bucket)))
        else:
            out.append((label, 0, 0.0, 0.0))
    undetected = [r for r in pos_records if not r["detected"] and not r.get("gated_out")]
    out.append(("完全未检出", len(undetected), 0.0, 0.0))
    return out


# ---------------------------------------------------------------- 输出

def fmt_pct(x):
    return f"{x * 100:.1f}%"


def render_summary(stats, table, rows, cols, buckets, timing, config_note):
    L = []
    L.append("# 人脸识别测试统计表\n")
    L.append(f"样本：已注册对象 {stats['n_pos']} 张，未注册对象 {stats['n_neg']} 张\n")

    L.append("## 一、总体指标\n")
    L.append("| 指标 | 数值 | 说明 |")
    L.append("|---|---|---|")
    L.append(f"| 漏检率 | {fmt_pct(stats['miss_rate'])} | 完全检不出人脸，占已注册样本 "
             f"({stats['no_face']}/{stats['n_pos']})，其中 {stats['gated_out']} 张是检出后被质量门控拒掉 |")
    L.append(f"| 拒识率 FRR | {fmt_pct(stats['frr'])} | 已注册对象未能正确识别，"
             f"= 漏检 + 门控拒 + 相似度不足（{stats['n_right']} 正确 / {stats['n_pos']} 总数）|")
    L.append(f"| 误识率 | {fmt_pct(stats['misid_rate'])} | 已注册对象被认成别人（{stats['n_wrong']} 张）|")
    L.append(f"| 准确率（全部样本）| {fmt_pct(stats['acc_total'])} | 正确识别 / 全部已注册样本 |")
    L.append(f"| 准确率（接受的样本）| {fmt_pct(stats['acc_accepted'])} | 仅在通过检出且放行的 "
             f"{stats['n_accepted']} 张上统计，覆盖率 {fmt_pct(stats['coverage'])} |")
    L.append(f"| 误识率 FAR | {fmt_pct(stats['far'])} | 未注册对象被放行"
             f"（未注册样本中 {stats['neg_detected']} 张检出人脸）|")

    L.append("\n## 二、混淆矩阵\n")
    L.append("行＝真实身份，列＝系统判定\n")
    L.append("| 真实 \\ 判定 | " + " | ".join(cols) + " |")
    L.append("|---" * (len(cols) + 1) + "|")
    for r in rows:
        L.append(f"| **{r}** | " + " | ".join(str(table[r][c]) for c in cols) + " |")

    L.append("\n## 三、按人脸占比分档（阈值取值依据）\n")
    L.append("| 人脸占画面比例 | 样本数 | 识别正确率 | 拒识率 |")
    L.append("|---|---|---|---|")
    for label, n, acc, rej in buckets:
        if n == 0:
            L.append(f"| {label} | 0 | — | — |")
        else:
            L.append(f"| {label} | {n} | {fmt_pct(acc)} | {fmt_pct(rej)} |")

    L.append("\n## 四、平均时延\n")
    L.append("| 阶段 | 耗时 |")
    L.append("|---|---|")
    for k, v in timing.items():
        L.append(f"| {k} | {v * 1000:.1f} ms |")
    if timing:
        L.append(f"| **合计** | **{sum(timing.values()) * 1000:.1f} ms** |")

    L.append("\n## 五、本次运行环境\n")
    L.append("```")
    L.append(config_note.strip())
    L.append("```")
    L.append("\n> 说明：被质量门控拒掉或检不出人脸的样本一律计入拒识率，"
             "不做静默剔除；「准确率（接受的样本）」单独列出以便与拒识率对照。\n")
    return "\n".join(L)


# ---------------------------------------------------------------- 主流程

def apply_overrides(cfg: dict, items):
    """应用 --override key.path=value 形式的配置覆盖（消融实验用）"""
    for item in items:
        if "=" not in item:
            print(f"忽略无效的 --override：{item}")
            continue
        path, raw = item.split("=", 1)
        try:
            value = json.loads(raw)
        except json.JSONDecodeError:
            value = raw
        node, keys = cfg, path.split(".")
        for k in keys[:-1]:
            node = node.setdefault(k, {})
        node[keys[-1]] = value
    return cfg


def mode_dataset(args):
    root = Path(args.dataset)
    if not root.is_dir():
        print(f"数据集目录不存在：{root}")
        return
    people, negatives = load_dataset(root)
    if not people:
        print(f"未在 {root} 找到任何人员子目录")
        return

    out_dir = Path(args.out) if args.out else (
        Path(__file__).resolve().parent.parent / "data" / "eval" /
        datetime.now().strftime("%Y%m%d-%H%M%S")
    )
    out_dir.mkdir(parents=True, exist_ok=True)

    # 评测必须用独立的人脸库：直接写 data/gallery.json 会把测试身份混进生产库，
    # 而且库里残留的旧身份会参与比对、污染结果
    config = apply_overrides(load_config(), args.override)
    config["gallery"] = dict(config.get("gallery", {}))
    config["gallery"]["path"] = str(out_dir / "gallery.json")
    if args.min_face_ratio is not None:
        config["quality_gate"] = dict(config.get("quality_gate", {}))
        config["quality_gate"]["min_face_ratio"] = args.min_face_ratio
    engine = FaceEngine(config)
    # 只关质量门控、保留其他设置的检测器，用于区分"没检出"和"被门控拒"
    probe_cfg = apply_overrides(load_config(), args.override)
    if args.min_face_ratio is not None:
        probe_cfg["quality_gate"] = dict(probe_cfg.get("quality_gate", {}))
        probe_cfg["quality_gate"]["min_face_ratio"] = args.min_face_ratio
    probe_cfg["quality_gate"] = dict(probe_cfg.get("quality_gate", {}))
    probe_cfg["quality_gate"]["enabled"] = False
    detector_nogate = FaceDetector(
        probe_cfg["model"]["detector"],
        input_size=tuple(probe_cfg["model"].get("detect_input_size", [320, 320])),
        score_threshold=probe_cfg["model"].get("score_threshold", 0.6),
        quality_config=probe_cfg["quality_gate"],
        enhance_config=probe_cfg["detection"].get("enhance", {}),
        normalize_config=probe_cfg.get("preprocess", {}),
    )

    print(f"数据集：{root}")
    print(f"对象 {len(people)} 名，负样本 {len(negatives)} 张")
    print(f"输出目录：{out_dir}\n")

    # ---- 注册 ----
    manifest = None
    if args.enroll_manifest and Path(args.enroll_manifest).exists():
        manifest = json.loads(Path(args.enroll_manifest).read_text(encoding="utf-8"))
        print(f"沿用固定注册集：{args.enroll_manifest}")
    reg_info, chosen = register_people(engine, people, args.enroll_count, manifest)
    for name, n in reg_info.items():
        flag = "" if n >= 3 else "   ← 注册向量偏少，建议补注册照"
        print(f"注册 {name}: {n} 个特征向量{flag}")
    if args.dump_enroll_manifest:
        Path(args.dump_enroll_manifest).parent.mkdir(parents=True, exist_ok=True)
        Path(args.dump_enroll_manifest).write_text(
            json.dumps({k: [str(p) for p in v] for k, v in chosen.items()},
                       ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"已记录本次注册集：{args.dump_enroll_manifest}")
    if not engine.list_users():
        print("无人注册成功，检查数据路径与质量门控参数")
        return

    names = [u["name"] for u in engine.list_users()]
    types = {u["name"]: u["embedding_count"] for u in engine.list_users()}

    # ---- 逐张判定 ----
    timing = {"检测": [], "特征提取": [], "库比对": []}
    pos_records, neg_records = [], []
    total_pos = total_neg = 0

    for name, files in people.items():
        for p in files["test"]:
            img = _read_image(p)
            if img is None:
                continue
            total_pos += 1
            rec = classify(engine, img, detector_nogate, args.threshold, timing)
            rec.update(true=name, file=p.name)
            pos_records.append(rec)

    for p in negatives:
        img = _read_image(p)
        if img is None:
            continue
        total_neg += 1
        rec = classify(engine, img, detector_nogate, args.threshold, timing)
        rec.update(true="未注册", file=p.name)
        neg_records.append(rec)

    stats = summarize(pos_records, neg_records, total_pos, total_neg)
    table, rows, cols = confusion(pos_records, neg_records, names)
    buckets = ratio_buckets(pos_records, names)

    # ---- 阈值扫描 ----
    sims_pos = [(r["true"], r["user_id"], r["sim"]) for r in pos_records if r["detected"]]
    sims_neg = [r["sim"] for r in neg_records if r["detected"]]
    sweep = []
    for t in np.arange(0.20, 0.71, 0.05):
        fp = sum(1 for _, _, s in sims_pos if s >= t)     # 被放行
        fn = sum(1 for _, _, s in sims_pos if s < t)      # 被拒
        right = sum(1 for true, uid, s in sims_pos if s >= t and uid == true)
        far = (sum(1 for s in sims_neg if s >= t) / len(sims_neg)) if sims_neg else 0.0
        sweep.append((float(t), far, (fn + sum(1 for true, uid, s in sims_pos
                                               if s >= t and uid != true)) / max(1, len(sims_pos)),
                      right / max(1, len(sims_pos))))

    # ---- 终端输出 ----
    print(f"\n{'=' * 62}")
    print(f"已注册样本 {stats['n_pos']} 张，未注册样本 {stats['n_neg']} 张"
          f"（判定规则：{'自适应阈值' if args.threshold == 'adaptive' else '全局阈值'}）")
    print(f"{'=' * 62}")
    print(f"漏检率            {fmt_pct(stats['miss_rate'])}  ({stats['no_face']}/{stats['n_pos']})"
          f"  其中被质量门控拒 {stats['gated_out']} 张")
    print(f"拒识率 FRR        {fmt_pct(stats['frr'])}  ({stats['n_pos'] - stats['n_right']}/{stats['n_pos']})")
    print(f"误识率（认成别人）{fmt_pct(stats['misid_rate'])}  ({stats['n_wrong']} 张)")
    print(f"准确率（全样本）  {fmt_pct(stats['acc_total'])}")
    print(f"准确率（接受样本）{fmt_pct(stats['acc_accepted'])}  覆盖率 {fmt_pct(stats['coverage'])}")
    print(f"误识率 FAR        {fmt_pct(stats['far'])}  (未注册 {stats['n_neg']} 张)")

    print(f"\n混淆矩阵（行=真实，列=判定）")
    hdr = "".join(f"{c:>10}" for c in cols)
    print(f"{'':<10}{hdr}")
    for r in rows:
        print(f"{r:<10}" + "".join(f"{table[r][c]:>10}" for c in cols))

    print(f"\n按人脸占比分档")
    print(f"{'占比':<10}{'样本':>6}{'正确率':>9}{'拒识率':>9}")
    for label, n, acc, rej in buckets:
        if n:
            print(f"{label:<10}{n:>6}{fmt_pct(acc):>9}{fmt_pct(rej):>9}")

    print(f"\n阈值扫描（仅在检出人脸的样本上）")
    print(f"{'阈值':>6}{'FAR':>9}{'FRR':>9}{'准确率':>9}")
    for t, far, frr, acc in sweep:
        mark = "  <= 当前" if abs(t - engine.recognizer.reject_threshold) < 1e-6 else ""
        print(f"{t:>6.2f}{fmt_pct(far):>9}{fmt_pct(frr):>9}{fmt_pct(acc):>9}{mark}")

    stage_mean = {k: float(np.mean(v)) for k, v in timing.items() if v}
    if stage_mean:
        print("\n平均时延：" + " | ".join(f"{k} {v * 1000:.1f}ms"
                                          for k, v in stage_mean.items())
              + f" | 合计 {sum(stage_mean.values()) * 1000:.1f}ms")

    # ---- 落盘 ----
    try:
        import onnxruntime

        env_note = (
            f"时间: {datetime.now():%Y-%m-%d %H:%M:%S}\n"
            f"数据集: {root}\n"
            f"Python: {sys.version.split()[0]}\n"
            f"OpenCV: {cv2.__version__}\n"
            f"ONNX Runtime: {onnxruntime.__version__}\n"
            f"numpy: {np.__version__}\n"
            f"检测模型: {config['model']['detector']}\n"
            f"特征模型: {config['model']['embedder']}\n"
            f"判定规则: {args.threshold}\n"
            f"全局拒绝阈值: {engine.recognizer.reject_threshold}\n"
            f"质量门控: {config.get('quality_gate')}\n"
            f"输入归一化: {config.get('preprocess')}\n"
            f"关键点对齐: {config.get('alignment')}\n"
            f"注册向量数: {types}\n"
            f"注册照选取: 按 检测置信度 x sqrt(人脸占比) 排序取前 {args.enroll_count} 张"
        )
    except Exception:
        env_note = f"时间: {datetime.now():%Y-%m-%d %H:%M:%S}\n数据集: {root}"

    (out_dir / "summary.md").write_text(
        render_summary(stats, table, rows, cols, buckets,
                       {k: float(np.mean(v)) for k, v in timing.items() if v},
                       env_note),
        encoding="utf-8")
    (out_dir / "config.json").write_text(
        json.dumps({"argv": sys.argv, "stats": stats,
                    "confusion": {r: table[r] for r in rows},
                    "buckets": buckets,
                    "sweep": sweep, "注册向量数": types},
                   ensure_ascii=False, indent=2), encoding="utf-8")

    with open(out_dir / "per_image.csv", "w", newline="", encoding="utf-8-sig") as fh:
        wr = csv.writer(fh)
        wr.writerow(["真实身份", "文件", "判定", "识别为", "相似度", "阈值",
                     "人脸占比", "模糊度", "亮度", "被门控拒"])
        for rec in pos_records + neg_records:
            wr.writerow([rec["true"], rec["file"], rec["outcome"], rec["user_id"] or "",
                         f"{rec['sim']:.4f}", f"{rec['threshold']:.4f}",
                         f"{rec.get('ratio', 0):.4f}",
                         f"{rec.get('quality', {}).get('blur', 0):.1f}",
                         f"{rec.get('quality', {}).get('brightness', 0):.1f}",
                         "是" if rec.get("gated_out") else ""])

    print(f"\n已写出：{out_dir}")
    print("  summary.md 统计表与混淆矩阵 | per_image.csv 逐张记录 | config.json 参数")


def mode_compare_logs(log_paths):
    def load(path):
        with open(path, newline="", encoding="utf-8") as f:
            return list(csv.DictReader(f))

    for label, path in zip(["A: 无投票（单帧）", "B: 多帧投票"], log_paths):
        rows = load(path)
        ids = [r["user_id"] or r["status"] for r in rows]
        transitions = sum(1 for i in range(1, len(ids)) if ids[i] != ids[i - 1])
        confirmed = sum(1 for r in rows if r["status"] == "confirmed")
        unknown = sum(1 for r in rows if r["status"] == "unknown")
        lost = sum(1 for r in rows if r["status"] == "lost")
        print(f"{label}：帧数={len(rows)} 身份跳变={transitions} "
              f"确认帧={confirmed} 陌生人帧={unknown} 丢失帧={lost}")
    print("身份跳变越少、确认帧越稳定说明鲁棒性越好（对比结果可直接用于报告）")


def main():
    parser = argparse.ArgumentParser(description="人脸识别评估工具")
    parser.add_argument("--dataset", help="数据集目录")
    parser.add_argument("--enroll-count", type=int, default=5, help="每人注册向量数")
    parser.add_argument("--threshold", choices=["global", "adaptive"], default="global",
                        help="判定规则：global=全局阈值，adaptive=每人自适应阈值")
    parser.add_argument("--out", help="输出目录（默认 data/eval/<时间戳>）")
    parser.add_argument("--min-face-ratio", type=float, default=None,
                        help="覆盖质量门控的人脸占比下限（用于阈值标定对比）")
    parser.add_argument("--override", action="append", default=[], metavar="KEY=VALUE",
                        help="覆盖任意配置项，可重复。消融实验用，"
                             "如 --override preprocess.enabled=false")
    parser.add_argument("--enroll-manifest", default=None,
                        help="固定注册集清单（JSON），消融实验用，保证各变体测试集一致")
    parser.add_argument("--dump-enroll-manifest", default=None,
                        help="把本次挑选的注册照写入 JSON，供后续变体复用")
    parser.add_argument("--compare-logs", nargs=2, metavar=("LOG_A", "LOG_B"))
    args = parser.parse_args()
    if args.dataset:
        mode_dataset(args)
    elif args.compare_logs:
        mode_compare_logs(args.compare_logs)
    else:
        parser.print_help()


if __name__ == "__main__":
    main()
