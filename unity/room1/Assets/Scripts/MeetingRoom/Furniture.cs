using UnityEngine;

namespace SmartRoom
{
    /// <summary>
    /// 全部家具都由代码程序化建模（无任何外部模型资源）：
    /// 会议桌、12 把人体工学办公椅、边柜、白板、绿植、饮水机、垃圾桶、衣帽架等。
    /// </summary>
    public static class Furniture
    {
        public const string Root = "02_家具";

        public static GameObject Build()
        {
            var root = new GameObject(Root);
            var p = root.transform;

            BuildConferenceTable(p);
            BuildChairs(p);
            BuildSideboard(p);
            BuildWhiteboard(p);
            BuildWaterStation(p);
            BuildTrashBin(p);
            BuildCoatStand(p);
            BuildRugAndDetails(p);

            return root;
        }

        // ================================================================== 会议桌

        static void BuildConferenceTable(Transform p)
        {
            var t = new GameObject("会议桌").transform;
            t.SetParent(p, false);

            float L = RoomDim.TableLength, D = RoomDim.TableDepth, H = RoomDim.TableHeight;
            float topT = RoomDim.TableTopThickness;

            // 桌面（圆角）
            Prim.RoundedBox("会议桌_桌面", t, new Vector3(0f, H - topT * 0.5f, 0f),
                new Vector3(L, topT, D), MatLib.Walnut);

            // 桌面下沿收边
            Prim.Box("会议桌_收边", t, new Vector3(0f, H - topT - 0.012f, 0f),
                new Vector3(L - 0.02f, 0.024f, D - 0.02f), MatLib.WalnutDark);

            // 两侧梯形支腿（用圆角盒近似）
            float legInset = L * 0.5f - 0.34f;
            for (int s = 0; s < 2; s++)
            {
                float x = s == 0 ? -legInset : legInset;
                Prim.RoundedBox("会议桌_支腿" + s, t, new Vector3(x, 0.30f, 0f),
                    new Vector3(0.14f, 0.60f, D - 0.26f), MatLib.MetalDark);
                Prim.RoundedBox("会议桌_支腿脚" + s, t, new Vector3(x, 0.014f, 0f),
                    new Vector3(0.34f, 0.028f, D - 0.20f), MatLib.Metal);
            }

            // 中央横梁
            Prim.Box("会议桌_横梁", t, new Vector3(0f, 0.60f, 0f),
                new Vector3(legInset * 2f, 0.10f, 0.16f), MatLib.MetalDark);

            // 前方挡板（走线遮挡，织物质感）
            Prim.Box("会议桌_挡板", t, new Vector3(0f, 0.50f, 0f),
                new Vector3(L - 0.72f, 0.30f, 0.03f), MatLib.BlackMesh);

            // 桌面中央走线盒（金属翻盖 + 插座）
            Prim.RoundedBox("会议桌_走线盒", t, new Vector3(0f, H + 0.004f, 0f),
                new Vector3(0.90f, 0.02f, 0.16f), MatLib.Metal);
            for (int i = 0; i < 4; i++)
            {
                float x = -0.30f + i * 0.20f;
                Prim.Box("会议桌_插座" + i, t, new Vector3(x, H + 0.014f, 0.02f),
                    new Vector3(0.075f, 0.008f, 0.05f), MatLib.Plastic);
            }

            // 桌面橄榄形麦克风阵列（会议拾音）
            BuildTableMic(t, new Vector3(0f, H + 0.014f, 0.30f));
            // 纸笔与铭牌
            BuildTableProps(t, H);
        }

        static void BuildTableMic(Transform t, Vector3 c)
        {
            Prim.RoundedBox("会议桌_拾音器底座", t, c, new Vector3(0.34f, 0.026f, 0.14f), MatLib.Plastic);
            for (int i = 0; i < 3; i++)
            {
                float x = c.x - 0.10f + i * 0.10f;
                Prim.Cylinder("会议桌_拾音孔" + i, t, new Vector3(x, c.y + 0.016f, c.z), 0.014f, 0.008f, MatLib.Rubber);
            }
            Prim.Sphere("会议桌_拾音指示灯", t, new Vector3(c.x + 0.14f, c.y + 0.014f, c.z), 0.010f,
                MatLib.Emissive("m_mic_led", new Color(0.30f, 1f, 0.55f), 2.0f));
        }

