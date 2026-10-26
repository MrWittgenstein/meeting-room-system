using System.IO;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;
using UnityEngine.Rendering;
using UnityEngine.SceneManagement;
using SmartRoom;

namespace SmartRoom.EditorTools
{
    /// <summary>
    /// 编辑器入口：一键生成会议室场景、批量出图。
    /// 命令行用法：
    ///   Unity.exe -batchmode -nographics -projectPath &lt;路径&gt; -executeMethod SmartRoom.EditorTools.MeetingRoomWizard.BuildFromCommandLine
    /// </summary>
    public static class MeetingRoomWizard
    {
        const string ScenePath = "Assets/Scenes/MeetingRoom.unity";
        const string ShotDir = "Assets/Screenshots";

        // 菜单同时注册中英文两套：编辑器语言为英文时，中文菜单项可能不显示。
        // 英文路径：Tools / Smart Meeting Room / ... 以及 GameObject / Smart Meeting Room / ...
        [MenuItem("Tools/Smart Meeting Room/1. Generate Meeting Room Scene", false, 10)]
        [MenuItem("GameObject/Smart Meeting Room/Generate Meeting Room Scene", false, 10)]
        [MenuItem("智能会议室/生成会议室场景", false, 10)]
        public static void BuildScene()
        {
            // Play 模式下不能新建场景，先给出明确提示（否则会抛 InvalidOperationException）
            if (EditorApplication.isPlayingOrWillChangePlaymode)
            {
                EditorUtility.DisplayDialog("Please exit Play mode",
                    "The scene cannot be generated while in Play mode.\n\n" +
                    "Stop Play mode (the ▶ button) first, then run this menu again.", "OK");
                return;
            }

            if (!EditorUtility.DisplayDialog("Generate Meeting Room Scene / 生成会议室场景",
                "This will create (and overwrite):\n" + ScenePath + "\n\nContinue?", "Generate", "Cancel"))
                return;

            var scene = BuildSceneInternal();
            EditorUtility.DisplayDialog("Done / 完成",
                "Scene generated:\n" + ScenePath + "\n\nEnter Play mode and click devices with the mouse.", "OK");
            Selection.activeGameObject = null;
            EditorSceneManager.MarkSceneDirty(scene);
        }

        [MenuItem("Tools/Smart Meeting Room/2. Generate Scene + Screenshots", false, 11)]
        [MenuItem("智能会议室/生成场景并出效果图", false, 11)]
        public static void BuildSceneAndShots()
        {
            BuildSceneInternal();
            CaptureShots();
            EditorUtility.DisplayDialog("Done / 完成",
                "Scene and screenshots generated:\n" + ScenePath + "\n" + ShotDir, "OK");
        }

        [MenuItem("Tools/Smart Meeting Room/Open Meeting Room Scene", false, 12)]
        [MenuItem("智能会议室/打开会议室场景", false, 12)]
        public static void OpenScene()
        {
            if (File.Exists(ScenePath)) EditorSceneManager.OpenScene(ScenePath);
            else Debug.LogWarning("Scene not found. Run 'Generate Meeting Room Scene' first.");
        }

        // ------------------------------------------------------------------ 核心

        public static Scene BuildSceneInternal()
        {
            EnsureFolder("Assets/Scenes");

            // 先彻底删掉旧场景资产再新建。
            // 之前出现过生成的场景里带"失效引用"（Console 刷 Resource ID out of range），
            // 从零开始建场景可以避免旧文件里的 fileID 残留被继承。
            AssetDatabase.DeleteAsset(ScenePath);
            AssetDatabase.Refresh();

            var scene = EditorSceneManager.NewScene(NewSceneSetup.EmptyScene, NewSceneMode.Single);

            MeetingRoomBuilder.BuildAll();
            MarkStatics();
            BakeLighting();
            StampBuildInfo();

            EditorSceneManager.SaveScene(scene, ScenePath);
            AssetDatabase.SaveAssets();
            AssetDatabase.Refresh();
            Debug.Log("[MeetingRoom] 场景已保存：" + ScenePath);
            return scene;
        }

