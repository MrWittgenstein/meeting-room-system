using UnityEngine;
using UnityEngine.InputSystem;

namespace SmartRoom
{
    /// <summary>
    /// 会议室总控制器：统一管理设备状态、模拟传感器数据、交互点选与视觉联动。
    /// 对外提供一套简单的开关/设定接口，后续接 Spring Boot 时只需替换数据来源。
    /// </summary>
    [DisallowMultipleComponent]
    [DefaultExecutionOrder(-100)]
    public class RoomController : MonoBehaviour
    {
        public static RoomController Instance { get; private set; }

        // ---------------------------------------------------------------- 设备

        public System.Collections.Generic.List<DeviceController> devices =
            new System.Collections.Generic.List<DeviceController>();

        readonly System.Collections.Generic.Dictionary<string, DeviceController> _byId =
            new System.Collections.Generic.Dictionary<string, DeviceController>();

        // ---------------------------------------------------------------- 布线/引用

        public Devices.Refs refs;
        public OrbitCamera orbit;
        public Camera cam;
        public RoomHud hud;   // 由 HUD 在运行时回填，便于反向查找

        // ---------------------------------------------------------------- 树莓派实时数据（WebSocket）

        [Header("树莓派数据源")]
        [Tooltip("树莓派 WebSocket 地址")]
        public string piUrl = "ws://192.168.43.232:8775/";

        [Tooltip("开启后：温度/湿度/光照/门状态来自树莓派；关闭则恢复本地模拟")]
        public bool useLiveData = true;

        [Tooltip("超过这个秒数没收到数据就判定为离线（回到模拟/显示离线）")]
        public float liveTimeoutSeconds = 12f;

        [Header("光照映射")]
        [Tooltip("lx 下限：对应最暗的场景亮度")]
        public float luxSceneMin = 150f;

        [Tooltip("lx 上限：对应最亮的场景亮度")]
        public float luxSceneMax = 600f;

        [Header("人数")]
        public int maxOccupancy = 12;

        /// <summary>WebSocket 客户端（运行时代码自动创建，不需要手动挂载）。</summary>
        public PiWebSocketClient pi { get; private set; }

        /// <summary>当前是否有新鲜的实时数据（5 秒内有收到帧）。</summary>
        public bool HasLiveData { get; private set; }

        /// <summary>硬件上报的门是否打开。</summary>
        public bool DoorOpenLive { get; private set; }

        /// <summary>硬件上报的实测温度（℃），空调数显用它。</summary>
        public float MeasuredTemp { get; private set; } = 24f;

        /// <summary>最近一帧里的采样时间字符串（yyyy-MM-dd HH:mm:ss）。</summary>
        public string PiLastTimestamp { get; private set; } = "";

        /// <summary>最近一帧里的 ADC 原始光照值（0-255），仅用于核对。</summary>
        public int PiLightRaw { get; private set; }

        /// <summary>本帧之前门是否已打开（用于 closed→open 边沿计数）。</summary>
        bool _prevDoorOpen;
        bool _doorStateInitialized;
        bool _piHooked;
        bool _hasPiFrame;

        AcDigitDisplay _acDigits;
        Light _sunLight;
        Light _fillLight;
        float _sceneLuxTarget = 0.5f;
        float _sceneLuxCurrent = 0.5f;

        // ---------------------------------------------------------------- 模拟传感器数据

        [Header("模拟环境数据（后续可替换为树莓派实时数据）")]
        public float temp = 24.6f;        // ℃
        public float humidity = 52f;      // %
        public float pm25 = 18f;          // μg/m³
        public float co2 = 620f;          // ppm
        public float lux = 420f;          // lx
        public float smoke = 0.02f;       // % obs/m
        public int occupancy = 6;         // 人

        // 空间化展示用的分布（长边 6 个采样区、短边 4 个）
        readonly float[] _tempField = new float[24];
        readonly float[] _luxField = new float[24];

        float _targetTemp = 24f;
        float _acRamp = 0f;

        // ---------------------------------------------------------------- 可视化开关

        [Header("可视化")]
        public bool showThermalOverlay = false;
        public bool showLuxField = false;
        public bool showSmokeField = false;
        public bool showBubbles = true;
        public bool showHelp = true;

        [Header("启动默认状态")]
        [Tooltip("进入 Play 时主吊灯保持关闭")]
        public bool startWithMainLightOff = true;

        [Tooltip("进入 Play 时不显示悬浮数据气泡")]
        public bool startWithBubblesHidden = true;

        // 启动机位固定在室内（对应"全景"预设，见 RoomHud.SnapView）
        static readonly Vector3 StartEye = new Vector3(3.25f, 1.72f, 2.30f);
        static readonly Vector3 StartLook = new Vector3(-0.80f, 1.00f, -0.90f);

        // ---------------------------------------------------------------- 交互状态

        public DeviceController hovered;
        public string lastAction = "";
        float _lastActionTime = -999f;

