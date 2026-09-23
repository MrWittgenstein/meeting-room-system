# 智能会议室 · Unity 三维模型

本项目是「智能会议室管理系统」的三维表现层，使用 **Unity 6000.3.24f1 LTS + URP 17.3.0** 构建。

会议室的**全部几何体、材质、贴图都由 C# 代码程序化生成**，不依赖任何外部模型资源（无 FBX、无图片素材）。

---

## 一、如何打开

1. **Unity Hub → Add → 选择本目录（`unity/`）**
   > 注意：要选 `unity/` 这一层（它里面有 `Assets`、`ProjectSettings`、`Packages`、`Packages/manifest.json`），不是仓库根目录。
2. 用 **Unity 6000.3.24f1** 打开（编辑器版本必须一致，否则会提示升级）
3. 首次打开会自动导入 URP、Input System 等包，**需要几分钟**，右下角进度条结束后再进行下一步
4. 打开场景 **`Assets/Scenes/MeetingRoom.unity`**
5. 点 ▶ 进入 Play 模式

> 首次打开若提示 "The referenced script is missing"，等待编译完成后再点 ▶ 即可，属于正常现象。

## 二、场景内容

**房间**：8m × 6m，净高 3.2m，现代简约商务风，可容纳 12 人。

| 组成 | 说明 |
|---|---|
| 房间壳体 | 地板（地毯）、四面墙、入口门洞、大窗、完整吊顶（灯带 / 筒灯 / 空调风口） |
| 家具 | 3.2×1.25m 会议桌、**12 把人体工学办公椅**、边柜、白板、饮水台、绿植、衣帽架、垃圾桶 |
| 智能设备 | 主吊灯（可调光）、电动窗帘、**落地式空调**、**电动门**、环境传感器 ×4、门口环境面板、人脸识别门禁 |
| 灯光 | 日光 + 天光补光 + 吊灯主光源（软阴影）+ 落地灯 |
| 后处理 | ACES 色调映射 + Bloom + 暗角 + 白平衡 |

## 三、操作方式

### 鼠标

| 操作 | 作用 |
|---|---|
| 左键拖拽 | 旋转视角 |
| 右键拖拽 | 平移视角 |
| 滚轮 | 缩放（往前推即进入房间） |
| 左键单击设备 | 开关该设备 |

### 键盘

| 按键 | 作用 |
|---|---|
| `1` / `5` | 电动门开 / 关 |
| `2` | 主吊灯开关（面板内可无级调光） |
| `3` | 空调开关与温度设定 |
| `4` | 电动窗帘全开 ↔ 全关 |
| `F` | 温度空间化叠加（地面热力） |
| `G` | 光照空间化叠加 |
| `H` | 烟雾空间化叠加 |
| `B` | 传感器悬浮数据气泡开关 |
| `R` | 全部恢复默认状态 |

> 数字键用主键盘区那一排。

### 界面

- **左上**：环境实时数据（温度 / 湿度 / 光照度 / PM2.5 / CO₂ / 烟雾 / 在室人数）
- **右上**：设备控制面板 + 6 个视角预设（全景 / 看门 / 看空调 / 看投影墙 / 看桌子 / 看传感器）
- **左下**：操作提示

## 四、代码结构

```
Assets/
├── Editor/MeetingRoom/
│   └── MeetingRoomWizard.cs      # 编辑器菜单：一键生成场景、出效果图
├── Scenes/
│   └── MeetingRoom.unity          # 主场景
├── Screenshots/                   # 生成的 12 张效果图
├── Settings/                      # URP 渲染管线与后处理配置
└── Scripts/MeetingRoom/
    ├── RoomData.cs                # 房间尺寸、设备类型等常量
    ├── TextureLib.cs              # 程序化贴图生成（地毯/木纹/织物/看板画面等 13 种）
    ├── MatLib.cs                  # URP PBR 材质库
    ├── Prim.cs                    # 建模图元工具（盒体/圆柱/圆角盒/圆盘）
    ├── RoomShell.cs               # 房间壳体（墙/地/吊顶/门窗/空调柜机）
    ├── Furniture.cs               # 家具（会议桌/12 把椅子/边柜/白板/绿植…）
    ├── Devices.cs                 # 智能设备（吊灯/窗帘/传感器/门禁/环境面板）
    ├── MeetingRoomBuilder.cs      # 场景组装（灯光/相机/后处理/控制器）
    ├── DeviceController.cs        # 单个设备的状态与悬停描边
    ├── RoomController.cs          # 总控制器：点控、联动、模拟数据
    ├── RoomHud.cs                 # 运行时界面（IMGUI）
    └── OrbitCamera.cs             # 轨道相机
```

## 五、重新生成场景

场景可以在编辑器内一键重建（不需要手动搭）：

> 菜单 **Tools → Smart Meeting Room → 1. Generate Meeting Room Scene**

会重新生成 `Assets/Scenes/MeetingRoom.unity` 并覆盖原文件。

`Assets/sss.unity` 是同一工程的另一份场景副本，仅为备份用途。

## 六、已知事项

- **光照未烘焙**：当前使用实时灯光。如需间接光与环境光遮蔽，在 Unity 内执行
  `Window → Rendering → Lighting → Generate Lighting`。
- **静态合批**：不可动几何已标记 `BatchingStatic`；门扇、窗帘等可动部件**刻意排除**，
  否则静态合批会把它们的顶点烘进世界空间网格，导致 Transform 动画失效。
- **WebGL**：代码层面无文件/线程/环境变量依赖，可移植到 WebGL；但构建前需要先在
  Unity Hub 中安装 **WebGL Build Support** 模块，且帧率与包体需要实测优化。

## 七、与其它模块的关系

```
浏览器前端 (frontend/)  ←→  Spring Boot 后端 (backend/)  ←→  树莓派 + 传感器 (raspberry-pi/)
                                    ↑
                          本 Unity 三维表现层（unity/）
```

业务与数据层保持不变，本模块只负责三维表现。设备状态目前由 `RoomController` 内的
模拟数据驱动，后续接入实时数据只需替换 `RoomController` 中的数据来源。
