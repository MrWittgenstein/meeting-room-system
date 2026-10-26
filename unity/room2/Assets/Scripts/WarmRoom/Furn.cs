using UnityEngine;

namespace WarmRoom
{
    /// <summary>
    /// 家具（暖色木质商务风）：船形实木会议桌、12 把皮革高背会议椅、
    /// 边柜、白板、饮水台、绿植、衣帽架、垃圾桶、地毯等。
    /// </summary>
    public static class Furn
    {
        public const string Root = "02_家具";

        public static GameObject Build()
        {
            var root = new GameObject(Root);
            var p = root.transform;

            BuildTable(p);
            BuildChairs(p);
            BuildSideboard(p);
            BuildDisplayShelf(p);
            BuildWhiteboard(p);
            BuildWaterStation(p);
            BuildTrashBin(p);
            BuildCoatStand(p);
            BuildAreaRug(p);

            return root;
        }

        // ================================================================== 会议桌

        static void BuildTable(Transform p)
        {
            var t = new GameObject("会议桌").transform;
            t.SetParent(p, false);

            float L = Dim.TableL, W = Dim.TableW, H = Dim.TableH;

            // 桌面：圆角实木台面。
            // 说明：原本用自写的"超椭圆板"网格做船形桌面，但该自定义网格在 URP 下光照异常，故改用圆角盒。
            var tableTop = Mat.Pbr("w_tabletop", new Color(1.20f, 1.14f, 1.07f), 0.02f, 0.26f,
                Tex.Walnut(), Tex.WalnutN(), new Vector2(1.05f, 1.05f));
            Prim.RoundedBox("会议桌_台面", t, new Vector3(0f, H - Dim.TableTopT * 0.5f, 0f),
                new Vector3(L, Dim.TableTopT, W), tableTop);

            // 台面下沿收边
            Prim.RoundedBox("会议桌_收边", t, new Vector3(0f, H - Dim.TableTopT - 0.014f, 0f),
                new Vector3(L - 0.05f, 0.028f, W - 0.05f), Mat.WalnutDark);

            // 实木箱型底座（两端）
            float baseX = L * 0.5f - 0.62f;
            foreach (var s in new[] { -1f, 1f })
            {
                var tag = s < 0 ? "西" : "东";
                var b = new GameObject("会议桌_底座" + tag).transform;
                b.SetParent(t, false);
                b.localPosition = new Vector3(s * baseX, 0f, 0f);

                // 箱体
                Prim.Box("箱体", b, new Vector3(0f, 0.345f, 0f), new Vector3(0.24f, 0.66f, W - 0.34f), Mat.Walnut);
                // 踢脚内收
                Prim.Box("踢脚", b, new Vector3(0f, 0.020f, 0f), new Vector3(0.20f, 0.040f, W - 0.44f), Mat.BlackMetal);
                // 黄铜装饰压条
                Prim.Box("铜条", b, new Vector3(s * 0.125f, 0.345f, 0f), new Vector3(0.012f, 0.60f, W - 0.40f), Mat.Brass);
            }

            // 中央连接梁
            Prim.Box("会议桌_横梁", t, new Vector3(0f, 0.60f, 0f), new Vector3(baseX * 2f, 0.11f, 0.18f), Mat.WalnutDark);
            // 中间挡板（织物质感，遮走线）
            Prim.Box("会议桌_挡板", t, new Vector3(0f, 0.48f, 0f), new Vector3(baseX * 2f - 0.30f, 0.30f, 0.035f), Mat.MeshFabric);

            // 桌面中央走线盒（黄铜翻盖 + 插座）
            Prim.RoundedBox("会议桌_走线盒", t, new Vector3(0f, H + 0.006f, 0f),
                new Vector3(0.95f, 0.022f, 0.17f), Mat.Brass);
            for (int i = 0; i < 4; i++)
                Prim.Box("会议桌_插座" + i, t, new Vector3(-0.31f + i * 0.21f, H + 0.017f, 0.02f),
                    new Vector3(0.078f, 0.009f, 0.052f), Mat.Plastic);

            BuildTableMic(t, new Vector3(0f, H + 0.016f, 0.30f));
            BuildTableProps(t, H);
        }