        // ---------------------------------------------------------------- 内部

        GameObject _thermalQuad;
        GameObject _fieldQuad;
        Material _thermalMat;
        Material _fieldMat;
        Texture2D _thermalTex;
        Vector3 _screenHitCenter;
        Vector3 _screenHitSize;
        float _clickGuardUntil;
        int _doorDiagLogged;
        static readonly int OffsetId = Shader.PropertyToID("_Offset");

        // ================================================================= 生命周期

        void Awake()
        {
            Instance = this;

            // 场景被重新打开后，运行时字段是空的，必须在这里重建：
            // 先按名字解析视觉引用，再收集设备，否则幕布/灯光联动会全部失效。
            ResolveVisualRefs();
            RefreshDeviceRegistry();

            foreach (var d in devices)
                if (d != null) d.ApplyInitial();

            ApplyStartupDefaults();

            _thermalTex = TextureLib.ThermalRamp();

            for (int i = 0; i < _tempField.Length; i++)
            {
                _tempField[i] = temp + Random.Range(-0.55f, 0.75f);
                _luxField[i] = lux * Random.Range(0.72f, 1.18f);
            }

            var ac = Get("ac");
            if (ac != null) _targetTemp = Mathf.Clamp(ac.value, 16f, 30f);

            SetupLiveData();
            BuildFields();
            ApplyVisuals();
        }

        /// <summary>
        /// 场景构建完成后由构建器调用；场景被重新打开/进入运行模式时也会通过 Awake 调用。
        /// 关键：不依赖 Devices.Build 返回的结构体引用（它不能可靠地序列化进场景），
        /// 而是在这里按对象名字从场景里解析出来。
        /// </summary>
        public void Initialize()
        {
            RefreshDeviceRegistry();
            ResolveVisualRefs();
            BuildFields();

            var ac = Get("ac");
            if (ac != null) _targetTemp = Mathf.Clamp(ac.value, 16f, 30f);

            SetupLiveData();
            ApplyVisuals();
            Log("系统就绪：已注册 " + devices.Count + " 个可交互设备");
        }

        /// <summary>
        /// 启动默认：主吊灯保持关闭、悬浮气泡不显示。
        /// 注意这些都写在运行时而不是只改字段默认值——场景里已经序列化过旧值，
        /// 只改字段初始值对已保存的场景不生效。
        /// </summary>
        void ApplyStartupDefaults()
        {
            if (startWithMainLightOff)
            {
                var lightDev = Get("mainlight");
                if (lightDev != null)
                {
                    lightDev.on = false;
                    lightDev.initialOn = false;   // 「恢复默认」后也保持关闭
                    lightDev.value = 1f;          // 亮度保留 100%，开灯即全亮
                }
            }

            if (startWithBubblesHidden) showBubbles = false;
        }

        /// <summary>
        /// 进 Play 时把相机摆到室内机位。
        /// 场景里序列化的 yaw/pitch/distance 是早期"室外总览"的参数，
        /// 按 pos = pivot - forward*distance 算出来会跑到屋顶上方，所以这里强制纠正一次。
        /// </summary>
        void EnsureInteriorCamera()
        {
            if (orbit == null) return;

            orbit.SnapLook(StartEye, StartLook);
            orbit.ApplyNow();
        }

        void Start()
        {
            EnsureInteriorCamera();
        }

        /// <summary>建立 id → 设备 的索引。若序列化的列表为空则重新在场景里查找。</summary>
        void RefreshDeviceRegistry()
        {
            _byId.Clear();

            if (devices == null) devices = new System.Collections.Generic.List<DeviceController>();
            devices.RemoveAll(d => d == null);

            if (devices.Count == 0)
            {
#if UNITY_2023_1_OR_NEWER
                var found = Object.FindObjectsByType<DeviceController>(FindObjectsSortMode.None);
#else
                var found = Object.FindObjectsOfType<DeviceController>();
#endif
                foreach (var d in found)
                    if (!devices.Contains(d)) devices.Add(d);
            }

            foreach (var d in devices)
                if (d != null && !string.IsNullOrEmpty(d.id) && !_byId.ContainsKey(d.id))
                    _byId[d.id] = d;
        }