        static void BuildTableProps(Transform t, float H)
        {
            float y = H + 0.004f;
            // 6 组纸笔
            for (int i = 0; i < 6; i++)
            {
                int side = i / 3;            // 0: -Z 侧, 1: +Z 侧
                int k = i % 3;
                float x = -1.05f + k * 1.05f;
                float z = side == 0 ? -0.32f : 0.32f;

                var g = new GameObject("桌面文具_" + i).transform;
                g.SetParent(t, false);
                g.localPosition = new Vector3(x, y, z);
                g.localRotation = Quaternion.Euler(0f, side == 0 ? 0f : 180f, 0f);

                // A4 记事本
                Prim.Box("便签本", g, new Vector3(0f, 0.006f, 0f), new Vector3(0.155f, 0.012f, 0.215f),
                    MatLib.Mat("m_paper", new Color(0.97f, 0.97f, 0.955f), 0f, 0.30f));
                // 铅笔
                Prim.Cylinder("铅笔", g, new Vector3(0.10f, 0.006f, 0.06f), 0.004f, 0.145f,
                    MatLib.Mat("m_pencil", new Color(0.85f, 0.72f, 0.20f), 0f, 0.35f),
                    Quaternion.Euler(90f, 0f, 0f));
                // 玻璃水杯
                Prim.Cylinder("水杯", g, new Vector3(-0.12f, 0.055f, -0.07f), 0.033f, 0.11f, MatLib.Sheer);
                Prim.Cylinder("水杯_水", g, new Vector3(-0.12f, 0.028f, -0.07f), 0.029f, 0.055f,
                    MatLib.Emissive("m_water", new Color(0.55f, 0.78f, 0.88f), 0.15f));
            }

            // 桌牌（主席位）
            var plate = new GameObject("桌牌").transform;
            plate.SetParent(t, false);
            plate.localPosition = new Vector3(-1.32f, y, 0f);
            plate.localRotation = Quaternion.Euler(0f, -90f, 0f);
            Prim.Box("桌牌_面板", plate, new Vector3(0f, 0.045f, 0f), new Vector3(0.24f, 0.09f, 0.014f),
                MatLib.Mat("m_plate", new Color(0.16f, 0.17f, 0.19f), 0.10f, 0.55f));
            Prim.Box("桌牌_底座", plate, new Vector3(0f, 0.005f, 0.02f), new Vector3(0.22f, 0.010f, 0.055f),
                MatLib.Metal);
        }

        // ================================================================== 办公椅

        static void BuildChairs(Transform p)
        {
            var chairRoot = new GameObject("办公椅组").transform;
            chairRoot.SetParent(p, false);

            int idx = 0;
            // 注意朝向基准：椅子模型"坐垫前缘"在本地 -0.235、"靠背"在 +0.215，
            // 也就是椅子正面是本地 -Z。所以：
            //   yaw =   0°  → 正面朝 -Z
            //   yaw = 180°  → 正面朝 +Z
            // -Z 侧那排（桌子在它们的 +Z 方向）→ 必须 yaw=180° 才面向桌子
            for (int i = 0; i < RoomDim.SeatsPerLongSide; i++)
            {
                float x = Mathf.Lerp(-1.40f, 1.40f, i / (float)(RoomDim.SeatsPerLongSide - 1));
                BuildChair(chairRoot, "办公椅_" + (++idx), new Vector3(x, 0f, -1.42f), 180f);
            }
            // +Z 侧那排（桌子在它们的 -Z 方向）→ yaw=0° 面向桌子
            for (int i = 0; i < RoomDim.SeatsPerLongSide; i++)
            {
                float x = Mathf.Lerp(-1.40f, 1.40f, i / (float)(RoomDim.SeatsPerLongSide - 1));
                BuildChair(chairRoot, "办公椅_" + (++idx), new Vector3(x, 0f, 1.42f), 0f);
            }
            // 两端主席位：正面朝 +X 用 -90°，正面朝 -X 用 +90°
            BuildChair(chairRoot, "办公椅_主席位", new Vector3(-2.12f, 0f, 0f), -90f);
            BuildChair(chairRoot, "办公椅_记录位", new Vector3(2.12f, 0f, 0f), 90f);
        }