        static void BuildTableMic(Transform t, Vector3 c)
        {
            Prim.RoundedBox("会议桌_拾音器", t, c, new Vector3(0.36f, 0.028f, 0.15f), Mat.BlackMetal);
            for (int i = 0; i < 3; i++)
                Prim.Cylinder("会议桌_拾音孔" + i, t, new Vector3(c.x - 0.10f + i * 0.10f, c.y + 0.018f, c.z),
                    0.014f, 0.008f, Mat.Rubber);
            Prim.Sphere("会议桌_拾音指示灯", t, new Vector3(c.x + 0.15f, c.y + 0.016f, c.z), 0.010f,
                Mat.Emissive("w_mic_led", new Color(0.55f, 1f, 0.45f), 2.0f));
        }

        static void BuildTableProps(Transform t, float H)
        {
            float y = H + 0.006f;
            for (int i = 0; i < 10; i++)
            {
                int side = i / 5;              // 0: -Z 侧, 1: +Z 侧
                int k = i % 5;
                float x = -1.28f + k * 0.64f;
                float z = side == 0 ? -0.30f : 0.30f;

                var g = new GameObject("桌面文具_" + i).transform;
                g.SetParent(t, false);
                g.localPosition = new Vector3(x, y, z);
                g.localRotation = Quaternion.Euler(0f, side == 0 ? 0f : 180f, 0f);

                Prim.Box("便签本", g, new Vector3(0f, 0.006f, 0f), new Vector3(0.15f, 0.012f, 0.21f), Mat.Paper);
                Prim.Cylinder("钢笔", g, new Vector3(0.095f, 0.007f, 0.055f), 0.005f, 0.14f,
                    Mat.BlackMetal, Quaternion.Euler(90f, 0f, 0f));
                Prim.Cylinder("水杯", g, new Vector3(-0.11f, 0.055f, -0.065f), 0.033f, 0.11f, Mat.Sheer);
                Prim.Cylinder("水杯_水", g, new Vector3(-0.11f, 0.028f, -0.065f), 0.029f, 0.055f,
                    Mat.Emissive("w_water", new Color(0.62f, 0.80f, 0.86f), 0.15f));
            }

            // 主位桌牌
            for (int i = 0; i < 2; i++)
            {
                var plate = new GameObject("桌牌_" + i).transform;
                plate.SetParent(t, false);
                plate.localPosition = new Vector3(i == 0 ? -1.40f : 1.40f, y, 0f);
                plate.localRotation = Quaternion.Euler(0f, i == 0 ? -90f : 90f, 0f);
                Prim.Box("面板", plate, new Vector3(0f, 0.048f, 0f), new Vector3(0.26f, 0.096f, 0.014f), Mat.Brass);
                Prim.Box("底座", plate, new Vector3(0f, 0.006f, 0.022f), new Vector3(0.23f, 0.012f, 0.06f), Mat.WalnutDark);
            }
        }

        // ================================================================== 会议椅

        static void BuildChairs(Transform p)
        {
            var root = new GameObject("会议椅组").transform;
            root.SetParent(p, false);

            int idx = 0;
            // 后方一排（z = -RowZ）：桌子在它们的 +Z 方向 → 正面朝 +Z ⇒ yaw = 180°
            // 会议椅模型：坐垫前缘在本地 -Z、靠背在本地 +Z ⇒ 正面 = 本地 -Z
            for (int i = 0; i < Dim.SeatsPerLongSide; i++)
            {
                float x = Mathf.Lerp(-1.42f, 1.42f, i / (float)(Dim.SeatsPerLongSide - 1));
                BuildChair(root, "会议椅_" + (++idx), new Vector3(x, 0f, -Dim.RowZ), 180f);
            }
            // 前方一排（z = +RowZ）：桌子在 -Z 方向 → 正面朝 -Z ⇒ yaw = 0°
            for (int i = 0; i < Dim.SeatsPerLongSide; i++)
            {
                float x = Mathf.Lerp(-1.42f, 1.42f, i / (float)(Dim.SeatsPerLongSide - 1));
                BuildChair(root, "会议椅_" + (++idx), new Vector3(x, 0f, Dim.RowZ), 0f);
            }
            // 两端主位：正面朝 +X ⇒ yaw = -90°；正面朝 -X ⇒ yaw = +90°
            BuildChair(root, "会议椅_主位", new Vector3(-Dim.HeadX, 0f, 0f), -90f);
            BuildChair(root, "会议椅_副主位", new Vector3(Dim.HeadX, 0f, 0f), 90f);
        }

