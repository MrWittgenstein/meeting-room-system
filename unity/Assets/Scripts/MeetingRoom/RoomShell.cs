using UnityEngine;

namespace SmartRoom
{
    /// <summary>房间壳体：地板、四面墙（含门洞/窗洞）、吊顶与灯具风口。</summary>
    public static class RoomShell
    {
        public const string Root = "01_房间壳体";

        public static GameObject Build()
        {
            var root = new GameObject(Root);
            float hx = RoomDim.HalfX, hz = RoomDim.HalfZ, H = RoomDim.Height, T = RoomDim.WallThickness;

            BuildFloor(root.transform, hx, hz, T);
            BuildEntryWall(root.transform, hx, hz, H, T);      // x = -4，含门洞
            BuildRightWall(root.transform, hx, hz, H, T);      // x = +4
            BuildWindowWall(root.transform, hx, hz, H, T);     // z = +3，含大窗
            BuildScreenWall(root.transform, hx, hz, H, T);     // z = -3，投影幕布墙
            BuildCeiling(root.transform, hx, hz, H, T);
            BuildSkirting(root.transform, hx, hz);
            BuildWindowFrame(root.transform, hz, H, T);
            BuildDoor(root.transform, hx, hz, T);

            return root;
        }

        // ------------------------------------------------------------------ 地板

        static void BuildFloor(Transform p, float hx, float hz, float T)
        {
            // 结构楼板
            Prim.Box("地板_结构层", p, new Vector3(0f, -T * 0.5f, 0f),
                new Vector3(hx * 2f + T * 2f, T, hz * 2f + T * 2f), MatLib.Plastic);
            // 深灰地毯面层
            Prim.Box("地板_地毯", p, new Vector3(0f, -0.012f, 0f),
                new Vector3(hx * 2f, 0.024f, hz * 2f), MatLib.Carpet);
            // 地毯压条（沿墙一圈，让收边干净）
            var strip = MatLib.MetalDark;
            Prim.Box("地毯压条_X负", p, new Vector3(-hx + 0.02f, 0.002f, 0f), new Vector3(0.04f, 0.012f, hz * 2f), strip);
            Prim.Box("地毯压条_X正", p, new Vector3(hx - 0.02f, 0.002f, 0f), new Vector3(0.04f, 0.012f, hz * 2f), strip);
            Prim.Box("地毯压条_Z负", p, new Vector3(0f, 0.002f, -hz + 0.02f), new Vector3(hx * 2f, 0.012f, 0.04f), strip);
            Prim.Box("地毯压条_Z正", p, new Vector3(0f, 0.002f, hz - 0.02f), new Vector3(hx * 2f, 0.012f, 0.04f), strip);
        }

        // ------------------------------------------------------------------ 墙体

        /// <summary>入口短墙 x=-4：门洞两侧 + 门头过梁。</summary>
        static void BuildEntryWall(Transform p, float hx, float hz, float H, float T)
        {
            float x0 = -hx - T, x1 = -hx;
            // 门洞 z ∈ [-2.35, -1.35]
            Prim.BoxBetween("入口墙_南段", p,
                new Vector3(x0, 0f, -hz), new Vector3(x1, H, RoomDim.DoorZMin), MatLib.Wall);
            Prim.BoxBetween("入口墙_北段", p,
                new Vector3(x0, 0f, RoomDim.DoorZMax), new Vector3(x1, H, hz), MatLib.Wall);
            // 门头
            Prim.BoxBetween("入口墙_门头", p,
                new Vector3(x0, RoomDim.DoorHeight, RoomDim.DoorZMin),
                new Vector3(x1, H, RoomDim.DoorZMax), MatLib.Wall);

            // 入口墙内侧做一段深色木饰面，突出入口
            Prim.Box("入口墙_木饰面", p,
                new Vector3(-hx + 0.015f, H * 0.5f, (RoomDim.DoorZMax + hz) * 0.5f + 0.2f),
                new Vector3(0.03f, H, hz - RoomDim.DoorZMax - 0.4f), MatLib.Walnut);
        }

        /// <summary>右侧短墙 x=+4（实墙，放边柜与装饰）。</summary>
        static void BuildRightWall(Transform p, float hx, float hz, float H, float T)
        {
            Prim.BoxBetween("右墙", p,
                new Vector3(hx, 0f, -hz), new Vector3(hx + T, H, hz), MatLib.Wall);

            // 竖向凹槽装饰线，避免大白墙单调
            for (int i = 0; i < 5; i++)
            {
                float z = -hz + 0.7f + i * 1.15f;
                Prim.Box("右墙_装饰槽" + i, p, new Vector3(hx - 0.012f, 1.5f, z),
                    new Vector3(0.024f, 1.5f, 0.05f), MatLib.WalnutDark);
            }
        }

