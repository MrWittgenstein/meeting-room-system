using UnityEngine;

namespace NordicRoom
{
    /// <summary>
    /// 家具与陈设（北欧明亮风）。
    /// 与前两个会议室的差别：**中央圆桌**（Ø2.90m）＋ 12 把浅色北欧壳椅绕圈均布；
    /// 沿墙做长条矮柜（非餐桌/边柜）+ 玻璃白板 + 羊毛地毯 + 绿植。
    ///
    /// 约定：所有装饰小件一律"直接在房间坐标系里算"，底部严格坐在承载面上，
    /// 避免出现浮空物体。
    /// </summary>
    public static class Furn
    {
        public const string Root = "02_家具陈设";

        public static GameObject Build()
        {
            var root = new GameObject(Root);
            var p = root.transform;

            BuildRug(p);
            BuildRoundTable(p);
            BuildChairs(p);
            BuildSideboard(p);
            BuildMirror(p);
            BuildWhiteboard(p);
            BuildPlants(p);
            BuildFloorLamp(p);
            BuildLoungeCorner(p);

            return root;
        }

        // ------------------------------------------------------------------ 地毯

        static void BuildRug(Transform p)
        {
            var m = Mat.Rug;
            // 羊毛地毯：中央大圆毯 + 一层薄底（贴地不浮空）
            Prim.Box("地毯_底衬", p, new Vector3(0f, 0.006f, 0f), new Vector3(7.60f, 0.012f, 5.60f), m);
            // 地毯包边（略深色一圈）
            var trim = Mat.Pbr("n_rug_trim", new Color(0.560f, 0.555f, 0.540f), 0f, 0.08f);
            float ex = 3.80f, ez = 2.80f;
            Prim.Box("地毯_包边_南", p, new Vector3(0f, 0.014f, -ez + 0.05f), new Vector3(ex * 2f, 0.006f, 0.10f), trim);
            Prim.Box("地毯_包边_北", p, new Vector3(0f, 0.014f, ez - 0.05f), new Vector3(ex * 2f, 0.006f, 0.10f), trim);
            Prim.Box("地毯_包边_西", p, new Vector3(-ex + 0.05f, 0.014f, 0f), new Vector3(0.10f, 0.006f, ez * 2f), trim);
            Prim.Box("地毯_包边_东", p, new Vector3(ex - 0.05f, 0.014f, 0f), new Vector3(0.10f, 0.006f, ez * 2f), trim);
        }

        // ------------------------------------------------------------------ 中央圆桌

