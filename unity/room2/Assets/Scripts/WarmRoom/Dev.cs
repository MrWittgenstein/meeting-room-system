using UnityEngine;

namespace WarmRoom
{
    /// <summary>
    /// 智能设备（暖色木质商务风）：主吊灯、两樘窗的电动窗帘、环境传感器组、
    /// 门口环境面板、人脸识别门禁。门与落地空调在 Shell 里随房间一起建。
    /// </summary>
    public static class Dev
    {
        public const string Root = "03_智能设备";

        public struct Refs
        {
            public Light chandelierLight;
            public MeshRenderer chandelierPanel;
            public Transform curtainA_L, curtainA_R;   // 南侧窗
            public Transform curtainB_L, curtainB_R;   // 北侧窗
            public MeshRenderer panelScreen;
            public MeshRenderer acUnitLed;
            public MeshRenderer acDisplay;
            public Transform doorLeaf;
        }

        public static Refs Build()
        {
            var refs = new Refs();
            var root = new GameObject(Root);
            var p = root.transform;

            refs.chandelierLight = BuildChandelier(p, out refs.chandelierPanel);
            BuildCurtains(p, refs);
            BuildSensorNodes(p);
            BuildEnvironmentPanel(p, out refs.panelScreen);
            BuildAccessControl(p);
            BuildWallClock(p);
            BuildSignage(p);

            return refs;
        }

        // ================================================================== 主吊灯

        /// <summary>
        /// 木质 + 黄铜线性吊灯（会议桌正上方）。
        /// 灯体由木质外框、黄铜压条、暖色发光扩散板组成；实际光源为一只点光源（软阴影）。
        /// </summary>
        static Light BuildChandelier(Transform p, out MeshRenderer panel)
        {
            float L = 3.20f, D = 0.62f, y = 2.36f;

            var root = new GameObject("主吊灯").transform;
            root.SetParent(p, false);
            var t = root;

            // 吊索（黄铜）
            float cableTop = Dim.Height, cableLen = cableTop - y - 0.07f;
            for (int i = 0; i < 2; i++)
            {
                float x = (i == 0 ? -1f : 1f) * (L * 0.5f - 0.20f);
                Prim.Cylinder("吊索" + i, t, new Vector3(x, y + 0.07f + cableLen * 0.5f, 0f), 0.006f, cableLen, Mat.Brass);
                Prim.Cylinder("吊顶座" + i, t, new Vector3(x, cableTop - 0.014f, 0f), 0.048f, 0.028f, Mat.Brass);
            }

            // 木质外框
            Prim.RoundedBox("灯体_木框", t, new Vector3(0f, y + 0.085f, 0f), new Vector3(L, 0.10f, D + 0.06f), Mat.Walnut);
            // 黄铜下压条
            Prim.Box("灯体_铜压条", t, new Vector3(0f, y + 0.020f, 0f), new Vector3(L + 0.02f, 0.022f, D + 0.07f), Mat.Brass);
            // 暖色发光扩散板
            var diff = Prim.Box("灯体_扩散板", t, new Vector3(0f, y - 0.004f, 0f),
                new Vector3(L - 0.10f, 0.018f, D - 0.04f),
                Mat.Emissive("w_chandelier_panel", new Color(1f, 0.84f, 0.62f), 1.7f));
            panel = diff.GetComponent<MeshRenderer>();
            // 灯体下沿细边
            Prim.Box("灯体_下边框", t, new Vector3(0f, y - 0.020f, 0f), new Vector3(L, 0.020f, D + 0.06f), Mat.Brass);

            // 实际光源：暖白点光源
            // 实际光源：暖白点光源。
            // **位置必须在灯体下方**：灯体木框在 y+0.02~y+0.13，木格栅吊顶在更上方。
            // 上一版把光源放在 y-0.03（灯体内部），被自身上下夹住，导致桌面完全照不到光、
            // 渲染成近黑色。这里下移到灯体下方 8cm。
            var lightGo = new GameObject("主灯光源");
            lightGo.transform.SetParent(t, false);
            lightGo.transform.localPosition = new Vector3(0f, y - 0.08f, 0f);
            var l = lightGo.AddComponent<Light>();
            l.type = LightType.Point;
            l.color = new Color(1f, 0.855f, 0.680f);   // 约 3000K 暖白
            l.intensity = 8.5f;
            l.range = 14f;
            l.shadows = LightShadows.Soft;
            l.shadowStrength = 0.70f;
            l.shadowBias = 0.06f;
            l.shadowNormalBias = 0.24f;
            l.renderMode = LightRenderMode.ForcePixel;

            // 命中盒
            var hit = Prim.Hitbox("设备_主吊灯", p, new Vector3(0f, y + 0.045f, 0f), new Vector3(L, 0.22f, D + 0.08f));
            var dc = hit.AddComponent<DeviceController>();
            dc.id = "mainlight";
            dc.displayName = "主吊灯（调光）";
            dc.kind = DeviceKind.MainLight;
            dc.initialOn = true;
            dc.initialValue = 1f;

            return l;
        }