        /// <summary>按名字在场景里找回所有需要驱动的视觉对象。</summary>
        void ResolveVisualRefs()
        {
            var map = new System.Collections.Generic.Dictionary<string, Transform>();
            foreach (var t in Object.FindObjectsByType<Transform>(FindObjectsSortMode.None))
                if (!string.IsNullOrEmpty(t.name) && !map.ContainsKey(t.name))
                    map[t.name] = t;

            Transform Find(string n) => map.TryGetValue(n, out var t) ? t : null;

            // ===== 引用解析：全部在运行时按名字/层级查找 =====
            // 不再依赖序列化的组件引用（DeviceController 上带非空 Transform 引用会导致
            // 该组件在场景重载后整体解析失败，详见 DeviceController 里的注释）。
            refs.doorRoot = ResolveDoorRoot(Find);

            refs.curtainLeft = Find("窗帘_左");
            refs.curtainRight = Find("窗帘_右");

            var panel = Find("面板_屏");
            refs.panelScreen = panel != null ? panel.GetComponent<MeshRenderer>() : null;

            var acPanel = Find("空调控制器_屏");
            refs.acIndicator = acPanel != null ? acPanel.GetComponent<MeshRenderer>() : null;
            // 落地空调本体的运行指示灯（优先用它表示空调状态，更直观）
            var acLed = Find("空调面板指示灯");
            refs.acUnitLed = acLed != null ? acLed.GetComponent<MeshRenderer>() : null;
            var acDigit = Find("空调面板数显");
            refs.acDisplay = acDigit != null ? acDigit.GetComponent<MeshRenderer>() : null;

            var lightGo = Find("主灯光源");
            refs.chandelierLight = lightGo != null ? lightGo.GetComponent<Light>() : null;

            var chandelier = Find("灯体_扩散板");
            refs.chandelierPanel = chandelier != null ? chandelier.GetComponent<MeshRenderer>() : null;

            // 传感器节点（用于悬浮气泡定位）
            var nodes = new System.Collections.Generic.List<Transform>();
            foreach (var n in new[] { "传感器_温湿度", "传感器_光照", "传感器_烟雾", "传感器_人体感应" })
            {
                var t = Find(n);
                if (t != null) nodes.Add(t);
            }
            refs.sensorNodes = nodes.ToArray();
        }

        /// <summary>
        /// 找门扇：优先按名字；找不到就按"离门设备最近、且没有 DeviceController 的同级物体"来找。
        /// 这样即使场景里对象被改名，门依然能动。
        /// </summary>
        Transform ResolveDoorRoot(System.Func<string, Transform> find)
        {
            var byName = find("门扇");
            if (byName != null) return byName;

            var doorDev = Get("door");
            if (doorDev == null) return null;

            // 门扇与门设备拥有同一个父物体（房间壳体），取同级里名字最像门扇的那个
            var parent = doorDev.transform.parent;
            if (parent == null) return null;

            Transform best = null;
            float bestScore = float.MaxValue;
            foreach (var t in parent.GetComponentsInChildren<Transform>(true))
            {
                if (t == parent) continue;
                if (t.GetComponent<DeviceController>() != null) continue;
                if (t.name.StartsWith("设备_")) continue;
                if (t.GetComponent<MeshRenderer>() == null) continue;
                // 优先名字里带"门"的，其次比距离
                float score = t.name.Contains("门") ? 0f : 1f;
                score += Mathf.Abs(t.position.x - doorDev.transform.position.x);
                if (score < bestScore)
                {
                    bestScore = score;
                    best = t;
                }
            }
            return best;
        }

        void Update()
        {
            HandleInput();
            AnimateDevices();
            UpdateSensors(Time.deltaTime);
            UpdateFields(Time.deltaTime);
            TickLiveScene(Time.deltaTime);
        }

        // ================================================================= 树莓派实时数据

        /// <summary>
        /// 建立 WebSocket 数据源。不需要在场景里手动挂 PiWebSocketClient：
        /// 找不到就运行时创建一个，这样场景不用重新生成也能跑。
        /// </summary>
        void SetupLiveData()
        {
            if (!useLiveData) return;

            if (pi == null)
            {
                pi = Object.FindFirstObjectByType<PiWebSocketClient>();
                if (pi == null)
                {
                    var go = new GameObject("树莓派数据源");
                    pi = go.AddComponent<PiWebSocketClient>();
                }
                if (!string.IsNullOrEmpty(piUrl)) pi.url = piUrl;
            }

            if (!_piHooked)
            {
                _piHooked = true;
                pi.FrameReceived += OnPiFrame;
                // 人数从 0 开始，之后只在"门被打开"的边沿累加
                occupancy = 0;
                _doorStateInitialized = false;
                ResolveSceneLights();
            }
        }

        /// <summary>找到场景里的主日光与天光补光，供光照强度映射使用。</summary>
        void ResolveSceneLights()
        {
            _sunLight = null;
            _fillLight = null;

            foreach (var l in Object.FindObjectsByType<Light>(FindObjectsSortMode.None))
            {
                if (l == null || l.type != LightType.Directional) continue;

                if (l.name.Contains("日光") && _sunLight == null) { _sunLight = l; continue; }
                if (_fillLight == null) _fillLight = l;
            }

            // 兜底：如果命名不匹配，就按强度大小取前两个方向光
            if (_sunLight == null || _fillLight == null)
            {
                var list = new System.Collections.Generic.List<Light>();
                foreach (var l in Object.FindObjectsByType<Light>(FindObjectsSortMode.None))
                    if (l != null && l.type == LightType.Directional) list.Add(l);
                list.Sort((a, b) => b.intensity.CompareTo(a.intensity));
                if (_sunLight == null && list.Count > 0) _sunLight = list[0];
                if (_fillLight == null && list.Count > 1) _fillLight = list[1];
            }
        }

