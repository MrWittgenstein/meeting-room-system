using UnityEngine;

namespace SmartRoom
{
    /// <summary>
    /// 全部设备与装置的程序化模型：投影幕布+投影仪、主吊灯、空调、电动窗帘、
    /// 环境传感器节点、门口环境面板、人脸识别门禁。
    /// </summary>
    public static class Devices
    {
        public const string Root = "03_智能设备";

        // 这些引用交给控制器做动画/状态联动
        [System.Serializable]
        public struct Refs
        {
            public Light chandelierLight;
            public MeshRenderer chandelierPanel;
            public MeshRenderer acIndicator;
            public Transform curtainLeft;
            public Transform curtainRight;
            public Transform[] sensorNodes;
            public MeshRenderer panelScreen;
            public Transform doorRoot;         // 门扇（绕轴旋转开关）
            public MeshRenderer acUnitLed;     // 落地空调运行指示灯
            public MeshRenderer acDisplay;     // 落地空调数字显示
        }

        public static Refs Build()
        {
            var refs = new Refs();
            var root = new GameObject(Root);
            var p = root.transform;

            // 投影幕布与投影仪已按要求整体移除（不再生成任何相关物体）
            refs.chandelierLight = BuildChandelier(p, out refs.chandelierPanel);
            BuildCurtains(p, out refs.curtainLeft, out refs.curtainRight);
            refs.acIndicator = BuildAcControl(p);
            BuildSensorNodes(p, out refs.sensorNodes);
            BuildEnvironmentPanel(p, out refs.panelScreen);
            BuildAccessControl(p);
            BuildWallClock(p);
            BuildSignage(p);

            // 收尾：摘掉所有"设备_xxx"命中盒的渲染体，只留碰撞体用于鼠标点选
            StripColliderRenderers(root.transform);

            return refs;
        }

        // ================================================================== 主吊灯

        static Light BuildChandelier(Transform p, out MeshRenderer panel)
        {
            float L = 3.0f, D = 0.55f;
            float y = 2.34f;

            var root = new GameObject("主吊灯");
            root.transform.SetParent(p, false);
            var t = root.transform;

            // 两根吊索
            float cableTop = RoomDim.Height, cableLen = cableTop - y - 0.06f;
            for (int i = 0; i < 2; i++)
            {
                float x = (i == 0 ? -1f : 1f) * (L * 0.5f - 0.22f);
                Prim.Cylinder("吊索" + i, t, new Vector3(x, y + 0.06f + cableLen * 0.5f, 0f), 0.006f, cableLen, MatLib.MetalDark);
                Prim.Cylinder("吊顶座" + i, t, new Vector3(x, cableTop - 0.012f, 0f), 0.045f, 0.024f, MatLib.MetalDark);
            }

            // 灯体框架
            Prim.RoundedBox("灯体_框架", t, new Vector3(0f, y + 0.075f, 0f), new Vector3(L, 0.09f, D + 0.05f), MatLib.Metal);
            // 发光扩散板
            var diff = Prim.Box("灯体_扩散板", t, new Vector3(0f, y + 0.020f, 0f), new Vector3(L - 0.06f, 0.02f, D - 0.02f),
                MatLib.Emissive("m_chandelier_panel", new Color(1f, 0.955f, 0.885f), 1.9f));
            panel = diff.GetComponent<MeshRenderer>();

            // 灯体下沿细边框
            Prim.Box("灯体_下边框", t, new Vector3(0f, y + 0.004f, 0f), new Vector3(L, 0.02f, D + 0.05f), MatLib.Metal);

            // 实际光源（主要照明 + 软阴影）
            var lightGo = new GameObject("主灯光源");
            lightGo.transform.SetParent(t, false);
            lightGo.transform.localPosition = new Vector3(0f, y - 0.02f, 0f);
            var light = lightGo.AddComponent<Light>();
            light.type = LightType.Point;
            light.color = new Color(1f, 0.955f, 0.885f);
            light.intensity = 6.8f;
            light.range = 15f;
            light.shadows = LightShadows.Soft;
            light.shadowStrength = 0.72f;
            light.shadowBias = 0.06f;
            light.shadowNormalBias = 0.22f;
            light.renderMode = LightRenderMode.ForcePixel;

            // 命中体
            var hit = Prim.BoxCollidable("设备_主吊灯", p, new Vector3(0f, y + 0.045f, 0f),
                new Vector3(L, 0.20f, D + 0.06f), MatLib.Metal);
            Configure(hit, "mainlight", "主吊灯（调光）", DeviceKind.MainLight, true, 1f, Vector3.zero);

            return light;
        }