        // ================================================================== 电动窗帘

        /// <summary>
        /// 两樘窗各两片窗帘，共 4 片；全部输出到 refs 供控制器驱动。
        ///
        /// 关键（上一版踩过的坑，这里从设计上规避）：
        ///   1) 每片布面宽度 = 半樘窗宽，从锚点**朝窗中心单向**铺开，
        ///      关合时同一樘的两片在窗中心拼合，正好等于窗宽，不会越出窗框；
        ///   2) 全部用**正缩放**，方向由几何决定，不靠负缩放镜像；
        ///   3) 动画面循环里**一定有窗帘**（上一版漏写导致按 4 没反应）。
        /// </summary>
        static void BuildCurtains(Transform p, Refs refs)
        {
            // 窗帘挂在窗内侧，比窗框更靠房间（x 更大）
            const float trackX = -Dim.HX + 0.16f;   // -3.84
            float trackY = Dim.WinTop + 0.13f;

            const float frameW = 0.07f;
            float curtainBottom = Dim.WinSill - 0.06f;
            float curtainH = (Dim.WinTop + 0.10f) - curtainBottom;

            // 窗帘盒 + 轨道（覆盖两樘窗之间的范围）
            float zAll0 = Dim.WinZMinA - 0.16f, zAll1 = Dim.WinZMaxB + 0.16f;
            Prim.Box("窗帘盒", p, new Vector3(trackX, trackY + 0.070f, (zAll0 + zAll1) * 0.5f),
                new Vector3(0.24f, 0.15f, zAll1 - zAll0), Mat.CeilingTrim);
            Prim.Box("窗帘轨道", p, new Vector3(trackX, trackY, (zAll0 + zAll1) * 0.5f),
                new Vector3(0.06f, 0.035f, zAll1 - zAll0), Mat.Brass);

            // ---- 南侧窗 A ----
            // 方向约定：dir = -1 表示布面从锚点朝 **-Z** 铺开，dir = +1 表示朝 **+Z** 铺开。
            // A 窗的两片锚在玻璃南北两端，"左"（z 较小端）需朝 +Z（窗中心），"右"（z 较大端）需朝 -Z。
            // 上一版把这两个方向写反了，导致每樘窗的两片都朝同一侧跑、玻璃盖不住。
            {
                float z0 = Dim.WinZMinA + frameW * 0.5f;   // 玻璃南缘（锚点）
                float z1 = Dim.WinZMaxA - frameW * 0.5f;   // 玻璃北缘（锚点）
                float halfW = (z1 - z0) * 0.5f;
                refs.curtainA_L = MakePanel(p, "窗帘A_左", new Vector3(trackX, trackY - 0.03f, z0), -1f, halfW, curtainH);
                refs.curtainA_R = MakePanel(p, "窗帘A_右", new Vector3(trackX, trackY - 0.03f, z1), +1f, halfW, curtainH);

                // 纱帘（固定）
                Prim.Box("纱帘A", p, new Vector3(-Dim.HX + 0.09f, (Dim.WinTop + Dim.WinSill) * 0.5f, (z0 + z1) * 0.5f),
                    new Vector3(0.004f, Dim.WinTop - Dim.WinSill, z1 - z0), Mat.Sheer);

                // 命中盒
                var hit = Prim.Hitbox("设备_电动窗帘A", p, new Vector3(trackX, (trackY + curtainBottom) * 0.5f, (z0 + z1) * 0.5f),
                    new Vector3(0.20f, curtainH, z1 - z0));
                var dc = hit.AddComponent<DeviceController>();
                dc.id = "curtainA";
                dc.displayName = "电动窗帘（南窗）";
                dc.kind = DeviceKind.Curtain;
                dc.initialOn = false;
                dc.initialValue = 1f;
            }

            // ---- 北侧窗 B ----
            // 同样：左片锚在 z 较小端(0.485)、需朝 +Z 展；右片锚在 z 较大端(2.315)、需朝 -Z 收。
            // 也就是与 A 窗相反。两处方向都由"锚点相对玻璃中心的位置"决定：
            //   center = -dir*span/2，所以 z 较小端 → dir=-1；z 较大端 → dir=+1。
            {
                float z0 = Dim.WinZMinB + frameW * 0.5f;
                float z1 = Dim.WinZMaxB - frameW * 0.5f;
                float halfW = (z1 - z0) * 0.5f;
                refs.curtainB_L = MakePanel(p, "窗帘B_左", new Vector3(trackX, trackY - 0.03f, z0), -1f, halfW, curtainH);
                refs.curtainB_R = MakePanel(p, "窗帘B_右", new Vector3(trackX, trackY - 0.03f, z1), +1f, halfW, curtainH);

                Prim.Box("纱帘B", p, new Vector3(-Dim.HX + 0.09f, (Dim.WinTop + Dim.WinSill) * 0.5f, (z0 + z1) * 0.5f),
                    new Vector3(0.004f, Dim.WinTop - Dim.WinSill, z1 - z0), Mat.Sheer);

                var hit = Prim.Hitbox("设备_电动窗帘B", p, new Vector3(trackX, (trackY + curtainBottom) * 0.5f, (z0 + z1) * 0.5f),
                    new Vector3(0.20f, curtainH, z1 - z0));
                var dc = hit.AddComponent<DeviceController>();
                dc.id = "curtainB";
                dc.displayName = "电动窗帘（北窗）";
                dc.kind = DeviceKind.Curtain;
                dc.initialOn = false;
                dc.initialValue = 1f;
            }

            // 窗帘电机（两个）
            Prim.Box("窗帘电机A", p, new Vector3(trackX, trackY, zAll0 + 0.10f), new Vector3(0.16f, 0.09f, 0.09f), Mat.Brass);
            Prim.Box("窗帘电机B", p, new Vector3(trackX, trackY, zAll1 - 0.10f), new Vector3(0.16f, 0.09f, 0.09f), Mat.Brass);
        }

