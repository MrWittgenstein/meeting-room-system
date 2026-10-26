using UnityEngine;
using UnityEngine.Rendering;

namespace NordicRoom
{
    /// <summary>URP PBR 材质库（北欧明亮风）：高明度、低饱和、哑光为主。</summary>
    public static class Mat
    {
        static readonly System.Collections.Generic.Dictionary<string, Material> Cache =
            new System.Collections.Generic.Dictionary<string, Material>();

        public static readonly int BaseColorId = Shader.PropertyToID("_BaseColor");
        public static readonly int BaseMapId = Shader.PropertyToID("_BaseMap");
        public static readonly int BumpMapId = Shader.PropertyToID("_BumpMap");
        public static readonly int SmoothnessId = Shader.PropertyToID("_Smoothness");
        public static readonly int MetallicId = Shader.PropertyToID("_Metallic");
        public static readonly int EmissionId = Shader.PropertyToID("_EmissionColor");

        static Shader _lit;
        static Shader Lit
        {
            get
            {
                if (_lit == null)
                {
                    _lit = Shader.Find("Universal Render Pipeline/Lit");
                    if (_lit == null) _lit = Shader.Find("Standard");
                }
                return _lit;
            }
        }

        public static Material Pbr(string key, Color color, float metallic, float smoothness,
            Texture2D albedo = null, Texture2D normal = null, Vector2 tiling = default, Color? emission = null)
        {
            if (Cache.TryGetValue(key, out var c) && c != null) return c;
            var m = new Material(Lit) { name = "M_" + key };
            m.SetColor(BaseColorId, color);
            m.SetFloat(MetallicId, metallic);
            m.SetFloat(SmoothnessId, smoothness);
            if (tiling == default) tiling = Vector2.one;
            if (albedo != null) { m.SetTexture(BaseMapId, albedo); m.SetTextureScale(BaseMapId, tiling); }
            if (normal != null)
            {
                m.SetTexture(BumpMapId, normal);
                m.SetTextureScale(BumpMapId, tiling);
                m.EnableKeyword("_NORMALMAP");
                m.SetFloat("_BumpScale", 1f);
            }
            if (emission.HasValue)
            {
                m.EnableKeyword("_EMISSION");
                m.SetColor(EmissionId, emission.Value);
                m.globalIlluminationFlags = MaterialGlobalIlluminationFlags.RealtimeEmissive;
            }
            Cache[key] = m;
            return m;
        }

        public static Material Clear(string key, Color color, float smoothness, float alpha)
        {
            if (Cache.TryGetValue(key, out var c) && c != null) return c;
            var m = new Material(Lit) { name = "M_" + key };
            color.a = alpha;
            m.SetColor(BaseColorId, color);
            m.SetFloat(SmoothnessId, smoothness);
            m.SetFloat(MetallicId, 0.04f);
            m.SetFloat("_Surface", 1f);
            m.SetFloat("_Blend", 0f);
            m.SetFloat("_ZWrite", 0f);
            m.SetFloat("_SrcBlend", (float)BlendMode.SrcAlpha);
            m.SetFloat("_DstBlend", (float)BlendMode.OneMinusSrcAlpha);
            m.EnableKeyword("_SURFACE_TYPE_TRANSPARENT");
            m.EnableKeyword("_ALPHAPREMULTIPLY_ON");
            m.SetShaderPassEnabled("ShadowCaster", false);
            m.renderQueue = (int)RenderQueue.Transparent;
            Cache[key] = m;
            return m;
        }

        public static Material Emissive(string key, Color color, float intensity)
            => Pbr(key, Color.white * 0.95f, 0f, 0.30f, null, null, Vector2.one, color * intensity);

        // ------------------------------------------------------------ 木（浅色）

        public static Material LightOak => Pbr("n_oak", new Color(1.02f, 0.99f, 0.95f), 0.02f, 0.26f,
            Tex.LightOak(), Tex.LightOakN(), new Vector2(3.2f, 3.2f));

        public static Material AshWood => Pbr("n_ash", new Color(1.05f, 1.02f, 0.98f), 0.02f, 0.24f,
            Tex.Ash(), Tex.AshN(), new Vector2(0.95f, 0.95f));

