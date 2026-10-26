using UnityEngine;

namespace WarmRoom
{
    /// <summary>
    /// 房间壳体（暖色木质商务风）。
    /// 布局与上一版整体对调：门在 +X 短墙，两樘大窗在 -X 长墙，主景木饰面墙在 -Z。
    /// </summary>
    public static class Shell
    {
        public const string Root = "01_房间壳体";

        public static GameObject Build()
        {
            var root = new GameObject(Root);
            var p = root.transform;
            float hx = Dim.HX, hz = Dim.HZ, H = Dim.Height, T = Dim.WallT;

            BuildFloor(p, hx, hz);
            BuildWallDoor(p, hx, hz, H, T);
            BuildWallWindow(p, hx, hz, H, T);
            BuildWallFeature(p, hx, hz, H, T);
            BuildWallSideboard(p, hx, hz, H, T);
            BuildCeiling(p, hx, hz, H);
            BuildSkirting(p, hx, hz);
            BuildWindowFrames(p, hx, H, T);
            BuildDoor(p, hx, T);
            BuildAcUnit(p, hx, hz);

            return root;
        }

        // ------------------------------------------------------------------ 地面

        static void BuildFloor(Transform p, float hx, float hz)
        {
            Prim.Box("地板_结构层", p, new Vector3(0f, -Dim.WallT * 0.5f, 0f),
                new Vector3(hx * 2f + 0.24f, Dim.WallT, hz * 2f + 0.24f), Mat.Plastic);

            // 暖灰地毯（主地面）
            Prim.Box("地板_地毯", p, new Vector3(0f, -0.012f, 0f),
                new Vector3(hx * 2f, 0.024f, hz * 2f), Mat.Carpet);

            // 浅橡木地板做一圈"踢脚边带"，把地毯收在中间（商务会议室常见做法）
            float band = 0.55f;
            var oak = Mat.OakFloor;
            Prim.Box("地板_木边带_X正", p, new Vector3(hx - band * 0.5f, 0.001f, 0f), new Vector3(band, 0.026f, hz * 2f), oak);
            Prim.Box("地板_木边带_X负", p, new Vector3(-hx + band * 0.5f, 0.001f, 0f), new Vector3(band, 0.026f, hz * 2f), oak);
            Prim.Box("地板_木边带_Z正", p, new Vector3(0f, 0.001f, hz - band * 0.5f), new Vector3(hx * 2f - band * 2f, 0.026f, band), oak);
            Prim.Box("地板_木边带_Z负", p, new Vector3(0f, 0.001f, -hz + band * 0.5f), new Vector3(hx * 2f - band * 2f, 0.026f, band), oak);
        }

        // ------------------------------------------------------------------ 墙体

        /// <summary>右短墙 x=+4：门洞两侧 + 门头。</summary>
        static void BuildWallDoor(Transform p, float hx, float hz, float H, float T)
        {
            float x0 = hx, x1 = hx + T;
            Prim.BoxBetween("门墙_南段", p,
                new Vector3(x0, 0f, -hz), new Vector3(x1, H, Dim.DoorZMin), Mat.Wall);
            Prim.BoxBetween("门墙_北段", p,
                new Vector3(x0, 0f, Dim.DoorZMax), new Vector3(x1, H, hz), Mat.Wall);
            Prim.BoxBetween("门墙_门头", p,
                new Vector3(x0, Dim.DoorH, Dim.DoorZMin), new Vector3(x1, H, Dim.DoorZMax), Mat.Wall);

            // 门旁一段木饰面，突出入口
            Prim.Box("门墙_木饰面", p,
                new Vector3(hx - 0.018f, 1.60f, (Dim.DoorZMax + hz) * 0.5f + 0.35f),
                new Vector3(0.036f, 3.20f, hz - Dim.DoorZMax - 0.70f), Mat.Walnut);
        }

