"""HTTP 服务壳：与小组重构框架的对接接口（自主设计：接口契约）

队友无论用什么语言/框架重构系统，均可通过 HTTP 与本模块对接：

    GET    /face/health            健康检查
    POST   /face/register          注册（multipart: user_id, name, images 多文件）
    POST   /face/recognize         单张图片识别（JSON: {"image": "<base64>"}）
    DELETE /face/users/{user_id}   删除用户
    GET    /face/users             用户列表

说明：HTTP 识别接口是无状态的（单张图 → 立即输出最佳匹配），
多帧投票用于树莓派本地实时摄像头链路（scripts/demo.py）。
运行：uvicorn face_server:app --host 0.0.0.0 --port 8000
"""

import base64
from typing import List, Optional

import cv2
import numpy as np
import yaml
from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from pydantic import BaseModel

from face_engine.engine import FaceEngine

with open("config.yaml", "r", encoding="utf-8") as f:
    CONFIG = yaml.safe_load(f)

engine = FaceEngine(CONFIG)
app = FastAPI(title="会议室人脸识别服务")


class RecognizeRequest(BaseModel):
    image: str  # base64 编码的图片


def _decode_image(b64: str) -> np.ndarray:
    try:
        data = base64.b64decode(b64)
        img = cv2.imdecode(np.frombuffer(data, np.uint8), cv2.IMREAD_COLOR)
    except Exception:
        raise HTTPException(400, "图片解码失败")
    if img is None:
        raise HTTPException(400, "图片解码失败")
    return img


@app.get("/face/health")
def health():
    return {"status": "ok", "users": len(engine.list_users())}


@app.post("/face/register")
async def register(
    user_id: str = Form(...),
    name: str = Form(...),
    images: List[UploadFile] = File(...),
):
    frames = []
    for f in images:
        data = await f.read()
        img = cv2.imdecode(np.frombuffer(data, np.uint8), cv2.IMREAD_COLOR)
        if img is not None:
            frames.append(img)
    count = engine.register(user_id, name, frames)
    if count == 0:
        raise HTTPException(400, "未能从图片中提取到合格人脸（质量门控未通过）")
    return {"user_id": user_id, "name": name, "registered_embeddings": count}


@app.post("/face/recognize")
def recognize(req: RecognizeRequest):
    img = _decode_image(req.image)
    result = engine.recognize_single(img)
    return {
        "status": result.status.value,
        "user_id": result.user_id,
        "name": result.name,
        "similarity": round(result.similarity, 4),
        "threshold": round(result.threshold, 4),
        "quality": result.quality,
    }


@app.delete("/face/users/{user_id}")
def delete_user(user_id: str):
    if not engine.delete_user(user_id):
        raise HTTPException(404, "用户不存在")
    return {"deleted": user_id}


@app.get("/face/users")
def list_users():
    return {"users": engine.list_users()}
