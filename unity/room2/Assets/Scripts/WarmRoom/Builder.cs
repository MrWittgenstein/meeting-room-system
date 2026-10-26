using UnityEngine;
using UnityEngine.Rendering;
using UnityEngine.Rendering.Universal;

namespace WarmRoom
{
    /// <summary>场景组装：灯光、相机、后处理、模型与控制器。</summary>
    public static class Builder
    {
        public static void BuildAll()
        {
            Mat.ClearCache();

            SetupRender();
            SetupLights();
            var vol = SetupPost();

            Shell.Build();
            Furn.Build();
            BuildDecor();

            var refs = Dev.Build();

            var cam = SetupCamera(out var orbit);
            var room = SetupRuntime(refs, orbit, cam);

            Debug.Log("[WarmRoom] 场景构建完成，可交互设备 " + room.devices.Count + " 个，Volume=" + (vol != null));
        }

        // ------------------------------------------------------------------ 渲染环境

        static void SetupRender()
        {
            RenderSettings.skybox = MakeSky();
            RenderSettings.ambientMode = AmbientMode.Trilight;
            // 暖色间接光（木质风的氛围主要靠环境光偏暖）
            RenderSettings.ambientSkyColor = new Color(0.470f, 0.420f, 0.340f);
            RenderSettings.ambientEquatorColor = new Color(0.320f, 0.278f, 0.225f);
            RenderSettings.ambientGroundColor = new Color(0.150f, 0.125f, 0.098f);
            RenderSettings.ambientIntensity = 1.35f;
            RenderSettings.fog = false;
            RenderSettings.defaultReflectionMode = DefaultReflectionMode.Skybox;
            RenderSettings.reflectionIntensity = 0.55f;
        }

        static Material MakeSky()
        {
            var sh = Shader.Find("Skybox/Procedural");
            if (sh == null) return null;
            var m = new Material(sh) { name = "M_Sky" };
            m.SetFloat("_SunSize", 0.040f);
            m.SetFloat("_SunSizeConvergence", 4f);
            m.SetFloat("_AtmosphereThickness", 0.95f);
            m.SetColor("_SkyTint", new Color(0.60f, 0.60f, 0.62f));
            m.SetColor("_GroundColor", new Color(0.30f, 0.27f, 0.24f));
            m.SetFloat("_Exposure", 1.20f);
            return m;
        }

        static void SetupLights()
        {
            var root = new GameObject("04_灯光");

            // 主日光：从窗侧（-X）斜射进来，暖白
            var sun = new GameObject("日光");
            sun.transform.SetParent(root.transform, false);
            sun.transform.rotation = Quaternion.Euler(38f, 74f, 0f);
            var sl = sun.AddComponent<Light>();
            sl.type = LightType.Directional;
            sl.color = new Color(1f, 0.940f, 0.855f);
            sl.intensity = 1.85f;
            sl.shadows = LightShadows.Soft;
            sl.shadowStrength = 0.55f;
            sl.shadowBias = 0.08f;
            sl.shadowNormalBias = 0.35f;
            sl.shadowNearPlane = 0.2f;
            sl.renderMode = LightRenderMode.ForcePixel;

            // 补光：偏暖的天光，压低阴影死黑
            var fill = new GameObject("天光补光");
            fill.transform.SetParent(root.transform, false);
            fill.transform.rotation = Quaternion.Euler(26f, -140f, 0f);
            var fl = fill.AddComponent<Light>();
            fl.type = LightType.Directional;
            fl.color = new Color(0.88f, 0.86f, 0.94f);
            fl.intensity = 0.80f;
            fl.shadows = LightShadows.None;
            fl.renderMode = LightRenderMode.ForceVertex;

            // 边柜台面的黄铜台灯（装饰性暖光）
            var lamp = new GameObject("台灯光源");
            lamp.transform.SetParent(root.transform, false);
            lamp.transform.position = new Vector3(-2.40f, 1.02f, Dim.HZ - 0.32f);
            var ll = lamp.AddComponent<Light>();
            ll.type = LightType.Point;
            ll.color = new Color(1f, 0.78f, 0.50f);
            ll.intensity = 1.35f;
            ll.range = 3.2f;
            ll.shadows = LightShadows.None;
            ll.renderMode = LightRenderMode.ForceVertex;

            // 台灯模型
            var l = new GameObject("台灯").transform;
            l.SetParent(root.transform, false);
            l.localPosition = new Vector3(-2.40f, 0.82f, Dim.HZ - 0.32f);
            Prim.Cylinder("台灯_底盘", l, new Vector3(0f, 0.012f, 0f), 0.10f, 0.024f, Mat.Brass);
            Prim.Cylinder("台灯_杆", l, new Vector3(0f, 0.20f, 0f), 0.010f, 0.40f, Mat.Brass);
            Prim.Cylinder("台灯_罩", l, new Vector3(0f, 0.44f, 0f), 0.13f, 0.20f, Mat.Pbr("w_shade", new Color(0.62f, 0.48f, 0.30f), 0f, 0.35f));
            Prim.Disc("台灯_发光", l, new Vector3(0f, 0.345f, 0f), 0.115f,
                Mat.Emissive("w_lamp_glow", new Color(1f, 0.80f, 0.55f), 2.2f), Quaternion.Euler(90f, 0f, 0f));
        }

