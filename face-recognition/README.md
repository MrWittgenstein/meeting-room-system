# 树莓派智能会议室管理系统 — 人脸识别模块

进阶式挑战性综合项目 III 的人脸识别部分（半自主开发）。
**方案**：YuNet 人脸检测 + MobileFaceNet 特征提取（ONNX 推理），树莓派 4B 本地端到端。
**场景**：会议签到 / 参会人身份识别。

## 原理概述（报告"基本原理"章节素材）

人脸识别流水线：**采集 → 检测 → 对齐 → 特征提取 → 比对 → 决策**

1. **人脸检测（YuNet）**：轻量 CNN 网络，通过特征金字塔在不同尺度上回归人脸框，
   输出人脸框 + 5 点关键点（双眼、鼻尖、嘴角）+ 置信度；非极大值抑制（NMS）合并重叠框。
2. **对齐**：利用 5 点关键点估算相似变换（旋转/缩放/平移），把人脸裁剪到 112×112 标准模板，
   消除姿态差异对特征的影响。
3. **特征提取（MobileFaceNet + ArcFace）**：轻量骨干网络把人脸映射为 512 维向量。
   ArcFace 损失在训练时引入角度间隔（margin），迫使同类向量在超球面上聚得更紧、
   异类分得更开，因此**余弦相似度**可以直接度量"两张脸是不是同一个人"。
4. **比对与决策**：当前人脸向量与库中所有向量比对，结合自适应阈值与多帧投票输出身份。

## 流水线与模块划分

```
采集(CameraSource) → 检测(FaceDetector: YuNet) → 质量门控(模糊/亮度/占比)
     → 对齐(5点→112x112) → 特征(FaceEmbedder: MobileFaceNet→512维)
     → 比对(FaceGallery: 余弦相似度+自适应阈值) → 决策(VotingRecognizer: 多帧投票)
     → FaceResult{status, user_id, name, similarity, ...}
```

### 开源库的作用、调用流程与本组自主设计（报告核心素材）

| 模块 | 开源库的作用 | 调用流程 | 本组自主设计/改进 |
|---|---|---|---|
| `camera.py` | OpenCV / Picamera2 抓帧 | `read()` → BGR 帧 | 双后端统一抽象、分辨率控制 |
| `detector.py` | cv2.FaceDetectorYN 加载 YuNet 推理 | 帧 → 人脸框+5点关键点+置信度 | 参数选型、边缘/尺寸过滤、**质量门控**、**输入归一化**、**关键点对齐的稳健估计**、检测增强回退链 |
| `embedder.py` | ONNX Runtime 执行 MobileFaceNet | 112×112 → 512 维向量 | 预处理归一化、L2 归一化、批量推理 |
| `gallery.py` | NumPy 矩阵运算 | 向量 → 全库余弦相似度 | 多向量库结构、**向量化比对**、**自适应阈值**、持久化 |
| `recognizer.py` | 无（纯自研） | 帧级匹配 → 身份 | **多帧投票**、陌生人判定、窗口重置 |
| `engine.py` | 组装上述模块 | — | 流水线设计、**帧间复用跳帧**、单帧/流式双模式 |

## 安装

```bash
# Windows（开发）
python -m venv .venv
.venv\Scripts\python -m pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

# 树莓派 4B（部署，Raspberry Pi OS 64-bit Bookworm）
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple
.venv/bin/pip install picamera2 -i https://pypi.tuna.tsinghua.edu.cn/simple   # 仅树莓派
```

树莓派注意事项：
- 推荐 64 位 Raspberry Pi OS（Bookworm）；摄像头使用 CSI Camera Module V2/V3 或 USB 摄像头
- **务必加散热片/风扇**：持续推理负载下 4B 会降频
- CSI 摄像头需 `sudo raspi-config` 中开启摄像头接口

## 模型下载

见 [models/README.md](models/README.md)：YuNet（OpenCV Zoo）+ MobileFaceNet（insightface buffalo_l 中的 w600k_mbf.onnx），
下载后放入 `models/` 目录。

## 快速开始

