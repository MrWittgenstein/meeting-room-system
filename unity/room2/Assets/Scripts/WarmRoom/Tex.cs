using UnityEngine;

namespace WarmRoom
{
    /// <summary>
    /// 程序化贴图库（暖色木质商务风）。全部由代码生成，不依赖任何外部图片。
    /// 与"现代简约"那版的区别：木纹以**深胡桃木**为主、地毯偏暖灰、皮革有细腻纹路。
    /// </summary>
    public static class Tex
    {
        static readonly System.Collections.Generic.Dictionary<string, Texture2D> Cache =
            new System.Collections.Generic.Dictionary<string, Texture2D>();

        // ---------------------------------------------------------------- 基础

        static Texture2D Make(string key, int size, System.Func<float, float, Color> fn)
        {
            if (Cache.TryGetValue(key, out var c) && c != null) return c;
            var tex = new Texture2D(size, size, TextureFormat.RGBA32, true, false);
            tex.name = "T_" + key;
            tex.wrapMode = TextureWrapMode.Repeat;
            tex.filterMode = FilterMode.Trilinear;
            tex.anisoLevel = 4;
            var px = new Color[size * size];
            for (int y = 0; y < size; y++)
            {
                float v = (y + 0.5f) / size;
                for (int x = 0; x < size; x++)
                    px[y * size + x] = fn((x + 0.5f) / size, v);
            }
            tex.SetPixels(px);
            tex.Apply(true, false);
            Cache[key] = tex;
            return tex;
        }

        static float FBM(float u, float v, int oct, float scale, float seed)
        {
            float sum = 0f, amp = 1f, norm = 0f;
            for (int i = 0; i < oct; i++)
            {
                sum += Mathf.PerlinNoise(seed + u * scale, seed * 1.7f + v * scale) * amp;
                norm += amp; amp *= 0.5f; scale *= 2f;
            }
            return sum / norm;
        }

        static float Hash(int x, int y, float seed)
        {
            float n = Mathf.Sin(x * 127.1f + y * 311.7f + seed * 74.7f) * 43758.5453f;
            return n - Mathf.Floor(n);
        }

        static Texture2D NormalFromHeight(string key, int size, System.Func<float, float, float> h, float strength)
        {
            string nk = key + "_n";
            if (Cache.TryGetValue(nk, out var c) && c != null) return c;
            var tex = new Texture2D(size, size, TextureFormat.RGBA32, true, true);
            tex.name = "N_" + key;
            tex.wrapMode = TextureWrapMode.Repeat;
            float step = 1f / size;
            var px = new Color[size * size];
            for (int y = 0; y < size; y++)
            {
                for (int x = 0; x < size; x++)
                {
                    float u = (x + 0.5f) / size, v = (y + 0.5f) / size;
                    float hl = h(u - step, v), hr = h(u + step, v);
                    float hd = h(u, v - step), hu = h(u, v + step);
                    var n = new Vector3((hl - hr) * strength, (hd - hu) * strength, 1f).normalized;
                    px[y * size + x] = new Color(n.x * 0.5f + 0.5f, n.y * 0.5f + 0.5f, n.z * 0.5f + 0.5f, 1f);
                }
            }
            tex.SetPixels(px);
            tex.Apply(true, false);
            Cache[nk] = tex;
            return tex;
        }

        // ---------------------------------------------------------------- 木材

        /// <summary>深胡桃木（护墙板 / 桌面）：宽年轮 + 深色纹理。</summary>
        public static Texture2D Walnut()
        {
            return Make("walnut2", 512, (u, v) =>
            {
                float warp = FBM(u, v, 3, 4.5f, 17.3f) - 0.5f;
                float rings = Mathf.Sin((v * 20f + warp * 6f) * Mathf.PI) * 0.5f + 0.5f;
                float grain = FBM(u * 0.3f, v * 1.6f, 4, 90f, 41.7f);
                float t = Mathf.Clamp01(rings * 0.55f + grain * 0.45f);
                // 木色区间：深端 0.36、亮端 0.72。
                // 一开始写成 0.215~0.455（"深胡桃木"），但在室内环境光较弱 + ACES 色调映射下
                // 会整体压成近黑（实测桌面采样只有 RGB(20,6,4)）。提到这个区间后才是正常的
                // "中深胡桃木"观感，同时保留明显的年轮对比。
                var dark = new Color(0.360f, 0.222f, 0.132f);
                var light = new Color(0.720f, 0.470f, 0.282f);
                var c = Color.Lerp(dark, light, t);
                c *= 1f - Mathf.Clamp01((grain - 0.88f) * 3.2f) * 0.35f;   // 木节
                return c;
            });
        }