        // ================================================================== 电动窗帘

        static void BuildCurtains(Transform p, out Transform left, out Transform right)
        {
            float z = RoomDim.HalfZ - 0.16f;
            float trackY = RoomDim.WindowTop + 0.16f;

            // 关键尺寸：窗帘要挡的"玻璃净宽"。
            // 窗框料宽 0.06，玻璃从 WindowXMin+0.03 到 WindowXMax-0.03。
            // 之前锚点放在 WindowXMin-0.18，比玻璃宽出 0.21m，所以合拢时会伸到墙里。
            const float frameW = 0.06f;
            float glassXMin = RoomDim.WindowXMin + frameW * 0.5f;   // -2.57
            float glassXMax = RoomDim.WindowXMax - frameW * 0.5f;   // +2.57
            float glassW = glassXMax - glassXMin;                  // 5.14

            // 窗帘只覆盖玻璃高度（下沿略低于窗台一点）
            float glassYMin = RoomDim.WindowSill - 0.06f;
            float curtainTop = RoomDim.WindowTop + 0.10f;
            float curtainH = curtainTop - glassYMin;

            // 每片布面的实际宽度 = 玻璃宽的一半（2.57m）。
            // 之前在 MakeCurtainPanel 里又乘了 2，导致每片都盖满整扇窗并甩出墙外。
            float panelW = glassW * 0.5f;

            // 窗帘盒 / 轨道：刚好覆盖 两片布面 + 两端一点收口
            float trackW = panelW * 2f + 0.16f;
            Prim.Box("窗帘盒", p, new Vector3(0f, curtainTop + 0.035f, z),
                new Vector3(trackW + 0.06f, 0.16f, 0.24f), MatLib.CeilingMat);
            Prim.Box("窗帘轨道", p, new Vector3(0f, curtainTop, z),
                new Vector3(trackW, 0.035f, 0.05f), MatLib.MetalDark);

            // 左右两片窗帘：锚在玻璃两侧边缘，几何以锚点为中心对称，
            // 关闭时两片在中线拼合，恰好覆盖整块玻璃。
            left = MakeCurtainPanel(p, "窗帘_左", new Vector3(glassXMin, curtainTop - 0.03f, z), -1f, panelW, curtainH);
            right = MakeCurtainPanel(p, "窗帘_右", new Vector3(glassXMax, curtainTop - 0.03f, z), 1f, panelW, curtainH);

            // 纱帘（固定，半透明）——同样只覆盖玻璃范围
            Prim.Box("纱帘", p, new Vector3(0f, (RoomDim.WindowTop + RoomDim.WindowSill) * 0.5f, RoomDim.HalfZ - 0.055f),
                new Vector3(glassW, RoomDim.WindowTop - RoomDim.WindowSill, 0.004f), MatLib.Sheer);

            // 点选区（透明，只留碰撞体）
            var hitInv = new GameObject("设备_电动窗帘");
            hitInv.transform.SetParent(p, false);
            hitInv.transform.localPosition = new Vector3(0f, (curtainTop + glassYMin) * 0.5f, z);
            var bc = hitInv.AddComponent<BoxCollider>();
            bc.size = new Vector3(glassW, curtainH, 0.14f);

            var dc = hitInv.AddComponent<DeviceController>();
            dc.id = "curtain";
            dc.displayName = "电动窗帘（开合）";
            dc.kind = DeviceKind.Curtain;
            dc.initialOn = false;
            dc.initialValue = 1f;
            dc.outlineOffset = Vector3.zero;

            // 窗帘电机（放在左端盒内）
            Prim.Box("窗帘电机", p, new Vector3(-(trackW * 0.5f - 0.10f), curtainTop + 0.02f, z),
                new Vector3(0.16f, 0.09f, 0.09f), MatLib.PlasticWhite);
        }