        /// <summary>
        /// 单片窗帘布。
        /// dir = +1：从锚点朝 +Z 铺开；dir = -1：朝 -Z 铺开。
        /// 布面宽度 = span，锚点即布面的**外侧边缘**，所以关闭时两片正好在窗中心拼合。
        /// 几何相对 root 的位置必须按 dir 折算到世界 Z（因为 root 没有旋转）。
        /// </summary>
        static Transform MakePanel(Transform p, string name, Vector3 anchor, float dir, float span, float height)
        {
            var root = new GameObject(name).transform;
            root.SetParent(p, false);
            root.localPosition = anchor;

            var geo = new GameObject("几何").transform;
            geo.SetParent(root, false);

            float center = -dir * span * 0.5f;   // 世界 Z 方向
            Prim.Box("布面", geo, new Vector3(0.014f, -height * 0.5f, center),
                new Vector3(0.035f, height, span), Mat.Curtain);
            Prim.Box("布面_折边内侧", geo, new Vector3(0.014f, -height * 0.5f, center * 2f),
                new Vector3(0.045f, height, 0.02f), Mat.Curtain);
            Prim.Box("布面_折边外侧", geo, new Vector3(0.014f, -height * 0.5f, 0f),
                new Vector3(0.045f, height, 0.02f), Mat.Curtain);

            for (int i = 0; i < 6; i++)
            {
                float t = 0.08f + i * 0.84f / 5f;
                Prim.Cylinder("挂钩环" + i, geo, new Vector3(0.014f, 0.015f, -dir * span * t),
                    0.018f, 0.012f, Mat.Brass);
            }
            return root;
        }