        /// <summary>左长墙 x=-4：两樘窗（中间墙垛），窗下墙 + 窗上墙 + 三段实墙。</summary>
        static void BuildWallWindow(Transform p, float hx, float hz, float H, float T)
        {
            float x0 = -hx - T, x1 = -hx;
            float sill = Dim.WinSill, top = Dim.WinTop;

            // 窗下墙 / 窗上墙（两樘各自一段）
            foreach (var w in new[] {
                (min: Dim.WinZMinA, max: Dim.WinZMaxA, tag: "A"),
                (min: Dim.WinZMinB, max: Dim.WinZMaxB, tag: "B") })
            {
                Prim.BoxBetween("窗下墙_" + w.tag, p,
                    new Vector3(x0, 0f, w.min), new Vector3(x1, sill, w.max), Mat.Wall);
                Prim.BoxBetween("窗上墙_" + w.tag, p,
                    new Vector3(x0, top, w.min), new Vector3(x1, H, w.max), Mat.Wall);
            }

            // 三段实墙：南端、中间墙垛、北端
            Prim.BoxBetween("窗墙_南段", p,
                new Vector3(x0, 0f, -hz), new Vector3(x1, H, Dim.WinZMinA), Mat.Wall);
            Prim.BoxBetween("窗墙_中垛", p,
                new Vector3(x0, 0f, Dim.WinZMaxA), new Vector3(x1, H, Dim.WinZMinB), Mat.Wall);
            Prim.BoxBetween("窗墙_北段", p,
                new Vector3(x0, 0f, Dim.WinZMaxB), new Vector3(x1, H, hz), Mat.Wall);
        }

        /// <summary>主景墙 z=-3：两侧木格栅 + 中央大理石装饰带。</summary>
        static void BuildWallFeature(Transform p, float hx, float hz, float H, float T)
        {
            Prim.BoxBetween("主景墙", p,
                new Vector3(-hx - T, 0f, -hz - T), new Vector3(hx + T, H, -hz), Mat.Wall);

            float fw = Dim.FeatureW * 0.5f;
            // 深色木框（把大理石带"嵌"进木饰面里，避免它突兀地糊在墙上）
            Prim.Box("主景墙_大理石墨框", p, new Vector3(0f, 1.55f, -hz + 0.014f),
                new Vector3(Dim.FeatureW + 0.10f, 2.42f, 0.028f), Mat.WalnutDark);
            // 中央大理石饰带（主位背景）
            Prim.Box("主景墙_大理石", p, new Vector3(0f, 1.55f, -hz + 0.030f),
                new Vector3(Dim.FeatureW, 2.30f, 0.030f), Mat.Marble);
            // 大理石的黄铜镶边
            var br = Mat.Brass;
            Prim.Box("主景墙_镶边_上", p, new Vector3(0f, 2.72f, -hz + 0.040f), new Vector3(Dim.FeatureW + 0.06f, 0.05f, 0.070f), br);
            Prim.Box("主景墙_镶边_下", p, new Vector3(0f, 0.40f, -hz + 0.040f), new Vector3(Dim.FeatureW + 0.06f, 0.05f, 0.070f), br);
            Prim.Box("主景墙_镶边_左", p, new Vector3(-fw - 0.03f, 1.55f, -hz + 0.040f), new Vector3(0.05f, 2.34f, 0.070f), br);
            Prim.Box("主景墙_镶边_右", p, new Vector3(fw + 0.03f, 1.55f, -hz + 0.040f), new Vector3(0.05f, 2.34f, 0.070f), br);

            // 两侧竖向木格栅墙板
            float sideW = hx - fw - 0.10f;
            foreach (var s in new[] { -1f, 1f })
            {
                float cx = s * (fw + 0.06f + sideW * 0.5f);
                Prim.Box("主景墙_格栅" + (s < 0 ? "左" : "右"), p,
                    new Vector3(cx, 1.60f, -hz + 0.028f),
                    new Vector3(sideW, 3.20f, 0.056f), Mat.SlatPanel);
                // 格栅上下收边
                Prim.Box("主景墙_格栅压条" + (s < 0 ? "左" : "右"), p,
                    new Vector3(cx, 3.16f, -hz + 0.036f),
                    new Vector3(sideW, 0.08f, 0.072f), Mat.WalnutDark);
            }
        }