        static Transform MakeCurtainPanel(Transform p, string name, Vector3 anchor, float dir, float span, float height)
        {
            var root = new GameObject(name).transform;
            root.SetParent(p, false);
            root.localPosition = anchor;
            // 让布面向房间内侧偏移一点，避免与纱帘/玻璃重叠
            root.localPosition += new Vector3(0f, 0f, dir * -0.005f);

            var geo = new GameObject("几何").transform;
            geo.SetParent(root, false);

            // 几何从锚点"朝房间中线"铺开，锚点 = 布面的外侧边缘。
            //   dir = -1（左片，锚在玻璃左缘 -2.57）：布面从 -2.57 → 0
            //   dir = +1（右片，锚在玻璃右缘 +2.57）：布面从 +2.57 → 0
            // 关闭(scale=1) 时两片在中线拼合、恰好覆盖整块玻璃，且绝不越出窗框；
            // 开启(scale→0) 时各自向锚点收拢。
            float center = -dir * span * 0.5f;
            Prim.Box("布面", geo, new Vector3(center, -height * 0.5f, 0f),
                new Vector3(span, height, 0.035f), MatLib.Curtain);
            // 内侧（靠中线）、外侧（靠窗框）两条折边
            Prim.Box("布面_折边_内侧", geo, new Vector3(center * 2f, -height * 0.5f, 0f),
                new Vector3(0.02f, height, 0.045f), MatLib.Curtain);
            Prim.Box("布面_折边_外侧", geo, new Vector3(0f, -height * 0.5f, 0f),
                new Vector3(0.02f, height, 0.045f), MatLib.Curtain);
            // 顶部挂钩环
            for (int i = 0; i < 6; i++)
            {
                float t = 0.08f + i * 0.84f / 5f;
                float off = -dir * span * t;
                Prim.Cylinder("挂钩环" + i, geo, new Vector3(off, 0.015f, 0f), 0.018f, 0.012f, MatLib.Chrome);
            }
            return root;
        }

        // ================================================================== 空调控制面板

        static MeshRenderer BuildAcControl(Transform p)
        {
            // 有线遥控器：装在右墙、紧挨落地空调，方便点控
            var root = new GameObject("空调控制器").transform;
            root.transform.SetParent(p, false);
            root.transform.localPosition = new Vector3(RoomDim.WallRight - 0.026f, 1.32f, 1.35f);
            root.transform.localRotation = Quaternion.Euler(0f, -90f, 0f);

            Prim.Box("空调控制器_底盒", root, Vector3.zero, new Vector3(0.022f, 0.14f, 0.10f), MatLib.PlasticWhite);
            // 独立屏幕材质，避免与其他面板共用而被联动改色
            var screen = Prim.Box("空调控制器_屏", root, new Vector3(0.013f, 0.014f, 0f),
                new Vector3(0.006f, 0.082f, 0.074f),
                MatLib.Mat("m_ac_screen", new Color(0.75f, 0.82f, 0.88f), 0.10f, 0.55f,
                    TextureLib.PanelGlass(), null, Vector2.one, new Color(0.16f, 0.42f, 0.55f)));
            Prim.Sphere("空调控制器_状态灯", root, new Vector3(0.017f, -0.048f, 0.032f), 0.006f,
                MatLib.Emissive("m_ac_led", new Color(0.35f, 0.85f, 1f), 2.0f));

            // 命中盒直接套在落地空调柜体上（鼠标点空调本体即可开关）
            var unitPos = new Vector3(RoomDim.HalfX - 0.48f, 0.95f, -RoomDim.HalfZ + 0.72f);
            var hit = Prim.BoxCollidable("设备_空调", p, unitPos,
                new Vector3(0.62f, 1.98f, 0.56f), MatLib.PlasticWhite);
            Configure(hit, "ac", "空调 / HVAC", DeviceKind.AirConditioner, true, 24f, Vector3.zero);

            return screen.GetComponent<MeshRenderer>();
        }

        // ================================================================== 环境传感器节点

        static void BuildSensorNodes(Transform p, out Transform[] nodes)
        {
            nodes = new Transform[4];

            // 节点 1：温湿度（右墙）
            nodes[0] = MakeSensorNode(p, "传感器_温湿度", new Vector3(RoomDim.HalfX - 0.03f, 1.72f, 0.30f),
                Quaternion.Euler(0f, -90f, 0f), "温湿度", new Color(1f, 0.62f, 0.25f));
            // 节点 2：光照（窗边）
            nodes[1] = MakeSensorNode(p, "传感器_光照", new Vector3(2.42f, 1.72f, RoomDim.HalfZ - 0.03f),
                Quaternion.Euler(0f, 180f, 0f), "光照", new Color(1f, 0.90f, 0.35f));
            // 节点 3：空气质量 / 烟雾（幕布墙侧）
            nodes[2] = MakeSensorNode(p, "传感器_烟雾", new Vector3(-2.05f, 2.35f, RoomDim.WallScreen + 0.03f),
                Quaternion.identity, "烟雾", new Color(0.55f, 0.75f, 0.95f));
            // 节点 4：人体感应（入口上方，接门禁联动）
            nodes[3] = MakeSensorNode(p, "传感器_人体感应", new Vector3(RoomDim.WallLeft + 0.03f, 2.42f, -1.85f),
                Quaternion.Euler(0f, 90f, 0f), "人体", new Color(0.45f, 0.95f, 0.60f));

            // 温湿度节点做成可点选，其他节点纯展示
            var hit = Prim.BoxCollidable("设备_环境传感器", p, nodes[0].position,
                new Vector3(0.10f, 0.14f, 0.14f), MatLib.PlasticWhite);
            Object.DestroyImmediate(hit.GetComponent<MeshRenderer>());
            Configure(hit, "sensors", "环境传感器组", DeviceKind.Sensor, true, 1f, Vector3.zero);
        }