        /// <summary>单把人体工学办公椅。yaw：0 = 正面朝本地 -Z（即世界 -Z）。</summary>
        public static GameObject BuildChair(Transform parent, string name, Vector3 pos, float yaw)
        {
            var root = new GameObject(name);
            root.transform.SetParent(parent, false);
            root.transform.localPosition = pos;
            root.transform.localRotation = Quaternion.Euler(0f, yaw, 0f);
            var t = root.transform;

            var pad = MatLib.SeatPad;
            const float seatH = 0.455f;

            // ---- 五星脚（5 条腿 + 5 个万向轮）----
            for (int i = 0; i < 5; i++)
            {
                float a = i * 72f;
                var dir = new Vector3(Mathf.Sin(a * Mathf.Deg2Rad), 0f, Mathf.Cos(a * Mathf.Deg2Rad));
                var arm = new GameObject("五星脚臂" + i).transform;
                arm.SetParent(t, false);
                arm.localPosition = new Vector3(0f, 0.075f, 0f);
                arm.localRotation = Quaternion.Euler(0f, a, 0f);
                Prim.RoundedBox("臂", arm, new Vector3(0f, 0f, 0.155f), new Vector3(0.058f, 0.040f, 0.34f), MatLib.MetalDark);

                var wheel = new GameObject("万向轮" + i).transform;
                wheel.SetParent(t, false);
                wheel.localPosition = new Vector3(dir.x * 0.29f, 0f, dir.z * 0.29f);
                wheel.localRotation = Quaternion.Euler(0f, a, 0f);
                Prim.Cylinder("轮架", wheel, new Vector3(0f, 0.030f, 0f), 0.032f, 0.028f, MatLib.Plastic);
                Prim.Sphere("轮子", wheel, new Vector3(0f, 0.030f, 0f), 0.030f, MatLib.Rubber);
            }

            // ---- 气压杆 + 底盘 ----
            Prim.Cylinder("气压杆", t, new Vector3(0f, 0.215f, 0f), 0.028f, 0.29f, MatLib.Chrome);
            Prim.Cylinder("气压杆下段", t, new Vector3(0f, 0.105f, 0f), 0.038f, 0.13f, MatLib.MetalDark);
            Prim.Cylinder("座椅底盘", t, new Vector3(0f, 0.375f, 0f), 0.105f, 0.022f, MatLib.Plastic);

            // ---- 坐垫（前缘微翘、带侧翼）----
            Prim.RoundedBox("坐垫", t, new Vector3(0f, seatH, 0.005f), new Vector3(0.495f, 0.088f, 0.480f), pad);
            Prim.RoundedBox("坐垫_前缘", t, new Vector3(0f, seatH + 0.012f, -0.235f), new Vector3(0.470f, 0.062f, 0.060f), pad);
            // 坐垫下方壳体
            Prim.RoundedBox("坐垫_底壳", t, new Vector3(0f, seatH - 0.055f, 0.005f), new Vector3(0.455f, 0.035f, 0.440f), MatLib.Plastic);

            // ---- 靠背（后倾 12°，腰部凸起）----
            var back = new GameObject("靠背").transform;
            back.SetParent(t, false);
            back.localPosition = new Vector3(0f, 0.735f, 0.215f);
            back.localRotation = Quaternion.Euler(-12f, 0f, 0f);

            Prim.RoundedBox("靠背_框", back, new Vector3(0f, 0f, 0f), new Vector3(0.470f, 0.560f, 0.052f), pad);
            Prim.RoundedBox("靠背_腰托", back, new Vector3(0f, -0.115f, -0.038f), new Vector3(0.400f, 0.150f, 0.045f), pad);
            Prim.RoundedBox("靠背_头枕", back, new Vector3(0f, 0.325f, -0.030f), new Vector3(0.330f, 0.115f, 0.055f), pad);
            // 靠背金属支架
            Prim.Box("靠背_支架", t, new Vector3(0f, 0.575f, 0.185f), new Vector3(0.30f, 0.16f, 0.035f), MatLib.MetalDark);

            // ---- 扶手（3D 可调样式）----
            for (int s = 0; s < 2; s++)
            {
                float x = s == 0 ? -0.315f : 0.315f;
                var arm = new GameObject("扶手" + s).transform;
                arm.SetParent(t, false);
                arm.localPosition = new Vector3(x, 0.60f, 0.02f);
                Prim.Box("扶手_立柱", arm, new Vector3(0f, -0.10f, 0f), new Vector3(0.045f, 0.20f, 0.045f), MatLib.MetalDark);
                Prim.Box("扶手_横臂", arm, new Vector3(0f, 0.005f, 0.02f), new Vector3(0.050f, 0.030f, 0.32f), MatLib.MetalDark);
                Prim.RoundedBox("扶手_软垫", arm, new Vector3(0f, 0.028f, -0.02f), new Vector3(0.065f, 0.028f, 0.235f), MatLib.Plastic);
            }

            return root;
        }