        /// <summary>边柜墙 z=+3：木饰面 + 中段凹龛（陈设）。</summary>
        static void BuildWallSideboard(Transform p, float hx, float hz, float H, float T)
        {
            Prim.BoxBetween("陈设墙", p,
                new Vector3(-hx - T, 0f, hz), new Vector3(hx + T, H, hz + T), Mat.Wall);

            // 整面木饰面基板
            Prim.Box("陈设墙_木饰面", p, new Vector3(0f, 1.60f, hz - 0.022f),
                new Vector3(hx * 2f, 3.20f, 0.044f), Mat.Walnut);
            // 中段凹龛：上下两块"凸出的置物台"，陈设品放在上面。
            // 关键：台面必须足够宽且位于器物下方，否则东西会看起来悬空
            // （上一版隔板只有 0.16 深、器物却摆在距墙更远的位置，视觉上就是飘在半空）。
            float shelfY0 = 2.50f, shelfY1 = 1.42f;
            float shelfZ = hz - 0.22f;            // 台面中心 z = 2.78
            const float shelfD = 0.44f;           // 台面进深 0.44
            const float shelfW = 2.58f;

            // 浅色石材背板 + 黄铜边框
            Prim.Box("陈设墙_龛背板", p, new Vector3(0f, (shelfY0 + shelfY1) * 0.5f, hz - 0.045f),
                new Vector3(shelfW + 0.06f, shelfY0 - shelfY1, 0.02f), Mat.Stone);
            var br = Mat.Brass;
            Prim.Box("陈设墙_龛框_上", p, new Vector3(0f, shelfY0 + 0.02f, hz - 0.055f), new Vector3(shelfW + 0.12f, 0.04f, 0.06f), br);
            Prim.Box("陈设墙_龛框_下", p, new Vector3(0f, shelfY1 - 0.02f, hz - 0.055f), new Vector3(shelfW + 0.12f, 0.04f, 0.06f), br);
            Prim.Box("陈设墙_龛框_左", p, new Vector3(-shelfW * 0.5f - 0.03f, (shelfY0 + shelfY1) * 0.5f, hz - 0.055f), new Vector3(0.04f, shelfY0 - shelfY1, 0.06f), br);
            Prim.Box("陈设墙_龛框_右", p, new Vector3(shelfW * 0.5f + 0.03f, (shelfY0 + shelfY1) * 0.5f, hz - 0.055f), new Vector3(0.04f, shelfY0 - shelfY1, 0.06f), br);

            // 两块凸出的实木置物台（器物就放在这两个面上）
            for (int i = 0; i < 2; i++)
            {
                float y = i == 0 ? shelfY0 : (shelfY0 + shelfY1) * 0.5f + 0.06f;
                Prim.Box("陈设墙_置物台" + i, p, new Vector3(0f, y, shelfZ),
                    new Vector3(shelfW, 0.045f, shelfD), Mat.Walnut);
                // 台面下沿黄铜收边
                Prim.Box("陈设墙_置物台铜边" + i, p, new Vector3(0f, y - 0.028f, shelfZ - shelfD * 0.5f + 0.01f),
                    new Vector3(shelfW, 0.014f, 0.02f), br);
            }
        }

        // ------------------------------------------------------------------ 吊顶