        /// <summary>窗墙 z=+3：窗下墙 + 窗上墙 + 窗两侧墙垛。</summary>
        static void BuildWindowWall(Transform p, float hx, float hz, float H, float T)
        {
            float z0 = hz, z1 = hz + T;
            float wx0 = RoomDim.WindowXMin, wx1 = RoomDim.WindowXMax;
            float sill = RoomDim.WindowSill, top = RoomDim.WindowTop;

            // 窗下墙（矮墙）
            Prim.BoxBetween("窗墙_窗下墙", p,
                new Vector3(wx0, 0f, z0), new Vector3(wx1, sill, z1), MatLib.Wall);
            // 窗上墙
            Prim.BoxBetween("窗墙_窗上墙", p,
                new Vector3(wx0, top, z0), new Vector3(wx1, H, z1), MatLib.Wall);
            // 两侧墙垛
            Prim.BoxBetween("窗墙_左垛", p,
                new Vector3(-hx - T, 0f, z0), new Vector3(wx0, H, z1), MatLib.Wall);
            Prim.BoxBetween("窗墙_右垛", p,
                new Vector3(wx1, 0f, z0), new Vector3(hx + T, H, z1), MatLib.Wall);
        }

        /// <summary>幕布墙 z=-3：整面实墙 + 投影区深色饰面。</summary>
        static void BuildScreenWall(Transform p, float hx, float hz, float H, float T)
        {
            Prim.BoxBetween("幕布墙", p,
                new Vector3(-hx - T, 0f, -hz - T), new Vector3(hx + T, H, -hz), MatLib.Wall);

            // 投影区深色饰面背板（提升画面对比度，也更像真实会议室）
            Prim.Box("幕布墙_投影区饰面", p,
                new Vector3(0f, 1.75f, -hz + 0.018f), new Vector3(4.4f, 2.3f, 0.036f), MatLib.WallAccent);
            // 饰面边框
            var fr = MatLib.MetalDark;
            Prim.Box("幕布墙_饰面框_上", p, new Vector3(0f, 1.75f + 1.15f, -hz + 0.028f), new Vector3(4.48f, 0.04f, 0.056f), fr);
            Prim.Box("幕布墙_饰面框_下", p, new Vector3(0f, 1.75f - 1.15f, -hz + 0.028f), new Vector3(4.48f, 0.04f, 0.056f), fr);
            Prim.Box("幕布墙_饰面框_左", p, new Vector3(-2.22f, 1.75f, -hz + 0.028f), new Vector3(0.04f, 2.34f, 0.056f), fr);
            Prim.Box("幕布墙_饰面框_右", p, new Vector3(2.22f, 1.75f, -hz + 0.028f), new Vector3(0.04f, 2.34f, 0.056f), fr);
        }

        // ------------------------------------------------------------------ 吊顶

        static void BuildCeiling(Transform p, float hx, float hz, float H, float T)
        {
            // 吊顶板
            Prim.Box("吊顶", p, new Vector3(0f, H + RoomDim.CeilingThickness * 0.5f, 0f),
                new Vector3(hx * 2f, RoomDim.CeilingThickness, hz * 2f), MatLib.CeilingMat);

            // 四周跌级灯槽（发光灯带）
            float inset = 0.55f, bandW = 0.10f;
            var emissive = MatLib.Emissive("m_cove", new Color(1f, 0.945f, 0.86f), 0.42f);
            Prim.Box("灯槽_X负", p, new Vector3(-hx + inset, H - 0.035f, 0f), new Vector3(bandW, 0.03f, hz * 2f - inset * 2f), emissive);
            Prim.Box("灯槽_X正", p, new Vector3(hx - inset, H - 0.035f, 0f), new Vector3(bandW, 0.03f, hz * 2f - inset * 2f), emissive);
            Prim.Box("灯槽_Z负", p, new Vector3(0f, H - 0.035f, -hz + inset), new Vector3(hx * 2f - inset * 2f, 0.03f, bandW), emissive);
            Prim.Box("灯槽_Z正", p, new Vector3(0f, H - 0.035f, hz - inset), new Vector3(hx * 2f - inset * 2f, 0.03f, bandW), emissive);

            // 灯槽跌级挡板
            var drop = MatLib.CeilingGrid;
            Prim.Box("跌级_X负", p, new Vector3(-hx + inset - bandW * 0.5f - 0.03f, H - 0.06f, 0f), new Vector3(0.06f, 0.12f, hz * 2f - inset * 2f), drop);
            Prim.Box("跌级_X正", p, new Vector3(hx - inset + bandW * 0.5f + 0.03f, H - 0.06f, 0f), new Vector3(0.06f, 0.12f, hz * 2f - inset * 2f), drop);
            Prim.Box("跌级_Z负", p, new Vector3(0f, H - 0.06f, -hz + inset - bandW * 0.5f - 0.03f), new Vector3(hx * 2f - inset * 2f, 0.12f, 0.06f), drop);
            Prim.Box("跌级_Z正", p, new Vector3(0f, H - 0.06f, hz - inset + bandW * 0.5f + 0.03f), new Vector3(hx * 2f - inset * 2f, 0.12f, 0.06f), drop);

            BuildDownlights(p, hx, hz, H);
            BuildAcUnit(p, hx, hz, H);
            BuildSensors(p, H);
        }