        // ================================================================== 边柜 / 白板 / 其他

        static void BuildSideboard(Transform p)
        {
            var s = new GameObject("边柜").transform;
            s.SetParent(p, false);
            s.localPosition = new Vector3(3.62f, 0f, 0.35f);
            s.localRotation = Quaternion.Euler(0f, -90f, 0f);

            const float W = 2.30f, H = 0.74f, D = 0.44f, legH = 0.12f;

            Prim.RoundedBox("边柜_柜体", s, new Vector3(0f, legH + (H - legH) * 0.5f, 0f),
                new Vector3(W, H - legH, D), MatLib.Walnut);
            Prim.Box("边柜_台面", s, new Vector3(0f, H + 0.018f, 0f), new Vector3(W + 0.04f, 0.036f, D + 0.03f), MatLib.Stone);

            // 四只金属细腿
            for (int i = 0; i < 4; i++)
            {
                float x = (i % 2 == 0 ? -1f : 1f) * (W * 0.5f - 0.14f);
                float z = (i < 2 ? -1f : 1f) * (D * 0.5f - 0.10f);
                Prim.Cylinder("边柜_腿" + i, s, new Vector3(x, legH * 0.5f, z), 0.018f, legH, MatLib.Metal);
            }

            // 柜门分缝与拉手
            for (int i = 0; i < 2; i++)
            {
                float x = i == 0 ? -W * 0.25f : W * 0.25f;
                Prim.Box("边柜_门缝" + i, s, new Vector3(x, legH + (H - legH) * 0.5f, D * 0.5f + 0.002f),
                    new Vector3(0.008f, H - legH - 0.04f, 0.004f), MatLib.MetalDark);
                Prim.Cylinder("边柜_拉手" + i, s,
                    new Vector3(i == 0 ? -0.06f : 0.06f, 0.50f, D * 0.5f + 0.022f),
                    0.011f, 0.20f, MatLib.Chrome, Quaternion.Euler(90f, 0f, 0f));
            }

            // 台面陈列：文件架、绿植小盆栽、矿泉水
            Prim.Box("边柜_文件架", s, new Vector3(-0.72f, H + 0.11f, 0.02f), new Vector3(0.30f, 0.15f, 0.24f), MatLib.MetalDark);
            for (int i = 0; i < 4; i++)
                Prim.Box("边柜_文件夹" + i, s, new Vector3(-0.82f + i * 0.055f, H + 0.135f, 0.0f),
                    new Vector3(0.012f, 0.20f, 0.20f), MatLib.Mat("m_folder" + i % 2,
                        i % 2 == 0 ? new Color(0.20f, 0.34f, 0.52f) : new Color(0.72f, 0.28f, 0.24f), 0f, 0.35f));

            BuildSmallPlant(s, new Vector3(0.66f, H + 0.036f, 0f), 0.55f);

            for (int i = 0; i < 3; i++)
                Prim.Cylinder("边柜_水瓶" + i, s, new Vector3(0.22f + i * 0.075f, H + 0.135f, 0.10f),
                    0.036f, 0.20f, MatLib.Sheer);
        }