        static void BuildCeiling(Transform p, float hx, float hz, float H)
        {
            // 吊顶板
            Prim.Box("吊顶", p, new Vector3(0f, H + Dim.CeilT * 0.5f, 0f),
                new Vector3(hx * 2f, Dim.CeilT, hz * 2f), Mat.Ceiling);

            // 中央木格栅天棚（会议区正上方，暖色木质风的标志性做法）。
            // 注意：它位于吊灯上方，所以吊灯的实际光源必须放在**灯体下方**，
            // 否则光源会被这块格栅 + 灯体木框夹在中间，桌面照不到光。
            float gw = 3.90f, gd = 2.10f;
            Prim.Box("吊顶_木格栅基板", p, new Vector3(0f, H - 0.030f, 0f), new Vector3(gw, 0.06f, gd), Mat.WalnutDark);
            // 格栅条（做出真正"木格栅"的观感）
            for (int i = 0; i < 11; i++)
            {
                float x = -gw * 0.5f + 0.18f + i * (gw - 0.36f) / 10f;
                Prim.Box("吊顶_格栅条" + i, p, new Vector3(x, H - 0.075f, 0f), new Vector3(0.055f, 0.04f, gd), Mat.Walnut);
            }

            // 周圈跌级 + 暖光灯槽
            float inset = 0.50f, bandW = 0.11f, dropH = 0.13f;
            var warm = Mat.Emissive("w_cove", new Color(1f, 0.82f, 0.58f), 0.55f);
            Prim.Box("灯槽_X负", p, new Vector3(-hx + inset, H - 0.040f, 0f), new Vector3(bandW, 0.03f, hz * 2f - inset * 2f), warm);
            Prim.Box("灯槽_X正", p, new Vector3(hx - inset, H - 0.040f, 0f), new Vector3(bandW, 0.03f, hz * 2f - inset * 2f), warm);
            Prim.Box("灯槽_Z负", p, new Vector3(0f, H - 0.040f, -hz + inset), new Vector3(hx * 2f - inset * 2f, 0.03f, bandW), warm);
            Prim.Box("灯槽_Z正", p, new Vector3(0f, H - 0.040f, hz - inset), new Vector3(hx * 2f - inset * 2f, 0.03f, bandW), warm);

            var trim = Mat.CeilingTrim;
            Prim.Box("跌级_X负", p, new Vector3(-hx + inset - bandW * 0.5f - 0.035f, H - dropH * 0.5f, 0f), new Vector3(0.07f, dropH, hz * 2f - inset * 2f), trim);
            Prim.Box("跌级_X正", p, new Vector3(hx - inset + bandW * 0.5f + 0.035f, H - dropH * 0.5f, 0f), new Vector3(0.07f, dropH, hz * 2f - inset * 2f), trim);
            Prim.Box("跌级_Z负", p, new Vector3(0f, H - dropH * 0.5f, -hz + inset - bandW * 0.5f - 0.035f), new Vector3(hx * 2f - inset * 2f, dropH, 0.07f), trim);
            Prim.Box("跌级_Z正", p, new Vector3(0f, H - dropH * 0.5f, hz - inset + bandW * 0.5f + 0.035f), new Vector3(hx * 2f - inset * 2f, dropH, 0.07f), trim);

            BuildDownlights(p, hx, hz, H);
            BuildCeilingSensors(p, H);
        }

        /// <summary>嵌入式筒灯：两排，暖色光。</summary>
        static void BuildDownlights(Transform p, float hx, float hz, float H)
        {
            var ring = Mat.Brass;
            var glow = Mat.Emissive("w_downlight", new Color(1f, 0.86f, 0.66f), 1.35f);
            for (int row = 0; row < 2; row++)
            {
                float z = row == 0 ? -2.05f : 2.05f;
                for (int i = 0; i < 4; i++)
                {
                    float x = -2.55f + i * 1.70f;
                    Prim.Cylinder("筒灯圈_" + row + "_" + i, p, new Vector3(x, H - 0.012f, z), 0.085f, 0.02f, ring);
                    Prim.Disc("筒灯发光_" + row + "_" + i, p, new Vector3(x, H - 0.021f, z), 0.072f, glow,
                        Quaternion.Euler(90f, 0f, 0f));
                }
            }
        }

        static void BuildCeilingSensors(Transform p, float H)
        {
            var white = Mat.PlasticWhite;
            var dark = Mat.Plastic;
            var smoke = new Vector3(-2.30f, H - 0.030f, -2.15f);
            Prim.Cylinder("烟雾探测器_壳体", p, smoke, 0.075f, 0.06f, white);
            Prim.Cylinder("烟雾探测器_格栅", p, new Vector3(smoke.x, smoke.y - 0.032f, smoke.z), 0.062f, 0.012f, dark);

            var lux = new Vector3(2.85f, H - 0.028f, 2.30f);
            Prim.Box("光照传感器_壳体", p, lux, new Vector3(0.10f, 0.05f, 0.10f), white);
            Prim.Disc("光照传感器_感光面", p, new Vector3(lux.x, lux.y - 0.028f, lux.z), 0.032f,
                Mat.Emissive("w_lux_led", new Color(1f, 0.85f, 0.45f), 1.2f), Quaternion.Euler(90f, 0f, 0f));
        }

        // ------------------------------------------------------------------ 踢脚与窗框