        /// <summary>中央大圆桌：白橡木圆台面 + 中心浅灰钢柱脚 + 木质底盘。</summary>
        static void BuildRoundTable(Transform p)
        {
            var t = new GameObject("中央圆桌").transform;
            t.SetParent(p, false);

            float R = Dim.TableDia * 0.5f;
            float topY = Dim.TableH;                 // 台面上表面
            float topT = Dim.TableTopT;

            // 台面用 RoundSlab（自建圆盘网格，法线朝上；不要用椭圆网格）
            Prim.RoundSlab("圆桌面_白橡木", t, new Vector3(0f, topY - topT * 0.5f, 0f), R, topT, Mat.WoodTop);
            // 台面下沿一圈收边
            Prim.Cylinder("圆桌面_收边", t, new Vector3(0f, topY - topT - 0.011f, 0f), R - 0.010f, 0.022f, Mat.AshWood, null, false, 72);

            // 中心柱脚
            Prim.Cylinder("桌_中心柱", t, new Vector3(0f, (topY - topT) * 0.5f, 0f), 0.115f, topY - topT - 0.05f, Mat.Steel, null, false, 48);
            Prim.Cylinder("桌_柱脚装饰环", t, new Vector3(0f, 0.16f, 0f), 0.145f, 0.05f, Mat.Chrome, null, false, 48);
            // 木质底盘（十字 + 圆盘）
            Prim.Cylinder("桌_底盘", t, new Vector3(0f, 0.028f, 0f), R * 0.42f, 0.056f, Mat.AshWood, null, false, 72);
            for (int i = 0; i < 4; i++)
            {
                float yaw = i * 90f;
                var q = Quaternion.Euler(0f, yaw, 0f);
                var dir = q * Vector3.forward;
                Prim.Box("桌_脚撑" + i, t, dir * (R * 0.22f) + new Vector3(0f, 0.050f, 0f),
                    new Vector3(0.10f, 0.048f, R * 0.44f), Mat.AshWood, q);
            }

            // 桌面中央：北欧风小摆件（浅色陶盘 + 一支绿枝）
            Prim.Cylinder("桌面摆件_陶盘", t, new Vector3(0f, topY + 0.018f, 0f), 0.165f, 0.036f, Mat.PotWhite, null, false, 48);
            Prim.Cylinder("桌面摆件_陶土", t, new Vector3(0f, topY + 0.031f, 0f), 0.150f, 0.014f, Mat.Soil, null, false, 48);
            BuildSpray(t, new Vector3(0f, topY + 0.038f, 0f), 0.15f, 13, 0.62f);

            // 桌面：三组"文档 + 笔"（按 120° 布置）
            for (int i = 0; i < 3; i++)
            {
                float a = 30f + i * 120f;
                var q = Quaternion.Euler(0f, a, 0f);
                var dir = q * Vector3.forward;
                Prim.Box("桌面文件_" + i, t, dir * (R * 0.60f) + new Vector3(0f, topY + 0.006f, 0f),
                    new Vector3(0.230f, 0.010f, 0.320f), Mat.Paper, q);
                Prim.Box("桌面文件_" + i + "_上", t, dir * (R * 0.60f) + new Vector3(0f, topY + 0.016f, 0f),
                    new Vector3(0.212f, 0.008f, 0.300f), Mat.Pbr("n_paper2", new Color(0.930f, 0.930f, 0.920f), 0f, 0.26f), q);
                Prim.Box("桌面笔_" + i, t, dir * (R * 0.60f) + new Vector3(0f, topY + 0.026f, 0f) + q * new Vector3(0.09f, 0f, 0.04f),
                    new Vector3(0.016f, 0.014f, 0.150f), Mat.AccentBlue, q);
            }

            // 桌面杯垫 + 水杯两组
            for (int i = 0; i < 2; i++)
            {
                float a = -140f + i * 60f;
                var q = Quaternion.Euler(0f, a, 0f);
                var dir = q * Vector3.forward;
                var basePos = dir * (R * 0.66f) + new Vector3(0f, topY, 0f);
                Prim.Cylinder("桌面杯垫_" + i, t, basePos + new Vector3(0f, 0.005f, 0f), 0.055f, 0.010f, Mat.PotGrey, null, false, 32);
                Prim.Cylinder("水杯_" + i, t, basePos + new Vector3(0f, 0.062f, 0f), 0.038f, 0.108f, Mat.GlassDoor, null, false, 32);
                Prim.Cylinder("水杯_" + i + "_水", t, basePos + new Vector3(0f, 0.042f, 0f), 0.033f, 0.062f,
                    Mat.Clear("n_water", new Color(0.780f, 0.900f, 0.940f), 0.9f, 0.55f), null, false, 32);
            }

            // 桌下走线（浅灰地面线槽，从桌心通到后墙）
            Prim.Box("桌下线槽", t, new Vector3(0f, 0.020f, 0f), new Vector3(0.070f, 0.024f, 0.10f), Mat.PotGrey);
        }

        // ------------------------------------------------------------------ 椅子

        static void BuildChairs(Transform p)
        {
            for (int i = 0; i < Dim.SeatsTotal; i++)
            {
                float a = i * (360f / Dim.SeatsTotal);
                var q = Quaternion.Euler(0f, a, 0f);
                var pos = q * Vector3.forward * Dim.SeatRadius;
                BuildChair(p, i, new Vector3(pos.x, 0f, pos.z), q, a);
            }
        }

