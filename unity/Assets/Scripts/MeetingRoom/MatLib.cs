using UnityEngine;
using UnityEngine.Rendering;

namespace SmartRoom
{
    /// <summary>
    /// 程序化 PBR 材质库（URP Lit）。所有材质由代码创建并缓存，不依赖外部资源。
    /// </summary>
    public static class MatLib
    {
        static readonly System.Collections.Generic.Dictionary<string, Material> Cache =
            new System.Collections.Generic.Dictionary<string, Material>();

        public static readonly int BaseColorId = Shader.PropertyToID("_BaseColor");
        public static readonly int BaseMapId = Shader.PropertyToID("_BaseMap");
        public static readonly int BumpMapId = Shader.PropertyToID("_BumpMap");
        public static readonly int SmoothnessId = Shader.PropertyToID("_Smoothness");
        public static readonly int MetallicId = Shader.PropertyToID("_Metallic");
        public static readonly int EmissionColorId = Shader.PropertyToID("_EmissionColor");

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

        /// <summary>创建一个 URP Lit 材质。</summary>
        public static Material Mat(string key, Color color, float metallic, float smoothness,
            Texture2D albedo = null, Texture2D normal = null, Vector2 tiling = default,
            Color? emission = null)
        {
            if (Cache.TryGetValue(key, out var cached) && cached != null) return cached;

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
                m.SetColor(EmissionColorId, emission.Value);
                m.globalIlluminationFlags = MaterialGlobalIlluminationFlags.RealtimeEmissive;
            }
            Cache[key] = m;
            return m;
        }

        /// <summary>创建透明材质（玻璃等）。</summary>
        public static Material Transparent(string key, Color color, float smoothness, float alpha)
        {
            if (Cache.TryGetValue(key, out var cached) && cached != null) return cached;

            var m = new Material(Lit) { name = "M_" + key };
            color.a = alpha;
            m.SetColor(BaseColorId, color);
            m.SetFloat(SmoothnessId, smoothness);
            m.SetFloat(MetallicId, 0.05f);

            m.SetFloat("_Surface", 1f);          // Transparent
            m.SetFloat("_Blend", 0f);            // Alpha
            m.SetFloat("_AlphaClip", 0f);
            m.SetFloat("_ZWrite", 0f);
            m.SetFloat("_SrcBlend", (float)BlendMode.SrcAlpha);
            m.SetFloat("_DstBlend", (float)BlendMode.OneMinusSrcAlpha);
            m.EnableKeyword("_SURFACE_TYPE_TRANSPARENT");
            m.EnableKeyword("_ALPHAPREMULTIPLY_ON");
            m.DisableKeyword("_ALPHATEST_ON");
            m.renderQueue = (int)RenderQueue.Transparent;
            m.SetShaderPassEnabled("ShadowCaster", false);

            Cache[key] = m;
            return m;
        }

        // ------------------------------------------------------------ 场景材质实例

        public static Material Carpet => Mat("m_carpet", new Color(0.395f, 0.415f, 0.450f), 0f, 0.04f,
            TextureLib.Carpet(), TextureLib.CarpetNormal(), new Vector2(8f, 6f));

        public static Material Wall => Mat("m_wall", new Color(0.905f, 0.898f, 0.872f), 0f, 0.10f,
            TextureLib.Plaster(), TextureLib.PlasterNormal(), new Vector2(4f, 2f));

        public static Material WallAccent => Mat("m_wall_accent", new Color(0.235f, 0.255f, 0.290f), 0.02f, 0.22f,
            TextureLib.Plaster(), TextureLib.PlasterNormal(), new Vector2(4f, 2f));

        public static Material CeilingMat => Mat("m_ceiling", new Color(0.945f, 0.945f, 0.940f), 0f, 0.28f);
        public static Material CeilingGrid => Mat("m_ceiling_grid", new Color(0.815f, 0.808f, 0.795f), 0f, 0.16f);
        public static Material CeilingVoid => Mat("m_ceiling_void", new Color(0.16f, 0.17f, 0.18f), 0f, 0.05f);

        public static Material Walnut => Mat("m_walnut", new Color(0.86f, 0.82f, 0.78f), 0.02f, 0.42f,
            TextureLib.Walnut(), TextureLib.WalnutNormal(), new Vector2(0.9f, 0.9f));

        public static Material WalnutDark => Mat("m_walnut_dark", new Color(0.60f, 0.58f, 0.56f), 0.02f, 0.35f,
            TextureLib.Walnut(), TextureLib.WalnutNormal(), new Vector2(0.9f, 0.9f));

        public static Material OakFloor => Mat("m_oak", new Color(0.94f, 0.90f, 0.86f), 0.02f, 0.30f,
            TextureLib.OakPlank(), TextureLib.OakNormal(), new Vector2(4f, 4f));

        public static Material BlackMesh => Mat("m_mesh", new Color(0.30f, 0.31f, 0.34f), 0.05f, 0.18f,
            TextureLib.MeshFabric(), TextureLib.MeshFabricNormal(), new Vector2(3f, 3f));

