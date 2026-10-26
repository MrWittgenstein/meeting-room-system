using UnityEngine;
using UnityEngine.InputSystem;

namespace WarmRoom
{
    /// <summary>运行时界面（IMGUI）：设备控制面板 + 环境数据面板 + 悬浮气泡 + 视角预设。</summary>
    [DisallowMultipleComponent]
    public class RoomHud : MonoBehaviour
    {
        public RoomController room;

        Rect _devicePanel = new Rect(0, 0, 300, 520);
        Rect _sensorPanel = new Rect(0, 0, 268, 402);
        Rect _helpBar = new Rect(0, 0, 452, 110);

        Texture2D _bg, _bgSoft, _accent, _switchOn, _switchOff, _sliderBg, _sliderFill, _white;
        Texture2D _hairline, _barTemp, _barHum, _barLux, _barPm, _barCo2, _barSmoke, _barAlarm;
        Texture2D _barCool, _barMild, _barHot, _sensorAccent;

        GUIStyle _title, _sub, _label, _value, _valueSmall, _small, _button, _bubble, _bubbleTitle, _toast;
        bool _stylesReady;
        float _fps, _uiScale = 1f;

        void Awake()
        {
            _white = Solid(Color.white);
            _bg = Solid(new Color(0.070f, 0.055f, 0.040f, 0.94f));
            _bgSoft = Solid(new Color(0.105f, 0.085f, 0.062f, 0.95f));
            _accent = Solid(new Color(0.95f, 0.72f, 0.32f, 1f));
            _switchOn = Solid(new Color(0.86f, 0.60f, 0.22f, 1f));
            _switchOff = Solid(new Color(0.30f, 0.285f, 0.265f, 1f));
            _sliderBg = Solid(new Color(0.20f, 0.180f, 0.160f, 1f));
            _sliderFill = Solid(new Color(0.95f, 0.72f, 0.32f, 1f));
            _hairline = Solid(new Color(0.95f, 0.80f, 0.55f, 0.18f));
            _sensorAccent = Solid(new Color(0.55f, 0.85f, 0.50f, 1f));
            _barTemp = Solid(new Color(0.95f, 0.68f, 0.30f, 1f));
            _barHum = Solid(new Color(0.45f, 0.72f, 0.95f, 1f));
            _barLux = Solid(new Color(0.98f, 0.85f, 0.35f, 1f));
            _barPm = Solid(new Color(0.72f, 0.60f, 0.95f, 1f));
            _barCo2 = Solid(new Color(0.50f, 0.85f, 0.68f, 1f));
            _barSmoke = Solid(new Color(0.60f, 0.76f, 0.95f, 1f));
            _barAlarm = Solid(new Color(1f, 0.30f, 0.24f, 1f));
            _barCool = Solid(new Color(0.35f, 0.65f, 0.98f, 1f));
            _barMild = Solid(new Color(0.60f, 0.88f, 0.42f, 1f));
            _barHot = Solid(new Color(1f, 0.44f, 0.30f, 1f));
        }

        static Texture2D Solid(Color c)
        {
            var t = new Texture2D(1, 1, TextureFormat.RGBA32, false);
            t.SetPixel(0, 0, c);
            t.Apply();
            t.hideFlags = HideFlags.HideAndDontSave;
            return t;
        }

