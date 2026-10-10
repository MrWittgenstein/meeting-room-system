# 模型文件

将以下两个模型下载到本目录（或修改 `config.yaml` 中的路径）。

## 1. 人脸检测：YuNet（约 230 KB）

- 来源：OpenCV Zoo（OpenCV 官方模型库）
- 协议：Apache-2.0
- 下载：
  https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx
- 文件名：`face_detection_yunet_2023mar.onnx`

## 2. 人脸特征：MobileFaceNet（w600k_mbf，约 4.5 MB）

- 来源：insightface 官方模型包 buffalo_l（deepinsight/insightface）
- 协议：MIT
- 下载：在 https://github.com/deepinsight/insightface/releases 页面下载
  `buffalo_l.zip`，解压后取其中的 `w600k_mbf.onnx` 放入本目录
- 文件名：`w600k_mbf.onnx`

## 说明

- `w600k_mbf` 是 MobileFaceNet 骨干 + ArcFace 损失在 WebFace600K 数据集上训练的
  人脸识别模型，输入 112×112，归一化到 [-1,1]（与 config.yaml 默认值一致）。
- 如使用其他转换版 MobileFaceNet，注意核对输入尺寸与归一化约定，
  必要时调整 `config.yaml` 中的 `embed_input_norm`。
- 报告中需注明模型来源、版本与开源协议。