        /// <summary>吊顶筒灯（发光面 + 金属圈）。</summary>
        static void BuildDownlights(Transform p, float hx, float hz, float H)
        {
            var ring = MatLib.Chrome;
            var glow = MatLib.Emissive("m_downlight", new Color(1f, 0.955f, 0.88f), 1.15f);

            // 两排，每排 4 个，避开中间吊灯与空调机位
            for (int row = 0; row < 2; row++)
            {
                float z = row == 0 ? -1.85f : 1.85f;
                for (int i = 0; i < 4; i++)
                {
                    float x = -2.6f + i * 1.7333f;
                    var c = new Vector3(x, H - 0.012f, z);
                    Prim.Cylinder("筒灯圈_" + row + "_" + i, p, c, 0.085f, 0.02f, ring);
                    Prim.Disc("筒灯发光_" + row + "_" + i, p, new Vector3(x, H - 0.021f, z), 0.072f, glow,
                        Quaternion.Euler(90f, 0f, 0f));
                }
            }
        }

        /// <summary>
        /// 落地式柜机空调（白色立式）。放在房间右后角，正面朝向房间中央，
        /// 顶部深色显示面板 + 出风口百叶 + 底部进风格栅 + 运行指示灯。
        /// </summary>
        static void BuildAcUnit(Transform p, float hx, float hz, float H)
        {
            var root = new GameObject("立式空调").transform;
            root.SetParent(p, false);
            root.localPosition = new Vector3(hx - 0.48f, 0f, -hz + 0.72f);
            // 朝向：这台机器的出风口/显示屏面板在本地 -Z。
            // 机器在右后角，指向房间中心的方向是 (-0.71, 0, +0.71)，对应 yaw = +135°。
            // （之前写成 -45° 正好差 180°，出风口对着墙，所以看到的是一整块白背板。）
            root.localRotation = Quaternion.Euler(0f, 135f, 0f);
            var t = root.transform;

            var body = MatLib.Mat("m_ac_body", new Color(0.93f, 0.94f, 0.95f), 0f, 0.42f);
            var bodyEdge = MatLib.Mat("m_ac_edge", new Color(0.78f, 0.79f, 0.81f), 0f, 0.35f);
            var panelDark = MatLib.Mat("m_ac_display", new Color(0.10f, 0.11f, 0.13f), 0.15f, 0.55f);
            var louver = MatLib.Mat("m_ac_louver", new Color(0.72f, 0.73f, 0.75f), 0f, 0.38f);
            var grille = MatLib.Mat("m_ac_grille", new Color(0.30f, 0.31f, 0.33f), 0.05f, 0.35f);

            const float W = 0.42f, D = 0.34f, BodyH = 1.86f;

            // 底座
            Prim.Box("空调_底座", t, new Vector3(0f, 0.022f, 0f), new Vector3(W + 0.05f, 0.044f, D + 0.05f), panelDark);
            // 柜体（圆角，正面稍作收边）
            Prim.RoundedBox("空调_柜体", t, new Vector3(0f, BodyH * 0.5f + 0.04f, 0f),
                new Vector3(W, BodyH, D), body);
            // 正面竖向装饰缝
            Prim.Box("空调_装饰缝_左", t, new Vector3(-W * 0.5f + 0.035f, BodyH * 0.5f + 0.04f, -D * 0.5f - 0.002f),
                new Vector3(0.012f, BodyH - 0.24f, 0.006f), bodyEdge);
            Prim.Box("空调_装饰缝_右", t, new Vector3(W * 0.5f - 0.035f, BodyH * 0.5f + 0.04f, -D * 0.5f - 0.002f),
                new Vector3(0.012f, BodyH - 0.24f, 0.006f), bodyEdge);

            // ---- 顶部深色显示面板 ----
            Prim.Box("空调_显示面板", t, new Vector3(0f, BodyH - 0.12f, -D * 0.5f - 0.004f),
                new Vector3(W - 0.06f, 0.20f, 0.010f), panelDark);
            // 数字显示（自发光，控制器会改颜色）
            Prim.Box("空调面板数显", t, new Vector3(-0.035f, BodyH - 0.12f, -D * 0.5f - 0.011f),
                new Vector3(0.16f, 0.085f, 0.005f),
                MatLib.Emissive("m_ac_display_digit", new Color(0.35f, 0.85f, 1f), 2.4f));
            // 运行指示灯（自发光，控制器会改颜色）——就是 refs.acIndicator
            Prim.Sphere("空调面板指示灯", t, new Vector3(0.135f, BodyH - 0.12f, -D * 0.5f - 0.012f), 0.014f,
                MatLib.Emissive("m_ac_run_led", new Color(0.35f, 0.85f, 1f), 2.6f));

            // ---- 出风口（斜向百叶）----
            Prim.Box("空调_出风口框", t, new Vector3(0f, BodyH - 0.32f, -D * 0.5f - 0.006f),
                new Vector3(W - 0.07f, 0.17f, 0.012f), grille);
            for (int i = 0; i < 5; i++)
            {
                float y = BodyH - 0.39f + i * 0.036f;
                Prim.Box("空调_出风百叶" + i, t, new Vector3(0f, y, -D * 0.5f - 0.013f),
                    new Vector3(W - 0.10f, 0.014f, 0.008f), louver, Quaternion.Euler(-22f, 0f, 0f));
            }

            // ---- 中部进风格栅 ----
            Prim.Box("空调_进风格栅", t, new Vector3(0f, 0.92f, -D * 0.5f - 0.005f),
                new Vector3(W - 0.10f, 0.62f, 0.010f), grille);
            for (int i = 0; i < 9; i++)
            {
                float y = 0.66f + i * 0.065f;
                Prim.Box("空调_进气百叶" + i, t, new Vector3(0f, y, -D * 0.5f - 0.011f),
                    new Vector3(W - 0.13f, 0.018f, 0.006f), louver);
            }

            // ---- 底部出风格栅（贴近地面）----
            Prim.Box("空调_下出风", t, new Vector3(0f, 0.16f, -D * 0.5f - 0.005f),
                new Vector3(W - 0.12f, 0.12f, 0.008f), grille);

            // 顶部品牌饰条
            Prim.Box("空调_顶部饰条", t, new Vector3(0f, BodyH + 0.028f, 0f), new Vector3(W - 0.02f, 0.02f, D - 0.02f), bodyEdge);
        }