        void BuildStyles()
        {
            if (_stylesReady) return;
            _stylesReady = true;
            _title = new GUIStyle(GUI.skin.label)
            { fontSize = 18, fontStyle = FontStyle.Bold, normal = { textColor = new Color(1f, 0.90f, 0.74f) } };
            _sub = new GUIStyle(GUI.skin.label)
            { fontSize = 13, normal = { textColor = new Color(0.72f, 0.64f, 0.54f) } };
            _label = new GUIStyle(GUI.skin.label)
            { fontSize = 15, normal = { textColor = new Color(0.94f, 0.90f, 0.84f) } };
            _value = new GUIStyle(GUI.skin.label)
            { fontSize = 15, fontStyle = FontStyle.Bold, alignment = TextAnchor.MiddleRight, normal = { textColor = new Color(1f, 0.78f, 0.36f) } };
            _valueSmall = new GUIStyle(_value) { fontSize = 14 };
            _small = new GUIStyle(GUI.skin.label)
            { fontSize = 14, normal = { textColor = new Color(0.68f, 0.62f, 0.54f) } };
            _button = new GUIStyle(GUI.skin.button) { fontSize = 15 };
            _bubble = new GUIStyle(GUI.skin.label)
            { fontSize = 13, fontStyle = FontStyle.Bold, alignment = TextAnchor.MiddleCenter, normal = { textColor = Color.white } };
            _bubbleTitle = new GUIStyle(GUI.skin.label)
            { fontSize = 11, alignment = TextAnchor.MiddleCenter, normal = { textColor = new Color(1f, 0.84f, 0.58f) } };
            _toast = new GUIStyle(GUI.skin.label)
            { fontSize = 15, alignment = TextAnchor.MiddleCenter, normal = { textColor = new Color(1f, 0.94f, 0.84f) } };
        }

        void Update()
        {
            if (room == null)
            {
                room = RoomController.Instance;
#if UNITY_2023_1_OR_NEWER
                if (room == null) room = Object.FindFirstObjectByType<RoomController>();
#else
                if (room == null) room = Object.FindObjectOfType<RoomController>();
#endif
                if (room != null) room.hud = this;
            }
            if (room == null) return;
            _fps = Mathf.Lerp(_fps, 1f / Mathf.Max(Time.unscaledDeltaTime, 1e-4f), 0.1f);

            var mouse = Mouse.current;
            if (mouse == null) { if (room.orbit != null) room.orbit.blocked = false; return; }
            var m = mouse.position.ReadValue();
            m.y = Screen.height - m.y;
            m /= _uiScale;
            if (room.orbit != null)
                room.orbit.blocked = _devicePanel.Contains(m) || _sensorPanel.Contains(m) || _helpBar.Contains(m);
        }

        void OnGUI()
        {
            if (room == null) return;
            BuildStyles();

            float w = Screen.width, h = Screen.height;
            _uiScale = Mathf.Clamp(h / 900f, 1f, 1.45f) * 1.08f;
            ComputeLayout(w / _uiScale, h / _uiScale);

            var saved = GUI.matrix;
            GUI.matrix = Matrix4x4.TRS(Vector3.zero, Quaternion.identity, new Vector3(_uiScale, _uiScale, 1f));

            DrawDevicePanel();
            DrawSensorPanel();
            DrawHelpBar();
            if (room.showBubbles) DrawBubbles();
            DrawToast();

            GUI.matrix = saved;
        }

        void ComputeLayout(float w, float h)
        {
            // 面板加宽 + 行高加大：原来 302 宽装不下"已开（硬件）"，会挤成一团
            _devicePanel = new Rect(w - 356f, 14f, 340f, 560f);
            _sensorPanel = new Rect(14f, 14f, 300f, 352f);
            _helpBar = new Rect(14f, Mathf.Max(14f, h - 124f), 452f, 110f);
        }

        // ------------------------------------------------------------ 设备面板

