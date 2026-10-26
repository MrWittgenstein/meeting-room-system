using UnityEngine;

namespace NordicRoom
{
    /// <summary>
    /// 可交互设备（北欧明亮风）。
    ///   · 主照明  → 吊轨上 3 盏北欧吊灯（可一键开关/调光）
    ///   · 窗帘    → 两堵长墙各 2 组（共 4 组）：外层浅灰遮光帘 + 内层纱帘，各自独立电机
    ///   · 电动门  → 见 Shell.BuildDoor
    ///   · 立式空调→ 见 Shell.BuildAcUnit
    ///   · 环境传感器 → 顶面烟雾/光照 + 墙面温湿度 + 柜上 CO₂ + 空间浮标
    ///   · 门禁面板 / 入口屏
    /// </summary>
    public static class Dev
    {
        public const string Root = "03_设备";

        /// <summary>
        /// 视觉引用容器（与"暖木会议室"的 Dev.Refs 同构）。
        /// 唯一区别：北欧风的窗户是 5 扇角窗，窗帘因此分成 4 组，用列表承载。
        /// </summary>
        public struct Refs
        {
            public Light chandelierLight;
            public MeshRenderer chandelierPanel;
            public System.Collections.Generic.List<System.Collections.Generic.List<Transform>> curtains;
            public MeshRenderer panelScreen;
            public MeshRenderer acUnitLed;
            public MeshRenderer acDisplay;
            public Transform doorLeaf;
        }

        public static Refs Build()
        {
            var refs = new Refs();
            refs.curtains = new System.Collections.Generic.List<System.Collections.Generic.List<Transform>>();
            var root = new GameObject(Root);
            var p = root.transform;

            BuildPendants(p);
            BuildCurtains(p);
            BuildEnvSensors(p);
            BuildWallPanel(p);
            BuildEntryPanel(p);
            BuildAccessControl(p);

            return refs;
        }

        // ================================================================== 吊灯

        static void BuildPendants(Transform p)
        {
            var shade = Mat.Pbr("n_shade", new Color(0.955f, 0.950f, 0.940f), 0f, 0.32f);
            var cord = Mat.BlackMetal;
            var fittings = Mat.AshWood;

            // 三盏吊灯挂在三条吊轨上（吊轨 z = -1.60 / 0 / +1.60，y = H-0.035）
            float[] railZ = { -1.60f, 0f, 1.60f };
            float[] xOff = { -0.72f, 0f, 0.72f };

            var g = new GameObject("主照明_吊灯组").transform;
            g.SetParent(p, false);

            // 实际光源：暖白点光源，**必须在灯罩下沿之下**。
            // 放进灯罩内部会在每盏灯下面糊出一圈假的自阴影"锥体"。
            var lightGo = new GameObject("主灯光源");
            lightGo.transform.SetParent(g, false);
            lightGo.transform.localPosition = new Vector3(0f, 1.97f, 0f);
            var pl = lightGo.AddComponent<Light>();
            pl.type = LightType.Point;
            pl.color = new Color(1f, 0.955f, 0.895f);
            pl.intensity = 6.0f;
            pl.range = 9.5f;
            pl.shadows = LightShadows.None;   // 点光源投影会在灯罩下糊出死黑锥体，这里不投影
            pl.renderMode = LightRenderMode.ForcePixel;

            for (int i = 0; i < 3; i++)
            {
                for (int k = 0; k < 3; k++)
                {
                    float x = xOff[k];
                    float z = railZ[i];
                    if (i == 1 && k == 1) continue;  // 中间一盏挂在正中，略去重叠位
                    string nm = "吊灯" + i + k;
                    BuildPendant(g, nm, new Vector3(x, 0f, z), shade, cord, fittings);
                }
            }
        }