        /// <summary>
        /// 皮革高背会议椅。正面 = 本地 -Z（坐垫前缘在此侧、靠背在 +Z 侧）。
        /// yaw = 0 表示正面朝世界 -Z。
        /// </summary>
        public static GameObject BuildChair(Transform parent, string name, Vector3 pos, float yaw)
        {
            var root = new GameObject(name);
            root.transform.SetParent(parent, false);
            root.transform.localPosition = pos;
            root.transform.localRotation = Quaternion.Euler(0f, yaw, 0f);
            var t = root.transform;

            var leather = Mat.Leather;
            var dark = Mat.BlackMetal;
            var brass = Mat.Brass;
            const float seatY = 0.470f;

            // ---- 五星脚 + 万向轮 ----
            for (int i = 0; i < 5; i++)
            {
                float a = i * 72f;
                var dir = new Vector3(Mathf.Sin(a * Mathf.Deg2Rad), 0f, Mathf.Cos(a * Mathf.Deg2Rad));
                var arm = new GameObject("脚臂" + i).transform;
                arm.SetParent(t, false);
                arm.localPosition = new Vector3(0f, 0.070f, 0f);
                arm.localRotation = Quaternion.Euler(0f, a, 0f);
                Prim.RoundedBox("臂", arm, new Vector3(0f, 0f, 0.155f), new Vector3(0.055f, 0.038f, 0.34f), dark);

                var wheel = new GameObject("轮" + i).transform;
                wheel.SetParent(t, false);
                wheel.localPosition = new Vector3(dir.x * 0.29f, 0f, dir.z * 0.29f);
                Prim.Cylinder("轮架", wheel, new Vector3(0f, 0.028f, 0f), 0.030f, 0.026f, Mat.Plastic);
                Prim.Sphere("轮子", wheel, new Vector3(0f, 0.028f, 0f), 0.029f, Mat.Rubber);
            }

            // ---- 气压杆 ----
            Prim.Cylinder("气压杆", t, new Vector3(0f, 0.215f, 0f), 0.028f, 0.30f, Mat.Chrome);
            Prim.Cylinder("气压杆座", t, new Vector3(0f, 0.100f, 0f), 0.038f, 0.14f, dark);
            Prim.Cylinder("座椅底盘", t, new Vector3(0f, 0.385f, 0f), 0.105f, 0.022f, dark);

            // ---- 坐垫（皮革，前缘略翘）----
            Prim.RoundedBox("坐垫", t, new Vector3(0f, seatY, 0.005f), new Vector3(0.500f, 0.095f, 0.490f), leather);
            Prim.RoundedBox("坐垫_前缘", t, new Vector3(0f, seatY + 0.014f, -0.240f),
                new Vector3(0.470f, 0.066f, 0.065f), leather);
            Prim.RoundedBox("坐垫_底壳", t, new Vector3(0f, seatY - 0.058f, 0.005f),
                new Vector3(0.460f, 0.036f, 0.450f), dark);

            // ---- 高靠背（后倾 11°，带腰托与头枕）----
            var back = new GameObject("靠背").transform;
            back.SetParent(t, false);
            back.localPosition = new Vector3(0f, 0.800f, 0.220f);
            back.localRotation = Quaternion.Euler(-11f, 0f, 0f);
            Prim.RoundedBox("靠背_主体", back, new Vector3(0f, 0f, 0f), new Vector3(0.470f, 0.660f, 0.075f), leather);
            Prim.RoundedBox("靠背_腰托", back, new Vector3(0f, -0.150f, -0.048f), new Vector3(0.400f, 0.170f, 0.055f), leather);
            Prim.RoundedBox("靠背_头枕", back, new Vector3(0f, 0.375f, -0.040f), new Vector3(0.320f, 0.130f, 0.070f), leather);
            // 靠背外框（深色，形成"皮面嵌在框里"的观感）
            Prim.Box("靠背_背板", back, new Vector3(0f, -0.020f, 0.052f), new Vector3(0.430f, 0.640f, 0.022f), dark);
            // 靠背支架
            Prim.Box("靠背_支架", t, new Vector3(0f, 0.600f, 0.190f), new Vector3(0.28f, 0.18f, 0.038f), dark);

            // ---- 扶手（黄铜立柱 + 木质扶手面）----
            for (int s = 0; s < 2; s++)
            {
                float x = s == 0 ? -0.320f : 0.320f;
                var arm = new GameObject("扶手" + s).transform;
                arm.SetParent(t, false);
                arm.localPosition = new Vector3(x, 0.610f, 0.020f);
                Prim.Box("立柱", arm, new Vector3(0f, -0.105f, 0f), new Vector3(0.042f, 0.21f, 0.042f), brass);
                Prim.RoundedBox("扶手面", arm, new Vector3(0f, 0.010f, -0.015f),
                    new Vector3(0.070f, 0.030f, 0.300f), Mat.WalnutDark);
            }

            return root;
        }

