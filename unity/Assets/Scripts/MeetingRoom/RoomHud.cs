using UnityEngine;
using UnityEngine.InputSystem;

namespace SmartRoom
{
    /// <summary>
    /// 运行时操作界面（IMGUI 绘制）：设备点控面板、传感器数据面板、
    /// 以及跟随传感器位置的悬浮数据气泡。
    /// </summary>
    [DisallowMultipleComponent]
    public class RoomHud : MonoBehaviour
    {
        public RoomController room;

        // 面板区域（用于判断鼠标是否落在 UI 上，避免拖拽相机时误点设备）
        Rect _devicePanel = new Rect(0, 0, 290, 336);
        Rect _sensorPanel = new Rect(0, 0, 250, 300);
        Rect _helpBar = new Rect(0, 0, 420, 96);

        Texture2D _bg, _bgSoft, _accent, _switchOn, _switchOff, _sliderBg, _sliderFill, _white;

        GUIStyle _title, _sub, _label, _value, _valueSmall, _small, _bubble, _bubbleTitle, _button, _toast;
        bool _stylesReady;
        float _fps;
        float _uiScale = 1f;

        // ------------------------------------------------------------------ 初始化

        void Awake()
        {
            _white = Solid(Color.white);
            _bg = Solid(new Color(0.055f, 0.070f, 0.090f, 0.94f));
            _bgSoft = Solid(new Color(0.085f, 0.105f, 0.130f, 0.95f));
            _accent = Solid(new Color(0.24f, 0.72f, 0.98f, 1f));
            _switchOn = Solid(new Color(0.20f, 0.66f, 0.92f, 1f));
            _switchOff = Solid(new Color(0.24f, 0.27f, 0.31f, 1f));
            _sliderBg = Solid(new Color(0.17f, 0.20f, 0.24f, 1f));
            _sliderFill = Solid(new Color(0.30f, 0.78f, 0.98f, 1f));
            // 这些颜色在 OnGUI 里会被反复使用，必须缓存（否则每帧新建纹理 → 卡顿 + 显存泄漏）
            _hairline = Solid(new Color(1f, 1f, 1f, 0.10f));
            _sensorAccent = Solid(new Color(0.30f, 0.86f, 0.55f, 1f));
            _barTemp = Solid(new Color(0.35f, 0.72f, 0.98f, 1f));
            _barLux = Solid(new Color(0.98f, 0.85f, 0.30f, 1f));
            _barPm = Solid(new Color(0.72f, 0.60f, 0.95f, 1f));
            _barCo2 = Solid(new Color(0.45f, 0.85f, 0.70f, 1f));
            _barSmokeSafe = Solid(new Color(0.55f, 0.75f, 0.95f, 1f));
            _barSmokeAlarm = Solid(new Color(1f, 0.28f, 0.24f, 1f));
            _barTempHot = Solid(new Color(1f, 0.42f, 0.30f, 1f));
            _barTempCool = Solid(new Color(0.30f, 0.62f, 0.98f, 1f));
            _barTempMild = Solid(new Color(0.55f, 0.88f, 0.42f, 1f));
        }

        Texture2D _hairline, _sensorAccent, _barTemp, _barLux, _barPm, _barCo2, _barSmokeSafe, _barSmokeAlarm, _barTempHot, _barTempCool, _barTempMild;