        /// <summary>吊顶上的环境传感器：烟雾探测、光照传感。</summary>
        static void BuildSensors(Transform p, float H)
        {
            var white = MatLib.PlasticWhite;
            var dark = MatLib.Plastic;

            // 烟雾探测器
            var smoke = new Vector3(-2.0f, H - 0.03f, -2.0f);
            Prim.Cylinder("烟雾探测器_壳体", p, smoke, 0.075f, 0.06f, white);
            Prim.Cylinder("烟雾探测器_格栅", p, new Vector3(smoke.x, smoke.y - 0.032f, smoke.z), 0.062f, 0.012f, dark);

            // 光照传感器（靠近窗侧，采集自然光）
            var lux = new Vector3(2.55f, H - 0.028f, RoomDim.HalfZ - 0.045f);
            Prim.Box("光照传感器_壳体", p, lux, new Vector3(0.10f, 0.05f, 0.10f), white);
            Prim.Disc("光照传感器_感光面", p, new Vector3(lux.x, lux.y - 0.028f, lux.z), 0.032f,
                MatLib.Emissive("m_lux_sensor", new Color(0.45f, 0.90f, 1f), 1.2f), Quaternion.Euler(90f, 0f, 0f));
        }

        // ------------------------------------------------------------------ 踢脚与窗框

