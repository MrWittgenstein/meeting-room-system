using UnityEngine;

namespace NordicRoom
{
    /// <summary>
    /// 程序化贴图库（北欧明亮风）。
    /// 与前面两版的区别：以**浅白橡木 / 白色乳胶漆 / 浅灰羊毛**为主，整体高明度低饱和。
    /// </summary>
    public static class Tex
    {
        static readonly System.Collections.Generic.Dictionary<string, Texture2D> Cache =
            new System.Collections.Generic.Dictionary<string, Texture2D>();

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
            tex.SetPixels(px); tex.Apply(true, false);
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
                for (int x = 0; x < size; x++)
                {
                    float u = (x + 0.5f) / size, v = (y + 0.5f) / size;
                    float hl = h(u - step, v), hr = h(u + step, v);
                    float hd = h(u, v - step), hu = h(u, v + step);
                    var n = new Vector3((hl - hr) * strength, (hd - hu) * strength, 1f).normalized;
                    px[y * size + x] = new Color(n.x * 0.5f + 0.5f, n.y * 0.5f + 0.5f, n.z * 0.5f + 0.5f, 1f);
                }
            tex.SetPixels(px); tex.Apply(true, false);
            Cache[nk] = tex;
            return tex;
        }

        // ---------------------------------------------------------------- 木材（浅色系）

        /// <summary>浅白橡木（地板 / 桌面）：窄拼板、高明度、低对比。</summary>
        public static Texture2D LightOak()
        {
            return Make("lightoak", 512, (u, v) =>
            {
                const int planks = 8;
                float py = v * planks;
                int idx = Mathf.FloorToInt(py);
                float local = py - idx;
                float shade = 0.94f + Hash(idx, 11, 5.3f) * 0.14f;      // 板间色差很小
                float grain = FBM(u * 0.22f, v * 4.5f, 4, 34f, 71.9f);
                float seam = Mathf.SmoothStep(0f, 0.030f, local) * Mathf.SmoothStep(0f, 0.030f, 1f - local);
                // 底色：米白到浅橡木，整体很亮
                var c = Color.Lerp(new Color(0.760f, 0.672f, 0.545f), new Color(0.905f, 0.845f, 0.740f), grain);
                c *= shade;
                return Color.Lerp(c * 0.55f, c, seam);
            });
        }

        public static Texture2D LightOakN() => NormalFromHeight("lightoak", 256, (u, v) =>
        {
            const int planks = 8;
            float py = v * planks;
            int idx = Mathf.FloorToInt(py);
            float local = py - idx;
            return Mathf.SmoothStep(0f, 0.030f, local) * Mathf.SmoothStep(0f, 0.030f, 1f - local);
        }, 1.6f);

        /// <summary>白蜡木（家具 / 窗框）：比橡木更浅更冷。</summary>
        public static Texture2D Ash()
        {
            return Make("ash", 512, (u, v) =>
            {
                float warp = FBM(u, v, 3, 5f, 23.1f) - 0.5f;
                float rings = Mathf.Sin((v * 14f + warp * 4f) * Mathf.PI) * 0.5f + 0.5f;
                float grain = FBM(u * 0.3f, v * 2.0f, 4, 70f, 37.5f);
                float t = Mathf.Clamp01(rings * 0.45f + grain * 0.55f);
                return Color.Lerp(new Color(0.800f, 0.740f, 0.640f), new Color(0.940f, 0.900f, 0.826f), t);
            });
        }

        public static Texture2D AshN() => NormalFromHeight("ash", 256, (u, v) =>
        {
            float warp = FBM(u, v, 3, 5f, 23.1f) - 0.5f;
            return Mathf.Sin((v * 14f + warp * 4f) * Mathf.PI) * 0.5f + 0.5f;
        }, 0.9f);

        // ---------------------------------------------------------------- 墙面 / 地面

        /// <summary>白色乳胶漆墙面：极浅、极细颗粒。</summary>
        public static Texture2D WhiteWall()
        {
            return Make("whitewall", 256, (u, v) =>
            {
                float g = FBM(u, v, 3, 34f, 41.3f);
                float b = FBM(u, v, 2, 3f, 9.9f);
                float c = 0.935f + (g - 0.5f) * 0.014f + (b - 0.5f) * 0.014f;
                return new Color(c, c * 0.996f, c * 0.986f);
            });
        }

        public static Texture2D WhiteWallN() => NormalFromHeight("whitewall", 128,
            (u, v) => FBM(u, v, 3, 34f, 41.3f), 0.35f);