        /// <summary>按温度区间取缓存的色条纹理。</summary>
        Texture2D TempBar(float t)
        {
            if (t < 20f) return _barTempCool;
            if (t < 24f) return _barTemp;
            if (t < 27f) return _barTempMild;
            if (t < 29f) return _barLux;
            return _barTempHot;
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
            { fontSize = 18, fontStyle = FontStyle.Bold, normal = { textColor = new Color(0.93f, 0.96f, 1f) } };
            _sub = new GUIStyle(GUI.skin.label)
            { fontSize = 13, normal = { textColor = new Color(0.62f, 0.72f, 0.82f) } };
            _label = new GUIStyle(GUI.skin.label)
            { fontSize = 15, normal = { textColor = new Color(0.86f, 0.90f, 0.95f) } };
            _value = new GUIStyle(GUI.skin.label)
            { fontSize = 15, fontStyle = FontStyle.Bold, alignment = TextAnchor.MiddleRight, normal = { textColor = new Color(0.42f, 0.86f, 1f) } };
            _valueSmall = new GUIStyle(_value) { fontSize = 14 };
            _toast = new GUIStyle(GUI.skin.label)
            { fontSize = 15, alignment = TextAnchor.MiddleCenter, normal = { textColor = new Color(0.90f, 0.95f, 1f) } };
            _small = new GUIStyle(GUI.skin.label)
            { fontSize = 13, normal = { textColor = new Color(0.62f, 0.70f, 0.78f) } };
            _button = new GUIStyle(GUI.skin.button) { fontSize = 14 };
            _bubble = new GUIStyle(GUI.skin.label)
            { fontSize = 13, fontStyle = FontStyle.Bold, alignment = TextAnchor.MiddleCenter, normal = { textColor = Color.white } };
            _bubbleTitle = new GUIStyle(GUI.skin.label)
            { fontSize = 11, alignment = TextAnchor.MiddleCenter, normal = { textColor = new Color(0.70f, 0.88f, 1f) } };
        }

        void Update()
        {
            // 容错：如果场景是旧版本或者引用丢失，这里自动在场景里重新找到控制器，
            // 避免每帧抛 NullReferenceException 把 Console 刷爆。
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

            // 判断鼠标是否压在面板上（项目只启用了新版 Input System，不能用旧版 Input.mousePosition）
            var mouse = UnityEngine.InputSystem.Mouse.current;
            if (mouse == null)
            {
                if (room.orbit != null) room.orbit.blocked = false;
                return;
            }
            var m = mouse.position.ReadValue();
            m.y = Screen.height - m.y;
            // 面板坐标是缩放前的逻辑坐标，判断前要先把鼠标位置换算回去
            m /= _uiScale;
            bool blocked = _devicePanel.Contains(m) || _sensorPanel.Contains(m) || _helpBar.Contains(m);
            if (room.orbit != null) room.orbit.blocked = blocked;
        }

        // ------------------------------------------------------------------ 绘制

        void OnGUI()
        {
            if (room == null) return;
            BuildStyles();

            float h = Screen.height, w = Screen.width;

            // 整个 HUD 按屏幕尺寸放大；再用一个可调系数整体微调大小
            _uiScale = Mathf.Clamp(h / 900f, 1f, 1.45f) * 1.10f;
            ComputeLayout(w / _uiScale, h / _uiScale);

            var saved = GUI.matrix;
            GUI.matrix = Matrix4x4.TRS(Vector3.zero, Quaternion.identity, new Vector3(_uiScale, _uiScale, 1f));

            DrawDevicePanel();
            DrawSensorPanel();
            DrawHelpBar();
            if (room.showBubbles) DrawSensorBubbles();
            DrawToast();

            GUI.matrix = saved;
        }

        /// <summary>按逻辑分辨率计算各面板位置（面板尺寸也跟着放大，字和控件看起来更大）。</summary>
        void ComputeLayout(float w, float h)
        {
            // 面板高度按内容实际需要给足（含设定温度滑块、开合程度滑块、视角预设两排按钮），
            // 之前 420 不够，导致最后的滑块被面板底边裁掉。
            _devicePanel = new Rect(w - 312f, 14f, 296f, 556f);
            _sensorPanel = new Rect(14f, 14f, 268f, 402f);
            _helpBar = new Rect(14f, Mathf.Max(14f, h - 124f), 452f, 110f);
        }

        // ---- 设备控制面板 ----