        // ================================================================== 传感器

        static void BuildSensorNodes(Transform p)
        {
            // 温湿度（陈设墙 - 中段）
            MakeSensor(p, "传感器_温湿度", new Vector3(2.20f, 1.70f, Dim.HZ - 0.045f),
                Quaternion.Euler(0f, 180f, 0f), new Color(1f, 0.62f, 0.25f));
            // 光照（窗侧）
            MakeSensor(p, "传感器_光照", new Vector3(-3.60f, 1.70f, 2.55f),
                Quaternion.Euler(0f, 90f, 0f), new Color(1f, 0.88f, 0.35f));
            // 烟雾（主景墙侧高处）
            MakeSensor(p, "传感器_烟雾", new Vector3(-2.30f, 2.35f, Dim.WallFeature + 0.03f),
                Quaternion.identity, new Color(0.60f, 0.78f, 0.95f));
            // 人体感应（门上方）
            MakeSensor(p, "传感器_人体感应", new Vector3(Dim.HX - 0.03f, 2.42f, 1.30f),
                Quaternion.Euler(0f, -90f, 0f), new Color(0.50f, 0.95f, 0.60f));

            var hit = Prim.Hitbox("设备_环境传感器", p, new Vector3(2.20f, 1.70f, Dim.HZ - 0.045f),
                new Vector3(0.16f, 0.16f, 0.10f));
            var dc = hit.AddComponent<DeviceController>();
            dc.id = "sensors";
            dc.displayName = "环境传感器组";
            dc.kind = DeviceKind.Sensor;
            dc.initialOn = true;
            dc.initialValue = 1f;
        }

        static Transform MakeSensor(Transform p, string name, Vector3 pos, Quaternion rot, Color led)
        {
            var root = new GameObject(name).transform;
            root.SetParent(p, false);
            root.localPosition = pos;
            root.localRotation = rot;

            Prim.RoundedBox("壳体", root, Vector3.zero, new Vector3(0.088f, 0.115f, 0.030f), Mat.PlasticWhite);
            Prim.Box("透光窗", root, new Vector3(0f, 0.020f, 0.017f), new Vector3(0.050f, 0.032f, 0.004f), Mat.AcScreen);
            Prim.Sphere("LED", root, new Vector3(0f, -0.034f, 0.018f), 0.0068f,
                Mat.Emissive("w_sensor_led_" + name, led, 2.4f));
            Prim.Box("格栅", root, new Vector3(0f, -0.046f, 0.013f), new Vector3(0.052f, 0.006f, 0.006f), Mat.Plastic);
            return root;
        }

        // ================================================================== 门口环境面板

        static void BuildEnvironmentPanel(Transform p, out MeshRenderer screen)
        {
            // 装在门墙（+X）内侧、门洞北侧
            var root = new GameObject("门口环境面板").transform;
            root.SetParent(p, false);
            root.localPosition = new Vector3(Dim.HX - 0.030f, 1.44f, 1.02f);
            root.localRotation = Quaternion.Euler(0f, -90f, 0f);

            Prim.RoundedBox("面板_框", root, Vector3.zero, new Vector3(0.32f, 0.46f, 0.024f), Mat.WalnutDark);
            var s = Prim.Box("面板_屏", root, new Vector3(0f, 0f, 0.014f), new Vector3(0.276f, 0.412f, 0.004f), Mat.PanelScreen);
            screen = s.GetComponent<MeshRenderer>();
            Prim.Box("面板_触摸条", root, new Vector3(0f, -0.202f, 0.015f), new Vector3(0.21f, 0.014f, 0.003f),
                Mat.Emissive("w_touchbar", new Color(1f, 0.76f, 0.34f), 1.4f));
            Prim.Box("面板_顶部饰条", root, new Vector3(0f, 0.235f, 0.010f), new Vector3(0.30f, 0.012f, 0.020f), Mat.Brass);

            var hit = Prim.Hitbox("设备_环境面板", p, new Vector3(Dim.HX - 0.06f, 1.44f, 1.02f),
                new Vector3(0.12f, 0.54f, 0.40f));
            var dc = hit.AddComponent<DeviceController>();
            dc.id = "panel";
            dc.displayName = "环境面板（门口）";
            dc.kind = DeviceKind.EnvironmentPanel;
            dc.initialOn = true;
            dc.initialValue = 1f;
        }