        // ================================================================== 边柜 / 陈设

        static void BuildSideboard(Transform p)
        {
            // 陈设墙（z=+3）下方的长边柜
            var s = new GameObject("边柜").transform;
            s.SetParent(p, false);
            s.localPosition = new Vector3(0f, 0f, Dim.HZ - 0.30f);

            const float W = 3.40f, H = 0.78f, D = 0.46f, legH = 0.14f;

            Prim.RoundedBox("边柜_柜体", s, new Vector3(0f, legH + (H - legH) * 0.5f, 0f),
                new Vector3(W, H - legH, D), Mat.Walnut);
            Prim.Box("边柜_台面", s, new Vector3(0f, H + 0.020f, 0f),
                new Vector3(W + 0.05f, 0.040f, D + 0.03f), Mat.Marble);

            for (int i = 0; i < 4; i++)
            {
                float x = (i % 2 == 0 ? -1f : 1f) * (W * 0.5f - 0.16f);
                float z = (i < 2 ? -1f : 1f) * (D * 0.5f - 0.11f);
                Prim.Cylinder("边柜_腿" + i, s, new Vector3(x, legH * 0.5f, z), 0.019f, legH, Mat.Brass);
            }

            // 四扇柜门 + 黄铜拉手
            for (int i = 0; i < 4; i++)
            {
                float x = -W * 0.5f + W * (i + 0.5f) / 4f;
                Prim.Box("边柜_门缝" + i, s, new Vector3(x, legH + (H - legH) * 0.5f, D * 0.5f + 0.003f),
                    new Vector3(0.007f, H - legH - 0.05f, 0.005f), Mat.WalnutDark);
                Prim.Cylinder("边柜_拉手" + i, s, new Vector3(x + (i % 2 == 0 ? 0.10f : -0.10f), 0.52f, D * 0.5f + 0.026f),
                    0.010f, 0.18f, Mat.Brass, Quaternion.Euler(90f, 0f, 0f));
            }

            // 台面陈设：文件架 + 小盆栽 + 矿泉水
            Prim.Box("边柜_文件架", s, new Vector3(-1.35f, H + 0.12f, 0.02f), new Vector3(0.32f, 0.16f, 0.24f), Mat.WalnutDark);
            for (int i = 0; i < 4; i++)
                Prim.Box("边柜_文件夹" + i, s, new Vector3(-1.46f + i * 0.056f, H + 0.145f, 0f),
                    new Vector3(0.013f, 0.21f, 0.20f),
                    Mat.Pbr("w_folder" + (i % 2), i % 2 == 0 ? new Color(0.42f, 0.24f, 0.12f) : new Color(0.22f, 0.28f, 0.36f), 0f, 0.35f));

            BuildSmallPlant(s, new Vector3(1.38f, H + 0.040f, 0f), 0.58f);
            for (int i = 0; i < 3; i++)
                Prim.Cylinder("边柜_水瓶" + i, s, new Vector3(0.60f + i * 0.078f, H + 0.145f, 0.10f), 0.036f, 0.20f, Mat.Sheer);
        }