        /// <summary>
        /// 北欧壳椅：浅灰麻布坐垫 + 白蜡木四条外撇腿 + 弧形靠背。
        /// 本地 -Z 为"人面向的方向"，因此绕 Y 旋转 a 度后自动朝向桌心。
        /// </summary>
        static void BuildChair(Transform p, int idx, Vector3 pos, Quaternion q, float yaw)
        {
            var t = new GameObject("椅子" + idx).transform;
            t.SetParent(p, false);
            t.localPosition = pos;
            t.localRotation = q;

            var frame = Mat.AshWood;
            var seatFab = (idx % 3 == 0) ? Mat.AccentOlive : Mat.Linen;
            var foot = Mat.Pbr("n_chair_foot", new Color(0.835f, 0.830f, 0.820f), 0f, 0.20f);

            const float seatY = 0.455f;     // 坐面高度
            const float seatT = 0.062f;
            const float W = 0.470f;         // 座宽（X）
            const float D = 0.440f;         // 座深（Z）

            // 坐垫
            Prim.RoundedBox("椅_坐垫", t, new Vector3(0f, seatY - seatT * 0.5f, 0f),
                new Vector3(W, seatT, D), seatFab);
            // 座框
            Prim.Box("椅_座框", t, new Vector3(0f, seatY - seatT - 0.017f, 0f),
                new Vector3(W + 0.012f, 0.034f, D + 0.012f), frame);

            // 四条外撇腿（下端比上端更靠外）
            float legBot = 0.012f, legTop = seatY - seatT - 0.034f;
            foreach (var sx in new[] { -1f, 1f })
                foreach (var sz in new[] { -1f, 1f })
                {
                    var top = new Vector3(sx * (W * 0.5f - 0.048f), legTop, sz * (D * 0.5f - 0.048f));
                    var bot = new Vector3(sx * (W * 0.5f - 0.008f), legBot, sz * (D * 0.5f - 0.008f));
                    Prim.BoxBetween("椅_腿" + (sx < 0 ? "L" : "R") + (sz < 0 ? "F" : "B"), t,
                        Vector3.Min(top, bot) - new Vector3(0.019f, 0f, 0.019f),
                        Vector3.Max(top, bot) + new Vector3(0.019f, 0f, 0.019f), frame);
                    // 腿端金属脚垫
                    Prim.Cylinder("椅_脚垫", t, bot + new Vector3(0f, -0.004f, 0f), 0.020f, 0.012f, foot, null, false, 20);
                }

            // 靠背：立柱 + 弧形靠板（三段折面模拟弧度），本地 +Z 为背后
            float backZ = D * 0.5f - 0.030f;
            foreach (var sx in new[] { -1f, 1f })
                Prim.Box("椅_背柱" + (sx < 0 ? "L" : "R"), t,
                    new Vector3(sx * (W * 0.5f - 0.030f), seatY + 0.185f, backZ),
                    new Vector3(0.042f, 0.400f, 0.032f), frame);

            float backTop = 0.905f;
            // 中段靠板
            Prim.RoundedBox("椅_靠板_中", t, new Vector3(0f, (seatY + 0.06f + backTop) * 0.5f, backZ + 0.006f),
                new Vector3(W - 0.020f, backTop - seatY - 0.06f, 0.048f), seatFab);
            // 两侧略前折，形成圆弧感
            foreach (var sx in new[] { -1f, 1f })
            {
                var qq = Quaternion.Euler(0f, sx * 17f, 0f);
                Prim.RoundedBox("椅_靠板_" + (sx < 0 ? "L" : "R"), t,
                    new Vector3(sx * (W * 0.5f - 0.045f), (seatY + 0.06f + backTop) * 0.5f, backZ - 0.036f),
                    new Vector3(W * 0.36f, backTop - seatY - 0.06f, 0.048f), seatFab, qq);
            }
            // 靠板顶沿木条
            Prim.Box("椅_靠板顶条", t, new Vector3(0f, backTop + 0.012f, backZ + 0.004f),
                new Vector3(W - 0.018f, 0.026f, 0.056f), frame);

            // 座下横撑
            foreach (var sx in new[] { -1f, 1f })
                Prim.Box("椅_横撑" + (sx < 0 ? "L" : "R"), t,
                    new Vector3(sx * (W * 0.5f - 0.026f), 0.235f, 0f), new Vector3(0.022f, 0.022f, D * 0.62f), frame);

            // 椅子命中体（仅碰撞器，方便后续点击选中）
            var hit = Prim.Hitbox("设备_座椅" + idx, t.parent, pos + new Vector3(0f, 0.48f, 0f),
                new Vector3(0.62f, 0.98f, 0.62f));
            hit.transform.localRotation = q;
            var dc = hit.AddComponent<DeviceController>();
            dc.id = "seat" + idx;
            dc.displayName = "座椅 " + (idx + 1);
            dc.kind = DeviceKind.Sensor;   // 只作展示/占位，不参与开关
            dc.initialOn = false;
            dc.initialValue = 0f;
            dc.passive = true;
        }

        // ------------------------------------------------------------------ 长条矮柜