```bash
# 0. 下载模型并自检
.venv\Scripts\python scripts/download_models.py
.venv\Scripts\python scripts/smoke_test.py --with-models

# 1. 注册人脸（多角度采集 5 张）
.venv\Scripts\python scripts/enroll.py --id s2023001 --name 张三

# 2. 实时识别演示
.venv\Scripts\python scripts/demo.py

# 3. 启动 HTTP 服务（供小组框架对接）
.venv\Scripts\python -m uvicorn face_server:app --host 0.0.0.0 --port 8000
```

其他演示能力：

```bash
.venv\Scripts\python scripts/demo.py --headless          # 无窗口模式（树莓派无显示器）
.venv\Scripts\python scripts/demo.py --save-video d.mp4  # 导出带框结果视频（答辩预演）
.venv\Scripts\python scripts/servo_door.py --mock-servo  # 舵机门禁（电脑模拟调试）
```

树莓派部署见 [RPI_DEPLOY.md](RPI_DEPLOY.md)。

## 测试与评估（GR5.3 素材）

数据集目录结构（两种写法都支持）：

```
样本集/
  <对象>/             该对象的测试照片
  <对象>——注册集/     人工挑选的注册照（可缺省，缺省时按质量自动挑）
  负样本/             未注册人员照片（__negative__ 亦可）
```

**完整评测**：

```bash
.venv\Scripts\python scripts/eval.py --dataset "路径/样本集"
# 输出目录默认 data/eval/<时间戳>/，含：
#   summary.md      统计表与混淆矩阵（报告直接可用）
#   per_image.csv   逐张判定记录（运行记录）
#   config.json     本次运行的全部参数
```

指标口径：**被质量门控拒掉或检不出人脸的样本一律计入拒识率，不做静默剔除**；
同时给出"准确率（全样本）"与"准确率（接受的样本）"两个数，避免用多拒识来虚高准确率。

**消融实验**（量化每个改进点的贡献，各变体沿用同一套注册集与测试集）：

```bash
.venv\Scripts\python scripts/ablation.py --dataset "路径/样本集"
# 输出 data/eval/ablation/summary.md
```

单点消融可直接覆盖任意配置项：

```bash
.venv\Scripts\python scripts/eval.py --dataset "路径/样本集" --override preprocess.enabled=false
.venv\Scripts\python scripts/eval.py --dataset "路径/样本集" --min-face-ratio 0.05
```

**多帧投票消融**：录一段含转头/遮挡/多人经过的视频，分别以两种模式跑并对比：

```bash
.venv\Scripts\python scripts/demo.py --source clip.mp4 --no-voting --log raw.csv
.venv\Scripts\python scripts/demo.py --source clip.mp4 --log voting.csv
.venv\Scripts\python scripts/eval.py --compare-logs raw.csv voting.csv
# 输出：身份跳变次数、确认帧数、陌生人帧数对比
```

## 改进点与实验开关（报告"本组改进内容"）

| 改进点 | 开关 | 对比实验方法 | 实测效果 |
|---|---|---|---|
| **输入归一化** | `preprocess.enabled`（默认开） | `--override preprocess.enabled=false` | 覆盖率 89.5% → 47.4%，且出现 5.3% 误识 |
| **关键点对齐稳健估计** | `alignment.method`（`lmeds`/`ransac`） | `--override alignment.method=ransac` | 覆盖率 89.5% → 63.2% |
| 质量评估门控 | `quality_gate.enabled`（默认开） | `--override quality_gate.enabled=false` | 关掉后覆盖率升到 94.7%，但出现 2.6% 误识 |
| 人脸占比阈值 | `quality_gate.min_face_ratio`（0.02） | `--min-face-ratio 0.05` | 收紧后覆盖率 89.5% → 76.3% |
| 多帧投票 | `voting.enabled` 或 `--no-voting` | 视频日志对比（见上） | 待补（需录视频） |
| 暗光/侧脸检测增强 | `detection.enhance.enabled` | 暗光数据集上有/无回退链对比 | 本数据集未触发，待补弱光样本 |
| 自适应阈值 | `gallery.adaptive_threshold.enabled` | `--threshold adaptive` | 待补（`_decide_single` 尚未走该阈值） |
| 多角度注册+多向量比对 | `gallery.merge_strategy` | 对比"单张注册 vs 多角度注册" | — |
| 帧间复用跳帧 | `pipeline.embed_every` | 对比不同跳帧间隔下的 FPS 与准确率 | — |

