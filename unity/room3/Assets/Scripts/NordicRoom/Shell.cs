using UnityEngine;

namespace NordicRoom
{
    /// <summary>
    /// 房间壳体（北欧明亮风）。
    /// 门窗组合与前两个会议室都不同——**角窗方案**：
    ///   门在 -X 短墙；两堵长墙各在南北两端开角窗（中间大片墙垛）；
    ///   +X 短墙（门对面）开一扇中央大窗。共 5 扇窗，窗台低（0.45m）、窗高大（2.17m）。
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
            BuildDoorWall(p, hx, hz, H, T);
            BuildBackWall(p, hx, hz, H, T);
            BuildNorthWall(p, hx, hz, H, T);
            BuildSouthWall(p, hx, hz, H, T);
            BuildCeiling(p, hx, hz, H);
            BuildSkirting(p, hx, hz);
            BuildBackWindow(p, hx, H, T);
            BuildCornerWindows(p, hx, H, T);
            BuildDoor(p, hx, T);
            BuildAcUnit(p, hx, hz);

            return root;
        }

        // ------------------------------------------------------------------ 地面

        static void BuildFloor(Transform p, float hx, float hz)
        {
            Prim.Box("地板_结构层", p, new Vector3(0f, -Dim.WallT * 0.5f, 0f),
                new Vector3(hx * 2f + 0.24f, Dim.WallT, hz * 2f + 0.24f), Mat.Plastic);
            // 浅白橡木地板（整铺，北欧风通铺地板）
            Prim.Box("地板_白橡木", p, new Vector3(0f, -0.012f, 0f),
                new Vector3(hx * 2f, 0.024f, hz * 2f), Mat.LightOak);

            // 入口处铺一块浅色水磨石地垫区（同高度薄层，做出材质变化）
            Prim.Box("入口_水磨石", p, new Vector3(-hx + 0.75f, 0.001f, 0f),
                new Vector3(1.50f, 0.026f, 2.60f), Mat.Terrazzo);
        }

        // ------------------------------------------------------------------ 墙体

        /// <summary>门墙 x=-4：门洞两侧 + 门头 + 木饰面。</summary>
        static void BuildDoorWall(Transform p, float hx, float hz, float H, float T)
        {
            float x0 = -hx - T, x1 = -hx;
            Prim.BoxBetween("门墙_南段", p,
                new Vector3(x0, 0f, -hz), new Vector3(x1, H, Dim.DoorZMin), Mat.WhiteWall);
            Prim.BoxBetween("门墙_北段", p,
                new Vector3(x0, 0f, Dim.DoorZMax), new Vector3(x1, H, hz), Mat.WhiteWall);
            Prim.BoxBetween("门墙_门头", p,
                new Vector3(x0, Dim.DoorH, Dim.DoorZMin), new Vector3(x1, H, Dim.DoorZMax), Mat.WhiteWall);
            // 门侧一条浅木饰面
            Prim.Box("门墙_木饰面", p,
                new Vector3(-hx + 0.018f, 1.60f, -(Dim.DoorZMin + hz) * 0.5f - 0.30f),
                new Vector3(0.036f, 3.20f, hz + Dim.DoorZMin - 0.60f), Mat.AshWood);
        }

        /// <summary>后墙 x=+4（门对面）：中央大窗 + 主位木饰面。</summary>
        static void BuildBackWall(Transform p, float hx, float hz, float H, float T)
        {
            float x0 = hx, x1 = hx + T;
            float z0 = Dim.BackWinZMin, z1 = Dim.BackWinZMax;
            float sill = Dim.BackWinSill, top = Dim.BackWinTop;

            Prim.BoxBetween("后墙_南段", p, new Vector3(x0, 0f, -hz), new Vector3(x1, H, z0), Mat.WhiteWall);
            Prim.BoxBetween("后墙_北段", p, new Vector3(x0, 0f, z1), new Vector3(x1, H, hz), Mat.WhiteWall);
            Prim.BoxBetween("后墙_窗下墙", p, new Vector3(x0, 0f, z0), new Vector3(x1, sill, z1), Mat.WhiteWall);
            Prim.BoxBetween("后墙_窗上墙", p, new Vector3(x0, top, z0), new Vector3(x1, H, z1), Mat.WhiteWall);

            // 主位背景：浅色木格栅墙板（夹在窗两侧）
            foreach (var s in new[] { -1f, 1f })
            {
                float cz = s * (z1 + (hz - z1) * 0.5f);
                float w = hz - z1 - 0.10f;
                if (w > 0.2f)
                    Prim.Box("后墙_木格栅" + (s < 0 ? "南" : "北"), p,
                        new Vector3(hx - 0.028f, 1.60f, cz), new Vector3(0.056f, 3.20f, w), Mat.SlatPanel);
            }
        }

        /// <summary>北墙 z=+3：两端角窗，中间实墙。</summary>
        static void BuildNorthWall(Transform p, float hx, float hz, float H, float T)
        {
            float z0 = hz, z1 = hz + T;
            float sill = Dim.WinSill, top = Dim.WinTop;

            // 中段实墙
            Prim.BoxBetween("北墙_中段", p,
                new Vector3(Dim.NorthWinXMax, 0f, z0), new Vector3(Dim.NorthWinXMin2, H, z1), Mat.WhiteWall);

            // 两扇角窗的窗下墙 / 窗上墙
            foreach (var w in new[] {
                (min: Dim.NorthWinXMin, max: Dim.NorthWinXMax, tag: "西"),
                (min: Dim.NorthWinXMin2, max: Dim.NorthWinXMax2, tag: "东") })
            {
                Prim.BoxBetween("北墙_窗下墙" + w.tag, p,
                    new Vector3(w.min, 0f, z0), new Vector3(w.max, sill, z1), Mat.WhiteWall);
                Prim.BoxBetween("北墙_窗上墙" + w.tag, p,
                    new Vector3(w.min, top, z0), new Vector3(w.max, H, z1), Mat.WhiteWall);
            }

            // 两端小段实墙（贴角）
            Prim.BoxBetween("北墙_西端", p,
                new Vector3(-hx - T, 0f, z0), new Vector3(Dim.NorthWinXMin, H, z1), Mat.WhiteWall);
            Prim.BoxBetween("北墙_东端", p,
                new Vector3(Dim.NorthWinXMax2, 0f, z0), new Vector3(hx + T, H, z1), Mat.WhiteWall);
        }

        /// <summary>南墙 z=-3：对称的两端角窗。</summary>
        static void BuildSouthWall(Transform p, float hx, float hz, float H, float T)
        {
            float z0 = -hz - T, z1 = -hz;
            float sill = Dim.WinSill, top = Dim.WinTop;

            Prim.BoxBetween("南墙_中段", p,
                new Vector3(Dim.NorthWinXMax, 0f, z0), new Vector3(Dim.NorthWinXMin2, H, z1), Mat.WhiteWall);

            foreach (var w in new[] {
                (min: Dim.NorthWinXMin, max: Dim.NorthWinXMax, tag: "西"),
                (min: Dim.NorthWinXMin2, max: Dim.NorthWinXMax2, tag: "东") })
            {
                Prim.BoxBetween("南墙_窗下墙" + w.tag, p,
                    new Vector3(w.min, 0f, z0), new Vector3(w.max, sill, z1), Mat.WhiteWall);
                Prim.BoxBetween("南墙_窗上墙" + w.tag, p,
                    new Vector3(w.min, top, z0), new Vector3(w.max, H, z1), Mat.WhiteWall);
            }

            Prim.BoxBetween("南墙_西端", p,
                new Vector3(-hx - T, 0f, z0), new Vector3(Dim.NorthWinXMin, H, z1), Mat.WhiteWall);
            Prim.BoxBetween("南墙_东端", p,
                new Vector3(Dim.NorthWinXMax2, 0f, z0), new Vector3(hx + T, H, z1), Mat.WhiteWall);
        }

        // ------------------------------------------------------------------ 吊顶

        static void BuildCeiling(Transform p, float hx, float hz, float H)
        {
            Prim.Box("吊顶", p, new Vector3(0f, H + Dim.CeilT * 0.5f, 0f),
                new Vector3(hx * 2f, Dim.CeilT, hz * 2f), Mat.Ceiling);

            // 北欧风：不做复杂跌级，只沿墙一圈浅色阴角线
            const float cw = 0.08f, cd = 0.06f;
            var trim = Mat.CeilingTrim;
            Prim.Box("阴角线_X负", p, new Vector3(-hx + cd * 0.5f, H - cw * 0.5f, 0f), new Vector3(cd, cw, hz * 2f), trim);
            Prim.Box("阴角线_X正", p, new Vector3(hx - cd * 0.5f, H - cw * 0.5f, 0f), new Vector3(cd, cw, hz * 2f), trim);
            Prim.Box("阴角线_Z负", p, new Vector3(0f, H - cw * 0.5f, -hz + cd * 0.5f), new Vector3(hx * 2f, cw, cd), trim);
            Prim.Box("阴角线_Z正", p, new Vector3(0f, H - cw * 0.5f, hz - cd * 0.5f), new Vector3(hx * 2f, cw, cd), trim);

            BuildPendantRails(p, hx, hz, H);
            BuildDownlights(p, hx, hz, H);
            BuildCeilingSensors(p, H);
        }

        /// <summary>三条细吊轨（北欧风常见：把吊灯挂在细金属轨道上）。</summary>
        static void BuildPendantRails(Transform p, float hx, float hz, float H)
        {
            for (int i = 0; i < 3; i++)
            {
                float z = -1.60f + i * 1.60f;
                Prim.Box("吊轨" + i, p, new Vector3(0f, H - 0.035f, z),
                    new Vector3(5.40f, 0.035f, 0.05f), Mat.Steel);
                Prim.Box("吊轨座" + i + "_左", p, new Vector3(-2.70f, H - 0.012f, z), new Vector3(0.12f, 0.02f, 0.12f), Mat.WhiteWall);
                Prim.Box("吊轨座" + i + "_右", p, new Vector3(2.70f, H - 0.012f, z), new Vector3(0.12f, 0.02f, 0.12f), Mat.WhiteWall);
            }
        }

        static void BuildDownlights(Transform p, float hx, float hz, float H)
        {
            var ring = Mat.Chrome;
            var glow = Mat.Emissive("n_downlight", new Color(1f, 0.975f, 0.940f), 1.25f);
            float[] zs = { -2.45f, 2.45f };
            foreach (var z in zs)
                for (int i = 0; i < 4; i++)
                {
                    float x = -2.90f + i * 1.9333f;
                    Prim.Cylinder("筒灯圈_" + z + "_" + i, p, new Vector3(x, H - 0.012f, z), 0.082f, 0.02f, ring);
                    Prim.Disc("筒灯发光_" + z + "_" + i, p, new Vector3(x, H - 0.021f, z), 0.070f, glow,
                        Quaternion.Euler(90f, 0f, 0f));
                }
        }

        static void BuildCeilingSensors(Transform p, float H)
        {
            var smoke = new Vector3(-2.60f, H - 0.030f, -2.20f);
            Prim.Cylinder("烟雾探测器_壳体", p, smoke, 0.074f, 0.06f, Mat.PlasticWhite);
            Prim.Cylinder("烟雾探测器_格栅", p, new Vector3(smoke.x, smoke.y - 0.032f, smoke.z), 0.061f, 0.012f, Mat.Plastic);

            var lux = new Vector3(2.90f, H - 0.028f, 2.30f);
            Prim.Box("光照传感器_壳体", p, lux, new Vector3(0.10f, 0.05f, 0.10f), Mat.PlasticWhite);
            Prim.Disc("光照传感器_感光面", p, new Vector3(lux.x, lux.y - 0.028f, lux.z), 0.032f,
                Mat.Emissive("n_lux_led", new Color(0.55f, 0.92f, 1f), 1.2f), Quaternion.Euler(90f, 0f, 0f));
        }

        // ------------------------------------------------------------------ 踢脚

        static void BuildSkirting(Transform p, float hx, float hz)
        {
            var m = Mat.AshDark;
            const float h = 0.075f, t = 0.018f;
            Prim.Box("踢脚_X负", p, new Vector3(-hx + t * 0.5f, h * 0.5f, 0f), new Vector3(t, h, hz * 2f), m);
            Prim.Box("踢脚_X正", p, new Vector3(hx - t * 0.5f, h * 0.5f, 0f), new Vector3(t, h, hz * 2f), m);
            Prim.Box("踢脚_Z负", p, new Vector3(0f, h * 0.5f, -hz + t * 0.5f), new Vector3(hx * 2f, h, t), m);
            Prim.Box("踢脚_Z正", p, new Vector3(0f, h * 0.5f, hz - t * 0.5f), new Vector3(hx * 2f, h, t), m);
        }

        // ------------------------------------------------------------------ 窗（5 扇）

        /// <summary>后墙中央大窗（+X 墙，窗沿 Z 排布）。</summary>
        static void BuildBackWindow(Transform p, float hx, float H, float T)
        {
            var frame = Mat.AshWood;
            float x = hx + T * 0.5f;
            const float fw = 0.055f, fd = 0.15f;
            float sill = Dim.BackWinSill, top = Dim.BackWinTop;
            float z0 = Dim.BackWinZMin, z1 = Dim.BackWinZMax;
            float cz = (z0 + z1) * 0.5f, w = z1 - z0;

            Prim.Box("后窗_框上", p, new Vector3(x, top, cz), new Vector3(fd, fw, w + fw), frame);
            Prim.Box("后窗_框下", p, new Vector3(x, sill, cz), new Vector3(fd, fw, w + fw), frame);
            Prim.Box("后窗_框左", p, new Vector3(x, (sill + top) * 0.5f, z0), new Vector3(fd, top - sill, fw), frame);
            Prim.Box("后窗_框右", p, new Vector3(x, (sill + top) * 0.5f, z1), new Vector3(fd, top - sill, fw), frame);
            // 两根竖挺（分三格）
            for (int i = 1; i <= 2; i++)
            {
                float z = Mathf.Lerp(z0, z1, i / 3f);
                Prim.Box("后窗_竖挺" + i, p, new Vector3(x, (sill + top) * 0.5f, z),
                    new Vector3(fd * 0.8f, top - sill, 0.042f), frame);
            }
            float gz0 = z0 + fw * 0.5f, gz1 = z1 - fw * 0.5f;
            float gy0 = sill + fw * 0.5f, gy1 = top - fw * 0.5f;
            Prim.Box("后窗_玻璃", p, new Vector3(x, (gy0 + gy1) * 0.5f, (gz0 + gz1) * 0.5f),
                new Vector3(0.012f, gy1 - gy0, gz1 - gz0), Mat.GlassWindow);
            Prim.Box("后窗_窗台板", p, new Vector3(hx - 0.085f, sill - 0.014f, cz),
                new Vector3(0.28f, 0.028f, w + 0.14f), Mat.MatteStone);
        }

        /// <summary>两长墙上的 4 扇角窗（窗沿 X 排布）。</summary>
        static void BuildCornerWindows(Transform p, float hx, float H, float T)
        {
            var frame = Mat.AshWood;
            const float fw = 0.055f, fd = 0.15f;
            float sill = Dim.WinSill, top = Dim.WinTop;

            var walls = new[] {
                (z: Dim.HZ + T * 0.5f, tag: "北"),
                (z: -Dim.HZ - T * 0.5f, tag: "南") };

            foreach (var wl in walls)
            {
                var wins = new[] {
                    (min: Dim.NorthWinXMin, max: Dim.NorthWinXMax, tag: "西"),
                    (min: Dim.NorthWinXMin2, max: Dim.NorthWinXMax2, tag: "东") };
                foreach (var w in wins)
                {
                    string pre = wl.tag + w.tag + "窗";
                    float cx = (w.min + w.max) * 0.5f, ww = w.max - w.min;
                    Prim.Box(pre + "_框上", p, new Vector3(cx, top, wl.z), new Vector3(ww + fw, fw, fd), frame);
                    Prim.Box(pre + "_框下", p, new Vector3(cx, sill, wl.z), new Vector3(ww + fw, fw, fd), frame);
                    Prim.Box(pre + "_框左", p, new Vector3(w.min, (sill + top) * 0.5f, wl.z), new Vector3(fw, top - sill, fd), frame);
                    Prim.Box(pre + "_框右", p, new Vector3(w.max, (sill + top) * 0.5f, wl.z), new Vector3(fw, top - sill, fd), frame);
                    // 一根中挺
                    Prim.Box(pre + "_中挺", p, new Vector3(cx, (sill + top) * 0.5f, wl.z),
                        new Vector3(0.042f, top - sill, fd * 0.8f), frame);

                    float gx0 = w.min + fw * 0.5f, gx1 = w.max - fw * 0.5f;
                    float gy0 = sill + fw * 0.5f, gy1 = top - fw * 0.5f;
                    Prim.Box(pre + "_玻璃", p, new Vector3((gx0 + gx1) * 0.5f, (gy0 + gy1) * 0.5f, wl.z),
                        new Vector3(gx1 - gx0, gy1 - gy0, 0.012f), Mat.GlassWindow);
                    Prim.Box(pre + "_窗台板", p, new Vector3(cx, sill - 0.014f, wl.z + (wl.tag == "北" ? -0.085f : 0.085f)),
                        new Vector3(ww + 0.14f, 0.028f, 0.28f), Mat.MatteStone);
                }
            }
        }

        // ------------------------------------------------------------------ 电动门

        /// <summary>
        /// 浅木框玻璃门（-X 短墙）。
        /// 门扇沿"本地 +Z"从铰链铺到门宽，绕本地 Y 轴 +92° 时自由端转向 +X（房间内侧）。
        /// 门扇及子物体在编辑器脚本里不会被标记 Static，否则旋转不会反映到渲染。
        /// </summary>
        static void BuildDoor(Transform p, float hx, float T)
        {
            var frame = Mat.AshWood;
            var handle = Mat.Steel;

            const float frameW = 0.06f;
            float leafT = T + 0.04f;
            float h = Dim.DoorH;
            float z0 = Dim.DoorZMin, z1 = Dim.DoorZMax;
            float leafW = z1 - z0;
            float cx = -hx - T * 0.5f;

            Prim.Box("门套_上", p, new Vector3(cx, h + 0.035f, (z0 + z1) * 0.5f),
                new Vector3(T + 0.06f, 0.07f, leafW + 0.14f), frame);
            Prim.Box("门套_铰链侧", p, new Vector3(cx, (h + 0.07f) * 0.5f, z0 - 0.035f),
                new Vector3(T + 0.06f, h + 0.07f, 0.07f), frame);
            Prim.Box("门套_开合侧", p, new Vector3(cx, (h + 0.07f) * 0.5f, z1 + 0.035f),
                new Vector3(T + 0.06f, h + 0.07f, 0.07f), frame);

            var leaf = new GameObject("门扇").transform;
            leaf.SetParent(p, false);
            leaf.localPosition = new Vector3(cx, 0f, z0);
            leaf.localRotation = Quaternion.Euler(0f, Dim.DoorClosedYaw, 0f);
            float zc = leafW * 0.5f;

            Prim.Box("门扇_玻璃", leaf, new Vector3(0f, h * 0.5f, zc),
                new Vector3(0.028f, h - 0.18f, leafW - 0.08f), Mat.GlassDoor);
            Prim.Box("门扇_横档_下", leaf, new Vector3(0f, 0.11f, zc), new Vector3(leafT, 0.22f, leafW), frame);
            Prim.Box("门扇_横档_上", leaf, new Vector3(0f, h - 0.065f, zc), new Vector3(leafT, 0.13f, leafW), frame);
            Prim.Box("门扇_竖挺_铰链侧", leaf, new Vector3(0f, h * 0.5f, frameW * 0.5f), new Vector3(leafT, h, frameW), frame);
            Prim.Box("门扇_竖挺_开合侧", leaf, new Vector3(0f, h * 0.5f, leafW - frameW * 0.5f), new Vector3(leafT, h, frameW), frame);

            // 细长不锈钢拉手
            Prim.Cylinder("门扇_拉手_内", leaf, new Vector3(-leafT * 0.62f, 1.06f, leafW - 0.15f), 0.016f, 1.10f, handle);
            Prim.Cylinder("门扇_拉手_外", leaf, new Vector3(leafT * 0.62f, 1.06f, leafW - 0.15f), 0.016f, 1.10f, handle);
            Prim.Box("门扇_拉手座_内", leaf, new Vector3(-leafT * 0.36f, 1.06f, leafW - 0.15f), new Vector3(0.02f, 0.09f, 0.05f), handle);
            Prim.Box("门扇_拉手座_外", leaf, new Vector3(leafT * 0.36f, 1.06f, leafW - 0.15f), new Vector3(0.02f, 0.09f, 0.05f), handle);

            for (int i = 0; i < 3; i++)
            {
                float y = 0.40f + i * 0.74f;
                Prim.Cylinder("门扇_合页" + i, leaf, new Vector3(0f, y, 0.014f), 0.022f, 0.09f, handle,
                    Quaternion.Euler(90f, 0f, 0f));
            }

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
        /// 白色落地空调：移到后墙（+X）中段、落地放置，正面正对房间中心。
        ///
        /// 两处修正：
        ///   · 原先放在 +X/-Z 角，和角落那盆绿植的包围盒重叠（穿模）——改到后墙中段；
        ///   · 朝向公式符号写反了（原来算出来的正面是**背对**房间中心的），
        ///     正解是 yaw = atan2(-dirX, -dirZ)；dir 为"空调指向房间中心"的方向。
        /// </summary>
        static void BuildAcUnit(Transform p, float hx, float hz)
        {
            // 机身位置：贴后墙（+X）中段，宽度方向不挡两侧角窗
            var pos = new Vector3(hx - 0.42f, 0f, 0.10f);

            // "空调指向房间中心"的方向，再求出让本地 -Z 对准它的 yaw
            var dir = (Vector3.zero - pos).normalized;
            float yaw = Mathf.Atan2(-dir.x, -dir.z) * Mathf.Rad2Deg;

            var root = new GameObject("立式空调").transform;
            root.SetParent(p, false);
            root.localPosition = pos;
            root.localRotation = Quaternion.Euler(0f, yaw, 0f);
            var t = root;

            var body = Mat.Pbr("n_ac_body", new Color(0.955f, 0.955f, 0.950f), 0f, 0.38f);
            var edge = Mat.Pbr("n_ac_edge", new Color(0.855f, 0.850f, 0.840f), 0.05f, 0.28f);
            var display = Mat.Pbr("n_ac_display", new Color(0.115f, 0.118f, 0.122f), 0.12f, 0.52f);
            var louver = Mat.Pbr("n_ac_louver", new Color(0.870f, 0.868f, 0.860f), 0f, 0.34f);
            var grille = Mat.Pbr("n_ac_grille", new Color(0.320f, 0.325f, 0.330f), 0.05f, 0.30f);

            const float W = 0.42f, D = 0.34f, BodyH = 1.82f;

            Prim.Box("空调_底座", t, new Vector3(0f, 0.022f, 0f), new Vector3(W + 0.05f, 0.044f, D + 0.05f), display);
            Prim.RoundedBox("空调_柜体", t, new Vector3(0f, BodyH * 0.5f + 0.04f, 0f), new Vector3(W, BodyH, D), body);

            // 顶部显示面板
            Prim.Box("空调_显示面板", t, new Vector3(0f, BodyH - 0.13f, -D * 0.5f - 0.004f),
                new Vector3(W - 0.07f, 0.20f, 0.011f), display);
            Prim.Box("空调面板数显", t, new Vector3(-0.036f, BodyH - 0.13f, -D * 0.5f - 0.012f),
                new Vector3(0.16f, 0.084f, 0.005f),
                Mat.Emissive("n_ac_digit", new Color(0.55f, 0.92f, 1f), 2.4f));
            Prim.Sphere("空调面板指示灯", t, new Vector3(0.138f, BodyH - 0.13f, -D * 0.5f - 0.013f), 0.013f,
                Mat.Emissive("n_ac_led", new Color(0.55f, 0.92f, 1f), 2.6f));

            // 出风口
            Prim.Box("空调_出风口框", t, new Vector3(0f, BodyH - 0.33f, -D * 0.5f - 0.006f),
                new Vector3(W - 0.08f, 0.17f, 0.013f), grille);
            for (int i = 0; i < 5; i++)
            {
                float y = BodyH - 0.40f + i * 0.037f;
                Prim.Box("空调_出风百叶" + i, t, new Vector3(0f, y, -D * 0.5f - 0.014f),
                    new Vector3(W - 0.11f, 0.015f, 0.008f), louver, Quaternion.Euler(-20f, 0f, 0f));
            }

            // 中部进风格栅
            Prim.Box("空调_进风格栅", t, new Vector3(0f, 0.90f, -D * 0.5f - 0.005f),
                new Vector3(W - 0.10f, 0.60f, 0.010f), grille);
            for (int i = 0; i < 9; i++)
            {
                float y = 0.64f + i * 0.064f;
                Prim.Box("空调_进气百叶" + i, t, new Vector3(0f, y, -D * 0.5f - 0.012f),
                    new Vector3(W - 0.13f, 0.018f, 0.006f), louver);
            }

            Prim.Box("空调_下出风", t, new Vector3(0f, 0.15f, -D * 0.5f - 0.005f),
                new Vector3(W - 0.12f, 0.11f, 0.008f), grille);
            Prim.Box("空调_顶部饰条", t, new Vector3(0f, BodyH + 0.028f, 0f),
                new Vector3(W - 0.02f, 0.02f, D - 0.02f), edge);

            var hit = Prim.Hitbox("设备_空调", p, new Vector3(pos.x, BodyH * 0.5f + 0.04f, pos.z),
                new Vector3(0.66f, BodyH + 0.10f, 0.58f));
            var dc = hit.AddComponent<DeviceController>();
            dc.id = "ac";
            dc.displayName = "空调 / HVAC";
            dc.kind = DeviceKind.AirConditioner;
            dc.initialOn = true;
            dc.initialValue = 24f;
        }
    }
}
