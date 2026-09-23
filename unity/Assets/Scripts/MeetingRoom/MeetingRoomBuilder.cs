using UnityEngine;
using UnityEngine.Rendering;
using UnityEngine.Rendering.Universal;

namespace SmartRoom
{
    /// <summary>
    /// 场景组装器：灯光、相机、后处理、全部模型与控制器。可在运行时调用（编辑器脚本也用它）。
    /// </summary>
    public static class MeetingRoomBuilder
    {
        public const string SceneName = "MeetingRoom";

        public static void BuildAll()
        {
            MatLib.ClearAll();

            SetupRenderSettings();
            SetupLights();
            var vol = SetupPostProcessing();

            var shell = RoomShell.Build();
            RoomShell.FinalizeShell(shell);
            Furniture.Build();
            BuildDecorations();

            // 设备要放在最后建：这样 Devices 内部收尾时能一次性清掉所有命中盒的渲染体
            var refs = Devices.Build();

            var cam = SetupCamera(out var orbit);
            var room = SetupController(refs, orbit, cam);

            Debug.Log("[MeetingRoom] 场景构建完成，可交互设备 " + room.devices.Count + " 个，Volume=" + (vol != null));
        }

        // ------------------------------------------------------------------ 渲染环境

        static void SetupRenderSettings()
        {
            // 用程序化天空，窗外的光感更自然
            RenderSettings.skybox = MakeSkyMaterial();
            RenderSettings.ambientMode = AmbientMode.Trilight;
            RenderSettings.ambientSkyColor = new Color(0.420f, 0.470f, 0.550f);
            RenderSettings.ambientEquatorColor = new Color(0.280f, 0.300f, 0.335f);
            RenderSettings.ambientGroundColor = new Color(0.130f, 0.125f, 0.120f);
            RenderSettings.ambientIntensity = 1.55f;
            RenderSettings.fog = false;
            RenderSettings.defaultReflectionMode = DefaultReflectionMode.Skybox;
            RenderSettings.reflectionIntensity = 0.65f;
        }

        static Material MakeSkyMaterial()
        {
            var shader = Shader.Find("Skybox/Procedural");
            if (shader == null) return null;
            var m = new Material(shader) { name = "M_Sky" };
            m.SetFloat("_SunSize", 0.035f);
            m.SetFloat("_SunSizeConvergence", 4f);
            m.SetFloat("_AtmosphereThickness", 0.85f);
            m.SetColor("_SkyTint", new Color(0.52f, 0.60f, 0.72f));
            m.SetColor("_GroundColor", new Color(0.28f, 0.28f, 0.28f));
            m.SetFloat("_Exposure", 1.25f);
            return m;
        }

        // ------------------------------------------------------------------ 灯光

        static void SetupLights()
        {
            var root = new GameObject("04_灯光");

            // 主方向光：模拟窗外日光，投软阴影
            var sun = new GameObject("日光");
            sun.transform.SetParent(root.transform, false);
            sun.transform.rotation = Quaternion.Euler(42f, 205f, 0f);
            var sl = sun.AddComponent<Light>();
            sl.type = LightType.Directional;
            sl.color = new Color(1f, 0.965f, 0.905f);
            sl.intensity = 2.10f;
            sl.shadows = LightShadows.Soft;
            sl.shadowStrength = 0.52f;
            sl.shadowBias = 0.08f;
            sl.shadowNormalBias = 0.35f;
            sl.shadowNearPlane = 0.2f;
            sl.renderMode = LightRenderMode.ForcePixel;

            // 补光：从窗侧来的冷色天光，压低阴影死黑
            var fill = new GameObject("天光补光");
            fill.transform.SetParent(root.transform, false);
            fill.transform.rotation = Quaternion.Euler(28f, 20f, 0f);
            var fl = fill.AddComponent<Light>();
            fl.type = LightType.Directional;
            fl.color = new Color(0.74f, 0.83f, 0.98f);
            fl.intensity = 1.05f;
            fl.shadows = LightShadows.None;
            fl.renderMode = LightRenderMode.ForceVertex;

            // 角落落地灯的实际光源
            var lamp = new GameObject("落地灯光源");
            lamp.transform.SetParent(root.transform, false);
            lamp.transform.position = new Vector3(3.35f, 1.42f, 2.45f);
            var ll = lamp.AddComponent<Light>();
            ll.type = LightType.Point;
            ll.color = new Color(1f, 0.92f, 0.80f);
            ll.intensity = 1.1f;
            ll.range = 3.4f;
            ll.shadows = LightShadows.None;
            ll.renderMode = LightRenderMode.ForceVertex;
        }

