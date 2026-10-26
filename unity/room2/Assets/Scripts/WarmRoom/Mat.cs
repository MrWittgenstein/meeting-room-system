using UnityEngine;
using UnityEngine.Rendering;

namespace WarmRoom
{
    /// <summary>
    /// URP PBR 材质库（暖色木质商务风）。全部由代码创建，无外部资源依赖。
    /// </summary>
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
            if (albedo != null)
            {
                m.SetTexture(BaseMapId, albedo);
                m.SetTextureScale(BaseMapId, tiling);
            }
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
            => Pbr(key, Color.white * 0.92f, 0f, 0.32f, null, null, Vector2.one, color * intensity);

        // ------------------------------------------------------------ 场景材质

        // 木材（贴图已提亮到合理区间，材质基色用正常值即可）
        public static Material Walnut => Pbr("w_walnut", new Color(1.05f, 1.00f, 0.94f), 0.02f, 0.28f,
            Tex.Walnut(), Tex.WalnutN(), new Vector2(0.85f, 0.85f));

        public static Material WalnutDark => Pbr("w_walnut_dark", new Color(0.72f, 0.68f, 0.64f), 0.02f, 0.24f,
            Tex.Walnut(), Tex.WalnutN(), new Vector2(0.8f, 0.8f));

        public static Material OakFloor => Pbr("w_oak", new Color(0.95f, 0.92f, 0.88f), 0.02f, 0.30f,
            Tex.Oak(), Tex.OakN(), new Vector2(3.5f, 3.5f));

        public static Material SlatPanel => Pbr("w_slat", new Color(0.92f, 0.90f, 0.88f), 0.02f, 0.32f,
            Tex.Slat(), Tex.SlatN(), new Vector2(2.4f, 1f));

        // 墙面 / 地面
        public static Material Wall => Pbr("w_wall", new Color(0.905f, 0.880f, 0.845f), 0f, 0.10f,
            Tex.Plaster(), Tex.PlasterN(), new Vector2(4f, 2f));

        public static Material Carpet => Pbr("w_carpet", new Color(0.400f, 0.385f, 0.355f), 0f, 0.04f,
            Tex.Carpet(), Tex.CarpetN(), new Vector2(8f, 6f));

        public static Material Rug => Pbr("w_rug", new Color(0.300f, 0.238f, 0.190f), 0f, 0.05f,
            Tex.Carpet(), Tex.CarpetN(), new Vector2(6f, 4f));

        public static Material Ceiling => Pbr("w_ceiling", new Color(0.905f, 0.895f, 0.880f), 0f, 0.24f);
        public static Material CeilingTrim => Pbr("w_ceil_trim", new Color(0.62f, 0.50f, 0.36f), 0.10f, 0.35f);

        // 软装
        public static Material Leather => Pbr("w_leather", new Color(0.92f, 0.90f, 0.88f), 0.02f, 0.30f,
            Tex.Leather(), Tex.LeatherN(), new Vector2(2f, 2f));

        public static Material Curtain => Pbr("w_curtain", new Color(0.86f, 0.84f, 0.82f), 0f, 0.22f,
            Tex.Curtain(), Tex.CurtainN(), new Vector2(1.4f, 1f));

        public static Material Sheer => Clear("w_sheer", new Color(0.98f, 0.96f, 0.92f), 0.32f, 0.28f);

        public static Material MeshFabric => Pbr("w_mesh", new Color(0.32f, 0.30f, 0.28f), 0.05f, 0.16f,
            Tex.Mesh(), Tex.MeshN(), new Vector2(3f, 3f));

        // 石 / 金属
        public static Material Stone => Pbr("w_stone", new Color(0.90f, 0.88f, 0.85f), 0.03f, 0.42f,
            Tex.Stone(), null, new Vector2(3f, 1f));

        public static Material Marble => Pbr("w_marble", new Color(0.95f, 0.93f, 0.90f), 0.05f, 0.48f,
            Tex.Marble(), null, new Vector2(1.6f, 1f));

        public static Material Brass => Pbr("w_brass", new Color(0.92f, 0.84f, 0.68f), 0.88f, 0.62f,
            Tex.Brass(), null, new Vector2(1f, 2f));

        public static Material BlackMetal => Pbr("w_blackmetal", new Color(0.16f, 0.155f, 0.15f), 0.85f, 0.45f,
            Tex.Brass(), null, new Vector2(1f, 2f));

        public static Material Chrome => Pbr("w_chrome", new Color(0.88f, 0.88f, 0.90f), 1f, 0.86f);
        public static Material Plastic => Pbr("w_plastic", new Color(0.13f, 0.125f, 0.12f), 0f, 0.40f);
        public static Material PlasticWhite => Pbr("w_plastic_white", new Color(0.90f, 0.89f, 0.875f), 0f, 0.36f);
        public static Material Rubber => Pbr("w_rubber", new Color(0.055f, 0.052f, 0.05f), 0f, 0.10f);

        // 玻璃
        public static Material GlassWindow => Clear("w_glass_win", new Color(0.88f, 0.92f, 0.94f), 0.94f, 0.09f);
        public static Material GlassDoor => Clear("w_glass_door", new Color(0.90f, 0.92f, 0.92f), 0.92f, 0.17f);

        // 面板
        public static Material PanelScreen => Pbr("w_panel_screen", new Color(0.80f, 0.84f, 0.86f), 0.10f, 0.55f,
            Tex.PanelGlass(), null, Vector2.one, new Color(0.30f, 0.20f, 0.08f));

        public static Material AcScreen => Pbr("w_ac_screen", new Color(0.78f, 0.82f, 0.84f), 0.10f, 0.55f,
            Tex.PanelGlass(), null, Vector2.one, new Color(0.28f, 0.20f, 0.09f));

        // 陈设
        public static Material Whiteboard => Pbr("w_board", new Color(0.955f, 0.95f, 0.945f), 0f, 0.70f);
        public static Material WbFrame => Pbr("w_board_frame", new Color(0.52f, 0.40f, 0.28f), 0.35f, 0.45f);
        public static Material Leaf => Pbr("w_leaf", new Color(0.155f, 0.320f, 0.135f), 0f, 0.34f);
        public static Material LeafLight => Pbr("w_leaf2", new Color(0.245f, 0.430f, 0.180f), 0f, 0.34f);
        public static Material Soil => Pbr("w_soil", new Color(0.110f, 0.082f, 0.062f), 0f, 0.06f);
        public static Material PotCeramic => Pbr("w_pot", new Color(0.72f, 0.62f, 0.48f), 0.02f, 0.50f);
        public static Material PotConcrete => Pbr("w_pot_c", new Color(0.50f, 0.47f, 0.43f), 0f, 0.24f);
        public static Material Paper => Pbr("w_paper", new Color(0.965f, 0.955f, 0.930f), 0f, 0.30f);

        public static Material ThermalRamp => Pbr("w_thermal", Color.white, 0f, 0.05f,
            Tex.Thermal(), null, Vector2.one);

        public static Material GlowSprite => Pbr("w_glow", Color.white, 0f, 0f,
            Tex.Glow(), null, Vector2.one, Color.white);

        public static void ClearCache() => Cache.Clear();
    }
}