        public static Texture2D WalnutN() => NormalFromHeight("walnut2", 256, (u, v) =>
        {
            float warp = FBM(u, v, 3, 4.5f, 17.3f) - 0.5f;
            return Mathf.Sin((v * 20f + warp * 6f) * Mathf.PI) * 0.5f + 0.5f;
        }, 1.0f);

        /// <summary>浅橡木（地板 / 侧柜台面）：窄条拼板。</summary>
        public static Texture2D Oak()
        {
            return Make("oak2", 512, (u, v) =>
            {
                const int planks = 7;
                float py = v * planks;
                int idx = Mathf.FloorToInt(py);
                float local = py - idx;
                float shade = 0.84f + Hash(idx, 7, 3.1f) * 0.28f;
                float grain = FBM(u * 0.25f, v * 4.0f, 4, 30f, 63.5f);
                float seam = Mathf.SmoothStep(0f, 0.035f, local) * Mathf.SmoothStep(0f, 0.035f, 1f - local);
                var c = Color.Lerp(new Color(0.470f, 0.330f, 0.190f), new Color(0.660f, 0.500f, 0.320f), grain);
                c *= shade;
                return Color.Lerp(c * 0.35f, c, seam);
            });
        }

        public static Texture2D OakN() => NormalFromHeight("oak2", 256, (u, v) =>
        {
            const int planks = 7;
            float py = v * planks;
            int idx = Mathf.FloorToInt(py);
            float local = py - idx;
            return Mathf.SmoothStep(0f, 0.035f, local) * Mathf.SmoothStep(0f, 0.035f, 1f - local);
        }, 2.0f);

        /// <summary>木质格栅（墙面装饰条）：竖向细密条纹。</summary>
        public static Texture2D Slat()
        {
            return Make("slat", 512, (u, v) =>
            {
                const float slats = 14f;
                float f = Mathf.Repeat(u * slats, 1f);
                float edge = Mathf.SmoothStep(0f, 0.10f, f) * Mathf.SmoothStep(0f, 0.10f, 1f - f);
                float grain = FBM(u * 0.4f, v * 6f, 3, 60f, 29.4f);
                var c = Color.Lerp(new Color(0.075f, 0.045f, 0.028f), new Color(0.230f, 0.135f, 0.078f), grain);
                return Color.Lerp(c * 0.45f, c, edge);
            });
        }

        public static Texture2D SlatN() => NormalFromHeight("slat", 256, (u, v) =>
        {
            const float slats = 14f;
            float f = Mathf.Repeat(u * slats, 1f);
            return Mathf.SmoothStep(0f, 0.10f, f) * Mathf.SmoothStep(0f, 0.10f, 1f - f);
        }, 2.4f);

        // ---------------------------------------------------------------- 织物与软装

        /// <summary>暖灰地毯：细密纤维 + 轻微色斑。</summary>
        public static Texture2D Carpet()
        {
            return Make("carpet2", 512, (u, v) =>
            {
                float fiber = FBM(u, v, 4, 100f, 5.9f);
                float blotch = FBM(u, v, 3, 5f, 13.7f);
                float g = 0.190f + fiber * 0.070f + (blotch - 0.5f) * 0.040f;
                return new Color(g * 1.04f, g * 0.99f, g * 0.92f);   // 偏暖
            });
        }

        public static Texture2D CarpetN() => NormalFromHeight("carpet2", 256,
            (u, v) => FBM(u, v, 4, 100f, 5.9f), 1.5f);