        /// <summary>收到一帧（主线程回调）。</summary>
        void OnPiFrame(PiFrame frame)
        {
            var c = frame != null ? frame.current : null;
            if (c == null) return;

            _hasPiFrame = true;

            // ---- 温湿度 / 光照 ----
            MeasuredTemp = c.temperature;
            temp = c.temperature;
            humidity = Mathf.Clamp(c.humidity, 0f, 100f);
            lux = Mathf.Max(0f, c.light);
            PiLightRaw = c.light_raw;
            PiLastTimestamp = c.timestamp ?? "";

            // ---- 门状态 + 人数 ----
            bool open = string.Equals((c.door_state ?? "").Trim(), "open", System.StringComparison.OrdinalIgnoreCase);

            if (!_doorStateInitialized)
            {
                // 首帧只记录基线：刚连上时门若已经开着，不算一次"开门事件"
                _doorStateInitialized = true;
                _prevDoorOpen = open;
            }
            else
            {
                if (open && !_prevDoorOpen)
                {
                    occupancy = Mathf.Min(maxOccupancy, occupancy + 1);
                    Log("检测到开门，在室人数 → " + occupancy + " 人");
                }
                _prevDoorOpen = open;
            }

            DoorOpenLive = open;

            // 让门设备状态跟随硬件（AnimateDevices 会按 door.on 平滑转到开门/关门角度）
            var doorDev = Get("door");
            if (doorDev != null && doorDev.on != open) doorDev.on = open;

            // ---- 光照 → 场景亮度 ----
            float t = Mathf.InverseLerp(luxSceneMin, luxSceneMax, lux);
            _sceneLuxTarget = Mathf.Clamp01(t);

            // ---- 空调数显 ----
            UpdateAcDigits();
        }

        /// <summary>每帧：把光照映射平滑地应用到场景灯光上，并维护数据在线状态。</summary>
        void TickLiveScene(float dt)
        {
            if (!useLiveData) return;

            HasLiveData = pi != null && pi.HasFreshData(liveTimeoutSeconds);
            if (!HasLiveData) return;

            // 一帧一个值会让灯光跳变，这里做指数平滑（约 0.3 秒稳定）
            _sceneLuxCurrent = Mathf.Lerp(_sceneLuxCurrent, _sceneLuxTarget, 1f - Mathf.Exp(-4f * dt));

            if (_sunLight != null) _sunLight.intensity = Mathf.Lerp(0.45f, 2.60f, _sceneLuxCurrent);
            if (_fillLight != null) _fillLight.intensity = Mathf.Lerp(0.25f, 1.55f, _sceneLuxCurrent);
            RenderSettings.ambientIntensity = Mathf.Lerp(0.65f, 1.90f, _sceneLuxCurrent);
        }

        /// <summary>把实测温度画到空调数显面板上。</summary>
        void UpdateAcDigits()
        {
            // 还没收到任何一帧时保持原来的发光色块，避免数显上先亮一个假温度
            if (!useLiveData || !_hasPiFrame || refs.acDisplay == null) return;

            if (_acDigits == null)
            {
                try { _acDigits = new AcDigitDisplay(refs.acDisplay); }
                catch (System.Exception e)
                {
                    Debug.LogWarning("[MeetingRoom] 空调数显初始化失败: " + e.Message);
                    return;
                }
            }

            var ac = Get("ac");
            _acDigits.SetValue(MeasuredTemp, ac == null || ac.on);
        }

        // ================================================================= 对外接口（接后端时替换这里）

        public DeviceController Get(string id)
        {
            return _byId.TryGetValue(id, out var d) ? d : null;
        }

        public bool IsOn(string id)
        {
            var d = Get(id);
            return d != null && d.on;
        }

        public float GetValue(string id)
        {
            var d = Get(id);
            return d != null ? d.value : 0f;
        }

        /// <summary>设置设备开关。返回是否真的发生了变化。</summary>
        public bool SetDeviceOn(string id, bool on)
        {
            var d = Get(id);
            if (d == null || d.on == on) return false;
            d.on = on;
            if (d.kind == DeviceKind.AirConditioner)
            {
                _targetTemp = Mathf.Clamp(d.value, 16f, 30f);
                if (on) _acRamp = 0f;
            }
            Log((on ? "开启 " : "关闭 ") + d.displayName);
            ApplyVisuals();
            return true;
        }