        /// <summary>浅灰羊毛地毯（会议区）：细密纤维、低对比。</summary>
        public static Texture2D WoolRug()
        {
            return Make("woolrug", 512, (u, v) =>
            {
                float fiber = FBM(u, v, 4, 110f, 3.7f);
                float blotch = FBM(u, v, 3, 5f, 17.1f);
                float g = 0.560f + fiber * 0.090f + (blotch - 0.5f) * 0.045f;
                return new Color(g * 0.995f, g * 0.99f, g * 0.975f);
            });
        }

        public static Texture2D WoolRugN() => NormalFromHeight("woolrug", 256,
            (u, v) => FBM(u, v, 4, 110f, 3.7f), 1.5f);

        /// <summary>浅灰布艺（椅子坐垫 / 沙发）。</summary>
        public static Texture2D Linen()
        {
            return Make("linen", 512, (u, v) =>
            {
                const int threads = 46;
                float fu = Mathf.Abs(Mathf.Sin(u * threads * Mathf.PI));
                float fv = Mathf.Abs(Mathf.Sin(v * threads * Mathf.PI));
                float weave = Mathf.Max(fu, fv) * 0.55f + Mathf.Min(fu, fv) * 0.45f;
                float n = FBM(u, v, 3, 100f, 61.3f);
                float g = 0.520f + weave * 0.075f + n * 0.055f;
                return new Color(g * 0.99f, g, g * 1.02f);
            });
        }

        public static Texture2D LinenN() => NormalFromHeight("linen", 256, (u, v) =>
        {
            const int threads = 46;
            return Mathf.Max(Mathf.Abs(Mathf.Sin(u * threads * Mathf.PI)), Mathf.Abs(Mathf.Sin(v * threads * Mathf.PI)));
        }, 1.4f);

        /// <summary>薄纱窗帘：细密竖向褶皱、高明度。</summary>
        public static Texture2D SheerFabric()
        {
            return Make("sheerfab", 512, (u, v) =>
            {
                float f1 = Mathf.Sin(u * 12f * Mathf.PI) * 0.5f + 0.5f;
                float f2 = Mathf.Sin(u * 30f * Mathf.PI + 0.7f) * 0.5f + 0.5f;
                float weave = FBM(u, v * 2f, 3, 140f, 29.7f);
                float t = f1 * 0.45f + f2 * 0.15f + weave * 0.40f;
                return Color.Lerp(new Color(0.830f, 0.845f, 0.855f), new Color(0.975f, 0.980f, 0.985f), t);
            });
        }

        public static Texture2D SheerFabricN() => NormalFromHeight("sheerfab", 256,
            (u, v) => Mathf.Sin(u * 12f * Mathf.PI) * 0.5f + 0.5f, 1.4f);

        /// <summary>灰色亚麻遮光帘（外层厚帘）。</summary>
        public static Texture2D GreyCurtain()
        {
            return Make("greycurtain", 512, (u, v) =>
            {
                float f1 = Mathf.Sin(u * 8f * Mathf.PI) * 0.5f + 0.5f;
                float f2 = Mathf.Sin(u * 19f * Mathf.PI + 1.4f) * 0.5f + 0.5f;
                float weave = FBM(u, v * 2f, 3, 120f, 53.1f);
                float t = f1 * 0.58f + f2 * 0.12f + weave * 0.30f;
                return Color.Lerp(new Color(0.560f, 0.575f, 0.590f), new Color(0.790f, 0.800f, 0.812f), t);
            });
        }

        public static Texture2D GreyCurtainN() => NormalFromHeight("greycurtain", 256,
            (u, v) => Mathf.Sin(u * 8f * Mathf.PI) * 0.5f + 0.5f, 2.0f);

        // ---------------------------------------------------------------- 硬装

        /// <summary>浅色水磨石 / 微水泥地面砖（用于入口区）。</summary>
        public static Texture2D Terrazzo()
        {
            return Make("terrazzo", 512, (u, v) =>
            {
                var baseC = new Color(0.880f, 0.872f, 0.855f);
                // 细碎骨料
                for (int k = 0; k < 3; k++)
                {
                    float n = FBM(u * (1f + k * 0.7f), v * (1f + k * 0.9f), 2, 26f + k * 40f, 11f + k * 7f);
                    float spot = Mathf.Clamp01((n - 0.62f) * 5f);
                    var tint = k == 0 ? new Color(0.62f, 0.60f, 0.57f)
                             : k == 1 ? new Color(0.74f, 0.71f, 0.64f)
                                      : new Color(0.55f, 0.58f, 0.60f);
                    baseC = Color.Lerp(baseC, tint, spot * (0.35f - k * 0.08f));
                }
                return baseC;
            });
        }