        /// <summary>皮革（会议椅）：细腻颗粒 + 轻微褶皱。</summary>
        public static Texture2D Leather()
        {
            return Make("leather", 512, (u, v) =>
            {
                float grain = FBM(u, v, 4, 150f, 71.1f);
                float crease = Mathf.Abs(FBM(u * 0.5f, v * 0.5f, 3, 12f, 23.3f) - 0.5f) * 2f;
                float t = grain * 0.65f + (1f - crease) * 0.35f;
                var dark = new Color(0.170f, 0.088f, 0.048f);
                var light = new Color(0.385f, 0.205f, 0.108f);
                return Color.Lerp(dark, light, t);
            });
        }

        public static Texture2D LeatherN() => NormalFromHeight("leather", 256,
            (u, v) => FBM(u, v, 4, 150f, 71.1f), 1.3f);

        /// <summary>窗帘布：粗一些的竖褶皱（厚帘面料）。</summary>
        public static Texture2D Curtain()
        {
            return Make("curtain2", 512, (u, v) =>
            {
                float f1 = Mathf.Sin(u * 7f * Mathf.PI) * 0.5f + 0.5f;
                float f2 = Mathf.Sin(u * 17f * Mathf.PI + 1.1f) * 0.5f + 0.5f;
                float weave = FBM(u, v * 2f, 3, 110f, 47.2f);
                float t = f1 * 0.58f + f2 * 0.14f + weave * 0.28f;
                return Color.Lerp(new Color(0.310f, 0.225f, 0.160f), new Color(0.585f, 0.455f, 0.325f), t);
            });
        }

        public static Texture2D CurtainN() => NormalFromHeight("curtain2", 256,
            (u, v) => Mathf.Sin(u * 7f * Mathf.PI) * 0.5f + 0.5f, 2.2f);

        // ---------------------------------------------------------------- 墙面与硬装

        /// <summary>暖白墙面（乳胶漆、略带暖调）。</summary>
        public static Texture2D Plaster()
        {
            return Make("plaster2", 256, (u, v) =>
            {
                float g = FBM(u, v, 3, 36f, 33.9f);
                float b = FBM(u, v, 2, 3f, 8.8f);
                float c = 0.880f + (g - 0.5f) * 0.022f + (b - 0.5f) * 0.022f;
                return new Color(c, c * 0.978f, c * 0.940f);
            });
        }

        public static Texture2D PlasterN() => NormalFromHeight("plaster2", 128,
            (u, v) => FBM(u, v, 3, 36f, 33.9f), 0.45f);

        /// <summary>暖色石材（窗台板 / 台面）。</summary>
        public static Texture2D Stone()
        {
            return Make("stone2", 256, (u, v) =>
            {
                float n = FBM(u, v, 4, 26f, 91.3f);
                float vein = Mathf.Abs(Mathf.Sin((v * 2.4f + FBM(u, v, 3, 5f, 15.1f) * 3f) * Mathf.PI));
                float g = 0.680f + n * 0.14f - vein * 0.06f;
                return new Color(g * 1.03f, g * 0.99f, g * 0.93f);
            });
        }

        /// <summary>拉丝金属（暖色黄铜感）。</summary>
        public static Texture2D Brass()
        {
            return Make("brass", 256, (u, v) =>
            {
                float streak = FBM(u * 0.05f, v * 5f, 3, 70f, 79.5f);
                float g = 0.60f + streak * 0.26f;
                return new Color(g * 1.06f, g * 0.94f, g * 0.72f);
            });
        }