        static Volume SetupPost()
        {
            var go = new GameObject("05_后处理 Volume");
            var vol = go.AddComponent<Volume>();
            vol.isGlobal = true;
            vol.priority = 10f;

            var profile = ScriptableObject.CreateInstance<VolumeProfile>();
            profile.name = "WarmRoomProfile";
            vol.sharedProfile = profile;

            var tone = profile.Add<Tonemapping>(true);
            tone.mode.overrideState = true;
            tone.mode.value = TonemappingMode.ACES;

            var ca = profile.Add<ColorAdjustments>(true);
            ca.postExposure.overrideState = true; ca.postExposure.value = 0.14f;
            ca.contrast.overrideState = true; ca.contrast.value = 8f;
            ca.saturation.overrideState = true; ca.saturation.value = 10f;

            var wb = profile.Add<WhiteBalance>(true);
            wb.temperature.overrideState = true; wb.temperature.value = 16f;   // 偏暖
            wb.tint.overrideState = true; wb.tint.value = 4f;

            var bloom = profile.Add<Bloom>(true);
            bloom.threshold.overrideState = true; bloom.threshold.value = 1.05f;
            bloom.intensity.overrideState = true; bloom.intensity.value = 0.52f;
            bloom.scatter.overrideState = true; bloom.scatter.value = 0.60f;
            bloom.tint.overrideState = true; bloom.tint.value = new Color(1f, 0.92f, 0.82f);

            var vig = profile.Add<Vignette>(true);
            vig.intensity.overrideState = true; vig.intensity.value = 0.20f;
            vig.smoothness.overrideState = true; vig.smoothness.value = 0.5f;

            return vol;
        }

        // ------------------------------------------------------------------ 相机

        static Camera SetupCamera(out OrbitCamera orbit)
        {
            var root = new GameObject("06_相机");
            var go = new GameObject("主相机");
            go.transform.SetParent(root.transform, false);
            go.tag = "MainCamera";

            var cam = go.AddComponent<Camera>();
            cam.fieldOfView = 52f;
            cam.nearClipPlane = 0.05f;
            cam.farClipPlane = 220f;
            cam.clearFlags = CameraClearFlags.Skybox;
            cam.allowHDR = true;
            cam.allowMSAA = true;

            var data = go.AddComponent<UniversalAdditionalCameraData>();
            data.renderPostProcessing = true;
            data.antialiasing = AntialiasingMode.SubpixelMorphologicalAntiAliasing;
            data.antialiasingQuality = AntialiasingQuality.High;
            data.renderShadows = true;

            orbit = go.AddComponent<OrbitCamera>();
            orbit.pivot = Dim.Pivot0;
            orbit.distance = Dim.CamDist;
            orbit.yaw = Dim.CamYaw;
            orbit.pitch = Dim.CamPitch;

            var rot = Quaternion.Euler(orbit.pitch, orbit.yaw, 0f);
            var pos = orbit.pivot - rot * Vector3.forward * orbit.distance;
            if (pos.y < orbit.minHeight) pos.y = orbit.minHeight;
            go.transform.position = pos;
            go.transform.rotation = rot;
            return cam;
        }

        // ------------------------------------------------------------------ 运行时

        static RoomController SetupRuntime(Dev.Refs refs, OrbitCamera orbit, Camera cam)
        {
            var go = new GameObject("07_会议室控制器");
            var room = go.AddComponent<RoomController>();
            room.refs = refs;
            room.orbit = orbit;
            room.cam = cam;

            var hudGo = new GameObject("08_操作界面");
            var hud = hudGo.AddComponent<RoomHud>();
            hud.room = room;

            room.Initialize();
            return room;
        }

        // ------------------------------------------------------------------ 装饰绿植

        static void BuildDecor()
        {
            var p = new GameObject("02b_装饰绿植").transform;
            Furn.BuildPlant(p, "绿植_东北角", new Vector3(3.35f, 0f, -2.35f), 1.22f, 2027);
            Furn.BuildPlant(p, "绿植_西南角", new Vector3(-3.45f, 0f, -2.45f), 1.05f, 771);
            Furn.BuildPlant(p, "绿植_西北角", new Vector3(-3.55f, 0f, 1.15f), 0.92f, 313);
        }
    }
}