        /// <summary>设置设备连续量（主灯亮度 0..1 / 空调温度 16..30 / 窗帘开合 0..1）。</summary>
        public void SetDeviceValue(string id, float value)
        {
            var d = Get(id);
            if (d == null) return;
            switch (d.kind)
            {
                case DeviceKind.AirConditioner: d.value = Mathf.Clamp(value, 16f, 30f); _targetTemp = d.value; break;
                default: d.value = Mathf.Clamp01(value); break;
            }
            ApplyVisuals();
        }

        public void Toggle(string id)
        {
            // 门由树莓派（舵机）控制，面板/点击/快捷键一律不改门状态，只跟随硬件上报
            if (id == "door" && useLiveData)
            {
                Log(HasLiveData
                    ? "门由硬件控制（" + (DoorOpenLive ? "已开" : "已关") + "），面板只读"
                    : "门由硬件控制，等待树莓派数据…");
                return;
            }

            var d = Get(id);
            if (d != null) SetDeviceOn(id, !d.on);
        }

        public void Log(string msg)
        {
            lastAction = msg;
            _lastActionTime = Time.unscaledTime;
        }

        // ================================================================= 交互

        void HandleInput()
        {
            var mouse = Mouse.current;
            var kb = Keyboard.current;
            if (mouse == null || cam == null) return;

            var screenPos = mouse.position.ReadValue();
            hovered = PickDevice(screenPos);

            // 点击（排除拖拽与刚点完 HUD 的情况）
            if (mouse.leftButton.wasPressedThisFrame) _clickGuardUntil = Time.unscaledTime + 0.12f;

            if (mouse.leftButton.wasReleasedThisFrame && Time.unscaledTime > _clickGuardUntil)
            {
                if (hovered != null)
                {
                    Toggle(hovered.id);
                }
                else if (Physics.Raycast(cam.ScreenPointToRay(screenPos), out var hit, 100f))
                {
                    Log("点选：" + hit.collider.name);
                }
            }

            if (kb != null)
            {
                // 1 键改成电动门（投影已移除）
                if (kb.digit1Key.wasPressedThisFrame) Toggle("door");
                if (kb.digit2Key.wasPressedThisFrame) Toggle("mainlight");
                if (kb.digit3Key.wasPressedThisFrame) Toggle("ac");
                // 4 键：窗帘在"全关(0)"和"全开(1)"之间切换。
                // 注意不能只取反 on，否则视觉上开合值直接归零，用户会觉得"按了没反应"。
                if (kb.digit4Key.wasPressedThisFrame) ToggleCurtain();
                if (kb.digit5Key.wasPressedThisFrame) Toggle("door");
                if (kb.fKey.wasPressedThisFrame) showThermalOverlay = !showThermalOverlay;
                if (kb.gKey.wasPressedThisFrame) showLuxField = !showLuxField;
                if (kb.hKey.wasPressedThisFrame) showSmokeField = !showSmokeField;
                if (kb.bKey.wasPressedThisFrame) showBubbles = !showBubbles;
                if (kb.rKey.wasPressedThisFrame) ResetAll();
            }
        }

        /// <summary>窗帘在"全开"和"全关"之间切换。</summary>
        public void ToggleCurtain()
        {
            var cur = Get("curtain");
            if (cur == null) return;

            bool currentlyOpen = cur.on && cur.value > 0.02f;
            SetDeviceOn("curtain", !currentlyOpen);
            SetDeviceValue("curtain", currentlyOpen ? 0f : 1f);
            Log(currentlyOpen ? "关闭 电动窗帘" : "开启 电动窗帘");
        }

        DeviceController PickDevice(Vector2 screenPos)
        {
            var ray = cam.ScreenPointToRay(screenPos);
            if (!Physics.Raycast(ray, out var hit, 100f)) return null;
            var dc = hit.collider.GetComponentInParent<DeviceController>();
            if (dc == null) dc = hit.collider.GetComponent<DeviceController>();

            // 更新悬停描边
            foreach (var d in devices)
                if (d != null) d.SetHover(d == dc);

            return dc;
        }

        public void ResetAll()
        {
            foreach (var d in devices)
            {
                if (d == null) continue;
                d.ApplyInitial();
            }
            _targetTemp = Mathf.Clamp(GetValue("ac"), 16f, 30f);
            _acRamp = 0f;
            Log("已恢复默认状态");
            ApplyVisuals();
        }

        // ================================================================= 动画与视觉联动

