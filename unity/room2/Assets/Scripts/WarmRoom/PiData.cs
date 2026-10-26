using System;

namespace WarmRoom
{
    /// <summary>
    /// 树莓派推送上来的单帧数据。
    /// 对应 JSON：{"current":{"door_state":"open","servo_angle":90,"temperature":27.3,
    ///   "humidity":55,"light":320,"light_raw":88,"timestamp":"2026-10-25 23:10:00"}}
    /// 用 JsonUtility 解析，因此字段名必须与 JSON 完全一致。
    /// </summary>
    [Serializable]
    public class PiCurrent
    {
        public string door_state;      // "open" / "closed"
        public float servo_angle;      // 舵机角度（开门 90、关门 0）
        public float temperature;      // ℃
        public float humidity;         // %RH
        public float light;            // 换算后的光照度 lx
        public int light_raw;          // ADC 原始值 0-255（仅用于核对/记录）
        public string timestamp;       // 采样时间 yyyy-MM-dd HH:mm:ss
    }

    [Serializable]
    public class PiFrame
    {
        public PiCurrent current;
    }
}
