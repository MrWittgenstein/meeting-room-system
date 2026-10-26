using System.IO;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;
using UnityEngine.SceneManagement;
using WarmRoom;

namespace WarmRoom.EditorTools
{
    /// <summary>
    /// 编辑器入口：生成"暖色木质商务风"会议室场景、出效果图。
    /// 菜单同时注册中英文两套（编辑器语言为英文时中文菜单可能不显示）。
    /// </summary>
    public static class Wizard
    {
        const string ScenePath = "Assets/Scenes/WarmMeetingRoom.unity";
        const string ShotDir = "Assets/Screenshots";

        // ================================================================== 菜单

        [MenuItem("Tools/Warm Meeting Room/1. Generate Scene", false, 10)]
        [MenuItem("暖木会议室/生成场景", false, 10)]
        public static void GenerateScene()
        {
            if (EditorApplication.isPlayingOrWillChangePlaymode)
            {
                EditorUtility.DisplayDialog("Please exit Play mode",
                    "Stop Play mode (the ▶ button) first, then run this menu again.", "OK");
                return;
            }
            if (!EditorUtility.DisplayDialog("Generate Warm Meeting Room",
                "This will create (and overwrite):\n" + ScenePath + "\n\nContinue?", "Generate", "Cancel"))
                return;

            var scene = BuildInternal();
            EditorUtility.DisplayDialog("Done",
                "Scene generated:\n" + ScenePath + "\n\nPress ▶ and click devices with the mouse.", "OK");
            Selection.activeGameObject = null;
            EditorSceneManager.MarkSceneDirty(scene);
        }

        [MenuItem("Tools/Warm Meeting Room/2. Generate Scene + Screenshots", false, 11)]
        [MenuItem("暖木会议室/生成场景并出图", false, 11)]
        public static void GenerateAndShoot()
        {
            if (EditorApplication.isPlayingOrWillChangePlaymode)
            {
                EditorUtility.DisplayDialog("Please exit Play mode", "Stop Play mode first.", "OK");
                return;
            }
            BuildInternal();
            CaptureShots();
            EditorUtility.DisplayDialog("Done", "Scene + screenshots generated.", "OK");
        }

        [MenuItem("Tools/Warm Meeting Room/3. Screenshots Only", false, 12)]
        [MenuItem("暖木会议室/仅出图", false, 12)]
        public static void ShootOnly()
        {
            if (EditorApplication.isPlayingOrWillChangePlaymode)
            {
                EditorUtility.DisplayDialog("Please exit Play mode", "Stop Play mode first.", "OK");
                return;
            }
            if (!File.Exists(ScenePath)) { EditorSceneManager.OpenScene(ScenePath); }
            CaptureShots();
        }

        // ================================================================== 核心

        public static Scene BuildInternal()
        {
            EnsureFolder("Assets/Scenes");
            AssetDatabase.DeleteAsset(ScenePath);
            AssetDatabase.Refresh();

            var scene = EditorSceneManager.NewScene(NewSceneSetup.EmptyScene, NewSceneMode.Single);

            Builder.BuildAll();
            MarkStatics();
            BakeLighting();
            Stamp();

            EditorSceneManager.SaveScene(scene, ScenePath);
            AssetDatabase.SaveAssets();
            AssetDatabase.Refresh();
            Debug.Log("[WarmRoom] 场景已保存：" + ScenePath);
            return scene;
        }

        /// <summary>
        /// 标记静态物（开启静态合批）。
        ///
        /// **关键：会动的物体绝对不能打 BatchingStatic。**
        /// 静态合批会把顶点烘进世界空间的合并网格，之后 Transform 再变也不会重新渲染——
        /// 上一个项目就是这样导致"门扇角度从 0° 变成 90°、画面却纹丝不动"。
        /// 这里显式排除门扇、窗帘、设备命中盒及其子树。
        /// </summary>
        static void MarkStatics()
        {
            var flags = StaticEditorFlags.BatchingStatic | StaticEditorFlags.OccludeeStatic
                      | StaticEditorFlags.ContributeGI | StaticEditorFlags.ReflectionProbeStatic;

            // 可动部件（含其子树一律不标记）
            var movablePrefix = new[] { "门扇", "窗帘A_", "窗帘B_", "纱帘", "设备_", "选中描边" };

            foreach (var rootName in new[] { Shell.Root, Furn.Root, "02b_装饰绿植", "04_灯光" })
            {
                var go = GameObject.Find(rootName);
                if (go == null) continue;

                foreach (var t in go.GetComponentsInChildren<Transform>(true))
                {
                    bool skip = false;
                    for (var cur = t; cur != null && cur != go.transform; cur = cur.parent)
                    {
                        foreach (var pfx in movablePrefix)
                            if (cur.name.StartsWith(pfx)) { skip = true; break; }
                        if (skip) break;
                    }
                    GameObjectUtility.SetStaticEditorFlags(t.gameObject, skip ? 0 : flags);
                }
            }
        }