        void DrawDevicePanel()
        {
            GUI.DrawTexture(_devicePanel, _bg);
            GUI.DrawTexture(new Rect(_devicePanel.x, _devicePanel.y, _devicePanel.width, 3f), _accent);
            float x = _devicePanel.x + 14f, y = _devicePanel.y + 12f, iw = _devicePanel.width - 28f;

            GUI.Label(new Rect(x, y, iw, 22f), "暖木会议室 · 设备控制", _title); y += 25f;
            GUI.Label(new Rect(x, y, iw, 18f), "单击场景中的设备也可直接开关", _sub); y += 24f;
            Line(x, y, iw); y += 8f;

            y = Row(x, y, iw, "主吊灯", room.Get("mainlight"), id => Toggle(id, "mainlight"),
                d => Mathf.RoundToInt(d.value * 100f) + "%");
            var light = room.Get("mainlight");
            if (light != null && light.on)
                y = Slider(x, y, iw, "亮度调节", light.value, v => room.SetDeviceValue("mainlight", v));

            y = Row(x, y, iw, "空调 / HVAC", room.Get("ac"), id => Toggle(id, "ac"),
                d => (room.useLiveData && room.HasLiveData)
                    ? "实测 " + room.MeasuredTemp.ToString("0.0") + "℃"
                    : Mathf.RoundToInt(d.value) + "℃");
            var ac = room.Get("ac");
            if (ac != null && ac.on)
                y = Slider(x, y, iw,
                    room.useLiveData && room.HasLiveData ? "设定温度（上方读数为实测）" : "设定温度",
                    Mathf.InverseLerp(16f, 30f, ac.value),
                    v => room.SetDeviceValue("ac", Mathf.Lerp(16f, 30f, v)));

            y = Row(x, y, iw, "窗帘（南窗）", room.Get("curtainA"), room.ToggleCurtain,
                d => (d.on && d.value > 0.02f) ? "开 " + Mathf.RoundToInt(d.value * 100f) + "%" : "已关");
            y = Row(x, y, iw, "窗帘（北窗）", room.Get("curtainB"), room.ToggleCurtain,
                d => (d.on && d.value > 0.02f) ? "开 " + Mathf.RoundToInt(d.value * 100f) + "%" : "已关");

            // 门状态与门扇角度合并成一行（原来角度那行会和下一行叠字）
            var doorDev = room.Get("door");
            string doorAngle = "";
            if (room.refs.doorLeaf != null)
            {
                float a = room.refs.doorLeaf.localRotation.eulerAngles.y;
                if (a > 180f) a -= 360f;
                doorAngle = " " + Mathf.RoundToInt(a) + "°";
            }
            y = Row(x, y, iw, room.useLiveData ? "电动门 · 硬件" : "电动门", doorDev,
                id => room.Log(room.useLiveData ? "门由硬件控制，面板只读" : "电动门"),
                d => (d.on ? "已开" : "已关") + doorAngle);

            y += 4f;
            Line(x, y, iw); y += 8f;
            // 「温度叠加」按钮已按要求移除，恢复默认占满整行
            if (GUI.Button(new Rect(x, y, iw, 30f), "恢复默认", _button)) room.ResetAll();
            y += 38f;

            // 全部改成"室内机位 + 注视点"，相机不可能再落到墙外
            GUI.Label(new Rect(x, y, iw, 17f), "视角预设", _small); y += 20f;
            float bw = (iw - 8f) / 3f;
            if (GUI.Button(new Rect(x, y, bw, 30f), "全景", _button))
                SnapView("全景", new Vector3(3.30f, 1.70f, 2.35f), new Vector3(-0.90f, 1.05f, -0.80f));
            if (GUI.Button(new Rect(x + bw + 4f, y, bw, 30f), "看门", _button))
                SnapView("入口与门", new Vector3(1.20f, 1.55f, 0.40f), new Vector3(3.95f, 1.20f, 0.00f));
            if (GUI.Button(new Rect(x + (bw + 4f) * 2f, y, bw, 30f), "看窗", _button))
                SnapView("采光窗与窗帘", new Vector3(2.20f, 1.55f, 0.30f), new Vector3(-3.95f, 1.55f, 0.00f));
            y += 34f;
            if (GUI.Button(new Rect(x, y, bw, 30f), "看吊灯", _button))
                SnapView("主吊灯", new Vector3(1.60f, 1.55f, 1.60f), new Vector3(0.00f, 2.45f, 0.00f));
            if (GUI.Button(new Rect(x + bw + 4f, y, bw, 30f), "看桌子", _button))
                SnapView("会议桌", new Vector3(2.40f, 1.65f, 2.10f), new Vector3(0.00f, 0.80f, 0.00f));
            if (GUI.Button(new Rect(x + (bw + 4f) * 2f, y, bw, 30f), "看空调", _button))
                SnapView("落地空调", new Vector3(0.60f, 1.55f, 0.60f), new Vector3(-3.10f, 1.15f, 2.20f));
        }