        void DrawDevicePanel()
        {
            GUI.DrawTexture(_devicePanel, _bg);
            GUI.DrawTexture(new Rect(_devicePanel.x, _devicePanel.y, _devicePanel.width, 3f), _accent);
            float x = _devicePanel.x + 14f;
            float y = _devicePanel.y + 12f;
            float iw = _devicePanel.width - 28f;

            GUI.Label(new Rect(x, y, iw, 20f), "智能会议室 · 设备控制", _title);
            y += 22f;
            GUI.Label(new Rect(x, y, iw, 16f), "单击场景中的设备也可直接开关", _sub);
            y += 22f;
            Line(x, y, iw); y += 8f;

            // 电动门（放在第一行；投影已移除）——显示实时门扇角度，便于确认动画是否在跑
            var door = room.Get("door");
            if (door == null) y = Missing(x, y, iw, "电动门");
            else
            {
                // 状态文字保持精简，避免被开关挡住（详细角度读数列在下一行的小字里）
                string doorState = door.on ? "已开" : "已关";
                y = Row(x, y, iw, "电动门", door.on, doorState, () => room.Toggle("door"));
                // 门扇实时角度：用于确认动画是否真的在跑
                if (room.refs.doorRoot != null)
                {
                    float a = room.refs.doorRoot.localRotation.eulerAngles.y;
                    if (a > 180f) a -= 360f;
                    GUI.Label(new Rect(x + 4f, y - 4f, iw - 8f, 16f),
                        "门扇角度  " + Mathf.RoundToInt(a) + "°", _small);
                }
            }
            // 主灯
            var light = room.Get("mainlight");
            if (light == null) y = Missing(x, y, iw, "主吊灯");
            else
            {
                y = Row(x, y, iw, "主吊灯", light.on, Mathf.RoundToInt(light.value * 100f) + "%", () => room.Toggle("mainlight"));
                if (light.on)
                    y = Slider(x, y, iw, "亮度调节", light.value, v => room.SetDeviceValue("mainlight", v));
            }
            // 空调
            var ac = room.Get("ac");
            if (ac == null) y = Missing(x, y, iw, "空调 / HVAC");
            else
            {
                y = Row(x, y, iw, "空调 / HVAC", ac.on, Mathf.RoundToInt(ac.value) + "℃", () => room.Toggle("ac"));
                if (ac.on)
                    y = Slider(x, y, iw, "设定温度", Mathf.InverseLerp(16f, 30f, ac.value),
                        v => room.SetDeviceValue("ac", Mathf.Lerp(16f, 30f, v)));
            }
            // 窗帘（点击即在全开/全关之间切换）
            var cur = room.Get("curtain");
            if (cur == null) y = Missing(x, y, iw, "电动窗帘");
            else
            {
                bool curOpen = cur.on && cur.value > 0.02f;
                y = Row(x, y, iw, "电动窗帘", curOpen,
                    curOpen ? "开 " + Mathf.RoundToInt(cur.value * 100f) + "%" : "已关闭",
                    () => room.ToggleCurtain());
                if (cur.on)
                    y = Slider(x, y, iw, "开合程度", cur.value, v => room.SetDeviceValue("curtain", v));
            }
            // （电动门已移到面板第一行）

            y += 4f;
            Line(x, y, iw); y += 8f;
            if (GUI.Button(new Rect(x, y, iw * 0.48f, 26f), "恢复默认", _button)) room.ResetAll();
            if (GUI.Button(new Rect(x + iw * 0.52f, y, iw * 0.48f, 26f),
                room.showThermalOverlay ? "温度叠加：开" : "温度叠加：关", _button))
                room.showThermalOverlay = !room.showThermalOverlay;
            y += 32f;

            // 相机预设
            GUI.Label(new Rect(x, y, iw, 16f), "视角预设", _small);
            y += 18f;
            float bw = (iw - 8f) / 3f;
            if (GUI.Button(new Rect(x, y, bw, 26f), "全景", _button)) Snap("全景", 38f, 24f, 9.6f, new Vector3(0f, 1.05f, 0f));
            if (GUI.Button(new Rect(x + bw + 4f, y, bw, 26f), "看门", _button)) Snap("入口与门", 96f, 10f, 6.2f, new Vector3(-1.6f, 1.40f, -1.85f));
            if (GUI.Button(new Rect(x + (bw + 4f) * 2f, y, bw, 26f), "看空调", _button)) Snap("落地空调", 52f, 12f, 3.6f, new Vector3(3.10f, 1.15f, -2.20f));
            y += 30f;
            if (GUI.Button(new Rect(x, y, bw, 26f), "看幕布墙", _button)) Snap("投影墙", 0f, 10f, 6.4f, new Vector3(0f, 1.55f, -1.6f));
            if (GUI.Button(new Rect(x + bw + 4f, y, bw, 26f), "看桌子", _button)) Snap("会议桌", -34f, 30f, 5.0f, new Vector3(0f, 0.85f, 0f));
            if (GUI.Button(new Rect(x + (bw + 4f) * 2f, y, bw, 26f), "看传感器", _button)) Snap("传感器与面板", 120f, 12f, 5.6f, new Vector3(-2.4f, 1.55f, -1.30f));
        }