        // ================================================================== 人脸门禁

        static void BuildAccessControl(Transform p)
        {
            var root = new GameObject("人脸识别门禁").transform;
            root.SetParent(p, false);
            root.localPosition = new Vector3(Dim.HX - 0.028f, 1.40f, -1.22f);
            root.localRotation = Quaternion.Euler(0f, -90f, 0f);

            Prim.RoundedBox("门禁_机身", root, Vector3.zero, new Vector3(0.17f, 0.28f, 0.028f), Mat.BlackMetal);
            Prim.RoundedBox("门禁_屏", root, new Vector3(0f, 0.024f, 0.015f), new Vector3(0.142f, 0.165f, 0.006f), Mat.AcScreen);
            Prim.Cylinder("门禁_摄像头", root, new Vector3(0f, -0.104f, 0.017f), 0.014f, 0.012f,
                Mat.Rubber, Quaternion.Euler(90f, 0f, 0f));
            Prim.Disc("门禁_镜头", root, new Vector3(0f, -0.104f, 0.024f), 0.011f,
                Mat.Emissive("w_cam_lens", new Color(0.50f, 0.86f, 1f), 1.6f));
            Prim.Disc("门禁_读卡环", root, new Vector3(0f, -0.078f, 0.017f), 0.030f,
                Mat.Emissive("w_rfid", new Color(0.45f, 1f, 0.60f), 1.8f));
            Prim.Box("门禁_顶部标", root, new Vector3(0f, 0.132f, 0.010f), new Vector3(0.16f, 0.012f, 0.020f), Mat.Brass);

            var hit = Prim.Hitbox("设备_门禁", p, new Vector3(Dim.HX - 0.06f, 1.40f, -1.22f),
                new Vector3(0.12f, 0.36f, 0.24f));
            var dc = hit.AddComponent<DeviceController>();
            dc.id = "access";
            dc.displayName = "门禁 / 人脸识别";
            dc.kind = DeviceKind.AccessControl;
            dc.initialOn = true;
            dc.initialValue = 1f;
        }

        // ================================================================== 装饰

        static void BuildWallClock(Transform p)
        {
            var c = new GameObject("挂钟").transform;
            c.SetParent(p, false);
            c.localPosition = new Vector3(-3.96f, 2.35f, -1.35f);
            c.localRotation = Quaternion.Euler(0f, 90f, 0f);

            Prim.Cylinder("挂钟_壳体", c, Vector3.zero, 0.155f, 0.032f, Mat.Brass, Quaternion.Euler(90f, 0f, 0f));
            Prim.Disc("挂钟_表盘", c, new Vector3(0f, 0f, -0.018f), 0.140f,
                Mat.Pbr("w_clock", new Color(0.94f, 0.92f, 0.87f), 0f, 0.45f));
            Prim.Box("挂钟_时针", c, new Vector3(0f, 0.034f, -0.021f), new Vector3(0.010f, 0.072f, 0.004f), Mat.BlackMetal);
            Prim.Box("挂钟_分针", c, new Vector3(0.036f, 0f, -0.021f), new Vector3(0.082f, 0.008f, 0.004f), Mat.BlackMetal);
        }

        static void BuildSignage(Transform p)
        {
            // 门内侧上方标牌
            var s = new GameObject("会议室标牌").transform;
            s.SetParent(p, false);
            s.localPosition = new Vector3(Dim.HX - 0.020f, 2.42f, 0f);
            s.localRotation = Quaternion.Euler(0f, -90f, 0f);
            Prim.RoundedBox("标牌_底板", s, Vector3.zero, new Vector3(0.48f, 0.14f, 0.018f), Mat.WalnutDark);
            Prim.Box("标牌_面板", s, new Vector3(0f, 0f, 0.013f), new Vector3(0.44f, 0.11f, 0.005f),
                Mat.Emissive("w_sign", new Color(1f, 0.90f, 0.72f), 0.60f));
            Prim.Box("标牌_铜条", s, new Vector3(0f, -0.078f, 0.010f), new Vector3(0.46f, 0.010f, 0.014f), Mat.Brass);
        }
    }
}