        static Transform MakeSensorNode(Transform p, string name, Vector3 pos, Quaternion rot, string label, Color ledColor)
        {
            var root = new GameObject(name).transform;
            root.SetParent(p, false);
            root.localPosition = pos;
            root.localRotation = rot;

            Prim.RoundedBox("壳体", root, Vector3.zero, new Vector3(0.085f, 0.11f, 0.028f), MatLib.PlasticWhite);
            // 传感器透光窗
            Prim.Box("透光窗", root, new Vector3(0f, 0.018f, 0.016f), new Vector3(0.048f, 0.030f, 0.004f), MatLib.PanelGlass);
            // 状态 LED
            Prim.Sphere("LED", root, new Vector3(0f, -0.032f, 0.017f), 0.0065f,
                MatLib.Emissive("m_sensor_led_" + label, ledColor, 2.4f));
            // 底部通风缝
            Prim.Box("格栅", root, new Vector3(0f, -0.044f, 0.012f), new Vector3(0.05f, 0.006f, 0.006f), MatLib.Plastic);
            return root;
        }

        // ================================================================== 门口环境面板

        static void BuildEnvironmentPanel(Transform p, out MeshRenderer screen)
        {
            var root = new GameObject("门口环境面板").transform;
            root.transform.SetParent(p, false);
            root.transform.localPosition = new Vector3(RoomDim.WallLeft + 0.028f, 1.42f, -2.78f);
            root.transform.localRotation = Quaternion.Euler(0f, 90f, 0f);

            // 外框
            Prim.RoundedBox("面板_框", root, Vector3.zero, new Vector3(0.30f, 0.44f, 0.022f), MatLib.Plastic);
            // 屏幕（发光，内容由控制器绘制）
            // 单独一份屏幕材质，避免与空调控制器/传感器的面板共用材质而被联动改色
            var s = Prim.Box("面板_屏", root, new Vector3(0f, 0f, 0.013f), new Vector3(0.262f, 0.398f, 0.004f),
                MatLib.Mat("m_panel_screen", new Color(0.75f, 0.82f, 0.88f), 0.10f, 0.55f,
                    TextureLib.PanelGlass(), null, Vector2.one, new Color(0.16f, 0.42f, 0.55f)));
            screen = s.GetComponent<MeshRenderer>();
            // 底部触摸条
            Prim.Box("面板_触摸条", root, new Vector3(0f, -0.195f, 0.014f), new Vector3(0.20f, 0.014f, 0.003f),
                MatLib.Emissive("m_touchbar", new Color(0.30f, 0.85f, 1f), 1.4f));

            var hit = Prim.BoxCollidable("设备_环境面板", p, new Vector3(RoomDim.WallLeft + 0.05f, 1.42f, -2.78f),
                new Vector3(0.10f, 0.52f, 0.38f), MatLib.Plastic);
            Object.DestroyImmediate(hit.GetComponent<MeshRenderer>());
            Configure(hit, "panel", "环境面板（门口）", DeviceKind.EnvironmentPanel, true, 1f, Vector3.zero);
        }

        // ================================================================== 人脸识别门禁