        static void BuildSkirting(Transform p, float hx, float hz)
        {
            var m = Mat.WalnutDark;
            const float h = 0.09f, t = 0.022f;
            Prim.Box("踢脚_X正", p, new Vector3(hx - t * 0.5f, h * 0.5f, 0f), new Vector3(t, h, hz * 2f), m);
            Prim.Box("踢脚_Z负", p, new Vector3(0f, h * 0.5f, -hz + t * 0.5f), new Vector3(hx * 2f, h, t), m);
            Prim.Box("踢脚_Z正", p, new Vector3(0f, h * 0.5f, hz - t * 0.5f), new Vector3(hx * 2f, h, t), m);
        }

        /// <summary>两樘窗的木质窗框 + 玻璃 + 石材窗台。</summary>
        static void BuildWindowFrames(Transform p, float hx, float H, float T)
        {
            var frame = Mat.WalnutDark;
            float x = -hx - T * 0.5f;
            const float fw = 0.07f, fd = 0.16f;
            float sill = Dim.WinSill, top = Dim.WinTop;

            foreach (var w in new[] {
                (min: Dim.WinZMinA, max: Dim.WinZMaxA, tag: "A"),
                (min: Dim.WinZMinB, max: Dim.WinZMaxB, tag: "B") })
            {
                float cz = (w.min + w.max) * 0.5f, wz = w.max - w.min;
                Prim.Box("窗框" + w.tag + "_上", p, new Vector3(x, top, cz), new Vector3(fd, fw, wz + fw), frame);
                Prim.Box("窗框" + w.tag + "_下", p, new Vector3(x, sill, cz), new Vector3(fd, fw, wz + fw), frame);
                Prim.Box("窗框" + w.tag + "_左", p, new Vector3(x, (sill + top) * 0.5f, w.min), new Vector3(fd, top - sill, fw), frame);
                Prim.Box("窗框" + w.tag + "_右", p, new Vector3(x, (sill + top) * 0.5f, w.max), new Vector3(fd, top - sill, fw), frame);

                // 中间一根竖挺（每樘分两格）
                float mz = (w.min + w.max) * 0.5f;
                Prim.Box("窗框" + w.tag + "_竖挺", p, new Vector3(x, (sill + top) * 0.5f, mz),
                    new Vector3(fd * 0.75f, top - sill, 0.05f), frame);

                // 玻璃
                float gz0 = w.min + fw * 0.5f, gz1 = w.max - fw * 0.5f;
                float gy0 = sill + fw * 0.5f, gy1 = top - fw * 0.5f;
                Prim.Box("窗玻璃" + w.tag, p, new Vector3(x, (gy0 + gy1) * 0.5f, (gz0 + gz1) * 0.5f),
                    new Vector3(0.012f, gy1 - gy0, gz1 - gz0), Mat.GlassWindow);

                // 石材窗台板
                Prim.Box("窗台板" + w.tag, p, new Vector3(-hx + 0.09f, sill - 0.016f, cz),
                    new Vector3(0.30f, 0.032f, wz + 0.18f), Mat.Stone);
            }
        }

        // ------------------------------------------------------------------ 电动门