        void Snap(string label, float yaw, float pitch, float dist, Vector3 pivot)
        {
            if (room.orbit == null) return;
            room.orbit.SnapTo(yaw, pitch, dist, pivot);
            room.Log("视角：" + label);
        }

        // ---- 传感器数据面板 ----

        void DrawSensorPanel()
        {
            GUI.DrawTexture(_sensorPanel, _bg);
            GUI.DrawTexture(new Rect(_sensorPanel.x, _sensorPanel.y, _sensorPanel.width, 3f), _sensorAccent);

            float x = _sensorPanel.x + 14f;
            float y = _sensorPanel.y + 12f;
            float iw = _sensorPanel.width - 28f;

            GUI.Label(new Rect(x, y, iw, 20f), "环境实时数据", _title); y += 20f;
            GUI.Label(new Rect(x, y, iw, 16f), "模拟数据 · 预留树莓派接口", _sub); y += 20f;
            Line(x, y, iw); y += 8f;

            y = Metric(x, y, iw, "温度", room.temp.ToString("0.0") + " ℃",
                Mathf.InverseLerp(16f, 32f, room.temp), TempBar(room.temp));
            y = Metric(x, y, iw, "湿度", room.humidity.ToString("0") + " %",
                Mathf.InverseLerp(20f, 80f, room.humidity), _barTemp);
            y = Metric(x, y, iw, "光照度", Mathf.RoundToInt(room.lux) + " lx",
                Mathf.InverseLerp(0f, 900f, room.lux), _barLux);
            y = Metric(x, y, iw, "PM2.5", room.pm25.ToString("0") + " μg/m³",
                Mathf.InverseLerp(0f, 75f, room.pm25), _barPm);
            y = Metric(x, y, iw, "CO₂", Mathf.RoundToInt(room.co2) + " ppm",
                Mathf.InverseLerp(400f, 1500f, room.co2), _barCo2);

            bool smokeAlarm = room.smoke > 0.35f;
            y = Metric(x, y, iw, smokeAlarm ? "烟雾 ⚠ 异常" : "烟雾",
                (room.smoke * 100f).ToString("0.0") + " %obs/m",
                Mathf.InverseLerp(0f, 1f, room.smoke),
                smokeAlarm ? _barSmokeAlarm : _barSmokeSafe);

            y += 4f;
            Line(x, y, iw); y += 8f;
            GUI.Label(new Rect(x, y, iw, 18f), "在室人数：" + room.occupancy + " 人（人脸识别统计）", _label);
            y += 20f;
            GUI.Label(new Rect(x, y, iw, 16f), "门禁状态：正常 · 最近一次识别 2 分钟前", _small);
            y += 18f;

            // 可视化开关
            if (GUI.Button(new Rect(x, y, (iw - 6f) / 2f, 22f),
                room.showLuxField ? "光照叠加：开" : "光照叠加：关", _button))
                room.showLuxField = !room.showLuxField;
            if (GUI.Button(new Rect(x + (iw + 6f) / 2f, y, (iw - 6f) / 2f, 22f),
                room.showSmokeField ? "烟雾叠加：开" : "烟雾叠加：关", _button))
                room.showSmokeField = !room.showSmokeField;
            y += 26f;
            if (GUI.Button(new Rect(x, y, iw, 22f), room.showBubbles ? "悬浮数据气泡：开" : "悬浮数据气泡：关", _button))
                room.showBubbles = !room.showBubbles;
        }

