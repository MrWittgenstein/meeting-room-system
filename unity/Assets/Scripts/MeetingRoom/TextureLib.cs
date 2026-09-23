using UnityEngine;

namespace SmartRoom
{
    /// <summary>
    /// 程序化贴图工厂：所有纹理都在运行时/构建时用代码生成，不依赖任何外部图片资源。
    /// 生成结果按名称缓存，避免重复计算与内存膨胀。
    /// </summary>
    public static class TextureLib
    {
        static readonly System.Collections.Generic.Dictionary<string, Texture2D> Cache =
            new System.Collections.Generic.Dictionary<string, Texture2D>();

        // ---------------------------------------------------------------- 基础设施

        static Texture2D Make(string key, int size, System.Func<float, float, Color> fn, bool linear = false)
        {
            if (Cache.TryGetValue(key, out var cached) && cached != null) return cached;

            var tex = new Texture2D(size, size, TextureFormat.RGBA32, true, linear);
            tex.name = "T_" + key;
            tex.wrapMode = TextureWrapMode.Repeat;
            tex.filterMode = FilterMode.Trilinear;
            tex.anisoLevel = 4;

            var px = new Color[size * size];
            for (int y = 0; y < size; y++)
            {
                float v = (y + 0.5f) / size;
                for (int x = 0; x < size; x++)
                {
                    float u = (x + 0.5f) / size;
                    px[y * size + x] = fn(u, v);
                }
            }
            tex.SetPixels(px);
            tex.Apply(true, false);
            Cache[key] = tex;
            return tex;
        }

        /// <summary>平滑的伪随机噪声（Perlin 叠层），返回 0..1。</summary>
        static float FBM(float u, float v, int octaves, float scale, float seed)
        {
            float sum = 0f, amp = 1f, norm = 0f;
            for (int i = 0; i < octaves; i++)
            {
                sum += Mathf.PerlinNoise(seed + u * scale, seed * 1.7f + v * scale) * amp;
                norm += amp;
                amp *= 0.5f;
                scale *= 2f;
            }
            return sum / norm;
        }

        static float Hash(int x, int y, float seed)
        {
            float n = Mathf.Sin(x * 127.1f + y * 311.7f + seed * 74.7f) * 43758.5453f;
            return n - Mathf.Floor(n);
        }

        /// <summary>生成配套的法线贴图（由高度场做 Sobel 差分）。</summary>
        static Texture2D NormalFromHeight(string key, int size, System.Func<float, float, float> height, float strength)
        {
            string nk = key + "_n";
            if (Cache.TryGetValue(nk, out var cached) && cached != null) return cached;

            var tex = new Texture2D(size, size, TextureFormat.RGBA32, true, true);
            tex.name = "N_" + key;
            tex.wrapMode = TextureWrapMode.Repeat;
            tex.filterMode = FilterMode.Bilinear;

            var px = new Color[size * size];
            float step = 1f / size;
            for (int y = 0; y < size; y++)
            {
                for (int x = 0; x < size; x++)
                {
                    float u = (x + 0.5f) / size, v = (y + 0.5f) / size;
                    float hL = height(u - step, v), hR = height(u + step, v);
                    float hD = height(u, v - step), hU = height(u, v + step);
                    var n = new Vector3((hL - hR) * strength, (hD - hU) * strength, 1f).normalized;
                    px[y * size + x] = new Color(n.x * 0.5f + 0.5f, n.y * 0.5f + 0.5f, n.z * 0.5f + 0.5f, 1f);
                }
            }
            tex.SetPixels(px);
            tex.Apply(true, false);
            Cache[nk] = tex;
            return tex;
        }

        // ---------------------------------------------------------------- 具体材质纹理

        /// <summary>深灰地毯：细密纤维噪声 + 轻微色斑。</summary>
        public static Texture2D Carpet() => Make("carpet", 512, (u, v) =>
        {
            float fiber = FBM(u, v, 4, 96f, 3.1f);
            float blotch = FBM(u, v, 3, 6f, 11.3f);
            float g = 0.150f + fiber * 0.075f + (blotch - 0.5f) * 0.035f;
            return new Color(g * 0.98f, g * 0.99f, g * 1.04f);
        });

        public static Texture2D CarpetNormal() => NormalFromHeight("carpet", 256,
            (u, v) => FBM(u, v, 4, 96f, 3.1f), 1.6f);

        /// <summary>乳胶漆墙面：极细颗粒，接近纯色但避免死板。</summary>
        public static Texture2D Plaster() => Make("plaster", 256, (u, v) =>
        {
            float g = FBM(u, v, 3, 40f, 21.7f);
            float broad = FBM(u, v, 2, 3f, 5.5f);
            float c = 0.905f + (g - 0.5f) * 0.020f + (broad - 0.5f) * 0.020f;
            return new Color(c, c * 0.995f, c * 0.965f);
        });

        public static Texture2D PlasterNormal() => NormalFromHeight("plaster", 128,
            (u, v) => FBM(u, v, 3, 40f, 21.7f), 0.5f);