        /// <summary>北墙中段前的长条矮柜（白橡木、无拉手），柜面摆陈设，柜上挂圆镜。</summary>
        static void BuildSideboard(Transform p)
        {
            // 位置：北墙中段（z = HZ - 深度），x 偏西一点，避开主位视线
            const float cz = Dim.HZ - 0.24f;
            const float cx = -0.40f;   // 落在北墙中段实墙前（避开两扇角窗）
            const float W = 2.60f, D = 0.44f, H = 0.76f;

            var t = new GameObject("长条矮柜").transform;
            t.SetParent(p, false);
            t.localPosition = new Vector3(cx, 0f, cz);

            var body = Mat.LightOak;
            var front = Mat.AshWood;
            var metal = Mat.BlackMetal;

            Prim.Box("矮柜_柜体", t, new Vector3(0f, H * 0.5f, 0f), new Vector3(W, H, D), body);
            Prim.Box("矮柜_台面", t, new Vector3(0f, H + 0.018f, 0f), new Vector3(W + 0.04f, 0.036f, D + 0.03f), front);

            // 抽屉/柜门分格（朝 -Z 即房间内）
            float frontZ = -D * 0.5f - 0.008f;
            for (int i = 0; i < 3; i++)
            {
                float dx = -W * 0.5f + W / 6f + i * (W / 3f);
                Prim.Box("矮柜_门" + i, t, new Vector3(dx, H * 0.5f, frontZ),
                    new Vector3(W / 3f - 0.026f, H - 0.08f, 0.018f), front);
                // 无拉手：斜切凹槽
                Prim.Box("矮柜_扣手" + i, t, new Vector3(dx, H - 0.10f, frontZ - 0.012f),
                    new Vector3(W / 3f - 0.070f, 0.020f, 0.014f), metal);
            }
            // 细金属腿
            foreach (var sx in new[] { -1f, 1f })
                foreach (var sz in new[] { -1f, 1f })
                    Prim.Cylinder("矮柜_腿", t,
                        new Vector3(sx * (W * 0.5f - 0.08f), 0.070f, sz * (D * 0.5f - 0.07f)),
                        0.014f, 0.140f, metal, null, false, 16);
            Prim.Box("矮柜_背板", t, new Vector3(0f, H * 0.5f, D * 0.5f - 0.010f), new Vector3(W, H, 0.020f), body);

            // ---------------- 柜面陈设（底部严格坐在柜面 y = H + 0.036 上）----------------
            float topY = H + 0.036f;
            var cb = new GameObject("矮柜陈设").transform;
            cb.SetParent(t, false);

            // 1) 一组白瓷瓶（两高一低）
            var vaseA = new Vector3(-0.86f, 0f, 0.02f);
            Prim.Cylinder("陈设_白瓷瓶A_身", cb, vaseA + new Vector3(0f, topY + 0.115f, 0f), 0.072f, 0.230f, Mat.PotWhite, null, false, 40);
            Prim.Cylinder("陈设_白瓷瓶A_颈", cb, vaseA + new Vector3(0f, topY + 0.245f, 0f), 0.038f, 0.050f, Mat.PotWhite, null, false, 40);
            BuildSpray(cb, vaseA + new Vector3(0f, topY + 0.270f, 0f), 0.34f, 6, 0.52f);

            var vaseB = new Vector3(-0.60f, 0f, 0.06f);
            Prim.Cylinder("陈设_白瓷瓶B_身", cb, vaseB + new Vector3(0f, topY + 0.082f, 0f), 0.058f, 0.164f, Mat.PotWhite, null, false, 40);
            BuildSpray(cb, vaseB + new Vector3(0f, topY + 0.166f, 0f), 0.26f, 5, 0.60f);

            var bowl = new Vector3(-0.34f, 0f, -0.02f);
            Prim.Cylinder("陈设_浅陶碗", cb, bowl + new Vector3(0f, topY + 0.030f, 0f), 0.105f, 0.060f, Mat.PotGrey, null, false, 48);

            // 2) 书立 + 竖排书（底部坐在柜面）
            var books = new Vector3(0.30f, 0f, 0.01f);
            float bx = 0f;
            var bookCols = new[] { Mat.AccentOlive, Mat.AccentBlue, Mat.PotWhite, Mat.Paper };
            for (int i = 0; i < 7; i++)
            {
                float th = 0.026f + (i % 3) * 0.009f;
                float hh = 0.180f + (i % 4) * 0.024f;
                Prim.Box("陈设_书" + i, cb, books + new Vector3(bx + th * 0.5f, topY + hh * 0.5f, 0f),
                    new Vector3(th, hh, 0.128f), bookCols[i % bookCols.Length]);
                bx += th + 0.005f;
            }
            Prim.Box("陈设_书立", cb, books + new Vector3(bx + 0.012f, topY + 0.075f, 0f),
                new Vector3(0.018f, 0.150f, 0.140f), Mat.BlackMetal);

            // 3) 小盆栽
            var pot = new Vector3(0.86f, 0f, 0.03f);
            Prim.Cylinder("陈设_小盆_盆", cb, pot + new Vector3(0f, topY + 0.058f, 0f), 0.078f, 0.116f, Mat.PotWhite, null, false, 40);
            Prim.Sphere("陈设_小盆_土", cb, pot + new Vector3(0f, topY + 0.118f, 0f), 0.072f, Mat.Soil);
            BuildSpray(cb, pot + new Vector3(0f, topY + 0.140f, 0f), 0.28f, 7, 0.66f);

            // 4) 一叠浅色托盘
            var tray = new Vector3(0.60f, 0f, -0.10f);
            Prim.Box("陈设_托盘1", cb, tray + new Vector3(0f, topY + 0.008f, 0f), new Vector3(0.230f, 0.016f, 0.170f), Mat.AshWood);
            Prim.Box("陈设_托盘2", cb, tray + new Vector3(0f, topY + 0.026f, 0f), new Vector3(0.212f, 0.014f, 0.156f), Mat.AshWood);

            // 矮柜命中体
            var hit = Prim.Hitbox("设备_矮柜", p, new Vector3(cx, H * 0.5f, cz), new Vector3(W + 0.10f, H + 0.30f, D + 0.10f));
            var dc = hit.AddComponent<DeviceController>();
            dc.id = "cabinet";
            dc.displayName = "储物矮柜";
            dc.kind = DeviceKind.Sensor;
            dc.passive = true;
        }

