using System;
using UnityEngine;

namespace SmartRoom
{
    /// <summary>可交互设备的种类。</summary>
    public enum DeviceKind
    {
        MainLight,      // 主吊灯（开关 + 无级调光）
        Projector,      // 投影仪 + 投影幕布升降 + 墙面画面内容
        AirConditioner, // 空调（开关 + 温度设定）
        Curtain,        // 电动窗帘开合
        EnvironmentPanel, // 门口环境面板（温湿度/PM2.5）
        AccessControl,  // 门禁 / 人脸识别
        Sensor,         // 环境传感器节点
        Door            // 电动门（开关门）
    }

    /// <summary>一个可交互设备的运行状态。所有数值都是模拟数据，接口留给后续接 Spring Boot。</summary>
    [Serializable]
    public class DeviceState
    {
        public string id;
        public string displayName;
        public DeviceKind kind;

        [Tooltip("开关状态")]
        public bool on;

        [Tooltip("主灯调光 0..1 / 窗帘开合 0..1 / 设定温度 ℃ / 幕布升降 0..1")]
        public float value = 0.5f;

        public DeviceState() { }

        public DeviceState(string id, string name, DeviceKind kind, bool on, float value)
        {
            this.id = id;
            displayName = name;
            this.kind = kind;
            this.on = on;
            this.value = value;
        }
    }

    /// <summary>会议室的关键尺寸。全部单位：米。房间以原点为中心，X 方向 8m，Z 方向 6m，地面 y=0。</summary>
    public static class RoomDim
    {
        // ---- 房间外廓 ----
        public const float Length = 8.0f;   // X 方向（长边）
        public const float Width = 6.0f;    // Z 方向（短边）
        public const float Height = 3.2f;   // 净高（吊顶底面）
        public const float WallThickness = 0.12f;

        public const float HalfX = Length * 0.5f;   // 4.0
        public const float HalfZ = Width * 0.5f;    // 3.0

        // ---- 墙体位置（墙体内表面）----
        public const float WallLeft = -HalfX;    // x = -4 : 入口短墙（门在这面）
        public const float WallRight = HalfX;    // x = +4 : 短墙
        public const float WallScreen = -HalfZ;  // z = -3 : 投影幕布墙
        public const float WallWindow = HalfZ;   // z = +3 : 采光窗 + 边柜墙

        // ---- 吊顶 ----
        public const float CeilingThickness = 0.22f;

        // ---- 入口门（左短墙 x=-4 处）----
        public const float DoorZMin = -2.35f;
        public const float DoorZMax = -1.35f;   // 门洞净宽 1.0m
        public const float DoorHeight = 2.15f;
        public const float DoorClosedYaw = 0f;      // 关门：门扇贴合门洞
        // 开门方向：门扇铰链在 x=-4.06、扇体沿 +Z 延伸。
        // 必须是 -88°（扇体转向 +X，即向房间内侧/走道旋开）；
        // 若写成 +88°，扇体转向 -X 就转进墙体里了，肉眼完全看不出动。
        public const float DoorOpenYaw = -88f;

        // ---- 采光窗（窗墙 z=+3）----
        public const float WindowXMin = -2.60f;
        public const float WindowXMax = 2.60f;  // 采光面宽 5.2m
        public const float WindowSill = 0.85f;
        public const float WindowTop = 2.60f;

        // ---- 投影幕布（幕墙 z=-3）----
        public const float ScreenWidth = 3.0f;
        public const float ScreenHeight = 1.8f;
        public const float ScreenTopY = 2.85f;      // 完全展开时幕布上沿
        public const float ScreenOpenY = ScreenTopY - ScreenHeight; // 1.05
        public const float ScreenSurfaceZ = WallScreen + 0.14f;

        // ---- 会议桌 ----
        public const float TableLength = 3.2f;      // 沿 X
        public const float TableDepth = 1.25f;      // 沿 Z
        public const float TableHeight = 0.75f;
        public const float TableTopThickness = 0.06f;

        // ---- 坐席 ----
        public const int SeatsPerLongSide = 5;      // 每侧 5 人
        public const int SeatsTotal = 12;           // 含两端各 1 人
        public const float SeatSpacing = 0.66f;     // 长边座椅间距

        // ---- 相机初始 ----
        public static readonly Vector3 OrbitPivot = new Vector3(0f, 1.15f, 0f);
        public const float OrbitDistance = 12.5f;
        public const float OrbitYaw = 38f;
        public const float OrbitPitch = 26f;
    }
}