        void Toggle(string id, string display)
        {
            room.Toggle(id);
        }

        /// <summary>给定室内机位与注视点切换视角（由 OrbitCamera 反算 yaw / pitch）。</summary>
        void SnapView(string label, Vector3 eye, Vector3 target)
        {
            if (room.orbit == null) return;
            room.orbit.SnapLook(eye, target);
            room.Log("视角：" + label);
        }

        // ------------------------------------------------------------ 数据面板

        /// <summary>数据来源/连接状态的一行说明（保持简短，细节看 Console）。</summary>
        string DataSourceText()
        {
            if (!room.useLiveData) return "本地模拟数据";

            var pi = room.pi;
            if (pi == null) return "数据源未创建";
            if (room.HasLiveData) return "树莓派实时 · 已收 " + pi.FramesReceived + " 帧";
            if (pi.IsConnected) return "已连接，等待数据…";
            return string.IsNullOrEmpty(pi.LastError) ? "离线 · 自动重连中…" : "离线 · 连接失败，重连中…";
        }

        void DrawSensorPanel()
        {
            GUI.DrawTexture(_sensorPanel, _bg);
            GUI.DrawTexture(new Rect(_sensorPanel.x, _sensorPanel.y, _sensorPanel.width, 3f), _sensorAccent);
            float x = _sensorPanel.x + 14f, y = _sensorPanel.y + 12f, iw = _sensorPanel.width - 28f;

            GUI.Label(new Rect(x, y, iw, 22f), "环境实时数据", _title); y += 22f;
            GUI.Label(new Rect(x, y, iw, 18f), DataSourceText(), _sub); y += 22f;
            Line(x, y, iw); y += 8f;

            // 温度超出 20–26℃ 时用文字标注（不只靠颜色区分）
            string tempName = "温度";
            if (room.temp > 26.5f) tempName = "温度 ⚠ 偏高";
            else if (room.temp < 20f) tempName = "温度 ⚠ 偏低";

            y = Metric(x, y, iw, tempName, room.temp.ToString("0.0") + " ℃",
                Mathf.InverseLerp(16f, 32f, room.temp), TempBar(room.temp));
            y = Metric(x, y, iw, "湿度", room.humidity.ToString("0") + " %",
                Mathf.InverseLerp(20f, 80f, room.humidity), _barHum);
            y = Metric(x, y, iw, "光照度", Mathf.RoundToInt(room.lux) + " lx",
                Mathf.InverseLerp(0f, 900f, room.lux), _barLux);
            y = Metric(x, y, iw, "PM2.5", room.pm25.ToString("0") + " μg/m³",
                Mathf.InverseLerp(0f, 75f, room.pm25), _barPm);
            y = Metric(x, y, iw, "CO₂", Mathf.RoundToInt(room.co2) + " ppm",
                Mathf.InverseLerp(400f, 1500f, room.co2), _barCo2);

            bool alarm = room.smoke > 0.35f;
            y = Metric(x, y, iw, alarm ? "烟雾 ⚠ 异常" : "烟雾",
                (room.smoke * 100f).ToString("0.0") + " %obs/m",
                Mathf.InverseLerp(0f, 1f, room.smoke), alarm ? _barAlarm : _barSmoke);

            y += 6f;
            Line(x, y, iw); y += 10f;
            GUI.Label(new Rect(x, y, iw, 20f), "在室人数：" + room.occupancy + " 人（开门次数累计）", _label); y += 28f;

            // 「门状态」行与「光照叠加 / 烟雾叠加」按钮已按要求删除
            if (GUI.Button(new Rect(x, y, iw, 26f), room.showBubbles ? "悬浮气泡：开" : "悬浮气泡：关", _button))
                room.showBubbles = !room.showBubbles;
        }