        static void BakeLighting()
        {
            try
            {
                var settings = new LightingSettings
                {
                    name = "WarmRoomLighting",
                    bakedGI = true,
                    realtimeGI = false,
                    lightmapper = LightingSettings.Lightmapper.ProgressiveCPU,
                    lightmapResolution = 20f,
                    lightmapPadding = 2,
                    lightmapMaxSize = 1024,
                    ao = true,
                    aoMaxDistance = 1.1f,
                    directSampleCount = 24,
                    indirectSampleCount = 128,
                    environmentSampleCount = 96,
                    bounces = 2,
                };
                EnsureFolder("Assets/Settings");
                const string p = "Assets/Settings/WarmRoomLighting.asset";
                AssetDatabase.DeleteAsset(p);
                AssetDatabase.CreateAsset(settings, p);
                Lightmapping.lightingSettings = AssetDatabase.LoadAssetAtPath<LightingSettings>(p);

                if (!Lightmapping.BakeAsync())
                {
                    Debug.LogWarning("[WarmRoom] 光照烘焙无法启动，已跳过（实时灯光不受影响）");
                    return;
                }
                var sw = System.Diagnostics.Stopwatch.StartNew();
                const double timeout = 150.0;
                while (Lightmapping.isRunning && sw.Elapsed.TotalSeconds < timeout)
                    System.Threading.Thread.Sleep(200);
                if (Lightmapping.isRunning) { Lightmapping.Cancel(); Debug.LogWarning("[WarmRoom] 烘焙超时已取消"); }
                else Debug.Log("[WarmRoom] 光照烘焙完成 " + sw.Elapsed.TotalSeconds.ToString("0.0") + "s");
            }
            catch (System.Exception e)
            {
                Debug.LogWarning("[WarmRoom] 光照烘焙跳过：" + e.Message);
            }
        }