        static void BuildWhiteboard(Transform p)
        {
            var w = new GameObject("白板").transform;
            w.SetParent(p, false);
            // 注意：右墙 z∈[-2.6, 2.6] 是窗户范围，白板必须放在这个区间之外，
            // 否则会插进窗户玻璃里（之前放在 -1.05 就穿模了）
            w.localPosition = new Vector3(RoomDim.HalfX - 0.07f, 1.62f, -0.80f);
            w.localRotation = Quaternion.Euler(0f, -90f, 0f);

            const float W = 2.20f, H = 1.22f;
            Prim.Box("白板_板面", w, Vector3.zero, new Vector3(W, H, 0.018f), MatLib.Whiteboard);
            Prim.Box("白板_边框", w, new Vector3(0f, 0f, 0.002f), new Vector3(W + 0.07f, H + 0.07f, 0.016f), MatLib.WhiteboardFrame);
            // 笔槽
            Prim.Box("白板_笔槽", w, new Vector3(0f, -H * 0.5f - 0.05f, 0.035f), new Vector3(W, 0.03f, 0.07f), MatLib.Metal);
            for (int i = 0; i < 3; i++)
                Prim.Cylinder("白板_记号笔" + i, w, new Vector3(-0.30f + i * 0.12f, -H * 0.5f - 0.035f, 0.055f),
                    0.011f, 0.135f, MatLib.Mat("m_marker" + i,
                        i == 0 ? new Color(0.85f, 0.15f, 0.15f) : i == 1 ? new Color(0.15f, 0.35f, 0.75f) : new Color(0.15f, 0.15f, 0.15f),
                        0f, 0.40f), Quaternion.Euler(90f, 0f, 0f));
            // 板擦
            Prim.Box("白板_板擦", w, new Vector3(0.85f, -H * 0.5f - 0.055f, 0.06f), new Vector3(0.13f, 0.035f, 0.06f), MatLib.Plastic);
        }

        static void BuildWaterStation(Transform p)
        {
            var w = new GameObject("饮水台").transform;
            w.SetParent(p, false);
            w.localPosition = new Vector3(3.56f, 0f, 1.92f);
            // 同理：台体正面在本地 -Z，让它朝房间内侧（yaw = -50°）
            w.localRotation = Quaternion.Euler(0f, -50f, 0f);

            Prim.RoundedBox("饮水台_台体", w, new Vector3(0f, 0.37f, 0f), new Vector3(0.72f, 0.74f, 0.52f), MatLib.Walnut);
            Prim.Box("饮水台_台面", w, new Vector3(0f, 0.755f, 0f), new Vector3(0.76f, 0.03f, 0.56f), MatLib.Stone);

            // 饮水机（总高控制在 1.35m 左右，符合真实比例）
            Prim.Box("饮水机_机身", w, new Vector3(-0.16f, 1.03f, 0f), new Vector3(0.34f, 0.52f, 0.34f), MatLib.PlasticWhite);
            Prim.Box("饮水机_面板", w, new Vector3(-0.16f, 1.09f, -0.18f), new Vector3(0.22f, 0.20f, 0.02f), MatLib.PanelGlass);
            Prim.Box("饮水机_出水口", w, new Vector3(-0.16f, 0.87f, -0.15f), new Vector3(0.10f, 0.05f, 0.10f), MatLib.Plastic);
            Prim.Cylinder("饮水机_水瓶", w, new Vector3(-0.16f, 1.44f, 0f), 0.145f, 0.32f,
                MatLib.Emissive("m_bottle", new Color(0.62f, 0.82f, 0.92f), 0.20f));

            // 一次性水杯
            for (int i = 0; i < 4; i++)
                Prim.Cylinder("饮水台_纸杯" + i, w, new Vector3(0.20f, 0.785f + i * 0.075f, 0.10f), 0.033f, 0.075f,
                    MatLib.Mat("m_cup", new Color(0.94f, 0.93f, 0.90f), 0f, 0.30f));
        }