        Texture2D TempBar(float t)
        {
            if (t < 20f) return _barCool;
            if (t < 24f) return _barMild;
            if (t < 28f) return _barTemp;
            return _barHot;
        }

        // ------------------------------------------------------------ 提示与气泡

        void DrawHelpBar()
        {
            GUI.DrawTexture(_helpBar, _bgSoft);
            float x = _helpBar.x + 12f, y = _helpBar.y + 8f;
            GUI.Label(new Rect(x, y, _helpBar.width - 24f, 19f), "操作提示", _label); y += 20f;
            GUI.Label(new Rect(x, y, _helpBar.width - 24f, 18f), "左键拖拽：旋转　右键拖拽：平移　滚轮：缩放", _small); y += 17f;
            GUI.Label(new Rect(x, y, _helpBar.width - 24f, 18f), "左键单击设备：开关　快捷键 1主灯 2空调 3南窗帘 4北窗帘 5门(只读)", _small); y += 17f;
            GUI.Label(new Rect(x, y, _helpBar.width - 24f, 18f), "F/G/H：温度/光照/烟雾叠加　B：气泡　R：恢复默认", _small); y += 17f;
            GUI.Label(new Rect(x, y, _helpBar.width - 24f, 18f),
                "FPS " + Mathf.RoundToInt(_fps) + "　设备 " + room.devices.Count + " 个", _small);
        }

        void DrawBubbles()
        {
            Bubble(new Vector3(2.20f, 1.95f, Dim.HZ - 0.16f), "温湿度",
                room.temp.ToString("0.0") + "℃  " + room.humidity.ToString("0") + "%", TempBar(room.temp));
            Bubble(new Vector3(-3.60f, 1.95f, 2.55f), "光照",
                Mathf.RoundToInt(room.lux) + " lx  /  " + Mathf.RoundToInt(room.SampleLuxAt(new Vector3(-3.0f, 1f, 2.2f))) + " lx",
                _barLux);
            Bubble(new Vector3(-2.30f, 2.60f, Dim.WallFeature + 0.18f), "烟雾 / 空气",
                (room.smoke * 100f).ToString("0.0") + "%   PM2.5 " + room.pm25.ToString("0"),
                room.smoke > 0.35f ? _barAlarm : _barSmoke);
            Bubble(new Vector3(Dim.HX - 0.18f, 2.68f, 1.30f), "人体感应",
                room.occupancy + " 人在室", _sensorAccent);
            Bubble(new Vector3(Dim.HX - 0.36f, 1.44f, 1.02f), "门口环境面板",
                room.temp.ToString("0.0") + "℃ / " + room.humidity.ToString("0") + "% / PM2.5 " + room.pm25.ToString("0"),
                _accent);
            Bubble(new Vector3(-Dim.HX + 0.46f, 2.12f, Dim.HZ - 0.70f), "空调 · 设定温度",
                Mathf.RoundToInt(room.GetValue("ac")) + "℃   " + (room.IsOn("ac") ? "运行中" : "已关闭"),
                room.IsOn("ac") ? _accent : _switchOff);
        }

        void Bubble(Vector3 world, string title, string text, Texture2D accent)
        {
            if (!room.WorldToScreen(world, out var sp, out _)) return;
            sp /= _uiScale;
            float lw = Screen.width / _uiScale, lh = Screen.height / _uiScale;
            float bw = Mathf.Max(128f, text.Length * 6.8f + 22f);
            var r = new Rect(sp.x - bw * 0.5f, sp.y - 26f, bw, 42f);
            if (r.x < 4f || r.y < 4f || r.xMax > lw - 4f || r.yMax > lh - 4f) return;

            GUI.DrawTexture(r, _bgSoft);
            GUI.DrawTexture(new Rect(r.x, r.y, 3f, r.height), accent);
            GUI.Label(new Rect(r.x + 6f, r.y + 3f, r.width - 12f, 15f), title, _bubbleTitle);
            GUI.Label(new Rect(r.x + 6f, r.y + 19f, r.width - 12f, 20f), text, _bubble);
        }

