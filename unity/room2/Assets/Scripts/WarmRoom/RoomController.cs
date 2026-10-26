using UnityEngine;
using UnityEngine.InputSystem;

namespace WarmRoom
{
    /// <summary>
    /// 会议室总控制器：设备注册、点控交互、视觉联动、模拟传感器数据。
    ///
    /// 设计上刻意规避了上一版的三个坑：
    ///   1) 所有视觉引用在**运行时按层级查找**，不在组件里序列化 Transform 引用；
    ///   2) 窗帘动画**写在动画面循环里**（上一版漏写，导致按键后窗帘不动）；
    ///   3) 门扇只做"绕本地 Y 轴旋转"，且门扇不被标记 Static（否则静态合批会冻结其顶点）。
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

        // ---------------------------------------------------------------- 引用

        public Dev.Refs refs;
        public OrbitCamera orbit;
        public Camera cam;
        public RoomHud hud;

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


        // ---------------------------------------------------------------- 模拟数据

        [Header("模拟环境数据（后续替换为树莓派实时数据）")]
        public float temp = 23.8f;      // ℃
        public float humidity = 48f;    // %
        public float pm25 = 16f;        // μg/m³
        public float co2 = 590f;        // ppm
        public float lux = 460f;        // lx
        public float smoke = 0.02f;     // %obs/m
        public int occupancy = 6;

        readonly float[] _tempField = new float[24];
        readonly float[] _luxField = new float[24];
        float _targetTemp = 24f;
        float _acRamp;

        // ---------------------------------------------------------------- 可视化开关

        [Header("可视化")]
        public bool showThermalOverlay;
        public bool showLuxField;
        public bool showSmokeField;
        public bool showBubbles = true;

        [Header("启动默认状态")]
        [Tooltip("进入 Play 时主吊灯保持关闭")]
        public bool startWithMainLightOff = true;

        [Tooltip("进入 Play 时不显示悬浮数据气泡")]
        public bool startWithBubblesHidden = true;

        // ---------------------------------------------------------------- 交互状态

        public DeviceController hovered;
        public string lastAction = "";
        float _lastActionAt = -999f;
        float _clickGuardUntil;
        int _diagLogged;

        GameObject _thermalQuad, _fieldQuad;
        Material _thermalMat, _fieldMat;
        Texture2D _thermalTex;
        static readonly int OffsetId = Shader.PropertyToID("_Offset");

        // ================================================================= 生命周期

        void Awake()
        {
            Instance = this;
            ResolveRefs();
            RefreshRegistry();
            foreach (var d in devices) if (d != null) d.ApplyInitial();

            ApplyStartupDefaults();

            _thermalTex = Tex.Thermal();
            for (int i = 0; i < _tempField.Length; i++)
            {
                _tempField[i] = temp + Random.Range(-0.5f, 0.8f);
                _luxField[i] = lux * Random.Range(0.75f, 1.15f);
            }

            var ac = Get("ac");
            if (ac != null) _targetTemp = Mathf.Clamp(ac.value, 16f, 30f);

            SetupLiveData();
            BuildFields();
            ApplyVisuals();
        }