        /// <summary>北墙中段墙面上挂一面大圆镜（北欧常见）。</summary>
        static void BuildMirror(Transform p)
        {
            const float z = Dim.HZ - 0.03f;
            const float cx = -0.40f;    // 与矮柜对齐
            const float cy = 1.74f;
            var frame = Mat.AshWood;

            Prim.Cylinder("圆镜_背板", p, new Vector3(cx, cy, z - 0.012f), 0.330f, 0.030f, frame, Quaternion.Euler(90f, 0f, 0f), false, 64);
            Prim.Cylinder("圆镜_镜面", p, new Vector3(cx, cy, z - 0.030f), 0.300f, 0.008f,
                Mat.Pbr("n_mirror", new Color(0.880f, 0.905f, 0.920f), 1f, 0.95f), Quaternion.Euler(90f, 0f, 0f), false, 64);
        }

        // ------------------------------------------------------------------ 玻璃白板

        /// <summary>南墙中段的落地玻璃白板（浅木框 + 笔槽）。</summary>
        static void BuildWhiteboard(Transform p)
        {
            const float z = -Dim.HZ + 0.030f;
            const float cx = 0.00f;     // 落在南墙中段实墙（避开两端角窗）
            const float cy = 1.62f;
            const float W = 2.20f, H = 1.26f, TH = 0.040f;

            Prim.Box("白板_板面", p, new Vector3(cx, cy, z), new Vector3(W, H, TH), Mat.Board);
            var fr = Mat.AshWood;
            const float fw = 0.055f;
            Prim.Box("白板_框上", p, new Vector3(cx, cy + H * 0.5f + fw * 0.5f, z), new Vector3(W + fw * 2f, fw, TH * 0.9f), fr);
            Prim.Box("白板_框下", p, new Vector3(cx, cy - H * 0.5f - fw * 0.5f, z), new Vector3(W + fw * 2f, fw, TH * 0.9f), fr);
            Prim.Box("白板_框左", p, new Vector3(cx - W * 0.5f - fw * 0.5f, cy, z), new Vector3(fw, H, TH * 0.9f), fr);
            Prim.Box("白板_框右", p, new Vector3(cx + W * 0.5f + fw * 0.5f, cy, z), new Vector3(fw, H, TH * 0.9f), fr);

            // 笔槽
            float trayY = cy - H * 0.5f - fw - 0.022f;
            Prim.Box("白板_笔槽", p, new Vector3(cx, trayY, z + 0.038f), new Vector3(W * 0.62f, 0.030f, 0.080f), fr);
            for (int i = 0; i < 3; i++)
            {
                var cols = new[] { Mat.AccentBlue, Mat.AccentOlive, Mat.Plastic };
                Prim.Cylinder("白板_笔" + i, p, new Vector3(cx - 0.16f + i * 0.055f, trayY + 0.032f, z + 0.040f),
                    0.009f, 0.120f, cols[i], Quaternion.Euler(90f, 0f, 0f), false, 16);
            }
            Prim.Box("白板_板擦", p, new Vector3(cx + 0.24f, trayY + 0.028f, z + 0.040f), new Vector3(0.110f, 0.034f, 0.056f), Mat.Plastic);

            // 板上贴两张便签
            var note = Mat.Pbr("n_note", new Color(0.985f, 0.930f, 0.610f), 0f, 0.28f);
            var note2 = Mat.Pbr("n_note2", new Color(0.700f, 0.905f, 0.840f), 0f, 0.28f);
            Prim.Box("白板_便签1", p, new Vector3(cx - 0.62f, cy + 0.26f, z + 0.024f), new Vector3(0.190f, 0.190f, 0.004f), note,
                Quaternion.Euler(0f, 0f, -5f));
            Prim.Box("白板_便签2", p, new Vector3(cx - 0.36f, cy + 0.20f, z + 0.024f), new Vector3(0.170f, 0.170f, 0.004f), note2,
                Quaternion.Euler(0f, 0f, 6f));

            var hit = Prim.Hitbox("设备_白板", p, new Vector3(cx, cy, z + 0.060f), new Vector3(W + 0.16f, H + 0.20f, 0.16f));
            var dc = hit.AddComponent<DeviceController>();
            dc.id = "whiteboard";
            dc.displayName = "玻璃白板";
            dc.kind = DeviceKind.Sensor;
            dc.passive = true;
        }