        /// <summary>
        /// 陈设墙置物台上的陈设品。
        ///
        /// 坐标提示（这里连着出过两次问题）：
        ///   1) 父节点若带偏移，子物体坐标是"相对值"。上一版父节点 y=2.84 而子物体却写 2.00，
        ///      结果器物飞到 y≈4.4~5.2 的空中。
        ///   2) 器物必须落在置物台的**台面范围内**，否则看着就是悬空。
        /// 为了彻底避开这两点：本组直接把父节点放在原点，子物体坐标=世界坐标，
        /// 并让所有器物的底面严格等于台面上表面（shelfTopY）。
        /// </summary>
        static void BuildDisplayShelf(Transform p)
        {
            var t = new GameObject("陈设龛").transform;
            t.SetParent(p, false);
            t.localPosition = Vector3.zero;      // 坐标即世界坐标

            const float shelfTopY = 2.4775f;     // 上层置物台上表面（见 Shell.BuildWallSideboard）
            const float shelfZ = 2.78f;          // 台面中心 z

            // 三只陶罐 + 绿枝
            for (int i = 0; i < 3; i++)
            {
                float x = -0.85f + i * 0.85f;
                float h = i == 1 ? 0.34f : 0.26f;
                Prim.Cylinder("陶罐" + i, t, new Vector3(x, shelfTopY + h * 0.5f, shelfZ), 0.085f, h, Mat.PotCeramic);
                Prim.Cylinder("陶罐口" + i, t, new Vector3(x, shelfTopY + h + 0.012f, shelfZ), 0.045f, h * 0.18f, Mat.PotCeramic);
                Prim.Cylinder("罐内绿枝" + i, t, new Vector3(x, shelfTopY + h * 1.55f, shelfZ), 0.012f, h * 1.1f,
                    Mat.Leaf, Quaternion.Euler(6f * i - 6f, 0f, 4f - 4f * i));
            }

            // 一排书（立放在左端台面上）
            for (int i = 0; i < 7; i++)
                Prim.Box("书" + i, t, new Vector3(-1.12f + i * 0.030f, shelfTopY + 0.10f, shelfZ),
                    new Vector3(0.024f, 0.20f, 0.14f),
                    Mat.Pbr("w_book" + (i % 3),
                        i % 3 == 0 ? new Color(0.44f, 0.22f, 0.12f)
                                   : i % 3 == 1 ? new Color(0.20f, 0.26f, 0.34f) : new Color(0.34f, 0.32f, 0.22f),
                        0f, 0.30f));

            // 黄铜小摆件
            Prim.RoundedBox("摆件", t, new Vector3(1.08f, shelfTopY + 0.06f, shelfZ),
                new Vector3(0.16f, 0.12f, 0.10f), Mat.Brass);
        }