        static void BuildSkirting(Transform p, float hx, float hz)
        {
            var m = MatLib.WalnutDark;
            const float h = 0.09f, t = 0.02f;
            Prim.Box("踢脚_X负", p, new Vector3(-hx + t * 0.5f, h * 0.5f, 0f), new Vector3(t, h, hz * 2f), m);
            Prim.Box("踢脚_X正", p, new Vector3(hx - t * 0.5f, h * 0.5f, 0f), new Vector3(t, h, hz * 2f), m);
            Prim.Box("踢脚_Z负", p, new Vector3(0f, h * 0.5f, -hz + t * 0.5f), new Vector3(hx * 2f, h, t), m);
            // 窗墙只做窗下墙部分
            Prim.Box("踢脚_Z正", p, new Vector3(0f, h * 0.5f, hz - t * 0.5f), new Vector3(hx * 2f, h, t), m);
        }

        static void BuildWindowFrame(Transform p, float hz, float H, float T)
        {
            var frame = MatLib.MetalDark;
            float z = hz + T * 0.5f;
            float wx0 = RoomDim.WindowXMin, wx1 = RoomDim.WindowXMax;
            float sill = RoomDim.WindowSill, top = RoomDim.WindowTop;
            float fw = 0.06f;  // 框料宽度
            float fd = 0.14f;  // 框料进深

            // 上下框
            Prim.Box("窗框_上", p, new Vector3((wx0 + wx1) * 0.5f, top, z), new Vector3(wx1 - wx0 + fw, fw, fd), frame);
            Prim.Box("窗框_下", p, new Vector3((wx0 + wx1) * 0.5f, sill, z), new Vector3(wx1 - wx0 + fw, fw, fd), frame);
            // 左右框
            Prim.Box("窗框_左", p, new Vector3(wx0, (sill + top) * 0.5f, z), new Vector3(fw, top - sill, fd), frame);
            Prim.Box("窗框_右", p, new Vector3(wx1, (sill + top) * 0.5f, z), new Vector3(fw, top - sill, fd), frame);

            // 竖向分格（3 等分）
            int splits = 3;
            for (int i = 1; i < splits; i++)
            {
                float x = Mathf.Lerp(wx0, wx1, (float)i / splits);
                Prim.Box("窗框_竖挺" + i, p, new Vector3(x, (sill + top) * 0.5f, z), new Vector3(0.045f, top - sill, fd * 0.8f), frame);
            }

            // 窗台板（浅色石材）
            Prim.Box("窗台板", p, new Vector3((wx0 + wx1) * 0.5f, sill - 0.015f, hz - 0.09f),
                new Vector3(wx1 - wx0 + 0.16f, 0.03f, 0.30f), MatLib.Stone);

            // 玻璃
            float gx0 = wx0 + fw * 0.5f, gx1 = wx1 - fw * 0.5f;
            float gy0 = sill + fw * 0.5f, gy1 = top - fw * 0.5f;
            Prim.Box("窗玻璃", p, new Vector3((gx0 + gx1) * 0.5f, (gy0 + gy1) * 0.5f, z),
                new Vector3(gx1 - gx0, gy1 - gy0, 0.012f), MatLib.GlassWindow);
        }