        // ------------------------------------------------------------------ 绿植

        static void BuildPlants(Transform p)
        {
            // 大盆栽：西北角（角窗与北墙中段交角内侧），避开窗帘区
            BuildBigPlant(p, new Vector3(-3.28f, 0f, 2.34f), 0.34f, 1.62f);
            // 次盆栽：东南角
            BuildBigPlant(p, new Vector3(3.20f, 0f, -2.30f), 0.28f, 1.28f);
            // 落地小盆栽：后墙窗前
            BuildBigPlant(p, new Vector3(3.05f, 0f, 2.42f), 0.22f, 0.92f);
        }

        static void BuildBigPlant(Transform p, Vector3 pos, float potR, float totalH)
        {
            var t = new GameObject("绿植").transform;
            t.SetParent(p, false);
            t.localPosition = pos;

            float potH = Mathf.Min(0.42f, totalH * 0.34f);
            Prim.Cylinder("盆_身", t, new Vector3(0f, potH * 0.5f, 0f), potR, potH, Mat.PotWhite, null, false, 40);
            Prim.Cylinder("盆_口", t, new Vector3(0f, potH - 0.012f, 0f), potR * 1.045f, 0.030f, Mat.PotGrey, null, false, 40);
            Prim.Cylinder("盆_土", t, new Vector3(0f, potH - 0.010f, 0f), potR * 0.90f, 0.024f, Mat.Soil, null, false, 40);

            // 丛生枝干 + 层叠叶片
            int stalks = 5 + Mathf.RoundToInt(totalH * 2f);
            var rnd = new System.Random(Mathf.RoundToInt(pos.x * 100f + pos.z * 37f + potR * 1000f));
            float baseY = potH - 0.010f;
            for (int i = 0; i < stalks; i++)
            {
                float ang = (float)(rnd.NextDouble() * 360.0);
                float lean = 0.06f + (float)rnd.NextDouble() * 0.20f;
                float hgt = (totalH - baseY) * (0.62f + (float)rnd.NextDouble() * 0.42f);
                float sp = (float)(rnd.NextDouble() * 2.0 - 1.0) * potR * 0.45f;
                float sp2 = (float)(rnd.NextDouble() * 2.0 - 1.0) * potR * 0.45f;
                var dir = new Vector3(Mathf.Sin(ang * Mathf.Deg2Rad), 0f, Mathf.Cos(ang * Mathf.Deg2Rad));
                var top = new Vector3(sp + dir.x * lean * hgt, baseY + hgt, sp2 + dir.z * lean * hgt);
                var bot = new Vector3(sp * 0.5f, baseY, sp2 * 0.5f);
                Prim.BoxBetween("枝" + i, t, Vector3.Min(top, bot) - new Vector3(0.008f, 0f, 0.008f),
                    Vector3.Max(top, bot) + new Vector3(0.008f, 0f, 0.008f), Mat.Pbr("n_stem", new Color(0.260f, 0.320f, 0.190f), 0f, 0.24f));

                int leaves = 3 + rnd.Next(0, 3);
                for (int k = 0; k < leaves; k++)
                {
                    float u = 0.42f + (float)rnd.NextDouble() * 0.60f;
                    var c = Vector3.Lerp(bot, top, Mathf.Clamp01(u));
                    c += new Vector3((float)(rnd.NextDouble() * 2.0 - 1.0) * 0.055f, (float)rnd.NextDouble() * 0.03f,
                                     (float)(rnd.NextDouble() * 2.0 - 1.0) * 0.055f);
                    var leafMat = ((i + k) % 3 == 0) ? Mat.LeafLight : Mat.Leaf;
                    Prim.Box("叶", t, c, new Vector3(0.175f, 0.010f, 0.095f), leafMat,
                        Quaternion.Euler((float)(rnd.NextDouble() * 40.0 - 20.0), (float)(rnd.NextDouble() * 360.0),
                                         (float)(rnd.NextDouble() * 34.0 - 17.0)));
                }
            }
        }

