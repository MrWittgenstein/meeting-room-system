# 智能会议室 · Unity 三维模型（三个版本）

本目录包含**三个独立风格**的会议室三维场景，每个都是一份完整的 Unity 工程，可单独打开运行。

三个版本的**建筑风格不同**，而**交互、面板、数据接口、默认状态完全一致**——便于横向对比与后续统一维护。

| 目录 | 风格 | 场景文件 | 脚本命名空间 |
|---|---|---|---|
| [`room1/`](room1) | 现代简约商务风（深色墙面 + 长条会议桌） | `Assets/Scenes/MeetingRoom.unity` | `SmartRoom` |
| [`room2/`](room2) | 暖色木质商务风（木饰面主景墙 + 南/北双窗） | `Assets/Scenes/WarmMeetingRoom.unity` | `WarmRoom` |
| [`room3/`](room3) | 北欧明亮风（浅色木地板 + 中央圆桌 + 五组角窗） | `Assets/Scenes/NordicMeetingRoom.unity` | `NordicRoom` |

所有几何体、材质、贴图均由 C# 在编辑器内**程序化生成**，不依赖任何外部模型或图片素材（无 FBX、无贴图文件）。

---

## 一、如何打开

1. 安装 **Unity 6000.3.24f1 LTS**（URP 17.3.0）
2. Unity Hub → `Add project from disk` → 选择 `room1` / `room2` / `room3` 其中之一
3. 打开对应场景，点 **Play**

> 每个房间都是独立工程，需要**分别添加**，不能只添加 `unity/` 根目录。
> 首次导入会生成 `Library/`（已被 `.gitignore` 忽略）。

---

## 二、操作说明

| 操作 | 效果 |
|---|---|
| 左键拖拽 | 旋转视角 |
| 右键 / 中键拖拽 | 平移 |
| 滚轮 | 缩放 |
| 左键单击设备 | 开关该设备 |
| 快捷键 `1`~`5` | 依次：主灯 / 空调 / 窗帘 / 门（只读）/ 控制屏 |
| 右侧「视角预设」 | 六个室内机位：全景、看门、看窗、看空调、看桌子、看吊灯 |
| `R` | 恢复默认设备状态 |

**默认状态**：主吊灯**关闭**、悬浮数据气泡**关闭**、相机位于室内「全景」机位。

---

## 三、树莓派实时数据对接

三个房间都内置了 WebSocket 客户端，从树莓派接收真实传感器数据：

```
ws://192.168.43.232:8775/
```

地址可在场景里 `07_会议室控制器`（各房间命名略有不同）的 `PiUrl` 字段修改，或改代码里的默认值：

| 文件 | 字段 |
|---|---|
| `Assets/Scripts/*/RoomController.cs` | `piUrl` |
| `Assets/Scripts/*/PiWebSocketClient.cs` | `url` |

### 报文格式

```json
{
  "current": {
    "temperature": 25.0,
    "humidity": 50,
    "light": 229,
    "light_raw": 194,
    "door_state": "closed",
    "servo_angle": 0,
    "timestamp": "2026-10-26 08:36:26"
  }
}
```

多余字段会被自动忽略。推送间隔约 5~7 秒。

### 映射关系

| 数据 | 三维场景中的表现 |
|---|---|
| `temperature` | 左面板温度读数 + **落地空调数显显示实测温度**（七段数码管贴图） |
| `humidity` | 左面板湿度读数 |
| `light` | **场景整体亮度**：`150 lx → 最暗`，`600 lx → 最亮`（环境光 + 日光 + 补光同步变化，带平滑） |
| `door_state` | 门扇开合动画；面板上「电动门」标记为 `硬件`，**只读**，点击只提示不动作 |
| 门 `closed → open` 边沿 | **在室人数 +1**（初始 0，上限 12，重复收到 `open` 不累加） |

其余指标（PM2.5 / CO₂ / 烟雾）本轮未接真实数据，仍为本地模拟。

数据源参数（`RoomController` 上可调）：

| 参数 | 默认 | 说明 |
|---|---|---|
| `useLiveData` | `true` | 关掉则完全使用本地模拟 |
| `liveTimeoutSeconds` | `12` | 超过该秒数无数据判定离线（通信间隔约 6 秒，留了余量） |
| `luxSceneMin` / `luxSceneMax` | `150` / `600` | 光照到亮度的映射区间 |
| `maxOccupancy` | `12` | 人数上限 |

离线行为：**数值冻结在最后一次上报值**，HUD 显示「离线 · 自动重连中…」，客户端每 2 秒自动重连。

---

## 四、目录结构

```
unity/
├── room1/                     # 现代简约
│   ├── Assets/
│   │   ├── Scenes/MeetingRoom.unity
│   │   ├── Scripts/MeetingRoom/     # 场景生成 + 控制器 + HUD + 树莓派客户端
│   │   ├── Screenshots/             # 4 张主图
│   │   └── Settings/                # URP 配置
│   ├── Packages/
│   ├── ProjectSettings/
│   └── .gitignore
├── room2/                     # 暖色木质（结构同上，Scripts/WarmRoom）
└── room3/                     # 北欧明亮（结构同上，Scripts/NordicRoom）
```

每个房间的 `Assets/Scripts/<风格>/` 内含：

| 文件 | 作用 |
|---|---|
| `RoomController.cs` | 总控：设备状态、交互点选、传感器数据、灯光联动 |
| `RoomHud.cs` | IMGUI 面板：环境数据（左上）、设备控制（右上）、视角预设 |
| `OrbitCamera.cs` | 轨道相机：旋转/平移/缩放、室内机位预设 |
| `PiWebSocketClient.cs` | 树莓派 WebSocket 客户端（断线重连、主线程派发） |
| `PiData.cs` | 报文 DTO |
| `AcDigitDisplay.cs` | 空调数显的七段数码管纹理绘制 |
| 其余 | 房间壳体、家具、设备、材质、基础几何体等程序化生成代码 |

---

## 五、截图

每个房间保留 4 张主图（`Assets/Screenshots/`）：全景、布局俯视、环境数据叠加、平面布局图。

> 场景文件较大（16~32 MB）是因为程序化生成的网格被序列化进场景，属于正常现象。

---

## 六、说明

- 三个版本共用同一套交互与数据逻辑，只有建筑风格（墙体、门窗、家具、配色）不同
- 门由树莓派舵机控制，面板上的门开关为只读，避免与硬件状态冲突
- 场景中未使用任何第三方插件；WebSocket 使用 .NET 自带的 `ClientWebSocket`（不支持 WebGL 平台构建）