        static void BuildWhiteboard(Transform p)
        {
            var w = new GameObject("白板").transform;
            w.SetParent(p, false);
            // 放在门墙（+X）靠南一侧，避开门口
            w.localPosition = new Vector3(Dim.HX - 0.075f, 1.58f, -2.10f);
            w.localRotation = Quaternion.Euler(0f, -90f, 0f);

            const float W = 1.90f, H = 1.20f;
            Prim.Box("白板_板面", w, Vector3.zero, new Vector3(W, H, 0.018f), Mat.Whiteboard);
            Prim.Box("白板_边框", w, new Vector3(0f, 0f, 0.002f), new Vector3(W + 0.08f, H + 0.08f, 0.018f), Mat.WbFrame);
            Prim.Box("白板_笔槽", w, new Vector3(0f, -H * 0.5f - 0.055f, 0.04f), new Vector3(W, 0.032f, 0.075f), Mat.Brass);
            for (int i = 0; i < 3; i++)
                Prim.Cylinder("白板_笔" + i, w, new Vector3(-0.26f + i * 0.12f, -H * 0.5f - 0.040f, 0.060f),
                    0.011f, 0.14f,
                    Mat.Pbr("w_marker" + i, i == 0 ? new Color(0.78f, 0.16f, 0.12f) : i == 1 ? new Color(0.14f, 0.30f, 0.62f) : new Color(0.14f, 0.14f, 0.14f), 0f, 0.40f),
                    Quaternion.Euler(90f, 0f, 0f));
            Prim.Box("白板_板擦", w, new Vector3(0.72f, -H * 0.5f - 0.060f, 0.065f), new Vector3(0.13f, 0.036f, 0.06f), Mat.Plastic);
        }

        // ================================================================== 其他陈设

        static void BuildWaterStation(Transform p)
        {
            // 门墙（+X）靠北一侧
            var w = new GameObject("饮水台").transform;
            w.SetParent(p, false);
            w.localPosition = new Vector3(Dim.HX - 0.42f, 0f, 2.05f);
            w.localRotation = Quaternion.Euler(0f, -58f, 0f);

            Prim.RoundedBox("台体", w, new Vector3(0f, 0.40f, 0f), new Vector3(0.74f, 0.80f, 0.52f), Mat.Walnut);
            Prim.Box("台面", w, new Vector3(0f, 0.815f, 0f), new Vector3(0.78f, 0.032f, 0.56f), Mat.Marble);

            Prim.Box("饮水机_机身", w, new Vector3(-0.17f, 1.10f, 0f), new Vector3(0.34f, 0.54f, 0.34f), Mat.PlasticWhite);
            Prim.Box("饮水机_面板", w, new Vector3(-0.17f, 1.17f, -0.18f), new Vector3(0.22f, 0.20f, 0.02f), Mat.AcScreen);
            Prim.Box("饮水机_出水口", w, new Vector3(-0.17f, 0.93f, -0.15f), new Vector3(0.10f, 0.05f, 0.10f), Mat.Plastic);
            Prim.Cylinder("饮水机_水瓶", w, new Vector3(-0.17f, 1.51f, 0f), 0.145f, 0.32f,
                Mat.Emissive("w_bottle", new Color(0.66f, 0.84f, 0.90f), 0.20f));
            for (int i = 0; i < 4; i++)
                Prim.Cylinder("纸杯" + i, w, new Vector3(0.21f, 0.845f + i * 0.075f, 0.10f), 0.033f, 0.075f, Mat.Paper);
        }

        static void BuildTrashBin(Transform p)
        {
            var b = new GameObject("垃圾桶").transform;
            b.SetParent(p, false);
            b.localPosition = new Vector3(3.05f, 0f, 1.55f);
            Prim.Cylinder("桶身", b, new Vector3(0f, 0.145f, 0f), 0.115f, 0.29f, Mat.Brass);
            Prim.Cylinder("内桶", b, new Vector3(0f, 0.295f, 0f), 0.108f, 0.02f, Mat.Plastic);
            Prim.Cylinder("垃圾袋", b, new Vector3(0f, 0.20f, 0f), 0.098f, 0.10f,
                Mat.Pbr("w_bag", new Color(0.15f, 0.145f, 0.14f), 0f, 0.10f));
        }