        static void BuildTrashBin(Transform p)
        {
            var b = new GameObject("垃圾桶").transform;
            b.SetParent(p, false);
            b.localPosition = new Vector3(2.92f, 0f, 0.15f);
            Prim.Cylinder("垃圾桶_桶身", b, new Vector3(0f, 0.145f, 0f), 0.115f, 0.29f, MatLib.Metal);
            Prim.Cylinder("垃圾桶_内桶", b, new Vector3(0f, 0.295f, 0f), 0.108f, 0.02f, MatLib.Plastic);
            Prim.Cylinder("垃圾桶_垃圾袋", b, new Vector3(0f, 0.20f, 0f), 0.098f, 0.10f,
                MatLib.Mat("m_bag", new Color(0.16f, 0.17f, 0.18f), 0f, 0.10f));
        }

        static void BuildCoatStand(Transform p)
        {
            var c = new GameObject("衣帽架").transform;
            c.SetParent(p, false);
            c.localPosition = new Vector3(-3.45f, 0f, -2.45f);

            Prim.Cylinder("衣帽架_底盘", c, new Vector3(0f, 0.012f, 0f), 0.20f, 0.024f, MatLib.MetalDark);
            Prim.Cylinder("衣帽架_立柱", c, new Vector3(0f, 0.90f, 0f), 0.018f, 1.76f, MatLib.Metal);
            for (int i = 0; i < 4; i++)
            {
                var hook = new GameObject("挂钩" + i).transform;
                hook.SetParent(c, false);
                hook.localPosition = new Vector3(0f, 1.76f, 0f);
                hook.localRotation = Quaternion.Euler(0f, i * 90f, 0f);
                Prim.Cylinder("挂臂", hook, new Vector3(0f, 0.02f, 0.075f), 0.012f, 0.15f,
                    MatLib.Metal, Quaternion.Euler(80f, 0f, 0f));
                Prim.Sphere("挂头", hook, new Vector3(0f, 0.055f, 0.145f), 0.017f, MatLib.Metal);
            }

            // 挂一件外套，增强生活感
            Prim.RoundedBox("衣帽架_外套", c, new Vector3(0.02f, 1.32f, 0.06f), new Vector3(0.30f, 0.62f, 0.22f),
                MatLib.Mat("m_coat", new Color(0.115f, 0.135f, 0.180f), 0f, 0.20f), Quaternion.Euler(0f, 12f, 0f));
        }

        static void BuildRugAndDetails(Transform p)
        {
            // 桌子下方浅色地毯，划分会议区
            Prim.Box("会议区地毯", p, new Vector3(0f, 0.002f, 0f), new Vector3(5.4f, 0.008f, 3.5f),
                MatLib.Mat("m_rug", new Color(0.255f, 0.270f, 0.300f), 0f, 0.12f,
                    TextureLib.Carpet(), TextureLib.CarpetNormal(), new Vector2(6f, 4f)));

            // 角落落地装饰灯
            var lamp = new GameObject("落地灯").transform;
            lamp.SetParent(p, false);
            lamp.localPosition = new Vector3(3.35f, 0f, 2.45f);
            Prim.Cylinder("落地灯_底座", lamp, new Vector3(0f, 0.012f, 0f), 0.16f, 0.024f, MatLib.MetalDark);
            Prim.Cylinder("落地灯_灯杆", lamp, new Vector3(0f, 0.75f, 0f), 0.012f, 1.50f, MatLib.Metal);
            Prim.Cylinder("落地灯_灯罩", lamp, new Vector3(0f, 1.58f, 0f), 0.17f, 0.26f, MatLib.PlasticWhite);
            Prim.Disc("落地灯_发光", lamp, new Vector3(0f, 1.445f, 0f), 0.155f,
                MatLib.Emissive("m_lamp_glow", new Color(1f, 0.93f, 0.82f), 2.2f), Quaternion.Euler(90f, 0f, 0f));
        }