        static Color TempColor(float t)
        {
            if (t < 20f) return new Color(0.30f, 0.62f, 0.98f);
            if (t < 24f) return new Color(0.35f, 0.85f, 0.75f);
            if (t < 27f) return new Color(0.55f, 0.88f, 0.42f);
            if (t < 29f) return new Color(0.98f, 0.78f, 0.25f);
            return new Color(1f, 0.42f, 0.30f);
        }

        // ---- 底部操作提示 ----

        void DrawHelpBar()
        {
            GUI.DrawTexture(_helpBar, _bgSoft);
            float x = _helpBar.x + 12f, y = _helpBar.y + 8f;
            GUI.Label(new Rect(x, y, _helpBar.width - 24f, 18f), "操作提示", _label); y += 20f;
            GUI.Label(new Rect(x, y, _helpBar.width - 24f, 18f), "左键拖拽：旋转　右键拖拽：平移　滚轮：缩放", _small); y += 17f;
            GUI.Label(new Rect(x, y, _helpBar.width - 24f, 18f), "左键单击设备：开关　快捷键 1/5门 2主灯 3空调 4窗帘", _small); y += 17f;
            GUI.Label(new Rect(x, y, _helpBar.width - 24f, 18f), "F/G/H：温度/光照/烟雾叠加　B：气泡　R：恢复默认", _small); y += 17f;
            GUI.Label(new Rect(x, y, _helpBar.width - 24f, 18f),
                "FPS " + Mathf.RoundToInt(_fps) + "　设备 " + room.devices.Count + " 个　右上角有视角预设", _small);
        }

        // ---- 悬浮数据气泡 ----

        void DrawSensorBubbles()
        {
            DrawBubble(new Vector3(RoomDim.HalfX - 0.15f, 1.95f, 0.30f), "温湿度",
                room.temp.ToString("0.0") + "℃  " + room.humidity.ToString("0") + "%", TempColor(room.temp));
            DrawBubble(new Vector3(2.42f, 1.95f, RoomDim.HalfZ - 0.15f), "光照",
                Mathf.RoundToInt(room.lux) + " lx  /  " + Mathf.RoundToInt(room.SampleLuxAt(new Vector3(2.4f, 1f, 2.2f))) + " lx",
                new Color(0.98f, 0.85f, 0.30f));
            DrawBubble(new Vector3(-2.05f, 2.60f, RoomDim.WallScreen + 0.15f), "烟雾 / 空气",
                (room.smoke * 100f).ToString("0.0") + "%   PM2.5 " + room.pm25.ToString("0"),
                room.smoke > 0.35f ? new Color(1f, 0.30f, 0.26f) : new Color(0.55f, 0.78f, 0.98f));
            DrawBubble(new Vector3(RoomDim.WallLeft + 0.15f, 2.68f, -1.85f), "人体感应",
                room.occupancy + " 人在室", new Color(0.45f, 0.92f, 0.60f));
            // 落地空调本体
            DrawBubble(new Vector3(RoomDim.HalfX - 0.48f, 2.10f, -RoomDim.HalfZ + 0.72f), "空调 · 设定温度",
                Mathf.RoundToInt(room.GetValue("ac")) + "℃   " + (room.IsOn("ac") ? "运行中" : "已关闭"),
                room.IsOn("ac") ? new Color(0.40f, 0.85f, 1f) : new Color(0.55f, 0.58f, 0.62f));

            // 门口环境面板标题气泡
            DrawBubble(new Vector3(RoomDim.WallLeft + 0.30f, 1.42f, -2.78f), "门口环境面板",
                room.temp.ToString("0.0") + "℃ / " + room.humidity.ToString("0") + "% / PM2.5 " + room.pm25.ToString("0"),
                new Color(0.42f, 0.80f, 0.98f));
        }