        /// <summary>
        /// 暖色大理石（主景墙装饰带）。
        /// 用"低频扰动 + 单向流动细纹"生成**斜向连续纹路**，
        /// 而不是各向同性的斑点（上一版就是斑点感，看着像壁纸不像石材）。
        /// </summary>
        public static Texture2D Marble()
        {
            return Make("marble", 512, (u, v) =>
            {
                float s = u * 0.82f + v * 0.58f;                                  // 斜向流动坐标
                float warp = (FBM(u * 0.35f, v * 0.35f, 4, 3.2f, 55.2f) - 0.5f) * 0.55f;
                float band = Mathf.Abs(Mathf.Sin((s * 5.5f + warp) * Mathf.PI));
                float vein = Mathf.Pow(1f - band, 16f);                           // 主纹：细而连续
                float band2 = Mathf.Abs(Mathf.Sin((s * 13f + warp * 2.2f) * Mathf.PI));
                float vein2 = Mathf.Pow(1f - band2, 26f) * 0.45f;                 // 次级细纹

                float n = FBM(u, v, 4, 14f, 12.9f);
                float baseG = 0.790f + n * 0.055f;                                // 暖白/米黄底
                var c = new Color(baseG * 1.010f, baseG * 0.972f, baseG * 0.925f);
                var veinCol = new Color(0.520f, 0.412f, 0.312f);
                c = Color.Lerp(c, veinCol, Mathf.Clamp01(vein) * 0.55f);
                c = Color.Lerp(c, veinCol, Mathf.Clamp01(vein2) * 0.30f);
                return c;
            });
        }

        // ---------------------------------------------------------------- 屏幕内容

        /// <summary>门口环境面板的暗色玻璃底。</summary>
        public static Texture2D PanelGlass()
        {
            return Make("panelglass2", 256, (u, v) =>
            {
                var c = Color.Lerp(new Color(0.035f, 0.026f, 0.018f), new Color(0.085f, 0.068f, 0.045f), v);
                if (v > 0.93f) c = new Color(0.95f, 0.72f, 0.30f, 1f);
                if (u > 0.86f && v > 0.15f && v < 0.85f && Mathf.Repeat(v * 22f, 1f) < 0.35f)
                    c = Color.Lerp(c, new Color(0.60f, 0.48f, 0.28f), 0.55f);
                return c;
            });
        }

        /// <summary>温度色阶（空间化可视化）：蓝 → 青 → 绿 → 黄 → 红。</summary>
        public static Texture2D Thermal()
        {
            return Make("thermal2", 256, (u, v) =>
            {
                float t = Mathf.Clamp01(u);
                if (t < 0.25f) return Color.Lerp(new Color(0.10f, 0.30f, 0.88f), new Color(0.10f, 0.78f, 0.92f), t / 0.25f);
                if (t < 0.50f) return Color.Lerp(new Color(0.10f, 0.78f, 0.92f), new Color(0.38f, 0.86f, 0.34f), (t - 0.25f) / 0.25f);
                if (t < 0.75f) return Color.Lerp(new Color(0.38f, 0.86f, 0.34f), new Color(0.99f, 0.86f, 0.20f), (t - 0.5f) / 0.25f);
                return Color.Lerp(new Color(0.99f, 0.86f, 0.20f), new Color(0.93f, 0.15f, 0.11f), (t - 0.75f) / 0.25f);
            });
        }

        /// <summary>径向光斑（灯具光晕 / 发光贴图）。</summary>
        public static Texture2D Glow()
        {
            return Make("glow2", 256, (u, v) =>
            {
                float d = Vector2.Distance(new Vector2(u, v), new Vector2(0.5f, 0.5f)) * 2f;
                float a = Mathf.Clamp01(1f - d);
                a = a * a * (3f - 2f * a);
                return new Color(a, a, a, a);
            });
        }

        /// <summary>细密网孔（麦克风罩 / 出风格栅）。</summary>
        public static Texture2D Mesh()
        {
            return Make("mesh2", 256, (u, v) =>
            {
                const int cells = 30;
                float fu = Mathf.Abs(Mathf.Sin(u * cells * Mathf.PI));
                float fv = Mathf.Abs(Mathf.Sin(v * cells * Mathf.PI));
                float w = Mathf.Max(fu, fv);
                float n = FBM(u, v, 3, 80f, 51.7f);
                float g = 0.085f + w * 0.075f + n * 0.030f;
                return new Color(g * 1.04f, g, g * 0.94f);
            });
        }

        public static Texture2D MeshN() => NormalFromHeight("mesh2", 128, (u, v) =>
        {
            const int cells = 30;
            return Mathf.Max(Mathf.Abs(Mathf.Sin(u * cells * Mathf.PI)), Mathf.Abs(Mathf.Sin(v * cells * Mathf.PI)));
        }, 2.4f);
    }
}