        /// <summary>胡桃木饰面：年轮条纹 + 细纹扰动。</summary>
        public static Texture2D Walnut() => Make("walnut", 512, (u, v) =>
        {
            float warp = FBM(u, v, 3, 5f, 31.1f) - 0.5f;
            float rings = Mathf.Sin((v * 26f + warp * 5.5f) * Mathf.PI) * 0.5f + 0.5f;
            float grain = FBM(u, v * 0.4f, 4, 120f, 17.9f);
            float t = Mathf.Clamp01(rings * 0.65f + grain * 0.35f);

            var dark = new Color(0.135f, 0.086f, 0.055f);
            var light = new Color(0.315f, 0.198f, 0.118f);
            var c = Color.Lerp(dark, light, t);
            // 偶尔出现的深色木节纹
            c *= 1f - Mathf.Clamp01((grain - 0.86f) * 3.2f) * 0.45f;
            return c;
        });

        public static Texture2D WalnutNormal() => NormalFromHeight("walnut", 256, (u, v) =>
        {
            float warp = FBM(u, v, 3, 5f, 31.1f) - 0.5f;
            return Mathf.Sin((v * 26f + warp * 5.5f) * Mathf.PI) * 0.5f + 0.5f;
        }, 1.1f);

        /// <summary>浅橡木地板：细长条 + 拼缝。</summary>
        public static Texture2D OakPlank() => Make("oak", 512, (u, v) =>
        {
            const int planks = 5;
            float py = v * planks;
            int idx = Mathf.FloorToInt(py);
            float local = py - idx;
            float plankShade = 0.82f + Hash(idx, 3, 7.7f) * 0.30f;

            float grain = FBM(u * 0.35f, v * 3.2f, 4, 26f, 43.2f);
            float seam = Mathf.SmoothStep(0f, 0.045f, local) * Mathf.SmoothStep(0f, 0.045f, 1f - local);

            var c = Color.Lerp(new Color(0.415f, 0.300f, 0.185f), new Color(0.600f, 0.455f, 0.300f), grain);
            c *= plankShade;
            c = Color.Lerp(c * 0.35f, c, seam);
            return c;
        });

        public static Texture2D OakNormal() => NormalFromHeight("oak", 256, (u, v) =>
        {
            const int planks = 5;
            float py = v * planks;
            int idx = Mathf.FloorToInt(py);
            float local = py - idx;
            return Mathf.SmoothStep(0f, 0.045f, local) * Mathf.SmoothStep(0f, 0.045f, 1f - local);
        }, 2.2f);

        /// <summary>网眼织物（办公椅座面/靠背）。</summary>
        public static Texture2D MeshFabric() => Make("mesh", 256, (u, v) =>
        {
            const int cells = 26;
            float fu = Mathf.Abs(Mathf.Sin(u * cells * Mathf.PI));
            float fv = Mathf.Abs(Mathf.Sin(v * cells * Mathf.PI));
            float weave = Mathf.Max(fu, fv);
            float n = FBM(u, v, 3, 90f, 61.4f);
            float g = 0.075f + weave * 0.085f + n * 0.035f;
            return new Color(g, g * 0.98f, g * 1.05f);
        });

        public static Texture2D MeshFabricNormal() => NormalFromHeight("mesh", 128, (u, v) =>
        {
            const int cells = 26;
            float fu = Mathf.Abs(Mathf.Sin(u * cells * Mathf.PI));
            float fv = Mathf.Abs(Mathf.Sin(v * cells * Mathf.PI));
            return Mathf.Max(fu, fv);
        }, 2.6f);

        /// <summary>窗帘布：竖向褶皱 + 织物纹理。</summary>
        public static Texture2D CurtainFabric() => Make("curtain", 512, (u, v) =>
        {
            float folds = Mathf.Sin(u * 9f * Mathf.PI) * 0.5f + 0.5f;
            float folds2 = Mathf.Sin(u * 21f * Mathf.PI + 1.3f) * 0.5f + 0.5f;
            float weave = FBM(u, v * 2f, 3, 130f, 29.8f);
            float t = folds * 0.55f + folds2 * 0.15f + weave * 0.30f;
            var c = Color.Lerp(new Color(0.325f, 0.340f, 0.365f), new Color(0.585f, 0.600f, 0.630f), t);
            return c;
        });

        public static Texture2D CurtainNormal() => NormalFromHeight("curtain", 256, (u, v) =>
            Mathf.Sin(u * 9f * Mathf.PI) * 0.5f + 0.5f, 2.4f);

        /// <summary>金属拉丝（桌腿、柜体拉手）。</summary>
        public static Texture2D BrushedMetal() => Make("metal", 256, (u, v) =>
        {
            float streak = FBM(u * 0.06f, v * 4f, 3, 60f, 71.2f);
            float g = 0.62f + streak * 0.24f;
            return new Color(g, g, g);
        });

        /// <summary>投影幕布：均匀微颗粒的哑光白。</summary>
        public static Texture2D ScreenCanvas() => Make("screencanvas", 256, (u, v) =>
        {
            float n = FBM(u, v, 3, 160f, 91.5f);
            float g = 0.885f + (n - 0.5f) * 0.035f;
            return new Color(g, g, g);
        });