        /// <summary>写构建戳：记录时间、设备清单、关键模型自检，并做脚本引用自检。</summary>
        static void Stamp()
        {
            var sb = new System.Text.StringBuilder();
            sb.AppendLine("buildTime=" + System.DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"));
            sb.AppendLine("scene=" + ScenePath);

            var ids = new System.Collections.Generic.List<string>();
#if UNITY_2023_1_OR_NEWER
            var devs = Object.FindObjectsByType<DeviceController>(FindObjectsSortMode.None);
#else
            var devs = Object.FindObjectsOfType<DeviceController>();
#endif
            foreach (var d in devs) ids.Add(d.id + "(" + d.displayName + ")");
            ids.Sort();
            sb.AppendLine("deviceCount=" + ids.Count);
            sb.AppendLine("devices=" + string.Join(", ", ids));

            // 关键模型自检
            sb.AppendLine("hasDoorLeaf=" + (GameObject.Find("门扇") != null));
            sb.AppendLine("hasCurtainA_L=" + (GameObject.Find("窗帘A_左") != null));
            sb.AppendLine("hasCurtainA_R=" + (GameObject.Find("窗帘A_右") != null));
            sb.AppendLine("hasCurtainB_L=" + (GameObject.Find("窗帘B_左") != null));
            sb.AppendLine("hasCurtainB_R=" + (GameObject.Find("窗帘B_右") != null));
            sb.AppendLine("hasFloorAc=" + (GameObject.Find("空调_柜体") != null));

            // 脚本引用自检：统计解析失败的组件（missing script 的直接证据）
            int broken = 0, ok = 0;
            foreach (var go in Object.FindObjectsByType<GameObject>(FindObjectsSortMode.None))
                foreach (var c in go.GetComponents<Component>())
                    if (c == null) broken++; else if (c is MonoBehaviour) ok++;
            sb.AppendLine("scriptComponents_ok=" + ok);
            sb.AppendLine("scriptComponents_missing=" + broken);

            // 可动部件是否误打静态标记（会导致动画失效）
            foreach (var n in new[] { "门扇", "窗帘A_左", "窗帘B_右" })
            {
                var go = GameObject.Find(n);
                if (go == null) continue;
                var f = GameObjectUtility.GetStaticEditorFlags(go);
                sb.AppendLine("staticFlags_" + n + "=" + (int)f + (f == 0 ? " (OK, 可动)" : " (!! 不应为静态)"));
            }

            var text = sb.ToString();
            new GameObject("__BUILD_" + System.DateTime.Now.ToString("MMdd_HHmm") + "__")
                .transform.position = new Vector3(0f, Dim.Height + 1.2f, 0f);

            Debug.Log("[WarmRoom] 构建戳：\n" + text);
            File.WriteAllText(Path.Combine(Application.dataPath, "../dsh_warm_build.txt"), text);
        }

        // ================================================================== 出图

        static void CaptureShots()
        {
            var cam = Camera.main;
            if (cam == null) { Debug.LogError("找不到主相机"); return; }
            var orbit = cam.GetComponent<OrbitCamera>();
            var room = Object.FindFirstObjectByType<RoomController>();
            EnsureFolder(ShotDir);

            const int W = 1920, H = 1080;
            var rt = new RenderTexture(W, H, 24, RenderTextureFormat.ARGB32) { antiAliasing = 4 };
            var tex = new Texture2D(W, H, TextureFormat.RGB24, false);

            var shots = new (string name, Vector3 eye, Vector3 target, System.Action<RoomController> tweak)[]
            {
                ("01_全景",        new Vector3(3.55f, 2.45f, 2.80f),  new Vector3(-0.5f, 1.05f, -0.30f), r => Reset(r)),
                ("02_主景墙",      new Vector3(-0.10f, 1.60f, 2.30f), new Vector3(0.10f, 1.55f, -3f),   r => Reset(r)),
                ("03_斜俯视布局",  new Vector3(3.40f, 2.90f, 2.85f),  new Vector3(-0.2f, 0.55f, 0.10f), r => Reset(r)),
                ("04_入口与门",    new Vector3(1.10f, 1.50f, -0.30f), new Vector3(4f, 1.25f, 0f),       r => { Reset(r); if (r != null) r.SetDeviceOn("door", true); }),
                ("05_窗帘全关", new Vector3(0.35f, 1.50f, 0.10f), new Vector3(-4f, 1.55f, 0.20f),  r => { Reset(r); if (r != null) { foreach (var id in new[]{"curtainA","curtainB"}) { r.SetDeviceOn(id, true); r.SetDeviceValue(id, 0f); } r.SendMessage("ApplyCurtainScaleAll", SendMessageOptions.DontRequireReceiver); } }),
                ("05b_窗帘全开", new Vector3(0.35f, 1.50f, 0.10f), new Vector3(-4f, 1.55f, 0.20f),  r => { Reset(r); if (r != null) { foreach (var id in new[]{"curtainA","curtainB"}) { r.SetDeviceOn(id, true); r.SetDeviceValue(id, 1f); } r.SendMessage("ApplyCurtainScaleAll", SendMessageOptions.DontRequireReceiver); } }),
                ("06_会议桌细节",  new Vector3(-1.35f, 1.20f, 1.35f), new Vector3(0.35f, 0.85f, -0.20f), r => Reset(r)),
                ("07_落地空调",    new Vector3(1.15f, 1.45f, 0.10f),  new Vector3(-3.54f, 1.05f, 2.30f), r => Reset(r)),
                ("08_环境数据叠加", new Vector3(3.20f, 2.30f, 2.50f),  new Vector3(-0.4f, 0.80f, 0.30f), r => { Reset(r); if (r != null) r.showThermalOverlay = true; }),
                ("09_吊灯近景",    new Vector3(0.20f, 1.90f, 1.90f),  new Vector3(-0.10f, 2.35f, 0f),   r => Reset(r)),
            };

            var savedFov = cam.fieldOfView;
            cam.fieldOfView = 36f;

            foreach (var s in shots)
            {
                s.tweak?.Invoke(room);
                if (room != null) { room.ApplyVisuals(); room.SendMessage("Animate", SendMessageOptions.DontRequireReceiver); room.SendMessage("UpdateFields", 0.016f, SendMessageOptions.DontRequireReceiver); }

                var dir = s.target - s.eye;
                cam.transform.position = s.eye;
                cam.transform.rotation = Quaternion.LookRotation(dir.sqrMagnitude > 1e-4f ? dir.normalized : Vector3.forward, Vector3.up);

                cam.targetTexture = rt;
                cam.Render();
                RenderTexture.active = rt;
                tex.ReadPixels(new Rect(0, 0, W, H), 0, 0);
                tex.Apply();
                cam.targetTexture = null;
                RenderTexture.active = null;

                var path = ShotDir + "/" + s.name + ".png";
                File.WriteAllBytes(path, tex.EncodeToPNG());
                Debug.Log("[WarmRoom] 已出图 " + path);
            }

            cam.fieldOfView = savedFov;
            if (orbit != null)
            {
                orbit.SnapTo(Dim.CamYaw, Dim.CamPitch, Dim.CamDist, Dim.Pivot0);
                var rot = Quaternion.Euler(Dim.CamPitch, Dim.CamYaw, 0f);
                cam.transform.rotation = rot;
                cam.transform.position = Dim.Pivot0 - rot * Vector3.forward * Dim.CamDist;
            }

            CaptureFloorPlan(rt, tex, W, H);
            Object.DestroyImmediate(rt);
            Object.DestroyImmediate(tex);
            AssetDatabase.Refresh();
        }

        static void Reset(RoomController r)
        {
            if (r == null) return;
            r.showThermalOverlay = false; r.showLuxField = false; r.showSmokeField = false;
            var l = r.Get("mainlight"); if (l != null) { l.on = true; l.value = 1f; }
            var a = r.Get("ac"); if (a != null) { a.on = true; a.value = 24f; }
            var d = r.Get("door"); if (d != null) { d.on = false; }
            foreach (var id in new[] { "curtainA", "curtainB" })
            {
                var c = r.Get(id);
                if (c != null) { c.on = true; c.value = 1f; }
            }
        }

        static void CaptureFloorPlan(RenderTexture rt, Texture2D tex, int W, int H)
        {
            var cam = Camera.main;
            if (cam == null) return;
            GameObject temp = null;
            var hidden = new System.Collections.Generic.List<Renderer>();
            try
            {
                var shell = GameObject.Find(Shell.Root);
                if (shell != null)
                {
                    foreach (var t in shell.GetComponentsInChildren<Transform>(true))
                    {
                        var n = t.name;
                        if (n.StartsWith("吊顶") || n.StartsWith("灯槽") || n.StartsWith("跌级")
                            || n.StartsWith("筒灯") || n.StartsWith("窗帘盒") || n.StartsWith("烟雾探测器"))
                            foreach (var r in t.GetComponents<Renderer>())
                                if (r.enabled) { r.enabled = false; hidden.Add(r); }
                    }
                }

                temp = new GameObject("__顶视相机");
                var oc = temp.AddComponent<Camera>();
                oc.CopyFrom(cam);
                oc.orthographic = true;
                oc.orthographicSize = 4.35f;
                oc.aspect = (float)W / H;
                oc.transform.position = new Vector3(0f, 14f, -0.35f);
                oc.transform.rotation = Quaternion.Euler(90f, 0f, 0f);
                oc.nearClipPlane = 0.1f; oc.farClipPlane = 30f;
                oc.clearFlags = CameraClearFlags.SolidColor;
                oc.backgroundColor = new Color(0.06f, 0.05f, 0.04f, 1f);
                var data = temp.GetComponent<UnityEngine.Rendering.Universal.UniversalAdditionalCameraData>();
                if (data == null) data = temp.AddComponent<UnityEngine.Rendering.Universal.UniversalAdditionalCameraData>();
                data.renderPostProcessing = true;

                cam.enabled = false;
                oc.targetTexture = rt;
                oc.Render();
                cam.enabled = true;

                RenderTexture.active = rt;
                tex.ReadPixels(new Rect(0, 0, W, H), 0, 0);
                tex.Apply();
                oc.targetTexture = null;
                RenderTexture.active = null;

                var path = ShotDir + "/10_平面布局图.png";
                File.WriteAllBytes(path, tex.EncodeToPNG());
                Debug.Log("[WarmRoom] 已出图 " + path);
            }
            catch (System.Exception e)
            {
                Debug.LogWarning("[WarmRoom] 平面图失败：" + e.Message);
                cam.enabled = true;
            }
            finally
            {
                if (temp != null) Object.DestroyImmediate(temp);
                foreach (var r in hidden) if (r != null) r.enabled = true;
            }
        }

        // ================================================================== 命令行

        public static void BuildFromCommandLine()
        {
            try
            {
                BuildInternal();
                CaptureShots();
                File.WriteAllText(Path.Combine(Application.dataPath, "../dsh_warm_ok.txt"),
                    "OK " + ScenePath);
                Debug.Log("[WarmRoom] 命令行构建成功");
            }
            catch (System.Exception e)
            {
                File.WriteAllText(Path.Combine(Application.dataPath, "../dsh_warm_err.txt"), e.ToString());
                Debug.LogError("[WarmRoom] 命令行构建失败：" + e);
            }
        }

        static void EnsureFolder(string path)
        {
            if (AssetDatabase.IsValidFolder(path)) return;
            var parent = Path.GetDirectoryName(path).Replace('\\', '/');
            var leaf = Path.GetFileName(path);
            if (!string.IsNullOrEmpty(parent) && !AssetDatabase.IsValidFolder(parent)) EnsureFolder(parent);
            AssetDatabase.CreateFolder(parent, leaf);
        }
    }
}