        static void BuildPendant(Transform parent, string name, Vector3 pos, Material shade, Material cord, Material fittings)
        {
            const float H = Dim.Height;
            const float shadeTop = 2.06f, shadeH = 0.30f;
            float shadeR = 0.185f;

            var t = new GameObject(name).transform;
            t.SetParent(parent, false);
            t.localPosition = pos;

            // 细吊线
            Prim.Cylinder("吊线", t, new Vector3(0f, (H - 0.055f + shadeTop + shadeH) * 0.5f, 0f), 0.006f,
                H - 0.055f - (shadeTop + shadeH), cord, null, false, 12);
            Prim.Cylinder("吊线座", t, new Vector3(0f, H - 0.062f, 0f), 0.030f, 0.030f, fittings, null, false, 20);

            // 灯罩：整只圆柱（自带上下盖），本身用"亮着的灯罩"自发光材质。
            // 照"暖木会议室"的做法：发光体直接做在灯体上（灯罩自发光），
            // 不再单独放一片朝下的发光面 —— 那片盘子在斜下方会露出一根白色尖刺。
            Prim.Cylinder("灯罩_主体", t, new Vector3(0f, shadeTop + shadeH * 0.5f, 0f), shadeR, shadeH, Mat.ShadeLit, null, false, 56);
            // 顶部收口环 + 顶盖
            Prim.Cylinder("灯罩_收口", t, new Vector3(0f, shadeTop + shadeH + 0.012f, 0f), shadeR * 0.70f, 0.024f, Mat.ShadeLit, null, false, 48);
            Prim.Cylinder("灯罩_顶盖", t, new Vector3(0f, shadeTop + shadeH + 0.034f, 0f), shadeR * 0.34f, 0.022f, fittings, null, false, 32);
        }

        // ================================================================== 窗帘

        /// <summary>
        /// 两长墙各开 2 扇角窗 → 4 组窗帘，每组一套电机。
        /// 每组窗帘分左右两片：贴窗框两端，各占窗宽一半，闭合时正好拼满整个窗洞。
        /// </summary>
        static void BuildCurtains(Transform p)
        {
            var g = new GameObject("窗帘").transform;
            g.SetParent(p, false);

            float sill = Dim.WinSill, top = Dim.WinTop;
            float zN = Dim.HZ - 0.115f;
            float zS = -Dim.HZ + 0.115f;

            // 每堵墙两扇窗：(窗X起, 窗X止, 侧别标签)
            var wins = new[] {
                (x0: Dim.NorthWinXMin,  x1: Dim.NorthWinXMax,  tag: "西"),
                (x0: Dim.NorthWinXMin2, x1: Dim.NorthWinXMax2, tag: "东") };

            int gi = 0;
            foreach (var wl in new[] { (z: zN, tag: "北"), (z: zS, tag: "南") })
                foreach (var w in wins)
                {
                    var root = new GameObject("窗帘组_" + wl.tag + w.tag).transform;
                    root.SetParent(g, false);
                    root.localPosition = new Vector3(0f, 0f, wl.z);
                    BuildCurtainGroup(root, w.x0, w.x1, sill, top, wl.tag + w.tag, "curtain" + gi);
                    gi++;
                }
        }