        /// <summary>磨砂玻璃质感（用于隔断/门扇），带轻微不均匀。</summary>
        public static Texture2D FrostedGlass() => Make("frosted", 256, (u, v) =>
        {
            float n = FBM(u, v, 4, 24f, 51.9f);
            float g = 0.86f + (n - 0.5f) * 0.20f;
            return new Color(g, g * 1.01f, g * 1.03f, 1f);
        });

        // ---------------------------------------------------------------- 屏幕画面内容

        /// <summary>投影幕布上的演示画面（智能会议室系统看板）。</summary>
        public static Texture2D ScreenContent() => Make("screencontent", 512, (u, v) =>
        {
            // 深蓝渐变底
            var top = new Color(0.043f, 0.086f, 0.180f);
            var bot = new Color(0.020f, 0.043f, 0.098f);
            var c = Color.Lerp(bot, top, v);

            // 顶部标题条
            if (v > 0.855f) c = Color.Lerp(c, new Color(0.086f, 0.396f, 0.757f), 0.85f);

            // 三块数据卡片
            for (int i = 0; i < 3; i++)
            {
                float x0 = 0.065f + i * 0.315f, x1 = x0 + 0.265f;
                if (u > x0 && u < x1 && v > 0.60f && v < 0.795f)
                {
                    c = Color.Lerp(c, new Color(0.098f, 0.145f, 0.239f), 0.92f);
                    // 卡片顶部彩色强调线
                    if (v > 0.775f) c = new Color(0.259f, 0.780f, 0.980f, 1f);
                }
            }

            // 柱状图
            for (int i = 0; i < 12; i++)
            {
                float h = 0.10f + Hash(i, 5, 3.3f) * 0.42f;
                float x0 = 0.075f + i * 0.0715f, x1 = x0 + 0.042f;
                if (u > x0 && u < x1 && v > 0.11f && v < 0.11f + h)
                {
                    float t = (v - 0.11f) / h;
                    c = Color.Lerp(new Color(0.043f, 0.408f, 0.796f), new Color(0.259f, 0.847f, 0.960f), t);
                }
            }

            // 底部基线
            if (v > 0.095f && v < 0.112f && u > 0.06f && u < 0.945f)
                c = new Color(0.31f, 0.38f, 0.47f, 1f);

            return c;
        });

        /// <summary>门口环境面板 / 传感器显示屏的暗色玻璃底。</summary>
        public static Texture2D PanelGlass() => Make("panelglass", 256, (u, v) =>
        {
            var c = Color.Lerp(new Color(0.020f, 0.031f, 0.047f), new Color(0.055f, 0.078f, 0.110f), v);
            // 顶部一条状态高亮
            if (v > 0.93f) c = new Color(0.180f, 0.760f, 0.940f, 1f);
            // 右侧细小刻度
            if (u > 0.86f && v > 0.15f && v < 0.85f && Mathf.Repeat(v * 22f, 1f) < 0.35f)
                c = Color.Lerp(c, new Color(0.30f, 0.52f, 0.62f), 0.55f);
            return c;
        });

        /// <summary>冷色温感渐变（温度空间化用）：蓝 → 青 → 绿 → 黄 → 红。</summary>
        public static Texture2D ThermalRamp() => Make("thermal", 256, (u, v) =>
        {
            float t = Mathf.Clamp01(u);
            Color c;
            if (t < 0.25f) c = Color.Lerp(new Color(0.09f, 0.28f, 0.85f), new Color(0.10f, 0.75f, 0.90f), t / 0.25f);
            else if (t < 0.5f) c = Color.Lerp(new Color(0.10f, 0.75f, 0.90f), new Color(0.35f, 0.85f, 0.35f), (t - 0.25f) / 0.25f);
            else if (t < 0.75f) c = Color.Lerp(new Color(0.35f, 0.85f, 0.35f), new Color(0.98f, 0.85f, 0.20f), (t - 0.5f) / 0.25f);
            else c = Color.Lerp(new Color(0.98f, 0.85f, 0.20f), new Color(0.92f, 0.16f, 0.12f), (t - 0.75f) / 0.25f);
            return c;
        });

        /// <summary>柔和径向光斑（灯具发光、光晕用）。</summary>
        public static Texture2D RadialGlow() => Make("glow", 256, (u, v) =>
        {
            float d = Vector2.Distance(new Vector2(u, v), new Vector2(0.5f, 0.5f)) * 2f;
            float a = Mathf.Clamp01(1f - d);
            a = a * a * (3f - 2f * a);
            return new Color(a, a, a, a);
        });

        /// <summary>浅色石材（窗台板、台面）。</summary>
        public static Texture2D Stone() => Make("stone", 256, (u, v) =>
        {
            float n = FBM(u, v, 4, 30f, 83.1f);
            float vein = Mathf.Abs(Mathf.Sin((u * 2.2f + FBM(u, v, 3, 6f, 12.2f) * 3f) * Mathf.PI));
            float g = 0.70f + n * 0.14f - vein * 0.05f;
            return new Color(g, g * 0.99f, g * 0.965f);
        });
    }
}