        static void BuildCoatStand(Transform p)
        {
            var c = new GameObject("衣帽架").transform;
            c.SetParent(p, false);
            c.localPosition = new Vector3(-3.30f, 0f, 2.35f);

            Prim.Cylinder("底盘", c, new Vector3(0f, 0.014f, 0f), 0.21f, 0.026f, Mat.BlackMetal);
            Prim.Cylinder("立柱", c, new Vector3(0f, 0.90f, 0f), 0.018f, 1.76f, Mat.Brass);
            for (int i = 0; i < 4; i++)
            {
                var hook = new GameObject("挂钩" + i).transform;
                hook.SetParent(c, false);
                hook.localPosition = new Vector3(0f, 1.76f, 0f);
                hook.localRotation = Quaternion.Euler(0f, i * 90f, 0f);
                Prim.Cylinder("挂臂", hook, new Vector3(0f, 0.02f, 0.075f), 0.012f, 0.15f, Mat.Brass,
                    Quaternion.Euler(80f, 0f, 0f));
                Prim.Sphere("挂头", hook, new Vector3(0f, 0.055f, 0.145f), 0.017f, Mat.Brass);
            }
            Prim.RoundedBox("外套", c, new Vector3(0.02f, 1.32f, 0.06f), new Vector3(0.30f, 0.64f, 0.22f),
                Mat.Pbr("w_coat", new Color(0.13f, 0.115f, 0.10f), 0f, 0.22f), Quaternion.Euler(0f, 14f, 0f));
        }

        static void BuildAreaRug(Transform p)
        {
            // 会议区地毯（深棕，压在木边带之内）
            Prim.Box("会议区地毯", p, new Vector3(0f, 0.003f, 0f), new Vector3(5.30f, 0.010f, 3.60f), Mat.Rug);
        }

        // ================================================================== 绿植（复用）

        public static GameObject BuildPlant(Transform parent, string name, Vector3 pos, float scale, int seed)
        {
            var root = new GameObject(name);
            root.transform.SetParent(parent, false);
            root.transform.localPosition = pos;
            root.transform.localScale = Vector3.one * scale;
            var t = root.transform;

            Prim.Cylinder("花盆", t, new Vector3(0f, 0.20f, 0f), 0.20f, 0.40f, Mat.PotCeramic);
            Prim.Cylinder("花盆口沿", t, new Vector3(0f, 0.396f, 0f), 0.215f, 0.035f, Mat.PotCeramic);
            Prim.Cylinder("花盆土", t, new Vector3(0f, 0.396f, 0f), 0.185f, 0.02f, Mat.Soil);
            Prim.Cylinder("主干", t, new Vector3(0f, 0.62f, 0f), 0.028f, 0.46f,
                Mat.Pbr("w_bark", new Color(0.175f, 0.130f, 0.092f), 0f, 0.15f));

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
                    Prim.Box("叶", leaf, new Vector3(0f, leafLen * 0.30f, rad * 0.55f),
                        new Vector3(0.15f, leafLen, 0.012f),
                        (layer + i) % 2 == 0 ? Mat.Leaf : Mat.LeafLight,
                        Quaternion.Euler(-38f - layer * 6f, 0f, 0f));
                }
            }
            return root;
        }

        public static void BuildSmallPlant(Transform parent, Vector3 pos, float scale)
        {
            var t = new GameObject("小盆栽").transform;
            t.SetParent(parent, false);
            t.localPosition = pos;
            t.localScale = Vector3.one * scale;

            Prim.Cylinder("小盆", t, new Vector3(0f, 0.035f, 0f), 0.055f, 0.07f, Mat.PotCeramic);
            Prim.Cylinder("小盆土", t, new Vector3(0f, 0.068f, 0f), 0.050f, 0.012f, Mat.Soil);
            for (int i = 0; i < 9; i++)
            {
                float a = i * 40f;
                var leaf = new GameObject("叶").transform;
                leaf.SetParent(t, false);
                leaf.localPosition = new Vector3(0f, 0.075f, 0f);
                leaf.localRotation = Quaternion.Euler(0f, a, 0f);
                Prim.Box("叶", leaf, new Vector3(0f, 0.055f, 0.045f), new Vector3(0.045f, 0.12f, 0.008f),
                    i % 2 == 0 ? Mat.Leaf : Mat.LeafLight, Quaternion.Euler(-30f, 0f, 0f));
            }
        }
    }
}
