using UnityEngine;
using UnityEngine.InputSystem;

namespace NordicRoom
{
    /// <summary>
    /// 运行时界面（IMGUI）：设备控制面板 + 环境数据面板 + 悬浮气泡 + 视角预设。
    ///
    /// 本文件与"暖木会议室"的 WarmRoom/RoomHud.cs 保持一致，只改了三处：
    ///   · 命名空间与北欧风配色；
    ///   · 窗帘从 2 组变 4 组（北欧风是 5 扇角窗）；
    ///   · 悬浮气泡 / 视角预设的坐标改成北欧风的门窗布置。
    /// </summary>
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
            // 只允许建贴图。GUIStyle / GUI.skin 必须留到 OnGUI 里，
            // 否则会抛 "You can only call GUI functions from inside OnGUI."
            _white = Solid(Color.white);            _bg = Solid(new Color(0.075f, 0.085f, 0.100f, 0.94f));
            _bgSoft = Solid(new Color(0.105f, 0.120f, 0.140f, 0.95f));
            _accent = Solid(new Color(0.42f, 0.72f, 0.80f, 1f));
            _switchOn = Solid(new Color(0.35f, 0.78f, 0.62f, 1f));
            _switchOff = Solid(new Color(0.30f, 0.315f, 0.335f, 1f));
            _sliderBg = Solid(new Color(0.200f, 0.215f, 0.235f, 1f));
            _sliderFill = Solid(new Color(0.42f, 0.72f, 0.80f, 1f));
            _hairline = Solid(new Color(0.62f, 0.80f, 0.88f, 0.20f));
            _sensorAccent = Solid(new Color(0.55f, 0.85f, 0.50f, 1f));
            _barTemp = Solid(new Color(0.95f, 0.62f, 0.35f, 1f));
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
            { fontSize = 18, fontStyle = FontStyle.Bold, normal = { textColor = new Color(0.78f, 0.92f, 1f) } };
            _sub = new GUIStyle(GUI.skin.label)
            { fontSize = 13, normal = { textColor = new Color(0.62f, 0.70f, 0.78f) } };
            _label = new GUIStyle(GUI.skin.label)
            { fontSize = 15, normal = { textColor = new Color(0.92f, 0.95f, 0.97f) } };
            _value = new GUIStyle(GUI.skin.label)
            { fontSize = 15, fontStyle = FontStyle.Bold, alignment = TextAnchor.MiddleRight, normal = { textColor = new Color(0.55f, 0.86f, 0.98f) } };
            _valueSmall = new GUIStyle(_value) { fontSize = 14 };
            _small = new GUIStyle(GUI.skin.label)
            { fontSize = 14, normal = { textColor = new Color(0.64f, 0.72f, 0.79f) } };
            _button = new GUIStyle(GUI.skin.button) { fontSize = 15 };
            _bubble = new GUIStyle(GUI.skin.label)
            { fontSize = 13, fontStyle = FontStyle.Bold, alignment = TextAnchor.MiddleCenter, normal = { textColor = Color.white } };
            _bubbleTitle = new GUIStyle(GUI.skin.label)
            { fontSize = 11, alignment = TextAnchor.MiddleCenter, normal = { textColor = new Color(0.62f, 0.88f, 1f) } };
            _toast = new GUIStyle(GUI.skin.label)
            { fontSize = 15, alignment = TextAnchor.MiddleCenter, normal = { textColor = new Color(0.88f, 0.96f, 1f) } };
        }

        /// <summary>
        /// 确保拿得到 RoomController。
        /// 场景里序列化的引用一旦断掉（脚本重编译后偶发），面板会整个失效，
        /// 所以这里做自愈：先问单例，再全场查找，最后直接按名字找物体上的组件。
        /// </summary>
        bool EnsureRoom()
        {
            if (room != null) return true;
            room = RoomController.Instance;
#if UNITY_2023_1_OR_NEWER
            if (room == null) room = Object.FindFirstObjectByType<RoomController>();
#else
            if (room == null) room = Object.FindObjectOfType<RoomController>();
#endif
            if (room == null)
            {
                var go = GameObject.Find("07_会议室控制器");
                if (go != null) room = go.GetComponent<RoomController>();
            }
            if (room != null) room.hud = this;
            return room != null;
        }

        void Update()
        {
            if (!EnsureRoom()) return;
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
            // 每帧都自愈一次：引用断了就重新找回来，面板不会因为一次断链就永久失效
            if (!EnsureRoom()) return;
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
            _devicePanel = new Rect(w - 356f, 14f, 340f, 706f);
            _sensorPanel = new Rect(14f, 14f, 300f, 352f);
            _helpBar = new Rect(14f, Mathf.Max(14f, h - 124f), 452f, 110f);
        }

        // ------------------------------------------------------------ 设备面板

        void DrawDevicePanel()
        {
            GUI.DrawTexture(_devicePanel, _bg);
            GUI.DrawTexture(new Rect(_devicePanel.x, _devicePanel.y, _devicePanel.width, 3f), _accent);
            float x = _devicePanel.x + 14f, y = _devicePanel.y + 12f, iw = _devicePanel.width - 28f;

            GUI.Label(new Rect(x, y, iw, 22f), "北欧会议室 · 设备控制", _title); y += 25f;
            GUI.Label(new Rect(x, y, iw, 18f), "单击场景中的设备也可直接开关", _sub); y += 24f;
            Line(x, y, iw); y += 8f;

            y = Row(x, y, iw, "主吊灯", room.Get("mainlight"), id => Toggle(id), null);
            var light = room.Get("mainlight");
            if (light != null && light.on)
                y = Slider(x, y, iw, "亮度调节", light.value, v => room.SetDeviceValue("mainlight", v));

            y = Row(x, y, iw, "空调 / HVAC", room.Get("ac"), id => Toggle(id),
                d => (room.useLiveData && room.HasLiveData)
                    ? "实测 " + room.MeasuredTemp.ToString("0.0") + "℃"
                    : Mathf.RoundToInt(d.value) + "℃");
            var ac = room.Get("ac");
            if (ac != null && ac.on)
                y = Slider(x, y, iw,
                    room.useLiveData && room.HasLiveData ? "设定温度（上方读数为实测）" : "设定温度",
                    Mathf.InverseLerp(16f, 30f, ac.value),
                    v => room.SetDeviceValue("ac", Mathf.Lerp(16f, 30f, v)));

            y = Row(x, y, iw, "窗帘（北西窗）", room.Get("curtain0"), id => room.ToggleAllCurtains(), CurtainText);
            y = Row(x, y, iw, "窗帘（北东窗）", room.Get("curtain1"), id => room.ToggleAllCurtains(), CurtainText);
            y = Row(x, y, iw, "窗帘（南西窗）", room.Get("curtain2"), id => room.ToggleAllCurtains(), CurtainText);
            y = Row(x, y, iw, "窗帘（南东窗）", room.Get("curtain3"), id => room.ToggleAllCurtains(), CurtainText);

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

            y = Row(x, y, iw, "会议控制屏", room.Get("panel"), id => Toggle(id),
                d => d.on ? "在线" : "离线");
            y = Row(x, y, iw, "入口环境屏", room.Get("entry"), id => Toggle(id),
                d => d.on ? "在线" : "离线");
            y = Row(x, y, iw, "门禁 / 刷卡", room.Get("access"), id => Toggle(id),
                d => d.on ? "已布防" : "已撤防");

            y += 4f;
            Line(x, y, iw); y += 8f;
            // 「温度叠加」按钮已按要求移除，恢复默认占满整行
            if (GUI.Button(new Rect(x, y, iw, 30f), "恢复默认", _button)) room.ResetAll();
            y += 38f;

            GUI.Label(new Rect(x, y, iw, 17f), "视角预设", _small); y += 19f;
            float bw = (iw - 8f) / 3f;
            if (GUI.Button(new Rect(x, y, bw, 30f), "全景", _button)) Snap("全景", -3.05f, 1.62f, -2.15f, new Vector3(0f, 1.15f, 0.30f));
            if (GUI.Button(new Rect(x + bw + 4f, y, bw, 30f), "看门", _button)) Snap("入口与门", 2.60f, 1.60f, 1.60f, new Vector3(-3.60f, 1.30f, -0.30f));
            if (GUI.Button(new Rect(x + (bw + 4f) * 2f, y, bw, 30f), "看窗", _button)) Snap("角窗与窗帘", 1.90f, 1.62f, 0.90f, new Vector3(-3.20f, 1.45f, 2.30f));
            y += 31f;
            if (GUI.Button(new Rect(x, y, bw, 30f), "看吊灯", _button)) Snap("主吊灯", 3.10f, 1.45f, -2.55f, new Vector3(-0.10f, 2.25f, 0.20f));
            if (GUI.Button(new Rect(x + bw + 4f, y, bw, 30f), "看桌子", _button)) Snap("中央圆桌", 0f, 1.16f, 1.15f, new Vector3(0f, 0.85f, -1.30f));
            if (GUI.Button(new Rect(x + (bw + 4f) * 2f, y, bw, 30f), "看空调", _button)) Snap("立式空调", 1.20f, 1.55f, -0.60f, new Vector3(3.20f, 1.20f, -2.05f));
        }

        static string CurtainText(DeviceController d)
            => (d.on && d.value > 0.02f) ? "展开 " + Mathf.RoundToInt(d.value * 100f) + "%" : "已收拢";

        void Toggle(string id)
        {
            room.Toggle(id);
        }

        void Snap(string label, float ex, float ey, float ez, Vector3 look)
        {
            if (room.orbit == null) return;
            room.orbit.SnapLook(new Vector3(ex, ey, ez), look);
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
            GUI.Label(new Rect(x, y, _helpBar.width - 24f, 18f), "左键单击设备：开关　快捷键 1主灯 2空调 3全部窗帘 4门(只读) 5控制屏", _small); y += 17f;
            GUI.Label(new Rect(x, y, _helpBar.width - 24f, 18f), "F/G/H：温度/光照/烟雾叠加　B：气泡　R：恢复默认", _small); y += 17f;
            GUI.Label(new Rect(x, y, _helpBar.width - 24f, 18f),
                "FPS " + Mathf.RoundToInt(_fps) + "　设备 " + room.devices.Count + " 个", _small);
        }

        void DrawBubbles()
        {
            // 每个气泡都贴在它自己那台传感器的位置（只朝房间内侧让开一点点），
            // 不再让它们飘在房间中间。坐标取自 Dev.cs 里传感器的实际摆放：
            //   温湿度   北墙 (-3.10, 1.46,  2.945)
            //   CO₂      南墙 (-2.05, 1.50, -2.945)
            //   光照     后墙 ( 3.945, 2.86, -2.30)
            //   烟雾     吊顶 (-2.60, 3.165, -2.20)
            //   人体感应  吊顶 (-1.20, 3.165,  1.10)
            Bubble(new Vector3(-3.10f, 1.46f, Dim.HZ - 0.30f), "温湿度",
                room.temp.ToString("0.0") + "℃  " + room.humidity.ToString("0") + "%", TempBar(room.temp));
            Bubble(new Vector3(Dim.HX - 0.30f, 2.86f, -2.30f), "光照",
                Mathf.RoundToInt(room.lux) + " lx  /  " + Mathf.RoundToInt(room.SampleLuxAt(new Vector3(3.2f, 1f, 1.6f))) + " lx",
                _barLux);
            Bubble(new Vector3(-2.05f, 1.50f, -Dim.HZ + 0.30f), "CO₂ / 空气",
                Mathf.RoundToInt(room.co2) + " ppm   PM2.5 " + room.pm25.ToString("0"), _barCo2);
            Bubble(new Vector3(-2.60f, Dim.Height - 0.26f, -2.20f), "烟雾",
                (room.smoke * 100f).ToString("0.0") + " %obs/m",
                room.smoke > 0.35f ? _barAlarm : _barSmoke);
            Bubble(new Vector3(-1.20f, Dim.Height - 0.26f, 1.10f), "人体感应",
                room.occupancy + " 人在室", _sensorAccent);
            Bubble(new Vector3(Dim.WallDoor + 0.32f, 1.62f, 1.60f), "入口环境面板",
                room.temp.ToString("0.0") + "℃ / " + room.humidity.ToString("0") + "% / PM2.5 " + room.pm25.ToString("0"),
                _accent);
            Bubble(new Vector3(Dim.HX - 0.55f, 1.55f, 0.10f), "空调 · 设定温度",
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
            GUI.Label(new Rect(x + nameW + 2f, y + 4f, valW, 20f), text != null ? text(d) : "", _valueSmall);

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