        /// <summary>
        /// 一组窗帘：窗帘杆 + 左片 + 右片（各含遮光帘和纱帘）。
        /// 关键约定（沿用已验证的规则）：
        ///   · 每片宽度 = 窗洞宽的一半；
        ///   · 靠小坐标一端的那片，展开方向 dir = -1；靠大坐标一端 dir = +1；
        ///   · 片中心 = 锚点 - dir * span * 0.5，即从锚点向窗中心方向铺开。
        /// </summary>
        static void BuildCurtainGroup(Transform root, float x0, float x1, float sill, float top, string tag, string deviceId)
        {
            float span = x1 - x0;           // 窗洞宽
            float halfW = span * 0.5f;      // 每片帘宽
            float cy = (sill + top) * 0.5f;
            float hgt = top - sill;

            var motor = Mat.Pbr("n_motor", new Color(0.900f, 0.898f, 0.890f), 0.05f, 0.30f);

            // ---- 窗帘杆（细金属管 + 两端支座）----
            float rodY = top + 0.075f;
            Prim.Cylinder("窗帘杆", root, new Vector3((x0 + x1) * 0.5f, rodY, 0f), 0.016f, span + 0.34f, Mat.Steel,
                Quaternion.Euler(0f, 0f, 90f), false, 20);
            foreach (var s in new[] { -1f, 1f })
            {
                Prim.Box("杆座" + (s < 0 ? "L" : "R"), root,
                    new Vector3(s > 0 ? x1 + 0.15f : x0 - 0.15f, rodY + 0.022f, 0.02f),
                    new Vector3(0.05f, 0.07f, 0.05f), Mat.Steel);
                Prim.Cylinder("杆头" + (s < 0 ? "L" : "R"), root,
                    new Vector3(s > 0 ? x1 + 0.19f : x0 - 0.19f, rodY, 0f), 0.028f, 0.030f, Mat.Chrome,
                    Quaternion.Euler(0f, 0f, 90f), false, 24);
            }

            // ---- 两侧电机盒（藏在杆座下方）----
            foreach (var s in new[] { -1f, 1f })
                Prim.Box("窗帘电机" + (s < 0 ? "L" : "R"), root,
                    new Vector3(s > 0 ? x1 - 0.10f : x0 + 0.10f, rodY - 0.050f, 0.035f),
                    new Vector3(0.150f, 0.090f, 0.080f), motor);

            // ---- 帘片 ----
            var outer = Mat.GreyCurtain;
            var sheer = Mat.SheerFab;
            var sheerIn = Mat.Sheer;

            for (int side = 0; side < 2; side++)
            {
                // side 0 → 小坐标锚点（dir = -1）；side 1 → 大坐标锚点（dir = +1）
                float dir = (side == 0) ? -1f : 1f;
                float anchor = (side == 0) ? x0 : x1;                    // 锚点贴在窗洞边缘
                float centerX = anchor - dir * halfW * 0.5f;              // 帘片中心
                string sd = (side == 0) ? "左" : "右";

                var grp = new GameObject("帘片_" + tag + "_" + sd).transform;
                grp.SetParent(root, false);
                grp.localPosition = new Vector3(anchor, 0f, 0f);

                // 遮光帘（外层，靠室内）
                var c = Prim.Box("遮光帘", grp, new Vector3(-dir * halfW * 0.5f, cy, 0.055f),
                    new Vector3(halfW, hgt, 0.032f), outer);
                c.name = "遮光帘";

                // 纱帘（内层，贴窗）
                Prim.Box("纱帘", grp, new Vector3(-dir * halfW * 0.5f, cy, -0.012f),
                    new Vector3(halfW, hgt, 0.010f), sheer);

                // 一片薄纱提亮（起皱感）
                Prim.Box("纱帘_内衬", grp, new Vector3(-dir * halfW * 0.5f, cy, 0.006f),
                    new Vector3(halfW * 0.96f, hgt * 0.99f, 0.006f), sheerIn);

                // 帘顶挂环
                int rings = 6;
                for (int i = 0; i < rings; i++)
                {
                    float u = (i + 0.5f) / rings;
                    float x = -dir * halfW * u;
                    Prim.Cylinder("挂环", grp, new Vector3(x, rodY + 0.002f, 0.040f), 0.021f, 0.012f, Mat.Chrome,
                        Quaternion.Euler(90f, 0f, 0f), false, 16);
                }
            }

            // ---- 该组窗帘的命中体（点击可开关这一组）----
            var hit = Prim.Hitbox("设备_窗帘_" + tag, root,
                new Vector3((x0 + x1) * 0.5f, cy, 0.05f), new Vector3(span, hgt, 0.20f));
            var dc = hit.AddComponent<DeviceController>();
            dc.id = deviceId;   // curtain0..curtain3，与"暖木会议室"的 curtainA/B 同一套用法
            dc.displayName = "电动窗帘 " + tag + " 组";
            dc.kind = DeviceKind.Curtain;
            dc.initialOn = true;      // 白天默认拉开采光
            dc.initialValue = 1f;     // 1 = 全开
        }

        // ================================================================== 环境传感器

