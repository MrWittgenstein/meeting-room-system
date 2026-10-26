using System;
using UnityEngine;

namespace NordicRoom
{
    /// <summary>可交互设备种类。</summary>
    public enum DeviceKind
    {
        MainLight,
        Curtain,
        AirConditioner,
        Door,
        Sensor,
        EnvironmentPanel,
        AccessControl
    }

    /// <summary>设备状态（模拟数据，接口留给后续接后端）。</summary>
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
            this.id = id; displayName = name; this.kind = kind; this.on = on; this.value = value;
        }
    }

    /// <summary>
    /// 北欧明亮风会议室尺寸。房间以原点为中心：X 长 8m、Z 宽 6m、净高 3.2m。
    ///
    /// 与前两个会议室的布局差异——**角窗方案**：
    ///   门   → -X 短墙（与第 1 个相同的一侧，但窗完全不同）
    ///   窗   → **四个转角窗**：两堵长墙各在南北两端开窗，中间留大片墙垛；
    ///           +X 短墙（门对面）再开一扇中央窗。共 5 扇窗。
    ///   主位 → +X 短墙侧（门对面），圆桌正对入口
    /// 这样与"整条窗带"（第 1 个）、"两樘窗"（第 2 个）都明显不同。
    /// </summary>
    public static class Dim
    {
        // ---- 房间 ----
        public const float Length = 8.0f;
        public const float Width = 6.0f;
        public const float Height = 3.2f;
        public const float WallT = 0.12f;
        public const float HX = Length * 0.5f;   // 4.0
        public const float HZ = Width * 0.5f;    // 3.0

        // ---- 墙体（内表面）----
        public const float WallDoor = -HX;       // x = -4 : 门墙
        public const float WallBack = HX;        // x = +4 : 门对面（含中央窗 + 主位）
        public const float WallNorth = HZ;       // z = +3 : 含两端角窗
        public const float WallSouth = -HZ;      // z = -3 : 含两端角窗

        public const float CeilT = 0.22f;

        // ---- 门（-X 短墙）----
        public const float DoorZMin = -0.60f;
        public const float DoorZMax = 0.60f;     // 净宽 1.20m
        public const float DoorH = 2.25f;
        public const float DoorClosedYaw = 0f;
        // 铰链在 (WallDoor, DoorZMin)，扇体沿本地 +Z 铺开。
        // 绕本地 Y 轴 +92° → 自由端转向 +X，即向房间内侧旋开。
        public const float DoorOpenYaw = 92f;

        // ---- 转角窗（两长墙，南北各一，共 4 扇）----
        // 窗在墙面上的"长度方向"坐标（长墙沿 Z，短墙沿 X）
        public const float CornerWinLen = 2.00f;   // 每扇角窗长
        public const float CornerEdgeGap = 0.45f;  // 距墙角的距离
        public const float WinSill = 0.45f;        // 北欧风窗台低、窗更大
        public const float WinTop = 2.62f;
        public const float WinFrameW = 0.06f;

        // 北墙两扇角窗在 Z 轴上的范围（墙面 z=+3，窗沿 X 方向排布）
        public const float NorthWinXMin = -HX + CornerEdgeGap;                 // -3.55
        public const float NorthWinXMax = -HX + CornerEdgeGap + CornerWinLen;  // -1.55
        public const float NorthWinXMin2 = HX - CornerEdgeGap - CornerWinLen;  //  1.55
        public const float NorthWinXMax2 = HX - CornerEdgeGap;                 //  3.55

        // ---- 后墙中央窗（+X 短墙，墙面 x=+4，窗沿 Z 方向排布）----
        public const float BackWinZMin = -1.40f;
        public const float BackWinZMax = 1.40f;
        public const float BackWinSill = 0.45f;
        public const float BackWinTop = 2.62f;

        // ---- 中央圆桌 ----
        public const float TableDia = 2.90f;    // 圆桌直径
        public const float TableTopT = 0.06f;
        public const float TableH = 0.75f;

        // ---- 坐席（绕圆桌均布）----
        public const int SeatsTotal = 12;
        public const float SeatRadius = 1.98f;  // 椅子中心到桌心的距离

        // ---- 相机 ----
        // 与"暖木会议室"同一套写法（pivot + distance + yaw + pitch），数值换算到北欧风房间：
        // 算出来相机世界坐标 ≈ (-3.10, 1.72, -2.08)，落在房间西南侧**内部**，
        // 视线朝东北，一眼能看到中央圆桌 + 北墙长条柜 + 两侧角窗。
        public static readonly Vector3 Pivot0 = new Vector3(0f, 1.15f, 0.30f);
        public const float CamDist = 3.94f;
        public const float CamYaw = -75.2f;
        public const float CamPitch = 6.5f;
    }
}
