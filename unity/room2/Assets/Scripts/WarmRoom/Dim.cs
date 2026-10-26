using System;
using UnityEngine;

namespace WarmRoom
{
    /// <summary>可交互设备种类。</summary>
    public enum DeviceKind
    {
        MainLight,        // 主吊灯（开关 + 无级调光）
        Curtain,          // 电动窗帘开合
        AirConditioner,   // 落地空调（开关 + 温度设定）
        Door,             // 电动门开关
        Sensor,           // 环境传感器组
        EnvironmentPanel, // 门口环境面板
        AccessControl     // 人脸识别门禁
    }

    /// <summary>设备状态。数值为模拟数据，接口留给后续接后端。</summary>
    [Serializable]
    public class DeviceState
    {
        public string id;
        public string displayName;
        public DeviceKind kind;
        public bool on;
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

    /// <summary>
    /// 会议室尺寸（暖色木质商务风）。
    /// 房间以原点为中心：X 方向 8m（东西），Z 方向 6m（南北），地面 y=0。
    ///
    /// 与"现代简约"那版的关键差异——**布局方向整体对调**：
    ///   那版：门在 -X 短墙、大窗在 +Z 长墙、投影墙在 -Z
    ///   这版：门在 +X 短墙、大窗在 -X 长墙、主景墙（木饰面+主位）在 -Z
    /// </summary>
    public static class Dim
    {
        // ---- 房间外廓 ----
        public const float Length = 8.0f;    // X
        public const float Width = 6.0f;     // Z
        public const float Height = 3.2f;    // 净高（吊顶底面）
        public const float WallT = 0.12f;    // 墙厚

        public const float HX = Length * 0.5f;   // 4.0
        public const float HZ = Width * 0.5f;    // 3.0

        // ---- 墙体（内表面）----
        public const float WallDoor = HX;        // x = +4 : 门墙（短边）
        public const float WallWindow = -HX;     // x = -4 : 窗墙（长边）
        public const float WallFeature = -HZ;    // z = -3 : 主景墙（木饰面 + 主席位）
        public const float WallSideboard = HZ;   // z = +3 : 边柜/陈设墙

        // ---- 吊顶 ----
        public const float CeilT = 0.22f;

        // ---- 入口门（右短墙 x=+4）----
        public const float DoorZMin = -0.55f;
        public const float DoorZMax = 0.55f;     // 门洞净宽 1.10m（比上一版宽一点）
        public const float DoorH = 2.20f;
        public const float DoorClosedYaw = 0f;
        // 门扇铰链在 (WallDoor, z= DoorZMin)，扇体沿本地 +Z 延伸。
        // 绕本地 Y 轴 -92° → 自由端转向 -X，即向房间内侧旋开（可见）。
        public const float DoorOpenYaw = -92f;

        // ---- 采光窗（左长墙 x=-4，两樘窗中间留墙垛）----
        public const float WinZMinA = -2.35f;    // 窗 A：z -2.35 ~ -0.45
        public const float WinZMaxA = -0.45f;
        public const float WinZMinB = 0.45f;     // 窗 B：z  0.45 ~  2.35
        public const float WinZMaxB = 2.35f;
        public const float WinSill = 0.82f;
        public const float WinTop = 2.55f;

        /// <summary>单樘窗的净宽。</summary>
        public const float WinW = WinZMaxA - WinZMinA;   // 1.90

        // ---- 主景墙木饰面（-Z 墙）----
        public const float FeatureW = 3.20f;     // 中央主位区宽度
        public const float FeatureH = 2.90f;

        // ---- 会议桌（船形/长椭圆，木板底座）----
        public const float TableL = 3.30f;       // 沿 X
        public const float TableW = 1.30f;       // 沿 Z
        public const float TableH = 0.76f;
        public const float TableTopT = 0.07f;

        // ---- 坐席 ----
        public const int SeatsPerLongSide = 5;   // 每长边 5 人
        public const int SeatsTotal = 12;        // 含两端各 1 人
        public const float RowZ = 1.46f;         // 两排椅子距桌心的 Z 距离
        public const float HeadX = 2.25f;        // 两端椅子距桌心的 X 距离

        // ---- 相机 ----
        public static readonly Vector3 Pivot0 = new Vector3(0f, 1.15f, 0f);
        public const float CamDist = 9.8f;
        public const float CamYaw = -38f;
        public const float CamPitch = 25f;
    }
}