        void AnimateDevices()
        {
            float dt = Time.deltaTime;
            float k = 1f - Mathf.Exp(-7f * dt);

            // （投影幕布与投影仪已移除）

            // 电动窗帘：两片布面从中央向两侧收拢（value 1=全开，0=全关）
            // 两片都用"正缩放"：几何以各自锚点为中心对称建立，
            // 所以左片锚在左窗框、右片锚在右窗框，缩小即是各自向外收拢。
            var cur = Get("curtain");
            if (cur != null)
            {
                float openness = cur.on ? Mathf.Clamp01(cur.value) : 0f;
                // 全开时收拢到 6% 宽度，全关时铺满整个窗宽（两片在中线相接）
                float sx = Mathf.Lerp(1f, 0.06f, openness);
                if (refs.curtainLeft != null)
                    refs.curtainLeft.localScale = new Vector3(sx, 1f, 1f);
                if (refs.curtainRight != null)
                    refs.curtainRight.localScale = new Vector3(sx, 1f, 1f);
            }

            // 空调温度平滑趋近
            if (Get("ac") != null && IsOn("ac"))
                _acRamp = Mathf.Lerp(_acRamp, 1f, 1f - Mathf.Exp(-0.8f * dt));
            else
                _acRamp = Mathf.Lerp(_acRamp, 0f, 1f - Mathf.Exp(-0.8f * dt));

            // 电动门：绕门轴（本地 Y 竖轴）旋转开关。
            // 门扇几何严格沿本地 +Z 建立，铰链在本地原点，所以：
            //   yaw = 0    → 门扇沿 +Z 摆放（关门）
            //   yaw = -88° → 门扇自由端转向 +X（即向房间内侧旋开）
            var door = Get("door");
            if (door != null)
            {
                // 兜底：每帧检查一次，引用丢了就地重新查找
                if (refs.doorRoot == null) refs.doorRoot = FindDoorRootFallback(door);

                if (refs.doorRoot != null)
                {
                    float target = door.on ? RoomDim.DoorOpenYaw : RoomDim.DoorClosedYaw;
                    float curY = refs.doorRoot.localRotation.eulerAngles.y;
                    if (curY > 180f) curY -= 360f;
                    float next = Mathf.LerpAngle(curY, target, 1f - Mathf.Exp(-6f * dt));
                    refs.doorRoot.localRotation = Quaternion.Euler(0f, next, 0f);
                }
                else if (_doorDiagLogged < 1)
                {
                    _doorDiagLogged++;
                    Debug.LogWarning("[MeetingRoom] 门扇引用为 null，门无法动画。");
                }
            }
        }

        /// <summary>门扇引用的运行时兜底查找（按门设备位置就近找）。</summary>
        Transform FindDoorRootFallback(DeviceController doorDev)
        {
            var parent = doorDev.transform.parent;
            if (parent == null) return null;

            Transform best = null;
            float bestScore = float.MaxValue;
            foreach (var t in parent.GetComponentsInChildren<Transform>(true))
            {
                if (t == parent || t == doorDev.transform) continue;
                if (t.GetComponent<DeviceController>() != null) continue;
                if (t.name.StartsWith("设备_")) continue;
                if (t.GetComponent<MeshRenderer>() == null) continue;
                float score = (t.name.Contains("门") ? 0f : 1f)
                            + Mathf.Abs(t.position.x - doorDev.transform.position.x)
                            + Mathf.Abs(t.position.z - doorDev.transform.position.z);
                if (score < bestScore) { bestScore = score; best = t; }
            }
            return best;
        }

        void UpdateSensors(float dt)
        {
            var ac = Get("ac");
            float acOn = _acRamp;
            var cur = Get("curtain");
            float open = cur != null ? (cur.on ? Mathf.Clamp01(cur.value) : 0f) : 1f;

            // 温度/湿度/光照：
            //   · useLiveData=false → 完全本地模拟（老行为）
            //   · useLiveData=true  → 只由树莓派帧更新；断线时**冻结最后值**。
            //     若断线后偷偷回到模拟，面板数字会自己缓慢漂移，看起来像"数据还在更新"，很误导。
            if (!useLiveData)
            {
                // 空调把室温往设定值拉；没人时轻微回升
                float target = Mathf.Lerp(26.8f, _targetTemp, acOn);
                temp = Mathf.Lerp(temp, target, 1f - Mathf.Exp(-0.12f * dt));
                temp += Mathf.Sin(Time.time * 0.23f) * 0.0015f;

                // 湿度：空调制冷会除湿
                float humTarget = Mathf.Lerp(58f, 45f, acOn);
                humidity = Mathf.Lerp(humidity, humTarget, 1f - Mathf.Exp(-0.10f * dt));

                // 光照：窗帘关闭则自然光减少，主灯开启则增加
                float artificial = GetValue("mainlight") * (IsOn("mainlight") ? 1f : 0f);
                float luxTarget = 90f + open * 560f + artificial * 380f;
                lux = Mathf.Lerp(lux, Mathf.Max(20f, luxTarget), 1f - Mathf.Exp(-1.4f * dt));
            }

            // 烟雾与空气质量：这次没有接真实数据，继续本地模拟
            smoke = Mathf.Clamp01(smoke + (Mathf.PerlinNoise(Time.time * 0.05f, 3.7f) - 0.5f) * 0.004f);
            pm25 = Mathf.Lerp(pm25, 12f + (1f - open) * 14f + occupancy * 0.7f, 1f - Mathf.Exp(-0.2f * dt));
            co2 = Mathf.Lerp(co2, 430f + occupancy * 42f, 1f - Mathf.Exp(-0.15f * dt));
        }