        static void BuildEnvSensors(Transform p)
        {
            var shell = Mat.PlasticWhite;

            // 北墙中段：温湿度一体传感器
            var th = new Vector3(-3.10f, 1.46f, Dim.HZ - 0.055f);
            Prim.RoundedBox("温湿传感器_外壳", p, th, new Vector3(0.150f, 0.150f, 0.048f), shell);
            Prim.Disc("温湿传感器_屏", p, th + new Vector3(0f, 0.028f, -0.030f), 0.046f, Mat.AcScreen,
                Quaternion.Euler(90f, 0f, 0f));
            Prim.Sphere("温湿传感器_指示灯", p, th + new Vector3(0.052f, -0.048f, -0.030f), 0.012f,
                Mat.Emissive("n_th_led", new Color(0.45f, 0.95f, 0.62f), 0.35f));
            for (int i = 0; i < 4; i++)
                Prim.Box("温湿传感器_散热孔" + i, p, th + new Vector3(-0.045f, -0.030f + i * 0.020f, -0.026f),
                    new Vector3(0.060f, 0.008f, 0.006f), Mat.Plastic);

            // 南墙中段：空气质量（CO₂）传感器
            var co2 = new Vector3(-2.05f, 1.50f, -Dim.HZ + 0.055f);
            Prim.RoundedBox("CO2传感器_外壳", p, co2, new Vector3(0.140f, 0.170f, 0.046f), shell);
            Prim.Disc("CO2传感器_屏", p, co2 + new Vector3(0f, 0.032f, 0.029f), 0.042f, Mat.AcScreen,
                Quaternion.Euler(90f, 0f, 0f));
            Prim.Box("CO2传感器_进气格栅", p, co2 + new Vector3(0f, -0.048f, 0.026f),
                new Vector3(0.090f, 0.040f, 0.008f), Mat.Perforated);

            // 后墙：光照传感器（靠近大窗，测自然采光）
            var lux = new Vector3(Dim.HX - 0.055f, 2.86f, -2.30f);
            Prim.RoundedBox("光照传感器", p, lux, new Vector3(0.048f, 0.098f, 0.098f), shell);
            Prim.Sphere("光照传感器_探头", p, lux + new Vector3(-0.030f, 0.026f, 0f), 0.016f,
                Mat.Emissive("n_lux_led2", new Color(0.62f, 0.94f, 1f), 0.32f));

            // 顶面：CO₂ + 人流计数（毫米波）
            var occ = new Vector3(-1.20f, Dim.Height - 0.035f, 1.10f);
            Prim.Cylinder("人流传感器_外壳", p, occ, 0.070f, 0.070f, shell, null, false, 32);
            Prim.Disc("人流传感器_面", p, occ + new Vector3(0f, -0.038f, 0f), 0.060f, Mat.Perforated,
                Quaternion.Euler(90f, 0f, 0f));

            // 说明：这里**不再**建 3D 浮标。
            // 面板（RoomHud）已经有一套屏幕气泡，两套并存会重复，
            // 而且 3D 浮标钉在房间中间，看着就是"半空中的怪东西"。
            // 现在只保留面板气泡，并且把它们贴到各自的真实传感器位置上。
        }

        // （原 BuildBubble 已删除：3D 浮标与面板气泡重复，统一由面板气泡承担）

        // ================================================================== 墙面控制屏

        static void BuildWallPanel(Transform p)
        {
            // 北墙中段：会议控制屏（触摸面板）
            var pos = new Vector3(2.00f, 1.50f, Dim.HZ - 0.050f);
            Prim.RoundedBox("控制屏_外壳", p, pos, new Vector3(0.480f, 0.320f, 0.046f),
                Mat.Pbr("n_panel_case", new Color(0.925f, 0.922f, 0.915f), 0.05f, 0.34f));
            Prim.Box("控制屏_屏幕", p, pos + new Vector3(0f, 0.010f, -0.028f), new Vector3(0.420f, 0.250f, 0.008f), Mat.PanelScreen);
            // 屏幕上的示意按钮
            for (int i = 0; i < 4; i++)
                Prim.Box("控制屏_按钮" + i, p, pos + new Vector3(-0.128f + i * 0.086f, -0.086f, -0.036f),
                    new Vector3(0.062f, 0.048f, 0.006f), Mat.AcScreen);

            var hit = Prim.Hitbox("设备_控制屏", p, pos, new Vector3(0.560f, 0.400f, 0.18f));
            var dc = hit.AddComponent<DeviceController>();
            dc.id = "panel";
            dc.displayName = "会议控制屏";
            dc.kind = DeviceKind.EnvironmentPanel;
            dc.initialOn = true;
            dc.initialValue = 0.5f;
        }