        void DrawBubble(Vector3 world, string title, string text, Color accent)
        {
            if (!room.WorldToScreen(world, out var screenPt, out _)) return;

            // WorldToScreen 给的是真实屏幕坐标，而绘制在缩放后的逻辑坐标系里，必须换算回去
            var sp = screenPt / _uiScale;
            float lw = Screen.width / _uiScale, lh = Screen.height / _uiScale;

            float bw = Mathf.Max(126f, text.Length * 6.6f + 22f);
            var r = new Rect(sp.x - bw * 0.5f, sp.y - 26f, bw, 42f);
            if (r.x < 4f || r.y < 4f || r.xMax > lw - 4f || r.yMax > lh - 4f) return;

            GUI.DrawTexture(r, _bgSoft);
            GUI.DrawTexture(new Rect(r.x, r.y, 3f, r.height), Solid(accent));
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
            GUI.DrawTexture(new Rect(r.x, r.y, r.width, 2f), _accent);
            GUI.Label(r, msg, _toast);
        }

        // ------------------------------------------------------------------ 小部件

        void Line(float x, float y, float w)
        {
            GUI.DrawTexture(new Rect(x, y, w, 1f), _hairline);
        }

        /// <summary>
        /// 设备缺失时画一行占位（说明当前场景不是最新生成的）。
        /// 有这个兜底，旧场景也不会让 HUD 抛 NullReferenceException。
        /// </summary>
        float Missing(float x, float y, float w, string name)
        {
            GUI.Label(new Rect(x, y + 2f, w - 96f, 18f), name, _label);
            var warn = new GUIStyle(_value) { fontSize = 11, normal = { textColor = new Color(1f, 0.62f, 0.35f) } };
            GUI.Label(new Rect(x + w - 150f, y + 2f, 128f, 18f), "场景中缺失", warn);
            return y + 25f;
        }

        float Row(float x, float y, float w, string name, bool on, string value, System.Action onClick)
        {
            const float switchW = 46f;     // 开关宽
            const float gap = 10f;         // 开关与文字的间距
            const float valW = 76f;        // 数值文字宽度（"已打开 -88°" 放得下）
            float nameW = w - switchW - gap - valW - 6f;

            GUI.Label(new Rect(x, y + 2f, nameW, 20f), name, _label);
            GUI.Label(new Rect(x + nameW + 2f, y + 2f, valW, 20f), value, _value);

            var tr = new Rect(x + w - switchW, y + 1f, switchW, 22f);
            GUI.DrawTexture(tr, on ? _switchOn : _switchOff);
            var knob = new Rect(on ? tr.x + tr.width - 21f : tr.x + 2f, tr.y + 2f, 19f, 18f);
            GUI.DrawTexture(knob, _white);

            // 整行都是点击热区（盖住名字、数值、开关，一点就开关）
            if (GUI.Button(new Rect(x, y, w, 24f), GUIContent.none, GUIStyle.none)) onClick();
            return y + 28f;
        }

        float Slider(float x, float y, float w, string label, float value, System.Action<float> onChange)
        {
            GUI.Label(new Rect(x + 4f, y, 150f, 17f), label, _small);
            var tr = new Rect(x + 4f, y + 17f, w - 8f, 14f);
            GUI.DrawTexture(tr, _sliderBg);
            var fill = new Rect(tr.x, tr.y, tr.width * Mathf.Clamp01(value), tr.height);
            GUI.DrawTexture(fill, _sliderFill);
            var knob = new Rect(tr.x + tr.width * Mathf.Clamp01(value) - 5f, tr.y - 3f, 10f, 20f);
            GUI.DrawTexture(knob, _white);

            var ev = Event.current;
            if (ev != null && (ev.type == EventType.MouseDown || ev.type == EventType.MouseDrag) && tr.Contains(ev.mousePosition))
            {
                onChange(Mathf.Clamp01((ev.mousePosition.x - tr.x) / tr.width));
                ev.Use();
            }
            return y + 39f;
        }

        float Metric(float x, float y, float w, string name, string value, float fill, Texture2D barTex)
        {
            GUI.Label(new Rect(x, y, w * 0.5f, 18f), name, _small);
            GUI.Label(new Rect(x + w * 0.4f, y, w * 0.6f, 18f), value, _valueSmall);

            var bar = new Rect(x, y + 19f, w, 6f);
            GUI.DrawTexture(bar, _sliderBg);
            GUI.DrawTexture(new Rect(bar.x, bar.y, bar.width * Mathf.Clamp01(fill), bar.height), barTex);
            return y + 28f;
        }
    }
}