        /// <summary>
        /// 细枝束：用于花瓶 / 花盆上方的绿枝。
        /// 枝条自 basePos 向上外撇，沿枝身分层长叶（只把叶子堆在顶端会像一根"柱子"）。
        /// </summary>
        static void BuildSpray(Transform parent, Vector3 basePos, float len, int count, float spread)
        {
            var stem = Mat.Pbr("n_spray_stem", new Color(0.240f, 0.330f, 0.190f), 0f, 0.24f);
            const int leavesPer = 3;
            for (int i = 0; i < count; i++)
            {
                float a = i * (360f / count) + 11f;
                var dir = new Vector3(Mathf.Sin(a * Mathf.Deg2Rad), 0f, Mathf.Cos(a * Mathf.Deg2Rad));
                float h = len * (0.55f + (i % 3) * 0.14f);
                float outX = h * spread * 0.55f;
                var top = basePos + dir * outX + new Vector3(0f, h, 0f);
                Prim.BoxBetween("绿枝" + i, parent, Vector3.Min(top, basePos) - new Vector3(0.004f, 0f, 0.004f),
                    Vector3.Max(top, basePos) + new Vector3(0.004f, 0f, 0.004f), stem);

                for (int k = 0; k < leavesPer; k++)
                {
                    float u = 0.34f + k * 0.30f;                       // 沿枝身分层
                    var c = Vector3.Lerp(basePos, top, u);
                    float la = a + k * 47f;
                    var ldir = new Vector3(Mathf.Sin(la * Mathf.Deg2Rad), 0f, Mathf.Cos(la * Mathf.Deg2Rad));
                    // 叶子的抬升要跟"横向伸出多远"挂钩：
                    // 一律往上抬 len*0.05 的话，向外伸得最远的那几片会飘在植株旁边的半空里。
                    c += ldir * (len * 0.10f) + new Vector3(0f, len * 0.03f * (1f - u * 0.5f), 0f);
                    float sc = 1f - k * 0.16f;
                    Prim.Box("绿枝叶" + i + "_" + k, parent, c,
                        new Vector3(len * 0.42f * sc, 0.008f, len * 0.26f * sc),
                        ((i + k) % 2 == 0) ? Mat.Leaf : Mat.LeafLight,
                        Quaternion.Euler(0f, la, 10f + k * 6f));
                }
            }
        }

        // ------------------------------------------------------------------ 落地灯

        static void BuildFloorLamp(Transform p)
        {
            var pos = new Vector3(-3.25f, 0f, -2.32f);
            var t = new GameObject("落地灯").transform;
            t.SetParent(p, false);
            t.localPosition = pos;

            var metal = Mat.BlackMetal;
            Prim.Cylinder("落地灯_底座", t, new Vector3(0f, 0.012f, 0f), 0.155f, 0.024f, metal, null, false, 40);
            Prim.Cylinder("落地灯_杆", t, new Vector3(0f, 0.760f, 0f), 0.014f, 1.500f, metal, null, false, 20);
            // 亚麻灯罩（上窄下宽的圆台，用两段圆柱近似）
            Prim.Cylinder("落地灯_罩下", t, new Vector3(0f, 1.500f, 0f), 0.155f, 0.190f, Mat.Linen, null, false, 48);
            Prim.Cylinder("落地灯_罩上", t, new Vector3(0f, 1.688f, 0f), 0.122f, 0.180f, Mat.Linen, null, false, 48);
            Prim.Disc("落地灯_发光面", t, new Vector3(0f, 1.470f, 0f), 0.150f,
                Mat.Emissive("n_lamp_glow", new Color(1f, 0.955f, 0.885f), 1.35f), Quaternion.Euler(90f, 0f, 0f));
            Prim.Cylinder("落地灯_顶盖", t, new Vector3(0f, 1.786f, 0f), 0.070f, 0.020f, metal, null, false, 24);

            var hit = Prim.Hitbox("设备_落地灯", p, pos + new Vector3(0f, 0.90f, 0f), new Vector3(0.40f, 1.80f, 0.40f));
            var dc = hit.AddComponent<DeviceController>();
            dc.id = "mainlight";   // 与"暖木会议室"对齐 id，面板代码可直接复用
            dc.displayName = "落地灯";
            dc.kind = DeviceKind.MainLight;
            dc.initialOn = true;
            dc.initialValue = 0.72f;
        }

        // ------------------------------------------------------------------ 休闲角