        void DrawToast()
        {
            string msg = room.LastActionText;
            if (string.IsNullOrEmpty(msg)) return;
            float lw = Screen.width / _uiScale;
            var r = new Rect(lw * 0.5f - 170f, 24f, 340f, 34f);
            GUI.DrawTexture(r, _bg);
            GUI.DrawTexture(new Rect(r.x, r.y, r.width, 3f), _accent);
            GUI.Label(r, msg, _toast);
        }

        // ------------------------------------------------------------ 小部件

        void Line(float x, float y, float w) => GUI.DrawTexture(new Rect(x, y, w, 1f), _hairline);

        delegate void RowAction(string id);
        delegate string RowText(DeviceController d);

        float Row(float x, float y, float w, string name, DeviceController d, RowAction onClick, RowText text)
        {
            if (d == null) return Missing(x, y, w, name);

            const float sw = 48f, gap = 10f, valW = 104f;
            float nameW = w - sw - gap - valW - 6f;
            GUI.Label(new Rect(x, y + 4f, nameW, 20f), name, _label);
            GUI.Label(new Rect(x + nameW + 2f, y + 4f, valW, 20f), text(d), _valueSmall);

            var tr = new Rect(x + w - sw, y + 3f, sw, 22f);
            GUI.DrawTexture(tr, d.on ? _switchOn : _switchOff);
            GUI.DrawTexture(new Rect(d.on ? tr.x + tr.width - 21f : tr.x + 2f, tr.y + 2f, 19f, 18f), _white);

            if (GUI.Button(new Rect(x, y, w, 30f), GUIContent.none, GUIStyle.none)) onClick(d.id);
            return y + 34f;
        }

        float Missing(float x, float y, float w, string name)
        {
            GUI.Label(new Rect(x, y + 4f, w - 130f, 20f), name, _label);
            GUI.Label(new Rect(x + w - 128f, y + 4f, 126f, 20f), "场景中缺失", _value);
            return y + 34f;
        }

        float Slider(float x, float y, float w, string label, float value, System.Action<float> onChange)
        {
            GUI.Label(new Rect(x + 4f, y, w - 8f, 18f), label, _small);
            var tr = new Rect(x + 4f, y + 20f, w - 8f, 14f);
            GUI.DrawTexture(tr, _sliderBg);
            GUI.DrawTexture(new Rect(tr.x, tr.y, tr.width * Mathf.Clamp01(value), tr.height), _sliderFill);
            GUI.DrawTexture(new Rect(tr.x + tr.width * Mathf.Clamp01(value) - 5f, tr.y - 3f, 10f, 20f), _white);

            var ev = Event.current;
            if (ev != null && (ev.type == EventType.MouseDown || ev.type == EventType.MouseDrag) && tr.Contains(ev.mousePosition))
            {
                onChange(Mathf.Clamp01((ev.mousePosition.x - tr.x) / tr.width));
                ev.Use();
            }
            return y + 44f;
        }

        float Metric(float x, float y, float w, string name, string value, float fill, Texture2D bar)
        {
            GUI.Label(new Rect(x, y, w * 0.5f, 19f), name, _small);
            GUI.Label(new Rect(x + w * 0.40f, y, w * 0.60f, 19f), value, _valueSmall);
            GUI.DrawTexture(new Rect(x, y + 21f, w, 7f), _sliderBg);
            GUI.DrawTexture(new Rect(x, y + 21f, w * Mathf.Clamp01(fill), 7f), bar);
            return y + 34f;
        }
    }
}