        // ================================================================== 绿植（复用）

        /// <summary>中型落地绿植（花盆 + 叶片）。</summary>
        public static GameObject BuildPlant(Transform parent, string name, Vector3 pos, float scale, int seed)
        {
            var root = new GameObject(name);
            root.transform.SetParent(parent, false);
            root.transform.localPosition = pos;
            root.transform.localScale = Vector3.one * scale;
            var t = root.transform;

            // 花盆（上宽下窄，用圆柱近似 + 口沿）
            Prim.Cylinder("花盆", t, new Vector3(0f, 0.20f, 0f), 0.20f, 0.40f, MatLib.PotConcrete);
            Prim.Cylinder("花盆_口沿", t, new Vector3(0f, 0.395f, 0f), 0.215f, 0.035f, MatLib.PotConcrete);
            Prim.Cylinder("花盆_土", t, new Vector3(0f, 0.395f, 0f), 0.185f, 0.02f, MatLib.Soil);

            // 主干
            Prim.Cylinder("主干", t, new Vector3(0f, 0.62f, 0f), 0.028f, 0.46f,
                MatLib.Mat("m_bark", new Color(0.185f, 0.145f, 0.105f), 0f, 0.15f));

            // 叶片：3 层错落
            var rnd = new System.Random(seed);
            for (int layer = 0; layer < 3; layer++)
            {
                int count = 7 - layer;
                float y = 0.62f + layer * 0.20f;
                float rad = 0.30f - layer * 0.055f;
                float leafLen = 0.46f - layer * 0.075f;
                for (int i = 0; i < count; i++)
                {
                    float a = (i / (float)count) * 360f + layer * 27f + (float)rnd.NextDouble() * 14f;
                    var dir = new Vector3(Mathf.Sin(a * Mathf.Deg2Rad), 0f, Mathf.Cos(a * Mathf.Deg2Rad));
                    var leaf = new GameObject("叶片").transform;
                    leaf.SetParent(t, false);
                    leaf.localPosition = new Vector3(dir.x * rad * 0.35f, y, dir.z * rad * 0.35f);
                    leaf.localRotation = Quaternion.Euler(0f, a, 0f);
                    var l = Prim.Box("叶", leaf,
                        new Vector3(0f, leafLen * 0.30f, rad * 0.55f),
                        new Vector3(0.15f, leafLen, 0.012f),
                        (layer + i) % 2 == 0 ? MatLib.Leaf : MatLib.LeafLight,
                        Quaternion.Euler(-38f - layer * 6f, 0f, 0f));
                    l.transform.localScale = new Vector3(0.15f, leafLen, 0.012f);
                }
            }
            return root;
        }

        /// <summary>桌面小盆栽。</summary>
        public static void BuildSmallPlant(Transform parent, Vector3 pos, float scale)
        {
            var t = new GameObject("小盆栽").transform;
            t.SetParent(parent, false);
            t.localPosition = pos;
            t.localScale = Vector3.one * scale;

            Prim.Cylinder("小盆", t, new Vector3(0f, 0.035f, 0f), 0.055f, 0.07f, MatLib.PotCeramic);
            Prim.Cylinder("小盆_土", t, new Vector3(0f, 0.068f, 0f), 0.050f, 0.012f, MatLib.Soil);
            for (int i = 0; i < 9; i++)
            {
                float a = i * 40f;
                var dir = new Vector3(Mathf.Sin(a * Mathf.Deg2Rad), 0f, Mathf.Cos(a * Mathf.Deg2Rad));
                var leaf = new GameObject("叶").transform;
                leaf.SetParent(t, false);
                leaf.localPosition = new Vector3(0f, 0.075f, 0f);
                leaf.localRotation = Quaternion.Euler(0f, a, 0f);
                Prim.Box("叶", leaf, new Vector3(0f, 0.055f, 0.045f), new Vector3(0.045f, 0.12f, 0.008f),
                    i % 2 == 0 ? MatLib.Leaf : MatLib.LeafLight,
                    Quaternion.Euler(-30f, 0f, 0f));
            }
        }
    }
}