        /// <summary>
        /// 玻璃门（右短墙 x=+4）。
        /// 门扇严格沿"本地 +Z"从铰链铺到门宽，绕本地 Y 轴旋转即为绕铰链摆动。
        /// 门扇及子物体在编辑器脚本里**不会被打静态标记**，否则旋转不会反映到渲染。
        /// </summary>
        static void BuildDoor(Transform p, float hx, float T)
        {
            var frame = Mat.WalnutDark;
            var handle = Mat.Brass;

            const float frameW = 0.06f;
            float leafT = T + 0.04f;
            float h = Dim.DoorH;
            float z0 = Dim.DoorZMin, z1 = Dim.DoorZMax;
            float leafW = z1 - z0;
            float cx = hx + T * 0.5f;

            // 门套
            Prim.Box("门套_上", p, new Vector3(cx, h + 0.035f, (z0 + z1) * 0.5f),
                new Vector3(T + 0.06f, 0.07f, leafW + 0.14f), frame);
            Prim.Box("门套_铰链侧", p, new Vector3(cx, (h + 0.07f) * 0.5f, z0 - 0.035f),
                new Vector3(T + 0.06f, h + 0.07f, 0.07f), frame);
            Prim.Box("门套_开合侧", p, new Vector3(cx, (h + 0.07f) * 0.5f, z1 + 0.035f),
                new Vector3(T + 0.06f, h + 0.07f, 0.07f), frame);

            // 门扇
            var leaf = new GameObject("门扇").transform;
            leaf.SetParent(p, false);
            leaf.localPosition = new Vector3(cx, 0f, z0);
            leaf.localRotation = Quaternion.Euler(0f, Dim.DoorClosedYaw, 0f);
            float zc = leafW * 0.5f;

            Prim.Box("门扇_玻璃", leaf, new Vector3(0f, h * 0.5f, zc),
                new Vector3(0.030f, h - 0.16f, leafW - 0.07f), Mat.GlassDoor);
            Prim.Box("门扇_横档_下", leaf, new Vector3(0f, 0.10f, zc), new Vector3(leafT, 0.20f, leafW), frame);
            Prim.Box("门扇_横档_上", leaf, new Vector3(0f, h - 0.060f, zc), new Vector3(leafT, 0.12f, leafW), frame);
            Prim.Box("门扇_竖挺_铰链侧", leaf, new Vector3(0f, h * 0.5f, frameW * 0.5f), new Vector3(leafT, h, frameW), frame);
            Prim.Box("门扇_竖挺_开合侧", leaf, new Vector3(0f, h * 0.5f, leafW - frameW * 0.5f), new Vector3(leafT, h, frameW), frame);

            // 黄铜长拉手（内外各一）
            Prim.Cylinder("门扇_拉手_内", leaf, new Vector3(-leafT * 0.62f, 1.05f, leafW - 0.14f), 0.017f, 1.00f, handle);
            Prim.Cylinder("门扇_拉手_外", leaf, new Vector3(leafT * 0.62f, 1.05f, leafW - 0.14f), 0.017f, 1.00f, handle);
            Prim.Box("门扇_拉手座_内", leaf, new Vector3(-leafT * 0.36f, 1.05f, leafW - 0.14f), new Vector3(0.02f, 0.10f, 0.05f), handle);
            Prim.Box("门扇_拉手座_外", leaf, new Vector3(leafT * 0.36f, 1.05f, leafW - 0.14f), new Vector3(0.02f, 0.10f, 0.05f), handle);

            // 合页
            for (int i = 0; i < 3; i++)
            {
                float y = 0.38f + i * 0.72f;
                Prim.Cylinder("门扇_合页" + i, leaf, new Vector3(0f, y, 0.014f), 0.023f, 0.095f, handle,
                    Quaternion.Euler(90f, 0f, 0f));
            }

            // 点选区（只有 Collider，无渲染体）
            var hit = Prim.Hitbox("设备_电动门", p, new Vector3(cx, h * 0.5f, (z0 + z1) * 0.5f),
                new Vector3(0.32f, h, leafW + 0.12f));
            var dc = hit.AddComponent<DeviceController>();
            dc.id = "door";
            dc.displayName = "电动门（开关）";
            dc.kind = DeviceKind.Door;
            dc.initialOn = false;
            dc.initialValue = 1f;
        }

        // ------------------------------------------------------------------ 落地空调