        /// <summary>
        /// 在场景里留下构建戳（场景名 + 时间 + 设备清单），并写一份 txt 到工程根目录。
        /// 目的是随时能确认"当前场景是不是用最新代码生成的"，避免在旧场景上验证新功能。
        /// </summary>
        static void StampBuildInfo()
        {
            var time = System.DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");

            var sb = new System.Text.StringBuilder();
            sb.AppendLine("buildTime=" + time);
            sb.AppendLine("scene=" + ScenePath);

            var ids = new System.Collections.Generic.List<string>();
#if UNITY_2023_1_OR_NEWER
            var devices = Object.FindObjectsByType<DeviceController>(FindObjectsSortMode.None);
#else
            var devices = Object.FindObjectsOfType<DeviceController>();
#endif
            foreach (var d in devices) ids.Add(d.id + "(" + d.displayName + ")");
            ids.Sort();
            sb.AppendLine("deviceCount=" + ids.Count);
            sb.AppendLine("devices=" + string.Join(", ", ids));

            // 关键模型自检：用来判断场景里是新版几何还是旧版几何
            bool hasDoor = GameObject.Find("门扇") != null;
            bool hasScreen = GameObject.Find("幕布布面") != null;
            sb.AppendLine("hasDoorLeaf=" + hasDoor);
            sb.AppendLine("hasScreenFabric(应为False)=" + hasScreen);

            var text = sb.ToString();

            // 场景里的版本戳：直接用空物体名字，简单可靠（不掺脚本，避免丢引用）
            var stamp = new GameObject("__BUILD_" + System.DateTime.Now.ToString("MMdd_HHmm") + "__");
            stamp.transform.position = new Vector3(0f, RoomDim.Height + 1.2f, 0f);

            Debug.Log("[MeetingRoom] 构建戳：\n" + text);
            System.IO.File.WriteAllText(
                System.IO.Path.Combine(Application.dataPath, "../dsh_scene_build.txt"), text);
        }

        /// <summary>
        /// 把静态几何标记为 static：开启静态合批，并让光照烘焙参与。
        ///
        /// 注意：**会动的物体绝不能打 BatchingStatic**。
        /// Unity 的静态合批会把顶点烘进一个世界空间的合并网格，之后 Transform 再变也不会重新渲染——
        /// 这正是"门扇角度从 0° 变成 90°、但画面纹丝不动"的根因。
        /// 这里显式排除门扇、幕布/窗帘等可动部件。
        /// </summary>
        static void MarkStatics()
        {
            var flags = StaticEditorFlags.BatchingStatic | StaticEditorFlags.OccludeeStatic
                      | StaticEditorFlags.ContributeGI | StaticEditorFlags.ReflectionProbeStatic;

            // 可动部件的名字前缀：这些及其子物体都不打静态标记
            var movables = new[] { "门扇", "窗帘_左", "窗帘_右", "纱帘", "设备_" };

            foreach (var rootName in new[] { RoomShell.Root, Furniture.Root, "02b_装饰绿植" })
            {
                var go = GameObject.Find(rootName);
                if (go == null) continue;

                foreach (var t in go.GetComponentsInChildren<Transform>(true))
                {
                    bool skip = false;
                    for (var cur = t; cur != null && cur != go.transform; cur = cur.parent)
                    {
                        foreach (var m in movables)
                        {
                            if (cur.name.StartsWith(m)) { skip = true; break; }
                        }
                        if (skip) break;
                    }
                    if (skip)
                    {
                        GameObjectUtility.SetStaticEditorFlags(t.gameObject, 0);
                        continue;
                    }
                    GameObjectUtility.SetStaticEditorFlags(t.gameObject, flags);
                }
            }
        }