        // ================================================================== 入口屏

        static void BuildEntryPanel(Transform p)
        {
            // 门内侧墙面（-X 墙，门洞南侧）
            var pos = new Vector3(Dim.WallDoor + 0.038f, 1.44f, 1.60f);
            Prim.RoundedBox("入口屏_外壳", p, pos, new Vector3(0.040f, 0.340f, 0.240f),
                Mat.Pbr("n_entry_case", new Color(0.930f, 0.928f, 0.920f), 0.05f, 0.34f));
            Prim.Box("入口屏_屏幕", p, pos + new Vector3(0.026f, 0.030f, 0f), new Vector3(0.010f, 0.250f, 0.190f), Mat.PanelScreen);
            Prim.Box("入口屏_铭牌", p, pos + new Vector3(0.024f, -0.132f, 0f), new Vector3(0.008f, 0.052f, 0.170f), Mat.Paper);

            // 门内侧：门磁开关
            var mag = new Vector3(Dim.WallDoor + 0.030f, 2.36f, -1.05f);
            Prim.Box("门磁_本体", p, mag, new Vector3(0.030f, 0.062f, 0.100f), Mat.PlasticWhite);
            Prim.Box("门磁_磁条", p, mag + new Vector3(0.014f, 0f, 0.108f), new Vector3(0.020f, 0.050f, 0.080f), Mat.Plastic);

            var hit = Prim.Hitbox("设备_入口屏", p, pos, new Vector3(0.18f, 0.420f, 0.330f));
            var dc = hit.AddComponent<DeviceController>();
            dc.id = "entry";
            dc.displayName = "入口环境屏";
            dc.kind = DeviceKind.EnvironmentPanel;
            dc.initialOn = true;
            dc.initialValue = 0.5f;
        }

        // ================================================================== 门禁

        static void BuildAccessControl(Transform p)
        {
            // 门外侧读卡器（-X 墙外侧）
            var pos = new Vector3(Dim.WallDoor - Dim.WallT - 0.028f, 1.24f, 1.16f);
            Prim.RoundedBox("门禁_读卡器", p, pos, new Vector3(0.030f, 0.200f, 0.130f),
                Mat.Pbr("n_access_case", new Color(0.115f, 0.118f, 0.122f), 0.10f, 0.42f));
            Prim.Disc("门禁_感应区", p, pos + new Vector3(-0.018f, 0.038f, 0f), 0.042f,
                Mat.Emissive("n_access_led", new Color(0.45f, 0.92f, 1f), 0.32f), Quaternion.Euler(0f, 0f, 90f));
            Prim.Box("门禁_读卡槽", p, pos + new Vector3(-0.016f, -0.056f, 0f), new Vector3(0.008f, 0.052f, 0.086f), Mat.Perforated);

            // 门内上方：闭门器
            Prim.Box("闭门器", p, new Vector3(Dim.WallDoor + 0.055f, 2.30f, 1.05f), new Vector3(0.090f, 0.052f, 0.220f), Mat.Chrome);

            var hit = Prim.Hitbox("设备_门禁", p, pos, new Vector3(0.16f, 0.280f, 0.220f));
            var dc = hit.AddComponent<DeviceController>();
            dc.id = "access";
            dc.displayName = "门禁 / 刷卡";
            dc.kind = DeviceKind.AccessControl;
            dc.initialOn = true;
            dc.initialValue = 1f;
        }
    }
}