        /// <summary>
        /// 电动玻璃门。
        /// 关键点：门扇严格沿"本地 +Z"方向建立（从铰链 0 到门宽），
        /// 这样绕本地 Y 轴旋转就是绕着铰链摆动，方向一目了然。
        /// 门扇引用由 RoomController 运行时按层级查找（不序列化 Transform 引用，
        /// 否则 DeviceController 会在场景重载后整体解析失败）。
        /// </summary>
        static void BuildDoor(Transform p, float hx, float hz, float T)
        {
            var frame = MatLib.MetalDark;
            var handle = MatLib.Chrome;

            const float frameW = 0.055f;   // 门框料宽
            float leafT = T + 0.035f;      // 门扇厚（略厚于墙，形成门套感）
            float h = RoomDim.DoorHeight;
            float z0 = RoomDim.DoorZMin;   // 铰链侧
            float z1 = RoomDim.DoorZMax;
            float leafW = z1 - z0;         // 门扇宽 1.0m

            // ---------------- 门套（固定不动，属于墙体） ----------------
            float cx = -hx - T * 0.5f;
            Prim.Box("门套_上", p, new Vector3(cx, h + 0.03f, (z0 + z1) * 0.5f),
                new Vector3(T + 0.05f, 0.06f, leafW + 0.12f), frame);
            Prim.Box("门套_铰链侧", p, new Vector3(cx, (h + 0.06f) * 0.5f, z0 - 0.03f),
                new Vector3(T + 0.05f, h + 0.06f, 0.06f), frame);
            Prim.Box("门套_开合侧", p, new Vector3(cx, (h + 0.06f) * 0.5f, z1 + 0.03f),
                new Vector3(T + 0.05f, h + 0.06f, 0.06f), frame);

            // ---------------- 门扇（可动） ----------------
            var leaf = new GameObject("门扇").transform;
            leaf.SetParent(p, false);
            leaf.localPosition = new Vector3(cx, 0f, z0);   // 铰链点
            leaf.localRotation = Quaternion.Euler(0f, RoomDim.DoorClosedYaw, 0f);

            // 门扇本地坐标：Z 从 0（铰链）到 leafW（开合侧），X 是厚度方向，Y 是高度
            float zc = leafW * 0.5f;

            // 磨砂玻璃（主体）
            Prim.Box("门扇_玻璃", leaf, new Vector3(0f, h * 0.5f, zc),
                new Vector3(0.028f, h - 0.14f, leafW - 0.06f), MatLib.GlassDoor);

            // 上下横档
            Prim.Box("门扇_横档_下", leaf, new Vector3(0f, 0.09f, zc),
                new Vector3(leafT, 0.18f, leafW), frame);
            Prim.Box("门扇_横档_上", leaf, new Vector3(0f, h - 0.055f, zc),
                new Vector3(leafT, 0.11f, leafW), frame);

            // 两侧竖挺（铰链侧 / 开合侧）
            Prim.Box("门扇_竖挺_铰链侧", leaf, new Vector3(0f, h * 0.5f, frameW * 0.5f),
                new Vector3(leafT, h, frameW), frame);
            Prim.Box("门扇_竖挺_开合侧", leaf, new Vector3(0f, h * 0.5f, leafW - frameW * 0.5f),
                new Vector3(leafT, h, frameW), frame);

            // 长条不锈钢拉手（内外各一，竖向，现代玻璃门做法）
            Prim.Cylinder("门扇_拉手_外", leaf, new Vector3(-leafT * 0.6f, 1.05f, leafW - 0.13f),
                0.017f, 0.95f, handle);
            Prim.Cylinder("门扇_拉手_内", leaf, new Vector3(leafT * 0.6f, 1.05f, leafW - 0.13f),
                0.017f, 0.95f, handle);
            // 把手连接座
            Prim.Box("门扇_拉手座_外", leaf, new Vector3(-leafT * 0.35f, 1.05f, leafW - 0.13f),
                new Vector3(0.02f, 0.10f, 0.05f), handle);
            Prim.Box("门扇_拉手座_内", leaf, new Vector3(leafT * 0.35f, 1.05f, leafW - 0.13f),
                new Vector3(0.02f, 0.10f, 0.05f), handle);

            // 合页（铰链侧的三个小圆柱，强化"这扇门可以转"的观感）
            for (int i = 0; i < 3; i++)
            {
                float y = 0.35f + i * 0.72f;
                Prim.Cylinder("门扇_合页" + i, leaf, new Vector3(0f, y, 0.012f), 0.022f, 0.09f,
                    handle, Quaternion.Euler(90f, 0f, 0f));
            }

            // ---------------- 点选区（透明，只留碰撞体） ----------------
            var hit = new GameObject("设备_电动门");
            hit.transform.SetParent(p, false);
            hit.transform.localPosition = new Vector3(cx, h * 0.5f, (z0 + z1) * 0.5f);
            var bc = hit.AddComponent<BoxCollider>();
            bc.size = new Vector3(0.30f, h, leafW + 0.10f);

            var dc = hit.AddComponent<DeviceController>();
            dc.id = "door";
            dc.displayName = "电动门（开关）";
            dc.kind = DeviceKind.Door;
            dc.initialOn = false;
            dc.initialValue = 1f;
            dc.outlineOffset = Vector3.zero;
        }

        /// <summary>构建收尾：门扇用的是"先加碰撞体再放模型"的写法，这里清掉命中盒的多余渲染体。</summary>
        public static void FinalizeShell(GameObject root)
        {
            foreach (var t in root.GetComponentsInChildren<Transform>(true))
            {
                if (!t.name.StartsWith("设备_")) continue;
                var mr = t.GetComponent<MeshRenderer>();
                if (mr != null) UnityEngine.Object.DestroyImmediate(mr);
                var mf = t.GetComponent<MeshFilter>();
                if (mf != null) UnityEngine.Object.DestroyImmediate(mf);
            }
        }
    }
}