        public static Material SeatPad => Mat("m_seatpad", new Color(0.115f, 0.120f, 0.135f), 0f, 0.22f,
            TextureLib.MeshFabric(), TextureLib.MeshFabricNormal(), new Vector2(2f, 2f));

        public static Material Curtain => Mat("m_curtain", new Color(0.78f, 0.79f, 0.82f), 0f, 0.26f,
            TextureLib.CurtainFabric(), TextureLib.CurtainNormal(), new Vector2(1.6f, 1f));

        public static Material Sheer => Transparent("m_sheer", new Color(0.94f, 0.95f, 0.97f), 0.35f, 0.30f);

        public static Material GlassWindow => Transparent("m_glass_window", new Color(0.86f, 0.92f, 0.96f), 0.94f, 0.09f);
        public static Material GlassPartition => Transparent("m_glass_part", new Color(0.86f, 0.91f, 0.94f), 0.92f, 0.13f);
        public static Material GlassDoor => Transparent("m_glass_door", new Color(0.88f, 0.93f, 0.96f), 0.92f, 0.17f);

        public static Material Metal => Mat("m_metal", new Color(0.72f, 0.73f, 0.75f), 0.92f, 0.62f,
            TextureLib.BrushedMetal(), null, new Vector2(1f, 3f));

        public static Material MetalDark => Mat("m_metal_dark", new Color(0.30f, 0.31f, 0.33f), 0.88f, 0.48f,
            TextureLib.BrushedMetal(), null, new Vector2(1f, 3f));

        public static Material Chrome => Mat("m_chrome", new Color(0.86f, 0.87f, 0.89f), 1f, 0.85f);
        public static Material Plastic => Mat("m_plastic", new Color(0.135f, 0.140f, 0.150f), 0f, 0.42f);
        public static Material PlasticWhite => Mat("m_plastic_white", new Color(0.885f, 0.888f, 0.895f), 0f, 0.38f);
        public static Material Rubber => Mat("m_rubber", new Color(0.06f, 0.062f, 0.068f), 0f, 0.10f);

        public static Material Stone => Mat("m_stone", new Color(0.86f, 0.85f, 0.83f), 0.03f, 0.44f,
            TextureLib.Stone(), null, new Vector2(3f, 1f));

        public static Material ScreenCanvas => Mat("m_screen_canvas", new Color(0.96f, 0.96f, 0.96f), 0f, 0.22f,
            TextureLib.ScreenCanvas(), null, new Vector2(3f, 2f));

        public static Material ScreenHousing => Mat("m_screen_housing", new Color(0.80f, 0.80f, 0.82f), 0.30f, 0.45f);
        public static Material Whiteboard => Mat("m_whiteboard", new Color(0.955f, 0.960f, 0.960f), 0f, 0.72f);
        public static Material WhiteboardFrame => Mat("m_wb_frame", new Color(0.62f, 0.63f, 0.64f), 0.65f, 0.50f);

        public static Material Leaf => Mat("m_leaf", new Color(0.145f, 0.330f, 0.140f), 0f, 0.34f);
        public static Material LeafLight => Mat("m_leaf2", new Color(0.235f, 0.440f, 0.185f), 0f, 0.34f);
        public static Material Soil => Mat("m_soil", new Color(0.115f, 0.085f, 0.065f), 0f, 0.06f);
        public static Material PotCeramic => Mat("m_pot", new Color(0.80f, 0.78f, 0.74f), 0.02f, 0.50f);
        public static Material PotConcrete => Mat("m_pot_c", new Color(0.545f, 0.540f, 0.525f), 0f, 0.24f);

        public static Material PanelGlass => Mat("m_panel_glass", new Color(0.75f, 0.82f, 0.88f), 0.10f, 0.55f,
            TextureLib.PanelGlass(), null, Vector2.one, new Color(0.16f, 0.42f, 0.55f));

        public static Material FrostedGlass => Mat("m_frosted", new Color(0.86f, 0.90f, 0.92f), 0.05f, 0.62f,
            TextureLib.FrostedGlass(), null, new Vector2(2f, 1f));

        public static Material ThermalRamp => Mat("m_thermal", Color.white, 0f, 0.05f,
            TextureLib.ThermalRamp(), null, Vector2.one);

        public static Material Emissive(string key, Color color, float intensity)
        {
            return Mat(key, Color.white * 0.9f, 0f, 0.30f, null, null, Vector2.one, color * intensity);
        }

        public static Material GlowSprite => Mat("m_glow", new Color(1f, 1f, 1f, 1f), 0f, 0f,
            TextureLib.RadialGlow(), null, Vector2.one, new Color(1f, 1f, 1f));

        /// <summary>把材质缓存清空（重建场景时用，避免残留旧材质）。</summary>
        public static void ClearAll() => Cache.Clear();
    }
}