        public static Material AshDark => Pbr("n_ash_dark", new Color(0.78f, 0.75f, 0.71f), 0.02f, 0.22f,
            Tex.Ash(), Tex.AshN(), new Vector2(0.9f, 0.9f));

        // ------------------------------------------------------------ 墙 / 地 / 顶

        public static Material WhiteWall => Pbr("n_wall", new Color(0.960f, 0.955f, 0.945f), 0f, 0.10f,
            Tex.WhiteWall(), Tex.WhiteWallN(), new Vector2(4f, 2f));

        public static Material Ceiling => Pbr("n_ceiling", new Color(0.972f, 0.970f, 0.965f), 0f, 0.20f);
        public static Material CeilingTrim => Pbr("n_ceil_trim", new Color(0.905f, 0.895f, 0.880f), 0f, 0.18f);

        public static Material Rug => Pbr("n_rug", new Color(0.790f, 0.788f, 0.772f), 0f, 0.06f,
            Tex.WoolRug(), Tex.WoolRugN(), new Vector2(5f, 4f));

        public static Material Terrazzo => Pbr("n_terrazzo", new Color(0.940f, 0.935f, 0.925f), 0.03f, 0.34f,
            Tex.Terrazzo(), null, new Vector2(3f, 3f));

        public static Material SlatPanel => Pbr("n_slat", new Color(0.980f, 0.975f, 0.965f), 0.02f, 0.24f,
            Tex.LightSlat(), Tex.LightSlatN(), new Vector2(2.2f, 1f));

        // ------------------------------------------------------------ 布艺

        public static Material Linen => Pbr("n_linen", new Color(0.900f, 0.908f, 0.918f), 0f, 0.16f,
            Tex.Linen(), Tex.LinenN(), new Vector2(2.5f, 2.5f));

        public static Material AccentOlive => Pbr("n_olive", new Color(0.520f, 0.585f, 0.480f), 0f, 0.18f,
            Tex.Linen(), Tex.LinenN(), new Vector2(2f, 2f));

        public static Material AccentBlue => Pbr("n_blue", new Color(0.560f, 0.630f, 0.690f), 0f, 0.18f,
            Tex.Linen(), Tex.LinenN(), new Vector2(2f, 2f));

        public static Material Perforated => Pbr("n_perf", new Color(0.780f, 0.790f, 0.800f), 0.10f, 0.30f,
            Tex.Perforated(), null, new Vector2(3f, 3f));

        // ------------------------------------------------------------ 窗帘

        public static Material GreyCurtain => Pbr("n_curtain", new Color(0.900f, 0.905f, 0.915f), 0f, 0.16f,
            Tex.GreyCurtain(), Tex.GreyCurtainN(), new Vector2(1.5f, 1f));

        public static Material Sheer => Clear("n_sheer", new Color(0.965f, 0.972f, 0.980f), 0.30f, 0.26f);

        public static Material SheerFab => Pbr("n_sheerfab", new Color(0.960f, 0.968f, 0.975f), 0f, 0.18f,
            Tex.SheerFabric(), Tex.SheerFabricN(), new Vector2(1.2f, 1f));

        // ------------------------------------------------------------ 金属 / 石材

        public static Material Steel => Pbr("n_steel", new Color(0.780f, 0.785f, 0.795f), 0.85f, 0.55f,
            Tex.BrushedSteel(), null, new Vector2(1f, 2f));

        public static Material BlackMetal => Pbr("n_blackmetal", new Color(0.145f, 0.150f, 0.158f), 0.80f, 0.42f,
            Tex.BrushedSteel(), null, new Vector2(1f, 2f));

        public static Material Chrome => Pbr("n_chrome", new Color(0.885f, 0.890f, 0.900f), 1f, 0.84f);
        public static Material MatteStone => Pbr("n_stone", new Color(0.930f, 0.925f, 0.912f), 0.02f, 0.30f,
            Tex.MatteStone(), null, new Vector2(2f, 1f));

