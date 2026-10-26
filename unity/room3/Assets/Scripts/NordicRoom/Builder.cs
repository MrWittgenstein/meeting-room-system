using UnityEngine;
using UnityEngine.Rendering;
using UnityEngine.Rendering.Universal;

namespace NordicRoom
{
    /// <summary>场景组装：渲染环境、灯光、后处理、模型、相机、控制器。</summary>
    public static class Builder
    {
        public static void BuildAll()
        {
            Mat.ClearCache();

            SetupRender();
            SetupLights();
            SetupPost();

            Shell.Build();
            Furn.Build();
            var refs = Dev.Build();

            var cam = SetupCamera(out var orbit);
            var room = SetupRuntime(refs, orbit, cam);

            Debug.Log("[NordicRoom] 场景构建完成：可交互设备 " + room.devices.Count + " 个");
        }

        // ------------------------------------------------------------------ 渲染环境（明亮、通透）

        static void SetupRender()
        {
            RenderSettings.skybox = MakeSky();
            RenderSettings.ambientMode = AmbientMode.Trilight;
            // 北欧明亮风：环境光偏冷偏亮，把阴影压得很浅
            RenderSettings.ambientSkyColor = new Color(0.700f, 0.745f, 0.810f);
            RenderSettings.ambientEquatorColor = new Color(0.640f, 0.655f, 0.670f);   // 抬高环境光，避免背光墙发灰
            RenderSettings.ambientGroundColor = new Color(0.400f, 0.395f, 0.375f);
            RenderSettings.ambientIntensity = 1.30f;
            RenderSettings.fog = false;
            RenderSettings.defaultReflectionMode = DefaultReflectionMode.Skybox;
            RenderSettings.reflectionIntensity = 0.72f;
        }

        static Material MakeSky()
        {
            var sh = Shader.Find("Skybox/Procedural");
            if (sh == null) return null;
            var m = new Material(sh) { name = "M_NordicSky" };
            m.SetFloat("_SunSize", 0.038f);
            m.SetFloat("_SunSizeConvergence", 4f);
            m.SetFloat("_AtmosphereThickness", 0.72f);
            m.SetColor("_SkyTint", new Color(0.620f, 0.680f, 0.760f));
            m.SetColor("_GroundColor", new Color(0.360f, 0.355f, 0.335f));
            m.SetFloat("_Exposure", 1.42f);
            return m;
        }

        static void SetupLights()
        {
            var root = new GameObject("04_灯光");

            // 主日光：从南侧角窗斜射进来（偏冷白，北欧日光的特征）
            var sun = new GameObject("日光");
            sun.transform.SetParent(root.transform, false);
            sun.transform.rotation = Quaternion.Euler(48f, 158f, 0f);
            var sl = sun.AddComponent<Light>();
            sl.type = LightType.Directional;
            sl.color = new Color(1f, 0.975f, 0.925f);
            sl.intensity = 1.55f;
            sl.shadows = LightShadows.Soft;
            sl.shadowStrength = 0.42f;
            sl.shadowBias = 0.06f;
            sl.shadowNormalBias = 0.30f;
            sl.shadowNearPlane = 0.2f;
            sl.renderMode = LightRenderMode.ForcePixel;

            // 天光补光：把背光面提亮，符合"明亮"的调性
            var fill = new GameObject("天光补光");
            fill.transform.SetParent(root.transform, false);
            fill.transform.rotation = Quaternion.Euler(30f, -28f, 0f);
            var fl = fill.AddComponent<Light>();
            fl.type = LightType.Directional;
            fl.color = new Color(0.865f, 0.905f, 0.960f);
            fl.intensity = 0.95f;
            fl.shadows = LightShadows.None;
            fl.renderMode = LightRenderMode.ForceVertex;

            // 主吊灯的点光源由 RoomController 创建并联动控制，这里不重复建
        }

        static Volume SetupPost()
        {
            var go = new GameObject("05_后处理 Volume");
            var vol = go.AddComponent<Volume>();
            vol.isGlobal = true;
            vol.priority = 10f;

            var profile = ScriptableObject.CreateInstance<VolumeProfile>();
            profile.name = "NordicRoomProfile";
            vol.sharedProfile = profile;

            var tone = profile.Add<Tonemapping>(true);
            tone.mode.overrideState = true;
            tone.mode.value = TonemappingMode.ACES;

            var ca = profile.Add<ColorAdjustments>(true);
            ca.postExposure.overrideState = true; ca.postExposure.value = 0.08f;   // 明亮但别过曝
            ca.contrast.overrideState = true; ca.contrast.value = 4f;
            ca.saturation.overrideState = true; ca.saturation.value = 4f;

            var wb = profile.Add<WhiteBalance>(true);
            wb.temperature.overrideState = true; wb.temperature.value = -6f;       // 略偏冷
            wb.tint.overrideState = true; wb.tint.value = 0f;

            var bloom = profile.Add<Bloom>(true);
            bloom.threshold.overrideState = true; bloom.threshold.value = 1.45f;
            bloom.intensity.overrideState = true; bloom.intensity.value = 0.16f;
            bloom.scatter.overrideState = true; bloom.scatter.value = 0.58f;
            bloom.tint.overrideState = true; bloom.tint.value = new Color(0.96f, 0.98f, 1f);

            var vig = profile.Add<Vignette>(true);
            vig.intensity.overrideState = true; vig.intensity.value = 0.13f;       // 明亮风：暗角很轻
            vig.smoothness.overrideState = true; vig.smoothness.value = 0.55f;

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
            cam.fieldOfView = 50f;
            cam.nearClipPlane = 0.05f;
            cam.farClipPlane = 240f;
            cam.clearFlags = CameraClearFlags.Skybox;
            cam.allowHDR = true;
            cam.allowMSAA = true;

            var data = go.AddComponent<UniversalAdditionalCameraData>();
            data.renderPostProcessing = true;
            data.antialiasing = AntialiasingMode.SubpixelMorphologicalAntiAliasing;
            data.antialiasingQuality = AntialiasingQuality.High;
            data.renderShadows = true;

            orbit = go.AddComponent<OrbitCamera>();

            // 初始机位与"暖木会议室"同款做法（pivot + distance + yaw + pitch），
            // 只是把数值换算成北欧风房间里的入场视角。
            // 关键：算出来的相机位置必须在**房间内部**，
            // 否则进 Play 第一眼只能看到外墙和吊顶（这个坑踩过）。
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
            room.hud = hud;

            return room;
        }
    }
}