        void UpdateFields(float dt)
        {
            for (int i = 0; i < _tempField.Length; i++)
            {
                // 区域温度围绕实测值波动；靠近空调出风口更接近设定温度
                int ix = i % 6, iz = i / 6;
                float bias = Mathf.Lerp(-0.7f, 0.9f, iz / 3f) + Mathf.Sin(ix * 1.1f + iz * 0.7f) * 0.25f;
                float target = temp + bias + (IsOn("ac") ? -0.35f : 0f);
                _tempField[i] = Mathf.Lerp(_tempField[i], target, 1f - Mathf.Exp(-0.6f * dt));
                // 光照场：窗侧强、内侧弱
                float lx = Mathf.Lerp(0.55f, 1.35f, 1f - iz / 3f);
                _luxField[i] = Mathf.Lerp(_luxField[i], lux * lx, 1f - Mathf.Exp(-1.2f * dt));
            }

            if (_thermalMat != null)
            {
                // 用偏移动画表现缓慢流动的温度分布
                float scroll = Time.time * 0.012f;
                _thermalMat.SetVector(OffsetId, new Vector4(scroll, scroll * 0.6f, 0f, 0f));
                float t = Mathf.InverseLerp(18f, 30f, temp);
                _thermalMat.color = new Color(1f, 1f, 1f, showThermalOverlay ? 0.20f : 0f);
                _thermalMat.SetFloat("_AlphaBoost", t);
            }
            if (_fieldMat != null)
            {
                float scroll = -Time.time * 0.02f;
                _fieldMat.SetVector(OffsetId, new Vector4(scroll, scroll * 0.4f, 0f, 0f));
                _fieldMat.color = new Color(1f, 1f, 1f, (showLuxField || showSmokeField) ? 0.22f : 0f);
            }
        }

        /// <summary>把所有设备状态写进具体视觉表现（灯光、发光、缩放、贴图）。</summary>
        public void ApplyVisuals()
        {
            // ---- 主灯 ----
            var mainLight = Get("mainlight");
            if (mainLight != null && refs.chandelierLight != null)
            {
                float dim = mainLight.on ? Mathf.Clamp01(mainLight.value) : 0f;
                refs.chandelierLight.intensity = dim * 9.5f;
                refs.chandelierLight.shadows = dim > 0.35f ? LightShadows.Soft : LightShadows.None;
            }
            if (mainLight != null && refs.chandelierPanel != null)
            {
                float dim = mainLight.on ? Mathf.Clamp01(mainLight.value) : 0f;
                SetEmission(refs.chandelierPanel, new Color(1f, 0.955f, 0.885f), 0.05f + dim * 1.25f);
            }

            // （投影幕布与投影仪的相关视觉逻辑已随设备一并移除）

            // ---- 空调（落地柜机）：指示灯与数字屏随状态/设定温度变色 ----
            var acDev = Get("ac");
            bool acOn = acDev != null && acDev.on;
            float tNorm = acDev != null ? Mathf.InverseLerp(16f, 30f, acDev.value) : 0.5f;
            var acColor = acOn
                ? Color.Lerp(new Color(0.35f, 0.75f, 1f), new Color(1f, 0.45f, 0.30f), tNorm)
                : new Color(0.35f, 0.37f, 0.40f);

            if (refs.acUnitLed != null)
                SetEmission(refs.acUnitLed, acColor, acOn ? 2.8f : 0.03f);
            if (refs.acDisplay != null)
                SetEmission(refs.acDisplay, acOn ? new Color(0.34f, 0.86f, 1f) : new Color(0.12f, 0.13f, 0.15f),
                    acOn ? 2.2f : 0.02f);
            if (refs.acIndicator != null)
                SetEmission(refs.acIndicator, acColor, acOn ? 2.0f : 0.03f);

            // 空调数显：有实时数据后换成"实测温度"的数字贴图
            UpdateAcDigits();

            // ---- 环境面板屏幕 ----
            if (refs.panelScreen != null)
            {
                var panel = Get("panel");
                bool on = panel == null || panel.on;
                SetEmission(refs.panelScreen, on ? new Color(0.36f, 0.72f, 0.95f) : new Color(0.08f, 0.10f, 0.12f),
                    on ? 0.85f : 0.02f);
            }

            // ---- 空间化显示层 ----
            if (_thermalQuad != null) _thermalQuad.SetActive(true);
            if (_fieldQuad != null) _fieldQuad.SetActive(showLuxField || showSmokeField);
        }