        /// <summary>
        /// 烘焙光照：拿到间接光 + 环境光遮蔽，运行时就不用实时算，帧率更稳。
        /// 批处理模式下用 BakeAsync 并自己轮询，超时则跳过（不阻塞构建）。
        /// </summary>
        static void BakeLighting()
        {
            if (Application.isBatchMode && System.Environment.GetEnvironmentVariable("DSH_SKIP_BAKE") == "1")
            {
                Debug.Log("[MeetingRoom] 按 DSH_SKIP_BAKE=1 跳过光照烘焙");
                return;
            }

            try
            {
                var settings = new LightingSettings
                {
                    name = "MeetingRoomLighting",
                    bakedGI = true,
                    realtimeGI = false,
                    lightmapper = LightingSettings.Lightmapper.ProgressiveCPU,
                    lightmapResolution = 20f,
                    lightmapPadding = 2,
                    lightmapMaxSize = 1024,
                    ao = true,
                    aoMaxDistance = 1.1f,
                    aoExponentIndirect = 1.0f,
                    aoExponentDirect = 0.30f,
                    directSampleCount = 24,
                    indirectSampleCount = 128,
                    environmentSampleCount = 96,
                    maxBounces = 2,
                    prioritizeView = false,
                };

                // 烘焙设置必须存成资源，否则 BakeAsync 可能直接拒绝启动
                EnsureFolder("Assets/Settings");
                const string settingsPath = "Assets/Settings/MeetingRoomLighting.asset";
                AssetDatabase.DeleteAsset(settingsPath);
                AssetDatabase.CreateAsset(settings, settingsPath);
                Lightmapping.lightingSettings = AssetDatabase.LoadAssetAtPath<LightingSettings>(settingsPath);

                if (!Lightmapping.BakeAsync())
                {
                    Debug.LogWarning("[MeetingRoom] 光照烘焙无法启动，已跳过（场景仍为实时灯光，视觉不受影响）");
                    return;
                }

                // 批处理模式下没有编辑器循环，需要自己等
                var sw = System.Diagnostics.Stopwatch.StartNew();
                const double timeoutSec = 150.0;
                while (Lightmapping.isRunning && sw.Elapsed.TotalSeconds < timeoutSec)
                    System.Threading.Thread.Sleep(200);

                if (Lightmapping.isRunning)
                {
                    Lightmapping.Cancel();
                    Debug.LogWarning("[MeetingRoom] 光照烘焙超时已取消（" + (int)timeoutSec + "s），场景内容不受影响");
                }
                else
                {
                    Debug.Log("[MeetingRoom] 光照烘焙完成，耗时 " + sw.Elapsed.TotalSeconds.ToString("0.0") + "s");
                }
            }
            catch (System.Exception e)
            {
                Debug.LogWarning("[MeetingRoom] 光照烘焙跳过：" + e.Message);
            }
        }

        // ------------------------------------------------------------------ 出图