        /// <summary>白色哑光瓷砖（卫生间/入口用不上，这里给圆形底座用）。</summary>
        public static Texture2D MatteStone()
        {
            return Make("mattestone", 256, (u, v) =>
            {
                float n = FBM(u, v, 4, 22f, 87.3f);
                float g = 0.860f + n * 0.045f;
                return new Color(g, g * 0.997f, g * 0.985f);
            });
        }

        /// <summary>浅灰格栅（墙板 / 天花线条）：竖向细条。</summary>
        public static Texture2D LightSlat()
        {
            return Make("lightslat", 512, (u, v) =>
            {
                const float slats = 16f;
                float f = Mathf.Repeat(u * slats, 1f);
                float edge = Mathf.SmoothStep(0f, 0.09f, f) * Mathf.SmoothStep(0f, 0.09f, 1f - f);
                float grain = FBM(u * 0.5f, v * 7f, 3, 60f, 19.3f);
                var c = Color.Lerp(new Color(0.640f, 0.628f, 0.610f), new Color(0.880f, 0.868f, 0.848f), grain);
                return Color.Lerp(c * 0.72f, c, edge);
            });
        }

        public static Texture2D LightSlatN() => NormalFromHeight("lightslat", 256, (u, v) =>
        {
            const float slats = 16f;
            float f = Mathf.Repeat(u * slats, 1f);
            return Mathf.SmoothStep(0f, 0.09f, f) * Mathf.SmoothStep(0f, 0.09f, 1f - f);
        }, 1.8f);

        /// <summary>拉丝铝 / 不锈钢（北欧风多用哑光金属）。</summary>
        public static Texture2D BrushedSteel()
        {
            return Make("steel", 256, (u, v) =>
            {
                float streak = FBM(u * 0.06f, v * 4.5f, 3, 64f, 73.7f);
                float g = 0.700f + streak * 0.200f;
                return new Color(g, g, g * 1.01f);
            });
        }

        // ---------------------------------------------------------------- 屏幕 / 数据

        public static Texture2D PanelGlass()
        {
            return Make("panelglass3", 256, (u, v) =>
            {
                var c = Color.Lerp(new Color(0.055f, 0.062f, 0.070f), new Color(0.115f, 0.125f, 0.138f), v);
                if (v > 0.93f) c = new Color(0.34f, 0.78f, 0.86f, 1f);
                if (u > 0.86f && v > 0.15f && v < 0.85f && Mathf.Repeat(v * 22f, 1f) < 0.35f)
                    c = Color.Lerp(c, new Color(0.42f, 0.62f, 0.68f), 0.55f);
                return c;
            });
        }

        public static Texture2D Thermal()
        {
            return Make("thermal3", 256, (u, v) =>
            {
                float t = Mathf.Clamp01(u);
                if (t < 0.25f) return Color.Lerp(new Color(0.12f, 0.34f, 0.90f), new Color(0.14f, 0.80f, 0.94f), t / 0.25f);
                if (t < 0.50f) return Color.Lerp(new Color(0.14f, 0.80f, 0.94f), new Color(0.42f, 0.88f, 0.38f), (t - 0.25f) / 0.25f);
                if (t < 0.75f) return Color.Lerp(new Color(0.42f, 0.88f, 0.38f), new Color(0.99f, 0.87f, 0.22f), (t - 0.5f) / 0.25f);
                return Color.Lerp(new Color(0.99f, 0.87f, 0.22f), new Color(0.94f, 0.18f, 0.13f), (t - 0.75f) / 0.25f);
            });
        }

        public static Texture2D Glow()
        {
            return Make("glow3", 256, (u, v) =>
            {
                float d = Vector2.Distance(new Vector2(u, v), new Vector2(0.5f, 0.5f)) * 2f;
                float a = Mathf.Clamp01(1f - d);
                a = a * a * (3f - 2f * a);
                return new Color(a, a, a, a);
            });
        }

        public static Texture2D Perforated()
        {
            return Make("perf", 256, (u, v) =>
            {
                const int cells = 34;
                float fu = Mathf.Abs(Mathf.Sin(u * cells * Mathf.PI));
                float fv = Mathf.Abs(Mathf.Sin(v * cells * Mathf.PI));
                float w = Mathf.Max(fu, fv);
                float n = FBM(u, v, 3, 90f, 43.9f);
                float g = 0.600f + w * 0.110f + n * 0.040f;
                return new Color(g, g, g * 1.01f);
            });
        }
    }
}