        // ------------------------------------------------------------------ 后处理

        static Volume SetupPostProcessing()
        {
            var go = new GameObject("05_后处理 Volume");
            var vol = go.AddComponent<Volume>();
            vol.isGlobal = true;
            vol.priority = 10f;

            var profile = ScriptableObject.CreateInstance<VolumeProfile>();
            profile.name = "MeetingRoomProfile";
            vol.sharedProfile = profile;

            // ACES 色调映射 + 轻微曝光提升，画面更"贵"
            var tone = profile.Add<Tonemapping>(true);
            tone.mode.overrideState = true;
            tone.mode.value = TonemappingMode.ACES;
            var exposure = profile.Add<ColorAdjustments>(true);
            exposure.postExposure.overrideState = true;
            exposure.postExposure.value = 0.12f;
            exposure.contrast.overrideState = true;
            exposure.contrast.value = 8f;
            exposure.saturation.overrideState = true;
            exposure.saturation.value = 6f;

            var bloom = profile.Add<Bloom>(true);
            bloom.threshold.overrideState = true;
            bloom.threshold.value = 1.05f;
            bloom.intensity.overrideState = true;
            bloom.intensity.value = 0.55f;
            bloom.scatter.overrideState = true;
            bloom.scatter.value = 0.62f;
            bloom.tint.overrideState = true;
            bloom.tint.value = new Color(1f, 0.97f, 0.94f);

            var vig = profile.Add<Vignette>(true);
            vig.intensity.overrideState = true;
            vig.intensity.value = 0.16f;
            vig.smoothness.overrideState = true;
            vig.smoothness.value = 0.5f;

            var wb = profile.Add<WhiteBalance>(true);
            wb.temperature.overrideState = true;
            wb.temperature.value = 6f;
            wb.tint.overrideState = true;
            wb.tint.value = 2f;

            // 抗锯齿交给 Renderer 资产；这里不叠加 SMAA 以便性能可控
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
            cam.depth = 0f;

            var data = go.AddComponent<UniversalAdditionalCameraData>();
            data.renderPostProcessing = true;
            data.antialiasing = AntialiasingMode.SubpixelMorphologicalAntiAliasing;
            data.antialiasingQuality = AntialiasingQuality.High;
            data.renderShadows = true;

            orbit = go.AddComponent<OrbitCamera>();
            orbit.pivot = RoomDim.OrbitPivot;
            orbit.distance = RoomDim.OrbitDistance;
            orbit.yaw = RoomDim.OrbitYaw;
            orbit.pitch = RoomDim.OrbitPitch;

            // 立刻摆到位（避免第一帧在错误位置）
            var rot = Quaternion.Euler(orbit.pitch, orbit.yaw, 0f);
            var pos = orbit.pivot - rot * Vector3.forward * orbit.distance;
            if (pos.y < orbit.minHeight) pos.y = orbit.minHeight;
            go.transform.position = pos;
            go.transform.rotation = rot;

            return cam;
        }

        // ------------------------------------------------------------------ 控制器

        static RoomController SetupController(Devices.Refs refs, OrbitCamera orbit, Camera cam)
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

        // ------------------------------------------------------------------ 装饰（绿植等）

        static void BuildDecorations()
        {
            var p = new GameObject("02b_装饰绿植").transform;

            // 两株大型落地绿植，放在投影墙两角，不挡视线
            Furniture.BuildPlant(p, "绿植_前左", new Vector3(-3.35f, 0f, -2.35f), 1.25f, 1337);
            Furniture.BuildPlant(p, "绿植_前右", new Vector3(3.35f, 0f, 2.30f), 1.05f, 4242);
            // 会议桌两端靠近角落的小绿植
            Furniture.BuildPlant(p, "绿植_后左", new Vector3(-3.45f, 0f, 2.40f), 0.92f, 909);
        }
    }
}