        static void SetEmission(MeshRenderer r, Color c, float intensity)
        {
            if (r == null) return;
            var mpb = new MaterialPropertyBlock();
            r.GetPropertyBlock(mpb);
            mpb.SetColor(MatLib.EmissionColorId, c * intensity);
            r.SetPropertyBlock(mpb);
            // 同时保证材质开启了自发光关键字
            var m = r.sharedMaterial;
            if (m != null && !m.IsKeywordEnabled("_EMISSION"))
            {
                m.EnableKeyword("_EMISSION");
                m.globalIlluminationFlags = MaterialGlobalIlluminationFlags.RealtimeEmissive;
            }
        }

        // ================================================================= 空间化显示层

        void BuildFields()
        {
            // 防止重复创建（首次构建时 Awake 与 Initialize 都会走到这里）
            if (_thermalQuad != null)
            {
                Object.DestroyImmediate(_thermalQuad);
                _thermalQuad = null;
            }
            if (_fieldQuad != null)
            {
                Object.DestroyImmediate(_fieldQuad);
                _fieldQuad = null;
            }

            // 温度热力层（覆盖会议区）
            _thermalMat = MakeFieldMaterial("m_field_thermal", _thermalTex);
            _thermalQuad = MakeFieldQuad("温度空间化层", _thermalMat, new Vector3(0f, 0.035f, 0f),
                new Vector3(7.4f, 5.4f));

            // 光照/烟雾层（用同一张热力贴图，不同透明度与流动方向）
            _fieldMat = MakeFieldMaterial("m_field_lux", _thermalTex);
            _fieldQuad = MakeFieldQuad("光照与烟雾空间化层", _fieldMat, new Vector3(0f, 0.055f, 0f),
                new Vector3(7.4f, 5.4f));
            _fieldQuad.SetActive(false);
        }

        Material MakeFieldMaterial(string key, Texture2D tex)
        {
            var m = new Material(Shader.Find("Universal Render Pipeline/Lit")) { name = key };
            m.SetTexture(MatLib.BaseMapId, tex);
            m.SetColor(MatLib.BaseColorId, new Color(1f, 1f, 1f, 0.32f));
            m.SetFloat(MatLib.MetallicId, 0f);
            m.SetFloat(MatLib.SmoothnessId, 0.05f);
            m.SetFloat("_Surface", 1f);
            m.SetFloat("_Blend", 0f);
            m.SetFloat("_ZWrite", 0f);
            m.SetFloat("_SrcBlend", (float)UnityEngine.Rendering.BlendMode.SrcAlpha);
            m.SetFloat("_DstBlend", (float)UnityEngine.Rendering.BlendMode.OneMinusSrcAlpha);
            m.EnableKeyword("_SURFACE_TYPE_TRANSPARENT");
            m.EnableKeyword("_ALPHAPREMULTIPLY_ON");
            m.renderQueue = (int)UnityEngine.Rendering.RenderQueue.Transparent;
            m.SetShaderPassEnabled("ShadowCaster", false);
            return m;
        }

        GameObject MakeFieldQuad(string name, Material mat, Vector3 center, Vector2 size)
        {
            var go = new GameObject(name);
            var mf = go.AddComponent<MeshFilter>();
            mf.sharedMesh = Prim.DiscMesh();
            var mr = go.AddComponent<MeshRenderer>();
            mr.sharedMaterial = mat;
            mr.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;
            mr.receiveShadows = false;
            go.transform.position = center;
            go.transform.localScale = new Vector3(size.x, 1f, size.y);
            return go;
        }

        /// <summary>取某个空间位置附近的温度（用于气泡显示）。</summary>
        public float SampleTempAt(Vector3 worldPos)
        {
            int ix = Mathf.Clamp(Mathf.RoundToInt((worldPos.x + RoomDim.HalfX) / RoomDim.Length * 5f), 0, 5);
            int iz = Mathf.Clamp(Mathf.RoundToInt((worldPos.z + RoomDim.HalfZ) / RoomDim.Width * 3f), 0, 3);
            return _tempField[iz * 6 + ix];
        }

        public float SampleLuxAt(Vector3 worldPos)
        {
            int ix = Mathf.Clamp(Mathf.RoundToInt((worldPos.x + RoomDim.HalfX) / RoomDim.Length * 5f), 0, 5);
            int iz = Mathf.Clamp(Mathf.RoundToInt((worldPos.z + RoomDim.HalfZ) / RoomDim.Width * 3f), 0, 3);
            return _luxField[iz * 6 + ix];
        }

        // ================================================================= HUD 需要的辅助

        public string LastActionText
        {
            get { return Time.unscaledTime - _lastActionTime < 5f ? lastAction : ""; }
        }

        /// <summary>用于 HUD 绘制：把世界坐标转成屏幕坐标。</summary>
        public bool WorldToScreen(Vector3 world, out Vector2 screen, out float depth)
        {
            screen = Vector2.zero;
            depth = 0f;
            if (cam == null) return false;
            var v = cam.WorldToScreenPoint(world);
            if (v.z <= 0.05f) return false;
            screen = new Vector2(v.x, Screen.height - v.y);
            depth = v.z;
            return true;
        }
    }
}