        static void BuildAccessControl(Transform p)
        {
            var root = new GameObject("人脸识别门禁").transform;
            root.transform.SetParent(p, false);
            root.transform.localPosition = new Vector3(RoomDim.WallLeft + 0.026f, 1.40f, -1.14f);
            root.transform.localRotation = Quaternion.Euler(0f, 90f, 0f);

            Prim.RoundedBox("门禁_机身", root, Vector3.zero, new Vector3(0.16f, 0.26f, 0.026f), MatLib.Plastic);
            Prim.RoundedBox("门禁_屏", root, new Vector3(0f, 0.022f, 0.014f), new Vector3(0.135f, 0.155f, 0.006f),
                MatLib.PanelGlass);
            // 摄像头
            Prim.Cylinder("门禁_摄像头", root, new Vector3(0f, -0.098f, 0.016f), 0.014f, 0.012f,
                MatLib.Rubber, Quaternion.Euler(90f, 0f, 0f));
            Prim.Disc("门禁_镜头", root, new Vector3(0f, -0.098f, 0.023f), 0.011f,
                MatLib.Emissive("m_cam_lens", new Color(0.45f, 0.85f, 1f), 1.6f), Quaternion.Euler(90f, 0f, 0f));
            // 读卡区指示环
            Prim.Disc("门禁_读卡环", root, new Vector3(0f, -0.075f, 0.016f), 0.030f,
                MatLib.Emissive("m_rfid", new Color(0.30f, 1f, 0.55f), 1.8f), Quaternion.Euler(90f, 0f, 0f));

            var hit = Prim.BoxCollidable("设备_门禁", p, new Vector3(RoomDim.WallLeft + 0.05f, 1.40f, -1.14f),
                new Vector3(0.10f, 0.34f, 0.22f), MatLib.Plastic);
            Object.DestroyImmediate(hit.GetComponent<MeshRenderer>());
            Configure(hit, "access", "门禁 / 人脸识别", DeviceKind.AccessControl, true, 1f, Vector3.zero);
        }

        // ================================================================== 装饰

        static void BuildWallClock(Transform p)
        {
            var c = new GameObject("挂钟").transform;
            c.SetParent(p, false);
            c.localPosition = new Vector3(RoomDim.HalfX - 0.035f, 2.30f, 1.55f);
            c.localRotation = Quaternion.Euler(0f, -90f, 0f);

            Prim.Cylinder("挂钟_壳体", c, Vector3.zero, 0.155f, 0.030f, MatLib.MetalDark, Quaternion.Euler(90f, 0f, 0f));
            Prim.Disc("挂钟_表盘", c, new Vector3(0f, 0f, -0.017f), 0.142f,
                MatLib.Mat("m_clock_face", new Color(0.93f, 0.93f, 0.90f), 0f, 0.45f), Quaternion.Euler(90f, 0f, 0f));
            Prim.Box("挂钟_时针", c, new Vector3(0f, 0.035f, -0.020f), new Vector3(0.010f, 0.075f, 0.004f), MatLib.MetalDark);
            Prim.Box("挂钟_分针", c, new Vector3(0.038f, 0f, -0.020f), new Vector3(0.085f, 0.008f, 0.004f), MatLib.MetalDark);
        }

        static void BuildSignage(Transform p)
        {
            // 门外侧/内侧的会议室标牌
            var s = new GameObject("会议室标牌").transform;
            s.SetParent(p, false);
            s.localPosition = new Vector3(RoomDim.WallLeft + 0.018f, 2.32f, -1.85f);
            s.localRotation = Quaternion.Euler(0f, 90f, 0f);
            Prim.RoundedBox("标牌_底板", s, Vector3.zero, new Vector3(0.44f, 0.13f, 0.016f), MatLib.MetalDark);
            Prim.Box("标牌_面板", s, new Vector3(0f, 0f, 0.012f), new Vector3(0.40f, 0.10f, 0.005f),
                MatLib.Emissive("m_sign", new Color(0.90f, 0.95f, 1f), 0.55f));
        }

        // ================================================================== 通用：挂控制器

        /// <summary>
        /// 把所有"设备_xxx"命中盒的渲染体去掉，只保留碰撞体。
        /// 重要：这些命中盒是包住设备的透明选点盒，如果不摘掉渲染体，
        /// 它们会用创建时的材质把设备本体整个盖住（曾导致投影幕布永远显示为白板）。
        /// </summary>
        static void StripColliderRenderers(Transform root)
        {
            foreach (var t in root.GetComponentsInChildren<Transform>(true))
            {
                if (!t.name.StartsWith("设备_")) continue;
                var mr = t.GetComponent<MeshRenderer>();
                if (mr != null) Object.DestroyImmediate(mr);
                var mf = t.GetComponent<MeshFilter>();
                if (mf != null) Object.DestroyImmediate(mf);
                if (t.GetComponent<Collider>() == null)
                    t.gameObject.AddComponent<BoxCollider>();
            }
        }

        /// <summary>构建收尾：统一清理命中盒的渲染体。</summary>
        public static void FinalizeRoot(GameObject root)
        {
            StripColliderRenderers(root.transform);
        }

        static void Configure(GameObject go, string id, string name, DeviceKind kind, bool on, float value, Vector3 outlineOffset)
        {
            var dc = go.AddComponent<DeviceController>();
            dc.id = id;
            dc.displayName = name;
            dc.kind = kind;
            dc.initialOn = on;
            dc.initialValue = value;
            dc.outlineOffset = outlineOffset;
        }
    }
}