详细说明与完整数据见 `docs/`：

| 文档 | 内容 |
|---|---|
| `docs/01-第5周周报.md` | 本周目标、交付物清单、卡点、下周计划 |
| `docs/02-流程图与流程说明.md` | 七步流程的输入/输出/判定依据 |
| `docs/03-数据说明与授权脱敏记录.md` | 数据集说明、授权书模板、脱敏记录 |
| `docs/04-技术实现文档.md` | 分层架构、模块职责、数据流、数据结构、配置、接口契约、扩展点 |
| `docs/05-算法说明与改进理由.md` | 自研环节的原理、参数与消融证据 |
| `docs/06-库调用说明.md` | 库清单、版本、关键调用与输入输出 |
| `docs/07-测试报告.md` | 统计表、混淆矩阵、消融实验、局限 |
| `docs/word/` | 上述文档的 Word 版本（`scripts/md_to_docx.py` 生成） |

## 自动日志与门禁

- **事件日志**：demo 运行中自动记录进场/离场/身份切换/陌生人出现到 `data/recognition_log.csv`（签到流水，可直接对接报表模块）
- **陌生人截图**：识别到未注册人员时自动存图到 `data/unknown_faces/`（带 5 秒冷却去重），记录见 `data/unknown_log.csv`
- **出现统计**：退出 demo 时打印每人确认帧数与出现时长
- **舵机门禁**：`scripts/servo_door.py`，多帧投票确认 + 授权名单通过才开门，30 秒自动关门，支持 `--mock-servo` 电脑调试

## HTTP 接口（与小组框架对接）

```
POST   /face/register            multipart: user_id, name, images[]
POST   /face/recognize           JSON: {"image": "<base64>"}
DELETE /face/users/{user_id}
GET    /face/users
GET    /face/health
```

识别结果 JSON：`{status, user_id, name, similarity, threshold, quality}`
其中 `status ∈ {confirmed, pending, unknown, lost}`。签到业务方以 `confirmed` 为准写入签到记录。

## 项目结构

```
smart-meeting-face/
├── config.yaml              # 全部可调参数（改进点开关、阈值、模型路径）
├── face_server.py           # FastAPI 服务壳（对接小组框架）
├── face_engine/             # 核心引擎（可被直接 import）
│   ├── engine.py            # FaceEngine 组装
│   ├── detector.py          # YuNet 检测 + 质量门控 + 对齐
│   ├── embedder.py          # MobileFaceNet 特征提取
│   ├── gallery.py           # 人脸库 + 自适应阈值 + 向量化比对
│   ├── recognizer.py        # 多帧投票决策
│   ├── camera.py            # 摄像头抽象（Windows/Picamera2）
│   └── config.py            # 配置加载
├── models/                  # ONNX 模型（download_models.py 自动下载）
├── scripts/
│   ├── download_models.py   # 模型自动下载（重试+镜像+解压）
│   ├── enroll.py            # 多角度注册
│   ├── demo.py              # 实时识别演示（事件日志/陌生人截图/录屏/headless）
│   ├── servo_door.py        # 人脸识别舵机门禁（投票确认+授权名单）
│   ├── eval.py              # 数据集评估 + 阈值扫描 + 日志对比
│   └── smoke_test.py        # 冒烟自检
├── RPI_DEPLOY.md            # 树莓派部署手册
└── data/
    ├── gallery.json         # 人脸库（注册后生成）
    ├── recognition_log.csv  # 事件日志（签到流水）
    ├── unknown_log.csv      # 陌生人截图记录
    ├── unknown_faces/       # 陌生人截图
    └── dataset/             # 自建测试数据集
```