        /// <summary>
        /// 启动默认：主吊灯关闭、悬浮气泡不显示。
        /// 运行期覆盖——场景里已经序列化过旧值，只改字段初始值对已保存的场景不生效。
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
                    lightDev.value = 1f;
                }
            }

            if (startWithBubblesHidden) showBubbles = false;
        }

        // 启动机位：与 HUD 的「全景」预设一致（室内）
        static readonly Vector3 StartEye = new Vector3(3.30f, 1.70f, 2.35f);
        static readonly Vector3 StartLook = new Vector3(-0.90f, 1.05f, -0.80f);

        /// <summary>进 Play 时把相机摆到室内机位（场景里的旧参数会把相机放到屋顶外）。</summary>
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

        public void Initialize()
        {
            ResolveRefs();
            RefreshRegistry();
            SetupLiveData();
            BuildFields();
            ApplyVisuals();
            Log("系统就绪：已注册 " + devices.Count + " 个可交互设备");
        }

        void Update()
        {
            HandleInput();
            Animate();
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

        // ================================================================= 设备注册

        void RefreshRegistry()
        {
            _byId.Clear();
            if (devices == null) devices = new System.Collections.Generic.List<DeviceController>();
            devices.RemoveAll(d => d == null);

            if (devices.Count == 0)
            {
#if UNITY_2023_1_OR_NEWER
                foreach (var d in Object.FindObjectsByType<DeviceController>(FindObjectsSortMode.None))
                    devices.Add(d);
#else
                foreach (var d in Object.FindObjectsOfType<DeviceController>())
                    devices.Add(d);
#endif
            }

            foreach (var d in devices)
                if (d != null && !string.IsNullOrEmpty(d.id) && !_byId.ContainsKey(d.id))
                    _byId[d.id] = d;
        }

        /// <summary>
        /// 按层级查找视觉引用（不序列化组件引用）。
        /// 规则：设备命中盒与目标物体拥有同一个父物体。
        /// </summary>
        void ResolveRefs()
        {
            var door = Get("door");
            refs.doorLeaf = FindSiblingByName(door, "门扇");

            refs.curtainA_L = FindByName("窗帘A_左");
            refs.curtainA_R = FindByName("窗帘A_右");
            refs.curtainB_L = FindByName("窗帘B_左");
            refs.curtainB_R = FindByName("窗帘B_右");

            refs.chandelierLight = FindComponent<Light>("主灯光源");
            refs.chandelierPanel = FindRenderer("灯体_扩散板");
            refs.panelScreen = FindRenderer("面板_屏");
            refs.acUnitLed = FindRenderer("空调面板指示灯");
            refs.acDisplay = FindRenderer("空调面板数显");

            if (_diagLogged < 1)
            {
                _diagLogged++;
                Debug.Log("[WarmRoom] 引用解析：" +
                    "门扇=" + (refs.doorLeaf != null ? refs.doorLeaf.name : "NULL") +
                    " 窗帘=" + Count(refs.curtainA_L, refs.curtainA_R, refs.curtainB_L, refs.curtainB_R) + "/4" +
                    " 吊灯=" + (refs.chandelierLight != null ? "OK" : "NULL"));
            }
        }

        static int Count(params Transform[] ts)
        {
            int n = 0;
            foreach (var t in ts) if (t != null) n++;
            return n;
        }

        static Transform FindByName(string n)
        {
#if UNITY_2023_1_OR_NEWER
            var all = Object.FindObjectsByType<Transform>(FindObjectsSortMode.None);
#else
            var all = Object.FindObjectsOfType<Transform>();
#endif
            foreach (var t in all)
                if (t.name == n) return t;
            return null;
        }

        static T FindComponent<T>(string name) where T : Component
        {
            var t = FindByName(name);
            return t != null ? t.GetComponent<T>() : null;
        }

        static MeshRenderer FindRenderer(string name) => FindComponent<MeshRenderer>(name);

        /// <summary>在设备命中盒所在的父物体下，按名字找同级目标；找不到就按距离就近找。</summary>
        static Transform FindSiblingByName(DeviceController dev, string name)
        {
            if (dev == null) return null;
            var parent = dev.transform.parent;
            if (parent == null) return null;

            foreach (var t in parent.GetComponentsInChildren<Transform>(true))
                if (t != null && t.name == name) return t;

            // 兜底：同为该父物体下、带渲染体、且离设备命中盒最近的子物体
            Transform best = null;
            float bestD = float.MaxValue;
            foreach (var t in parent.GetComponentsInChildren<Transform>(true))
            {
                if (t == parent || t == dev.transform) continue;
                if (t.GetComponent<DeviceController>() != null) continue;
                if (t.parent != parent) continue;
                if (t.GetComponent<MeshRenderer>() == null) continue;
                float d = Vector3.Distance(t.position, dev.transform.position);
                if (d < bestD) { bestD = d; best = t; }
            }
            return best;
        }

        // ================================================================= 对外接口

        public DeviceController Get(string id)
            => _byId.TryGetValue(id, out var d) ? d : null;

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

        public void SetDeviceValue(string id, float v)
        {
            var d = Get(id);
            if (d == null) return;
            if (d.kind == DeviceKind.AirConditioner)
            {
                d.value = Mathf.Clamp(v, 16f, 30f);
                _targetTemp = d.value;
            }
            else d.value = Mathf.Clamp01(v);
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

        /// <summary>窗帘在全开 ↔ 全关之间切换（不能只取反 on，否则视觉上等于没反应）。</summary>
        public void ToggleCurtain(string id)
        {
            var c = Get(id);
            if (c == null) return;
            bool open = c.on && c.value > 0.02f;
            SetDeviceOn(id, !open);
            SetDeviceValue(id, open ? 0f : 1f);
            Log(open ? "关闭 " + c.displayName : "开启 " + c.displayName);
        }

        public void ResetAll()
        {
            foreach (var d in devices) if (d != null) d.ApplyInitial();
            _targetTemp = Mathf.Clamp(GetValue("ac"), 16f, 30f);
            _acRamp = 0f;
            // 关门与关窗帘需要显式同步，避免初始 on 与视觉不一致
            var door = Get("door");
            if (door != null) { door.on = false; }
            Log("已恢复默认状态");
            ApplyVisuals();
            SnapDoorAndCurtains();
        }

        /// <summary>把门与窗帘立刻摆到与状态一致的位置（不需要等动画）。</summary>
        void SnapDoorAndCurtains()
        {
            if (refs.doorLeaf != null)
            {
                var door = Get("door");
                float yaw = (door != null && door.on) ? Dim.DoorOpenYaw : Dim.DoorClosedYaw;
                refs.doorLeaf.localRotation = Quaternion.Euler(0f, yaw, 0f);
            }
            ApplyCurtainScale(Get("curtainA"));
            ApplyCurtainScale(Get("curtainB"));
        }

        public void Log(string msg)
        {
            lastAction = msg;
            _lastActionAt = Time.unscaledTime;
        }

        public string LastActionText
            => Time.unscaledTime - _lastActionAt < 5f ? lastAction : "";

        // ================================================================= 交互

        void HandleInput()
        {
            var mouse = Mouse.current;
            var kb = Keyboard.current;
            if (mouse == null || cam == null) return;

            var sp = mouse.position.ReadValue();
            hovered = Pick(sp);

            if (mouse.leftButton.wasPressedThisFrame) _clickGuardUntil = Time.unscaledTime + 0.12f;
            if (mouse.leftButton.wasReleasedThisFrame && Time.unscaledTime > _clickGuardUntil)
            {
                if (hovered != null) Toggle(hovered.id);
                else if (Physics.Raycast(cam.ScreenPointToRay(sp), out var hit, 100f))
                    Log("点选：" + hit.collider.name);
            }

            if (kb == null) return;
            if (kb.digit1Key.wasPressedThisFrame) Toggle("mainlight");
            if (kb.digit2Key.wasPressedThisFrame) Toggle("ac");
            if (kb.digit3Key.wasPressedThisFrame) ToggleCurtain("curtainA");
            if (kb.digit4Key.wasPressedThisFrame) ToggleCurtain("curtainB");
            if (kb.digit5Key.wasPressedThisFrame) Toggle("door");
            if (kb.fKey.wasPressedThisFrame) showThermalOverlay = !showThermalOverlay;
            if (kb.gKey.wasPressedThisFrame) showLuxField = !showLuxField;
            if (kb.hKey.wasPressedThisFrame) showSmokeField = !showSmokeField;
            if (kb.bKey.wasPressedThisFrame) showBubbles = !showBubbles;
            if (kb.rKey.wasPressedThisFrame) ResetAll();
        }

        DeviceController Pick(Vector2 screenPos)
        {
            var ray = cam.ScreenPointToRay(screenPos);
            DeviceController dc = null;
            if (Physics.Raycast(ray, out var hit, 100f))
            {
                dc = hit.collider.GetComponentInParent<DeviceController>();
                if (dc == null) dc = hit.collider.GetComponent<DeviceController>();
            }
            foreach (var d in devices) if (d != null) d.SetHover(d == dc);
            return dc;
        }

        // ================================================================= 动画

        /// <summary>
        /// 把两樘窗的窗帘立刻摆到与状态一致的位置（出图/重置时用，不等动画）。
        /// 由 ApplyCurtainScaleAll 统一入口，避免出图时只改了状态却没改视觉。
        /// </summary>
        public void ApplyCurtainScaleAll()
        {
            ApplyCurtainScale(Get("curtainA"));
            ApplyCurtainScale(Get("curtainB"));
        }

        void Animate()
        {
            float dt = Time.deltaTime;
            float k = 1f - Mathf.Exp(-7f * dt);

            // ---- 电动门：绕本地 Y 竖轴旋转（门扇正面沿 +Z 铺开，铰链在本地原点）----
            var door = Get("door");
            if (door != null)
            {
                if (refs.doorLeaf == null) refs.doorLeaf = FindSiblingByName(door, "门扇");
                if (refs.doorLeaf != null)
                {
                    float target = door.on ? Dim.DoorOpenYaw : Dim.DoorClosedYaw;
                    float cur = refs.doorLeaf.localRotation.eulerAngles.y;
                    if (cur > 180f) cur -= 360f;
                    refs.doorLeaf.localRotation = Quaternion.Euler(0f,
                        Mathf.LerpAngle(cur, target, 1f - Mathf.Exp(-6f * dt)), 0f);
                }
            }

            // ---- 电动窗帘：两樘窗各两片，从窗中心向两侧收拢 ----
            ApplyCurtainAnim(Get("curtainA"), refs.curtainA_L, refs.curtainA_R);
            ApplyCurtainAnim(Get("curtainB"), refs.curtainB_L, refs.curtainB_R);

            // ---- 空调温度平滑趋近 ----
            float acTarget = (Get("ac") != null && IsOn("ac")) ? 1f : 0f;
            _acRamp = Mathf.Lerp(_acRamp, acTarget, 1f - Mathf.Exp(-0.8f * dt));
        }

        void ApplyCurtainAnim(DeviceController c, Transform left, Transform right)
        {
            if (c == null) return;
            float open = c.on ? Mathf.Clamp01(c.value) : 0f;
            // 全关 = 1（铺满半樘窗）；全开 = 0.07（收拢到窗框外）
            float s = Mathf.Lerp(1f, 0.07f, open);
            if (left != null) left.localScale = new Vector3(1f, 1f, s);
            if (right != null) right.localScale = new Vector3(1f, 1f, s);
        }

        void ApplyCurtainScale(DeviceController c)
        {
            if (c == null) return;
            Transform l = null, r = null;
            if (c.id == "curtainA") { l = refs.curtainA_L; r = refs.curtainA_R; }
            else if (c.id == "curtainB") { l = refs.curtainB_L; r = refs.curtainB_R; }
            float open = c.on ? Mathf.Clamp01(c.value) : 0f;
            float s = Mathf.Lerp(1f, 0.07f, open);
            if (l != null) l.localScale = new Vector3(1f, 1f, s);
            if (r != null) r.localScale = new Vector3(1f, 1f, s);
        }

        // ================================================================= 传感器

        void UpdateSensors(float dt)
        {
            float acOn = _acRamp;

            float openA = CurtainOpenness("curtainA");
            float openB = CurtainOpenness("curtainB");
            float daylight = (openA + openB) * 0.5f;

            // 温度/湿度/光照：
            //   · useLiveData=false → 完全本地模拟（老行为）
            //   · useLiveData=true  → 只由树莓派帧更新；断线时冻结最后值，
            //     否则数字会自己漂移，看起来像"数据还在更新"，很误导。
            if (!useLiveData)
            {
                float target = Mathf.Lerp(26.5f, _targetTemp, acOn);
                temp = Mathf.Lerp(temp, target, 1f - Mathf.Exp(-0.12f * dt));
                temp += Mathf.Sin(Time.time * 0.23f) * 0.0015f;

                humidity = Mathf.Lerp(humidity, Mathf.Lerp(56f, 44f, acOn), 1f - Mathf.Exp(-0.10f * dt));

                float artificial = IsOn("mainlight") ? GetValue("mainlight") : 0f;
                float luxTarget = 95f + daylight * 520f + artificial * 360f;
                lux = Mathf.Lerp(lux, Mathf.Max(25f, luxTarget), 1f - Mathf.Exp(-1.4f * dt));
            }

            smoke = Mathf.Clamp01(smoke + (Mathf.PerlinNoise(Time.time * 0.05f, 3.7f) - 0.5f) * 0.004f);
            pm25 = Mathf.Lerp(pm25, 12f + (1f - daylight) * 12f + occupancy * 0.7f, 1f - Mathf.Exp(-0.2f * dt));
            co2 = Mathf.Lerp(co2, 420f + occupancy * 40f, 1f - Mathf.Exp(-0.15f * dt));
        }

        float CurtainOpenness(string id)
        {
            var c = Get(id);
            return c != null ? (c.on ? Mathf.Clamp01(c.value) : 0f) : 1f;
        }

        void UpdateFields(float dt)
        {
            for (int i = 0; i < _tempField.Length; i++)
            {
                int ix = i % 6, iz = i / 6;
                float bias = Mathf.Lerp(0.8f, -0.6f, iz / 3f) + Mathf.Sin(ix * 1.1f + iz * 0.7f) * 0.25f;
                float t = temp + bias + (IsOn("ac") ? -0.35f : 0f);
                _tempField[i] = Mathf.Lerp(_tempField[i], t, 1f - Mathf.Exp(-0.6f * dt));
                float lx = Mathf.Lerp(1.30f, 0.60f, iz / 3f);   // 靠窗侧更亮
                _luxField[i] = Mathf.Lerp(_luxField[i], lux * lx, 1f - Mathf.Exp(-1.2f * dt));
            }

            if (_thermalMat != null)
            {
                float s = Time.time * 0.012f;
                _thermalMat.SetVector(OffsetId, new Vector4(s, s * 0.6f, 0f, 0f));
                _thermalMat.color = new Color(1f, 1f, 1f, showThermalOverlay ? 0.20f : 0f);
            }
            if (_fieldMat != null)
            {
                float s = -Time.time * 0.02f;
                _fieldMat.SetVector(OffsetId, new Vector4(s, s * 0.4f, 0f, 0f));
                _fieldMat.color = new Color(1f, 1f, 1f, (showLuxField || showSmokeField) ? 0.22f : 0f);
            }
        }

        // ================================================================= 视觉联动

        public void ApplyVisuals()
        {
            // 主吊灯
            var ml = Get("mainlight");
            if (ml != null && refs.chandelierLight != null)
            {
                float dim = ml.on ? Mathf.Clamp01(ml.value) : 0f;
                refs.chandelierLight.intensity = dim * 10.5f;
                refs.chandelierLight.shadows = dim > 0.35f ? LightShadows.Soft : LightShadows.None;
            }
            if (ml != null && refs.chandelierPanel != null)
            {
                float dim = ml.on ? Mathf.Clamp01(ml.value) : 0f;
                SetEmission(refs.chandelierPanel, new Color(1f, 0.84f, 0.62f), 0.05f + dim * 1.30f);
            }

            // 落地空调：指示灯与数显随状态/设定温度变色
            var ac = Get("ac");
            bool acOn = ac != null && ac.on;
            float tn = ac != null ? Mathf.InverseLerp(16f, 30f, ac.value) : 0.5f;
            var acColor = acOn
                ? Color.Lerp(new Color(0.45f, 0.78f, 1f), new Color(1f, 0.52f, 0.28f), tn)
                : new Color(0.42f, 0.40f, 0.38f);
            if (refs.acUnitLed != null) SetEmission(refs.acUnitLed, acColor, acOn ? 2.8f : 0.03f);
            if (refs.acDisplay != null)
                SetEmission(refs.acDisplay, acOn ? new Color(1f, 0.78f, 0.36f) : new Color(0.10f, 0.09f, 0.08f),
                    acOn ? 2.2f : 0.02f);

            // 空调数显：有实时数据后换成实测温度的数字贴图
            UpdateAcDigits();

            // 门口面板
            if (refs.panelScreen != null)
            {
                var panel = Get("panel");
                bool on = panel == null || panel.on;
                SetEmission(refs.panelScreen, on ? new Color(0.95f, 0.72f, 0.32f) : new Color(0.08f, 0.07f, 0.06f),
                    on ? 0.90f : 0.02f);
            }

            if (_fieldQuad != null) _fieldQuad.SetActive(showLuxField || showSmokeField);
        }

        static void SetEmission(MeshRenderer r, Color c, float intensity)
        {
            if (r == null) return;
            var mpb = new MaterialPropertyBlock();
            r.GetPropertyBlock(mpb);
            mpb.SetColor(Mat.EmissionId, c * intensity);
            r.SetPropertyBlock(mpb);
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
            if (_thermalQuad != null) { Object.DestroyImmediate(_thermalQuad); _thermalQuad = null; }
            if (_fieldQuad != null) { Object.DestroyImmediate(_fieldQuad); _fieldQuad = null; }

            _thermalMat = FieldMaterial("w_field_thermal");
            _thermalQuad = FieldQuad("温度空间化层", _thermalMat, 0.035f, new Vector2(7.4f, 5.4f));

            _fieldMat = FieldMaterial("w_field_lux");
            _fieldQuad = FieldQuad("光照与烟雾空间化层", _fieldMat, 0.055f, new Vector2(7.4f, 5.4f));
            _fieldQuad.SetActive(false);
        }

        Material FieldMaterial(string key)
        {
            var m = new Material(Shader.Find("Universal Render Pipeline/Lit")) { name = key };
            m.SetTexture(Mat.BaseMapId, _thermalTex);
            m.SetColor(Mat.BaseColorId, new Color(1f, 1f, 1f, 0.32f));
            m.SetFloat(Mat.MetallicId, 0f);
            m.SetFloat(Mat.SmoothnessId, 0.05f);
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

        GameObject FieldQuad(string name, Material mat, float y, Vector2 size)
        {
            var go = new GameObject(name);
            go.AddComponent<MeshFilter>().sharedMesh = Prim.DiscMesh();
            var mr = go.AddComponent<MeshRenderer>();
            mr.sharedMaterial = mat;
            mr.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;
            mr.receiveShadows = false;
            go.transform.position = new Vector3(0f, y, 0f);
            go.transform.localScale = new Vector3(size.x, 1f, size.y);
            return go;
        }

        public float SampleLuxAt(Vector3 w)
        {
            int ix = Mathf.Clamp(Mathf.RoundToInt((w.x + Dim.HX) / Dim.Length * 5f), 0, 5);
            int iz = Mathf.Clamp(Mathf.RoundToInt((w.z + Dim.HZ) / Dim.Width * 3f), 0, 3);
            return _luxField[iz * 6 + ix];
        }

        // ================================================================= 界面辅助

        public bool WorldToScreen(Vector3 world, out Vector2 screen, out float depth)
        {
            screen = Vector2.zero; depth = 0f;
            if (cam == null) return false;
            var v = cam.WorldToScreenPoint(world);
            if (v.z <= 0.05f) return false;
            screen = new Vector2(v.x, Screen.height - v.y);
            depth = v.z;
            return true;
        }
    }
}