        /// <summary>东南角：一张小圆几 + 两把休闲椅（北欧常见窗边休闲角）。</summary>
        static void BuildLoungeCorner(Transform p)
        {
            var layout = new[] {
                (pos: new Vector3(2.42f, 0f, -1.72f), yaw: 152f),
                (pos: new Vector3(3.38f, 0f, -0.42f), yaw: 214f) };

            var t = new GameObject("休闲角").transform;
            t.SetParent(p, false);

            // 小圆几
            var tb = new GameObject("小圆几").transform;
            tb.SetParent(t, false);
            tb.localPosition = new Vector3(3.06f, 0f, -1.12f);
            Prim.RoundSlab("小圆几_台面", tb, new Vector3(0f, 0.475f, 0f), 0.300f, 0.038f, Mat.WoodTop);
            Prim.Cylinder("小圆几_柱", tb, new Vector3(0f, 0.235f, 0f), 0.036f, 0.470f, Mat.BlackMetal, null, false, 24);
            Prim.Cylinder("小圆几_底", tb, new Vector3(0f, 0.014f, 0f), 0.180f, 0.028f, Mat.BlackMetal, null, false, 40);
            Prim.Cylinder("小圆几_杯", tb, new Vector3(0.06f, 0.528f, 0.02f), 0.036f, 0.070f, Mat.PotWhite, null, false, 28);
            Prim.Box("小圆几_书", tb, new Vector3(-0.09f, 0.502f, -0.05f), new Vector3(0.150f, 0.022f, 0.115f), Mat.AccentOlive);

            for (int i = 0; i < layout.Length; i++)
                BuildLoungeChair(t, i, layout[i].pos, Quaternion.Euler(0f, layout[i].yaw, 0f));
        }

        static void BuildLoungeChair(Transform p, int idx, Vector3 pos, Quaternion q)
        {
            var t = new GameObject("休闲椅" + idx).transform;
            t.SetParent(p, false);
            t.localPosition = pos;
            t.localRotation = q;

            var frame = Mat.AshWood;
            var fab = (idx == 0) ? Mat.AccentBlue : Mat.AccentOlive;
            float seatY = 0.400f;

            Prim.RoundedBox("休闲椅_坐垫", t, new Vector3(0f, seatY - 0.045f, 0f), new Vector3(0.560f, 0.090f, 0.520f), fab);
            Prim.Box("休闲椅_座框", t, new Vector3(0f, seatY - 0.108f, 0f), new Vector3(0.580f, 0.042f, 0.540f), frame);

            float backZ = 0.245f;
            // 靠背整体后仰 12°
            var bq = Quaternion.Euler(12f, 0f, 0f);
            var back = new GameObject("靠背").transform;
            back.SetParent(t, false);
            back.localPosition = new Vector3(0f, seatY + 0.250f, backZ);
            back.localRotation = bq;
            Prim.RoundedBox("休闲椅_靠背", back, Vector3.zero, new Vector3(0.540f, 0.480f, 0.075f), fab);

            // 扶手
            foreach (var sx in new[] { -1f, 1f })
            {
                Prim.Box("休闲椅_扶手" + (sx < 0 ? "L" : "R"), t,
                    new Vector3(sx * 0.282f, seatY + 0.135f, 0.015f), new Vector3(0.052f, 0.048f, 0.470f), frame);
                Prim.Box("休闲椅_扶手柱" + (sx < 0 ? "L" : "R"), t,
                    new Vector3(sx * 0.282f, seatY - 0.028f, -0.190f), new Vector3(0.048f, 0.300f, 0.048f), frame);
            }

            // 四条木腿
            foreach (var sx in new[] { -1f, 1f })
                foreach (var sz in new[] { -1f, 1f })
                {
                    var top = new Vector3(sx * 0.230f, seatY - 0.128f, sz * 0.215f);
                    var bot = new Vector3(sx * 0.262f, 0.014f, sz * 0.248f);
                    Prim.BoxBetween("休闲椅_腿", t, Vector3.Min(top, bot) - new Vector3(0.021f, 0f, 0.021f),
                        Vector3.Max(top, bot) + new Vector3(0.021f, 0f, 0.021f), frame);
                }

            var hit = Prim.Hitbox("设备_休闲椅" + idx, p, pos + new Vector3(0f, 0.46f, 0f), new Vector3(0.72f, 0.96f, 0.72f));
            hit.transform.localRotation = q;
            var dc = hit.AddComponent<DeviceController>();
            dc.id = "lounge" + idx;
            dc.displayName = "休闲椅 " + (idx + 1);
            dc.kind = DeviceKind.Sensor;
            dc.passive = true;
        }
    }
}