        /// <summary>
        /// 落地式柜机空调（白色立式），放在左后角（窗墙与陈设墙交角），正面朝房间中央。
        /// 正面（出风口/显示屏）在本地 -Z。
        /// </summary>
        static void BuildAcUnit(Transform p, float hx, float hz)
        {
            var root = new GameObject("立式空调").transform;
            root.SetParent(p, false);
            root.localPosition = new Vector3(-hx + 0.46f, 0f, hz - 0.70f);
            // 机器在左后角（窗墙 × 陈设墙交角）。正面（出风口/显示屏）在本地 -Z。
            // 按用户反馈调整 180°：当前 yaw = 139° + 180° = 319°，即正面朝房间内侧。
            root.localRotation = Quaternion.Euler(0f, 319f, 0f);
            var t = root.transform;

            var body = Mat.Pbr("w_ac_body", new Color(0.94f, 0.935f, 0.925f), 0f, 0.42f);
            var edge = Mat.Pbr("w_ac_edge", new Color(0.74f, 0.71f, 0.66f), 0.08f, 0.34f);
            var display = Mat.Pbr("w_ac_display", new Color(0.085f, 0.080f, 0.075f), 0.15f, 0.55f);
            var louver = Mat.Pbr("w_ac_louver", new Color(0.70f, 0.68f, 0.64f), 0f, 0.38f);
            var grille = Mat.Pbr("w_ac_grille", new Color(0.26f, 0.25f, 0.24f), 0.06f, 0.34f);

            const float W = 0.44f, D = 0.36f, BodyH = 1.88f;

            Prim.Box("空调_底座", t, new Vector3(0f, 0.022f, 0f), new Vector3(W + 0.05f, 0.044f, D + 0.05f), display);
            Prim.RoundedBox("空调_柜体", t, new Vector3(0f, BodyH * 0.5f + 0.04f, 0f), new Vector3(W, BodyH, D), body);

            // 侧面木纹装饰条（呼应木质风）
            Prim.Box("空调_木饰条_左", t, new Vector3(-W * 0.5f - 0.002f, BodyH * 0.5f + 0.04f, 0f),
                new Vector3(0.006f, BodyH - 0.30f, D * 0.55f), Mat.WalnutDark);
            Prim.Box("空调_木饰条_右", t, new Vector3(W * 0.5f + 0.002f, BodyH * 0.5f + 0.04f, 0f),
                new Vector3(0.006f, BodyH - 0.30f, D * 0.55f), Mat.WalnutDark);

            // 顶部深色显示面板
            Prim.Box("空调_显示面板", t, new Vector3(0f, BodyH - 0.13f, -D * 0.5f - 0.004f),
                new Vector3(W - 0.07f, 0.21f, 0.011f), display);
            Prim.Box("空调面板数显", t, new Vector3(-0.038f, BodyH - 0.13f, -D * 0.5f - 0.012f),
                new Vector3(0.17f, 0.088f, 0.005f),
                Mat.Emissive("w_ac_digit", new Color(1f, 0.78f, 0.36f), 2.5f));
            Prim.Sphere("空调面板指示灯", t, new Vector3(0.142f, BodyH - 0.13f, -D * 0.5f - 0.013f), 0.014f,
                Mat.Emissive("w_ac_led", new Color(1f, 0.80f, 0.40f), 2.8f));

            // 出风口（斜百叶）
            Prim.Box("空调_出风口框", t, new Vector3(0f, BodyH - 0.34f, -D * 0.5f - 0.006f),
                new Vector3(W - 0.08f, 0.18f, 0.013f), grille);
            for (int i = 0; i < 5; i++)
            {
                float y = BodyH - 0.41f + i * 0.038f;
                Prim.Box("空调_出风百叶" + i, t, new Vector3(0f, y, -D * 0.5f - 0.014f),
                    new Vector3(W - 0.11f, 0.015f, 0.008f), louver, Quaternion.Euler(-22f, 0f, 0f));
            }

            // 中部进风格栅
            Prim.Box("空调_进风格栅", t, new Vector3(0f, 0.92f, -D * 0.5f - 0.005f),
                new Vector3(W - 0.10f, 0.62f, 0.010f), grille);
            for (int i = 0; i < 9; i++)
            {
                float y = 0.66f + i * 0.065f;
                Prim.Box("空调_进气百叶" + i, t, new Vector3(0f, y, -D * 0.5f - 0.012f),
                    new Vector3(W - 0.13f, 0.018f, 0.006f), louver);
            }

            Prim.Box("空调_下出风", t, new Vector3(0f, 0.16f, -D * 0.5f - 0.005f),
                new Vector3(W - 0.12f, 0.12f, 0.008f), grille);
            Prim.Box("空调_顶部饰条", t, new Vector3(0f, BodyH + 0.028f, 0f),
                new Vector3(W - 0.02f, 0.02f, D - 0.02f), edge);

            // 命中盒：套住柜体，鼠标点空调本体即可开关
            var hit = Prim.Hitbox("设备_空调", p, new Vector3(-hx + 0.46f, 0.96f, hz - 0.70f),
                new Vector3(0.66f, 2.00f, 0.60f));
            var dc = hit.AddComponent<DeviceController>();
            dc.id = "ac";
            dc.displayName = "空调 / HVAC";
            dc.kind = DeviceKind.AirConditioner;
            dc.initialOn = true;
            dc.initialValue = 24f;
        }
    }
}