        public static Material Plastic => Pbr("n_plastic", new Color(0.135f, 0.138f, 0.142f), 0f, 0.38f);
        public static Material PlasticWhite => Pbr("n_plastic_white", new Color(0.945f, 0.945f, 0.940f), 0f, 0.34f);
        public static Material Rubber => Pbr("n_rubber", new Color(0.060f, 0.062f, 0.065f), 0f, 0.10f);

        // ------------------------------------------------------------ 玻璃

        public static Material GlassWindow => Clear("n_glass_win", new Color(0.900f, 0.945f, 0.960f), 0.95f, 0.075f);
        public static Material GlassDoor => Clear("n_glass_door", new Color(0.925f, 0.950f, 0.955f), 0.93f, 0.15f);

        // ------------------------------------------------------------ 面板

        public static Material PanelScreen => Pbr("n_panel_screen", new Color(0.820f, 0.860f, 0.885f), 0.10f, 0.55f,
            Tex.PanelGlass(), null, Vector2.one, new Color(0.22f, 0.52f, 0.60f));

        public static Material AcScreen => Pbr("n_ac_screen", new Color(0.800f, 0.840f, 0.865f), 0.10f, 0.55f,
            Tex.PanelGlass(), null, Vector2.one, new Color(0.20f, 0.48f, 0.58f));

        // ------------------------------------------------------------ 陈设

        public static Material Board => Pbr("n_board", new Color(0.972f, 0.972f, 0.968f), 0f, 0.68f);
        public static Material BoardFrame => Pbr("n_board_frame", new Color(0.930f, 0.920f, 0.905f), 0.05f, 0.30f);
        public static Material Leaf => Pbr("n_leaf", new Color(0.235f, 0.420f, 0.215f), 0f, 0.32f);
        public static Material LeafLight => Pbr("n_leaf2", new Color(0.320f, 0.510f, 0.265f), 0f, 0.32f);
        public static Material Soil => Pbr("n_soil", new Color(0.135f, 0.108f, 0.085f), 0f, 0.06f);
        public static Material PotWhite => Pbr("n_pot_white", new Color(0.930f, 0.925f, 0.915f), 0.02f, 0.42f);
        public static Material PotGrey => Pbr("n_pot_grey", new Color(0.660f, 0.655f, 0.645f), 0f, 0.22f);
        public static Material Paper => Pbr("n_paper", new Color(0.975f, 0.975f, 0.968f), 0f, 0.30f);

        /// <summary>
        /// 圆台面专用：不带木纹贴图的纯净浅橡木色。
        /// 圆盘 UV 是 0..1 的径向映射，套带条纹的木材贴图会出现放射状"太阳纹"，
        /// 所以这里只用纯色 + 轻微光泽。
        /// </summary>
        public static Material WoodTop => Pbr("n_wood_top", new Color(0.930f, 0.850f, 0.715f), 0.02f, 0.34f);

        /// <summary>吊灯灯泡发光面（朝下）。</summary>
        public static Material PendantGlow => Emissive("n_pendant_glow", new Color(1f, 0.955f, 0.880f), 0.72f);

        /// <summary>
        /// 灯罩本体：带轻微自发光的暖白布面。
        /// 灯罩是"开口朝下的圆筒"，内表面背光时几乎全黑，看起来像挂了个黑锥子；
        /// 给它一点自发光就自然多了（灯灭时由 RoomController 换回 <see cref="PendantDark"/>）。
        /// </summary>
        public static Material ShadeLit => Emissive("n_shade_lit", new Color(1f, 0.962f, 0.905f), 0.55f);

        /// <summary>灯灭时的灯罩。</summary>
        public static Material ShadeOff => Pbr("n_shade_off", new Color(0.860f, 0.855f, 0.845f), 0f, 0.30f);

        /// <summary>吊灯熄灭时的灯泡面。</summary>
        public static Material PendantDark => Pbr("n_pendant_dark", new Color(0.315f, 0.305f, 0.288f), 0f, 0.24f);

        public static Material ThermalRamp => Pbr("n_thermal", Color.white, 0f, 0.05f,
            Tex.Thermal(), null, Vector2.one);

        public static void ClearCache() => Cache.Clear();
    }
}