        [MenuItem("Tools/Smart Meeting Room/3. Screenshots Only", false, 13)]
        [MenuItem("智能会议室/仅生成效果图", false, 13)]
        public static void CaptureShots()
        {
            var cam = Camera.main;
            if (cam == null)
            {
                Debug.LogError("找不到主相机，请先生成场景。");
                return;
            }
            var orbit = cam.GetComponent<OrbitCamera>();

            EnsureFolder("Assets");
            EnsureFolder(ShotDir);

            const int W = 1920, H = 1080;
            var rt = new RenderTexture(W, H, 24, RenderTextureFormat.ARGB32) { antiAliasing = 4 };
            var tex = new Texture2D(W, H, TextureFormat.RGB24, false);

            var room = ResolveRoom();
            if (room == null) Debug.LogWarning("[MeetingRoom] 场景里没找到 RoomController，出图时无法切换可视化状态");

            // 机位一律放在房间内部，直接用"相机位置 + 注视点"描述，避免出现跑到墙外拍外壳的情况
            var shots = new (string name, Vector3 eye, Vector3 target, System.Action<RoomController> tweak)[]
            {
                // 站在入口角落看整个会议区
                ("01_全景",        new Vector3(-3.45f, 2.30f, -2.55f), new Vector3(0.6f, 1.05f, 0.25f), r => ResetOverlays(r)),
                // 正对投影墙（投影设备已移除，这里只看空墙面与会议桌关系）
                ("02_正视_投影墙",  new Vector3(0f, 1.50f, 1.55f),    new Vector3(0f, 1.55f, -3f),   r => ResetOverlays(r)),
                // 从入口上方斜俯视，看整体布局
                ("03_斜俯视_布局",  new Vector3(-3.45f, 2.85f, -2.70f), new Vector3(0f, 0.55f, 0.10f), r => ResetOverlays(r)),
                // 站在门内侧向房间内看
                ("04_入口视角",    new Vector3(-3.60f, 1.60f, -1.90f), new Vector3(2.2f, 1.35f, 0.60f), r => ResetOverlays(r)),
                // 站在窗边（背光面）看会议桌
                ("05_窗边视角",    new Vector3(2.60f, 1.55f, 2.45f),  new Vector3(-1.4f, 1.15f, -0.60f), r => ResetOverlays(r)),
                // 演示模式：主灯关、窗帘关
                ("06_演示模式",    new Vector3(0.35f, 1.55f, 2.15f),  new Vector3(0f, 1.55f, -3f), r =>
                    {
                        ResetOverlays(r);
                        if (r == null) return;
                        r.SetDeviceOn("mainlight", false);
                        r.SetDeviceOn("curtain", false);
                    }),
                // 温度空间化叠加
                ("07_环境数据叠加", new Vector3(-2.60f, 2.35f, -2.20f), new Vector3(0.4f, 0.75f, 0.60f), r =>
                    {
                        ResetOverlays(r);
                        if (r == null) return;
                        r.showThermalOverlay = true;
                    }),
                // 桌面与座椅细节
                ("08_家具与桌面细节", new Vector3(1.35f, 1.18f, 1.32f), new Vector3(-0.35f, 0.84f, -0.15f), r => ResetOverlays(r)),
                // 入口与电动门（门打开状态，用于核对门扇是否真的转开）
                ("11_入口与电动门_开", new Vector3(-1.20f, 1.45f, -0.60f), new Vector3(-4f, 1.20f, -1.85f), r =>
                    {
                        ResetOverlays(r);
                        if (r != null) r.SetDeviceOn("door", true);
                    }),
                ("12_入口与电动门_关", new Vector3(-1.20f, 1.45f, -0.60f), new Vector3(-4f, 1.20f, -1.85f), r =>
                    {
                        ResetOverlays(r);
                        if (r != null) r.SetDeviceOn("door", false);
                    }),
                // 落地空调（正面朝房间）
                ("13_落地空调",      new Vector3(1.60f, 1.35f, -0.90f), new Vector3(3.52f, 1.05f, -2.28f), r => ResetOverlays(r)),
            };

            var savedFov = cam.fieldOfView;
            cam.fieldOfView = 34f;   // 出图统一用略窄的视角，画面更"正"

            // 直接用主相机出图：构建流程本来就是"重新生成场景"，不需要保护相机原状态。
            // （之前用临时相机 + CopyFrom 会把主相机的位姿一起复制过来，导致机位被覆盖）
            foreach (var s in shots)
            {
                s.tweak?.Invoke(room);
                if (room != null) room.ApplyVisuals();

                var dir = s.target - s.eye;
                var rot = Quaternion.LookRotation(dir.sqrMagnitude > 1e-4f ? dir.normalized : Vector3.forward, Vector3.up);
                var pos = s.eye;
                if (pos.y < 0.25f) pos.y = 0.25f;
                cam.transform.position = pos;
                cam.transform.rotation = rot;

                // 场景内可视化层（热力叠加等）与幕布升降也需要手动刷新一帧
                if (room != null)
                {
                    room.SendMessage("AnimateDevices", SendMessageOptions.DontRequireReceiver);
                    room.SendMessage("UpdateFields", 0.016f, SendMessageOptions.DontRequireReceiver);
                }

                cam.targetTexture = rt;
                cam.Render();
                RenderTexture.active = rt;
                tex.ReadPixels(new Rect(0, 0, W, H), 0, 0);
                tex.Apply();
                cam.targetTexture = null;
                RenderTexture.active = null;

                var path = ShotDir + "/" + s.name + ".png";
                File.WriteAllBytes(path, tex.EncodeToPNG());
                Debug.Log("[MeetingRoom] 已出图 " + path);
            }

            // 出图后把相机恢复到默认机位，并同步 OrbitCamera 的内部状态
            cam.fieldOfView = savedFov;
            if (orbit != null)
            {
                orbit.SnapTo(RoomDim.OrbitYaw, RoomDim.OrbitPitch, RoomDim.OrbitDistance, RoomDim.OrbitPivot);
                var defaultRot = Quaternion.Euler(RoomDim.OrbitPitch, RoomDim.OrbitYaw, 0f);
                cam.transform.position = RoomDim.OrbitPivot - defaultRot * Vector3.forward * RoomDim.OrbitDistance;
                cam.transform.rotation = defaultRot;
            }

            // ---- 额外出一张正交顶视平面图（不受房间尺寸限制，能完整看布局）----
            CaptureFloorPlan(rt, tex, W, H);

            Object.DestroyImmediate(rt);
            Object.DestroyImmediate(tex);
            AssetDatabase.Refresh();
        }

        /// <summary>正交俯视平面图：完整展示 8×6m 的家具布局与吊顶设备位置。</summary>
        static void CaptureFloorPlan(RenderTexture rt, Texture2D tex, int W, int H)
        {
            var cam = Camera.main;
            if (cam == null) return;

            GameObject temp = null;
            Camera oc = null;
            var hidden = new System.Collections.Generic.List<Renderer>();
            try
            {
                // 俯视时临时隐藏吊顶（含筒灯/风口/灯槽），否则只能拍到天花板背面
                var shell = GameObject.Find(RoomShell.Root);
                if (shell != null)
                {
                    foreach (var t in shell.GetComponentsInChildren<Transform>(true))
                    {
                        var n = t.name;
                        if (n.StartsWith("吊顶") || n.StartsWith("灯槽") || n.StartsWith("跌级")
                            || n.StartsWith("筒灯") || n.StartsWith("空调_") || n.StartsWith("烟雾探测器"))
                        {
                            foreach (var r in t.GetComponents<Renderer>())
                            {
                                if (r.enabled) { r.enabled = false; hidden.Add(r); }
                            }
                        }
                    }
                }

                temp = new GameObject("__顶视相机");
                oc = temp.AddComponent<Camera>();
                oc.CopyFrom(cam);
                oc.orthographic = true;
                oc.orthographicSize = 4.35f;          // 竖向覆盖 8.7m，横向 15.4m，8×6m 房间完整入画
                oc.aspect = (float)W / H;
                oc.transform.position = new Vector3(0f, 14f, -0.35f);
                oc.transform.rotation = Quaternion.Euler(90f, 0f, 0f);
                oc.nearClipPlane = 0.1f;
                oc.farClipPlane = 30f;
                oc.clearFlags = CameraClearFlags.SolidColor;
                oc.backgroundColor = new Color(0.05f, 0.06f, 0.08f, 1f);

                var data = temp.GetComponent<UnityEngine.Rendering.Universal.UniversalAdditionalCameraData>();
                if (data != null) data.renderPostProcessing = true;

                cam.enabled = false;
                oc.targetTexture = rt;
                oc.Render();
                cam.enabled = true;

                RenderTexture.active = rt;
                tex.ReadPixels(new Rect(0, 0, W, H), 0, 0);
                tex.Apply();
                oc.targetTexture = null;
                RenderTexture.active = null;

                var path = ShotDir + "/09_平面布局图.png";
                File.WriteAllBytes(path, tex.EncodeToPNG());
                Debug.Log("[MeetingRoom] 已出图 " + path);
            }
            catch (System.Exception e)
            {
                Debug.LogWarning("[MeetingRoom] 平面图输出失败：" + e.Message);
                cam.enabled = true;
            }
            finally
            {
                if (temp != null) Object.DestroyImmediate(temp);
                foreach (var r in hidden) if (r != null) r.enabled = true;
            }
        }

        static void ResetOverlays(RoomController r)
        {
            if (r == null) return;
            r.showThermalOverlay = false;
            r.showLuxField = false;
            r.showSmokeField = false;

            // 恢复默认设备状态，保证每张图都是"干净"的默认场景
            var light = r.Get("mainlight");
            if (light != null) { light.on = true; light.value = 1f; }
            var ac = r.Get("ac");
            if (ac != null) { ac.on = true; ac.value = 24f; }
            var cur = r.Get("curtain");
            if (cur != null) { cur.on = true; cur.value = 1f; }
            var door = r.Get("door");
            if (door != null) { door.on = false; door.value = 1f; }
        }

        /// <summary>编辑器非运行模式下 RoomController.Instance 为 null，这里按场景查找。</summary>
        static RoomController ResolveRoom()
        {
            if (RoomController.Instance != null) return RoomController.Instance;
#if UNITY_2023_1_OR_NEWER
            return Object.FindFirstObjectByType<RoomController>();
#else
            return Object.FindObjectOfType<RoomController>();
#endif
        }

        // ------------------------------------------------------------------ 命令行入口

        public static void BuildFromCommandLine()
        {
            try
            {
                BuildSceneInternal();
                CaptureShots();
                File.WriteAllText(Path.Combine(Application.dataPath, "../dsh_build_ok.txt"),
                    "OK " + ScenePath + " shots=" + ShotDir);
                Debug.Log("[MeetingRoom] 命令行构建全部成功");
            }
            catch (System.Exception e)
            {
                File.WriteAllText(Path.Combine(Application.dataPath, "../dsh_build_error.txt"), e.ToString());
                Debug.LogError("[MeetingRoom] 命令行构建失败：" + e);
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
