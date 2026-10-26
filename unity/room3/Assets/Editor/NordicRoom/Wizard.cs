using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;
using UnityEngine.SceneManagement;

namespace NordicRoom.EditorTools
{
    /// <summary>
    /// 北欧明亮风会议室：一键生成场景 + 自检 + 截图。
    /// 批处理入口：Unity.exe -batchmode -quit -projectPath &lt;项目&gt; -executeMethod NordicRoom.EditorTools.Wizard.BuildFromCommandLine
    /// </summary>
    public static class Wizard
    {
        const string SceneDir = "Assets/Scenes";
        const string ScenePath = SceneDir + "/NordicMeetingRoom.unity";
        const string ShotDir = "Assets/Screenshots";
        const string StampName = "dsh_nordic_build.txt";

        // ================================================================== 菜单

        [MenuItem("Tools/北欧会议室/生成场景 %#m", false, 10)]
        public static void BuildInteractive() => Run(true, true);

        [MenuItem("Tools/Nordic Room/Build Scene %#m", false, 11)]
        public static void BuildInteractiveEn() => Run(true, true);

        [MenuItem("Tools/北欧会议室/仅重建（保留相机位置）", false, 12)]
        public static void RebuildOnly() => Run(false, false);

        [MenuItem("Tools/北欧会议室/自检 + 截图", false, 30)]
        public static void VerifyOnly() => Run(false, true);

        /// <summary>诊断：把窗帘联动逐步打印出来（排查"开合没反应"这类问题）。</summary>
        [MenuItem("Tools/北欧会议室/诊断窗帘联动", false, 40)]
        public static void DiagnoseCurtain()
        {
            var sb = new StringBuilder();
            var room = UnityEngine.Object.FindFirstObjectByType<RoomController>();
            if (room == null)
            {
                // 批处理下没有已打开的场景：先生成一遍再诊断
                BuildScene();
                room = UnityEngine.Object.FindFirstObjectByType<RoomController>();
            }
            if (room == null) { Debug.Log("[NordicRoom] 没找到 RoomController"); return; }
            room.Initialize();
            DumpCurtain(sb, room, "Initialize 后");

            room.ToggleAllCurtains();
            DumpCurtain(sb, room, "第一次 ToggleAllCurtains 之后");
            room.ApplyCurtainScaleAll();
            DumpCurtain(sb, room, "第一次 ApplyCurtainScaleAll 之后");

            room.ToggleAllCurtains();
            DumpCurtain(sb, room, "第二次 ToggleAllCurtains 之后");
            room.ApplyCurtainScaleAll();
            DumpCurtain(sb, room, "第二次 ApplyCurtainScaleAll 之后");

            Debug.Log(sb.ToString());
            TryWriteStamp(sb.ToString());
        }

        /// <summary>诊断：同一机位分别关掉太阳阴影 / 点光源 / 全部光源各拍一张，定位异常暗块来源。</summary>
        [MenuItem("Tools/北欧会议室/诊断吊灯暗块", false, 42)]
        public static void DiagnosePendant()
        {
            if (UnityEngine.Object.FindFirstObjectByType<RoomController>() == null) BuildScene();
            var sb = new StringBuilder();
            Directory.CreateDirectory(ShotDir);

            var cam = Camera.main;
            if (cam == null) { Debug.Log("[NordicRoom] 无主相机"); return; }
            var orbit = cam.GetComponent<OrbitCamera>();
            var room = UnityEngine.Object.FindFirstObjectByType<RoomController>();
            if (room != null) room.Initialize();

            Light point = null, sun = null;
            foreach (var l in UnityEngine.Object.FindObjectsByType<Light>(FindObjectsSortMode.None))
            {
                sb.AppendLine("光: " + l.name + " type=" + l.type + " intensity=" + l.intensity
                    + " shadows=" + l.shadows + " pos=(" + l.transform.position.x.ToString("0.00") + ","
                    + l.transform.position.y.ToString("0.00") + "," + l.transform.position.z.ToString("0.00") + ")");
                if (l.type == LightType.Point && point == null) point = l;
                // 只认"真正投影的那个"方向光，否则会被不投影的天光补光抢走
                if (l.type == LightType.Directional && l.shadows != LightShadows.None) sun = l;
            }
            sb.AppendLine("选中: point=" + (point != null ? point.name : "无")
                        + "  sun=" + (sun != null ? sun.name : "无"));

            int w = 1600, h = 900;
            var rt = new RenderTexture(w, h, 24, RenderTextureFormat.ARGB32) { antiAliasing = 4 };
            var prev = cam.targetTexture;
            cam.targetTexture = rt;

            var eye = new Vector3(1.85f, 2.00f, -0.30f);
            var look = new Vector3(-0.30f, 2.22f, -1.00f);

            if (orbit != null) orbit.SnapLook(eye, look, true);
            cam.Render(); SavePng(rt, Path.Combine(ShotDir, "D1_原始.png"));

            if (sun != null) sun.shadows = LightShadows.None;
            cam.Render(); SavePng(rt, Path.Combine(ShotDir, "D2_关太阳阴影.png"));

            if (point != null) point.enabled = false;
            cam.Render(); SavePng(rt, Path.Combine(ShotDir, "D3_再关点光源.png"));

            if (sun != null) sun.enabled = false;
            cam.Render(); SavePng(rt, Path.Combine(ShotDir, "D4_全关.png"));

            // 恢复光照后关掉后处理，确认青色"尖刺"是不是 Bloom 造成的
            if (sun != null) { sun.enabled = true; sun.shadows = LightShadows.Soft; }
            if (point != null) point.enabled = true;
            var data = cam.GetComponent<UnityEngine.Rendering.Universal.UniversalAdditionalCameraData>();
            if (data != null)
            {
                data.renderPostProcessing = false;
                cam.Render(); SavePng(rt, Path.Combine(ShotDir, "D5_关后处理.png"));
                data.renderPostProcessing = true;
            }

            // 把吊灯整组藏起来：如果暗锥还在，说明它根本不是灯造成的
            var pendants = GameObject.Find("主照明_吊灯组");
            if (pendants != null)
            {
                pendants.SetActive(false);
                cam.Render(); SavePng(rt, Path.Combine(ShotDir, "D6_隐藏吊灯.png"));
                pendants.SetActive(true);
            }

            // 二分定位：只藏"发光面"，看白色尖刺是不是它
            var glows = new List<GameObject>();
            foreach (var t in pendants.GetComponentsInChildren<Transform>(true))
                if (t.name == "灯泡发光面") glows.Add(t.gameObject);
            foreach (var g in glows) g.SetActive(false);
            cam.Render(); SavePng(rt, Path.Combine(ShotDir, "D8_只藏发光面.png"));
            foreach (var g in glows) g.SetActive(true);

            // 再把灯罩也藏掉
            var shades = new List<GameObject>();
            foreach (var t in pendants.GetComponentsInChildren<Transform>(true))
                if (t.name.StartsWith("灯罩_")) shades.Add(t.gameObject);
            foreach (var g in shades) g.SetActive(false);
            cam.Render(); SavePng(rt, Path.Combine(ShotDir, "D9_再藏灯罩.png"));
            foreach (var g in shades) g.SetActive(true);

            // 只留点光源、关掉太阳：若暗锥由太阳阴影造成，这一步就会消失
            if (sun != null) sun.enabled = false;
            cam.Render(); SavePng(rt, Path.Combine(ShotDir, "D7_只留点光源.png"));
            if (sun != null) sun.enabled = true;

            cam.targetTexture = prev;
            rt.Release();
            Debug.Log(sb.ToString());
            TryWriteStamp(sb.ToString());
        }

        /// <summary>诊断：找出场景里所有"脚本引用丢失"的组件，以及 RoomHud/RoomController 的关联。</summary>
        [MenuItem("Tools/北欧会议室/诊断丢失脚本", false, 44)]
        public static void DiagnoseMissingScripts()
        {
            // 打开已保存的场景（不是新建），这样看到的才是用户实际加载的那一份
            var scene = EditorSceneManager.OpenScene(ScenePath, OpenSceneMode.Single);
            var sb = new StringBuilder();
            sb.AppendLine("=== 场景: " + ScenePath + " ===");

            int missing = 0;
            foreach (var go in scene.GetRootGameObjects())
                CountMissing(go.transform, sb, ref missing);
            sb.AppendLine("丢失脚本的组件数 = " + missing);

            sb.AppendLine();
            sb.AppendLine("=== 关键组件关联 ===");
            var hud = UnityEngine.Object.FindFirstObjectByType<RoomHud>();
            var room = UnityEngine.Object.FindFirstObjectByType<RoomController>();
            var cam = Camera.main;
            sb.AppendLine("RoomHud        = " + (hud != null ? hud.gameObject.name : "缺失"));
            sb.AppendLine("RoomController = " + (room != null ? room.gameObject.name : "缺失"));
            sb.AppendLine("MainCamera     = " + (cam != null ? cam.gameObject.name : "缺失"));
            if (hud != null) sb.AppendLine("hud.room       = " + (hud.room != null ? hud.room.gameObject.name : "NULL（面板会失效！）"));
            if (room != null)
            {
                sb.AppendLine("room.cam       = " + (room.cam != null ? room.cam.gameObject.name : "NULL"));
                sb.AppendLine("room.orbit     = " + (room.orbit != null ? room.orbit.gameObject.name : "NULL"));
                sb.AppendLine("room.hud       = " + (room.hud != null ? room.hud.gameObject.name : "NULL"));
                sb.AppendLine("room.devices   = " + room.devices.Count);
            }

            sb.AppendLine();
            sb.AppendLine("=== 全部 MonoBehaviour 的脚本名 ===");
            var seen = new System.Collections.Generic.Dictionary<string, int>();
            foreach (var go in scene.GetRootGameObjects())
                TallyScripts(go.transform, seen);
            foreach (var kv in seen) sb.AppendLine("  " + kv.Value + " × " + kv.Key);

            Debug.Log(sb.ToString());
            TryWriteStamp(sb.ToString());
        }

        static void CountMissing(Transform t, StringBuilder sb, ref int missing)
        {
            foreach (var c in t.GetComponents<Component>())
            {
                if (c == null)
                {
                    missing++;
                    sb.AppendLine("  ! 丢失脚本: " + PathOf(t));
                }
            }
            for (int i = 0; i < t.childCount; i++) CountMissing(t.GetChild(i), sb, ref missing);
        }

        static void TallyScripts(Transform t, System.Collections.Generic.Dictionary<string, int> seen)
        {
            foreach (var c in t.GetComponents<MonoBehaviour>())
            {
                string n = c == null ? "(null)" : c.GetType().FullName;
                seen.TryGetValue(n, out int v);
                seen[n] = v + 1;
            }
            for (int i = 0; i < t.childCount; i++) TallyScripts(t.GetChild(i), seen);
        }

        static string PathOf(Transform t)
        {
            var s = t.name;
            var p = t.parent;
            while (p != null) { s = p.name + "/" + s; p = p.parent; }
            return s;
        }

        /// <summary>诊断：把所有"离地"的对象按最低点从高到低列出来，用来找浮空物。</summary>
        [MenuItem("Tools/北欧会议室/列出浮空物", false, 43)]
        public static void ListFloating()
        {
            if (UnityEngine.Object.FindFirstObjectByType<RoomController>() == null) BuildScene();
            var sb = new StringBuilder();
            var all = new List<Transform>();
            foreach (var go in SceneManager.GetActiveScene().GetRootGameObjects()) Collect(go.transform, all);

            var rows = new List<string>();
            var keys = new List<float>();
            var suspicious = new List<string>();
            foreach (var t in all)
            {
                var r = t.GetComponent<MeshRenderer>();
                if (r == null) continue;
                if (t.GetComponent<DeviceController>() != null) continue;
                // 桌面上方 0.9~2.4m 且靠近房间中心 → 桌中央摆件那丛绿枝的浮空叶
                var c = r.bounds.center;
                if (c.y > 0.95f && c.y < 2.40f
                    && new Vector2(c.x, c.z).magnitude < Dim.TableDia * 0.5f)
                {
                    suspicious.Add(t.name + "  中心=" + c.ToString("0.00"));
                    continue;
                }
                var b = r.bounds;
                if (b.size.y > 1.8f) continue;                 // 墙板 / 帘子之类
                if (b.min.y <= 0.03f) continue;                // 落在地上的
                if (b.min.y > 2.55f) continue;                 // 吊顶 / 灯轨 / 窗帘杆等贴顶件
                if (b.size.magnitude < 0.02f) continue;        // 指示点之类的小东西
                keys.Add(b.min.y);
                rows.Add(string.Format("  minY={0,6:0.000}  {1,-24} 中心={2}  尺寸={3}",
                    b.min.y, t.name, b.center.ToString("0.0"), b.size.ToString("0.00")));
            }

            // 按 minY 从高到低排序（简单的插入排序，避免引入 Linq 依赖）
            for (int i = 1; i < keys.Count; i++)
            {
                float k = keys[i]; string row = rows[i];
                int j = i - 1;
                while (j >= 0 && keys[j] < k) { keys[j + 1] = keys[j]; rows[j + 1] = rows[j]; j--; }
                keys[j + 1] = k; rows[j + 1] = row;
            }

            sb.AppendLine("=== 离地对象（按最低点从高到低，前 200 条）===");
            for (int i = 0; i < rows.Count && i < 200; i++) sb.AppendLine(rows[i]);
            sb.AppendLine("合计离地对象 " + rows.Count + " 个");

            // 重点：桌面上方、靠近房间中心的小件 —— 这就是"半空中的奇怪东西"
            sb.AppendLine();
            sb.AppendLine("=== 桌面上方的浮空小件（应为 0）===");
            if (suspicious.Count == 0) sb.AppendLine("  （无）OK");
            else foreach (var s in suspicious) sb.AppendLine("  ! " + s);
            sb.AppendLine("浮空小件数 = " + suspicious.Count);

            Debug.Log(sb.ToString());
            TryWriteStamp(sb.ToString());
        }

        /// <summary>诊断：把某棵子树的每个渲染器（网格 / 材质 / 世界包围盒）转储出来。</summary>
        [MenuItem("Tools/北欧会议室/转储吊灯子树", false, 41)]
        public static void DumpSubtree()
        {
            var sb = new StringBuilder();
            if (UnityEngine.Object.FindFirstObjectByType<RoomController>() == null) BuildScene();
            DumpNode(sb, GameObject.Find("主照明_吊灯组"));
            DumpNode(sb, GameObject.Find("窗帘"));
            Debug.Log(sb.ToString());
            TryWriteStamp(sb.ToString());
        }

        static void DumpNode(StringBuilder sb, GameObject root)
        {
            if (root == null) { sb.AppendLine("(null)"); return; }
            var list = new List<Transform>();
            Collect(root.transform, list);
            sb.AppendLine("=== " + root.name + "  节点数 " + list.Count + " ===");
            foreach (var t in list)
            {
                var mf = t.GetComponent<MeshFilter>();
                var r = t.GetComponent<Renderer>();
                if (r == null) continue;
                var b = r.bounds;
                sb.AppendLine(string.Format(
                    "  {0,-22} mesh={1,-16} mat={2,-22} scale=({3:0.###},{4:0.###},{5:0.###}) 盒心=({6:0.000},{7:0.000},{8:0.000}) 盒尺寸=({9:0.000},{10:0.000},{11:0.000})",
                    t.name,
                    mf != null && mf.sharedMesh != null ? mf.sharedMesh.name : "无",
                    r.sharedMaterial != null ? r.sharedMaterial.name : "无",
                    t.lossyScale.x, t.lossyScale.y, t.lossyScale.z,
                    b.center.x, b.center.y, b.center.z, b.size.x, b.size.y, b.size.z));
            }
        }

        static void DumpCurtain(StringBuilder sb, RoomController room, string stage)
        {
            sb.AppendLine("=== " + stage + " ===");
            var panels = new List<Transform>();
            foreach (var go in SceneManager.GetActiveScene().GetRootGameObjects())
            { var l = new List<Transform>(); Collect(go.transform, l); panels.AddRange(l); }
            panels.RemoveAll(t => !t.name.StartsWith("帘片_", StringComparison.Ordinal));
            sb.AppendLine("  帘片数 = " + panels.Count + "   总覆盖宽度 = " + CurtainWorldWidth(panels).ToString("0.000"));
            foreach (var d in room.devices)
                if (d.kind == DeviceKind.Curtain)
                    sb.AppendLine(string.Format("    {0,-14} on={1,-6} value={2:0.00}", d.id, d.on, d.value));
            foreach (var t in panels)
            {
                var r = t.Find("遮光帘");
                sb.AppendLine(string.Format("    {0,-16} localScale.x={1:0.000} localPos.x={2:0.000} 世界宽={3:0.000}",
                    t.name, t.localScale.x, t.localPosition.x, r != null ? r.GetComponent<Renderer>().bounds.size.x : 0f));
            }
        }

        // ================================================================== 批处理入口

        public static void BuildFromCommandLine()
        {
            try
            {
                Run(true, true);
                Debug.Log("[NordicRoom] BuildFromCommandLine 完成");
                EditorApplication.Exit(0);
            }
            catch (Exception e)
            {
                Debug.LogError("[NordicRoom] BuildFromCommandLine 失败：" + e);
                TryWriteStamp("BUILD FAILED\n" + e.ToString());
                EditorApplication.Exit(1);
            }
        }

        // ================================================================== 主流程

        static void Run(bool rebuild, bool screenshots)
        {
            if (EditorApplication.isPlayingOrWillChangePlaymode)
            {
                EditorUtility.DisplayDialog("北欧会议室", "请先退出 Play 模式再生成场景。", "好");
                return;
            }

            var scene = rebuild ? BuildScene() : SceneManager.GetActiveScene();
            if (rebuild)
            {
                MarkStatics();
                EditorSceneManager.MarkSceneDirty(scene);
                Directory.CreateDirectory(SceneDir);
                EditorSceneManager.SaveScene(scene, ScenePath);
                Debug.Log("[NordicRoom] 场景已保存：" + ScenePath);
            }

            var report = Verify(scene);

            // 自检过程中会反复开关设备，最后停在 ResetAll() 的状态上。
            // 这里显式恢复一次初始状态，免得存进场景的是"灯全灭 + 窗帘收拢"。
            var restore = UnityEngine.Object.FindFirstObjectByType<RoomController>();
            if (restore != null)
            {
                restore.Initialize();
                restore.ApplyDoorSnap();
            }

            WriteStamp(report);
            Debug.Log(report);

            if (screenshots) CaptureScreenshots();

            if (restore != null)
            {
                restore.Initialize();
                restore.ApplyDoorSnap();
            }

            if (rebuild)
            {
                EditorSceneManager.MarkSceneDirty(scene);
                EditorSceneManager.SaveScene(scene, ScenePath);
                AssetDatabase.SaveAssets();
                AssetDatabase.Refresh(ImportAssetOptions.ForceSynchronousImport);

                // 关键：保存后再把场景重新打开一次，确认脚本引用真的落到磁盘上。
                // 之前出现过场景存下来但 m_Script 解析不出来（24 个 DeviceController + RoomController
                // 全变 null，hud.room = NULL），表现就是"按 Play 没反应、面板也不出来"。
                var reopened = EditorSceneManager.OpenScene(ScenePath, OpenSceneMode.Single);
                AssetDatabase.Refresh(ImportAssetOptions.ForceSynchronousImport);

                int lost = 0;
                var hud = UnityEngine.Object.FindFirstObjectByType<RoomHud>();
                var rc = UnityEngine.Object.FindFirstObjectByType<RoomController>();
                foreach (var go in reopened.GetRootGameObjects()) lost += CountLost(go.transform);
                Debug.Log("[NordicRoom] 场景落盘复检：丢失脚本 " + lost
                    + " · RoomController=" + (rc != null ? "OK" : "NULL")
                    + " · RoomHud=" + (hud != null ? "OK" : "NULL")
                    + " · hud.room=" + (hud != null && hud.room != null ? "OK" : "NULL"));

                if (lost > 0 || rc == null || hud == null || hud.room == null)
                    Debug.LogError("[NordicRoom] 场景脚本引用没有正确落地，Play 后交互会失效！");
            }

            AssetDatabase.SaveAssets();
            AssetDatabase.Refresh();
        }

        static int CountLost(Transform t)
        {
            int n = 0;
            foreach (var c in t.GetComponents<Component>()) if (c == null) n++;
            for (int i = 0; i < t.childCount; i++) n += CountLost(t.GetChild(i));
            return n;
        }

        static Scene BuildScene()
        {
            var scene = EditorSceneManager.NewScene(NewSceneSetup.EmptyScene, NewSceneMode.Single);
            scene.name = "NordicMeetingRoom";
            Builder.BuildAll();
            return scene;
        }

        // ================================================================== 静态标记

        /// <summary>
        /// 只给"永不移动"的子树打 BatchingStatic。
        /// 门扇 / 窗帘 / 纱帘 / 所有设备命中体必须排除 ——
        /// 静态合批会把顶点烘进世界空间，之后 Transform 再怎么动都不会反映到渲染上。
        /// </summary>
        static void MarkStatics()
        {
            string[] staticRoots = { Furn.Root, "04_灯光", "05_后处理 Volume" };
            string[] excludes = { "门扇", "帘片_", "设备_", "绿植", "休闲角", "主照明", "窗帘组_", "浮标_" };

            foreach (var rootName in staticRoots)
            {
                var go = GameObject.Find(rootName);
                if (go == null) continue;
                Mark(go.transform, excludes);
            }

            // 房间壳体单独处理：壳体里含门扇与设备命中体，必须逐个排除
            var shell = GameObject.Find(Shell.Root);
            if (shell != null) Mark(shell.transform, excludes);
        }

        static void Mark(Transform t, string[] excludes)
        {
            foreach (var ex in excludes)
                if (t.name.StartsWith(ex, StringComparison.Ordinal)) return;

            var flags = GameObjectUtility.GetStaticEditorFlags(t.gameObject);
            GameObjectUtility.SetStaticEditorFlags(t.gameObject, flags | StaticEditorFlags.BatchingStatic);
            for (int i = 0; i < t.childCount; i++) Mark(t.GetChild(i), excludes);
        }

        // ================================================================== 自检

        static string Verify(Scene scene)
        {
            var sb = new StringBuilder();
            sb.AppendLine("=== 北欧明亮风会议室 · 构建自检 ===");
            sb.AppendLine("buildTime      : " + DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"));
            sb.AppendLine("scene          : " + ScenePath);
            sb.AppendLine();

            var all = new List<Transform>();
            foreach (var go in scene.GetRootGameObjects()) Collect(go.transform, all);

            int renderers = 0, colliders = 0, missing = 0, noMat = 0;
            var missingNames = new List<string>();
            foreach (var t in all)
            {
                var r = t.GetComponent<Renderer>();
                if (r != null)
                {
                    renderers++;
                    var mats = r.sharedMaterial;
                    if (mats == null) { noMat++; if (missingNames.Count < 20) missingNames.Add(t.name + "(无材质)"); }
                }
                if (t.GetComponent<Collider>() != null) colliders++;

                foreach (var c in t.GetComponents<Component>())
                {
                    if (c == null)
                    {
                        missing++;
                        if (missingNames.Count < 40) missingNames.Add(t.name + "(Missing component)");
                    }
                }
            }

            // 相机初始机位（batchmode 进不了 Play，这里直接把场景里存的数值核对一遍）
            sb.AppendLine("--- 相机初始机位（进 Play 第一眼看到的画面）---");
            var mainCam = Camera.main;
            var oc = mainCam != null ? mainCam.GetComponent<OrbitCamera>() : null;
            if (mainCam == null || oc == null)
            {
                sb.AppendLine("  未找到主相机或 OrbitCamera");
            }
            else
            {
                var cp = mainCam.transform.position;
                var look = oc.pivot - cp;
                sb.AppendLine(string.Format("  pivot=({0:0.00},{1:0.00},{2:0.00})  distance={3:0.00}  yaw={4:0.0}°  pitch={5:0.0}°",
                    oc.pivot.x, oc.pivot.y, oc.pivot.z, oc.distance, oc.yaw, oc.pitch));
                sb.AppendLine(string.Format("  相机世界坐标 = ({0:0.00}, {1:0.00}, {2:0.00})", cp.x, cp.y, cp.z));
                sb.AppendLine(string.Format("  相机到视点距离 = {0:0.00} m（应等于 distance）", look.magnitude));

                // 最关键的一条：相机必须在房间**内部**，
                // 否则进 Play 第一眼只会看到外墙和吊顶（这个坑踩过）。
                bool inside = Mathf.Abs(cp.x) < Dim.HX - 0.15f
                           && Mathf.Abs(cp.z) < Dim.HZ - 0.15f
                           && cp.y > 0.30f && cp.y < Dim.Height - 0.15f;
                sb.AppendLine("  相机在房间内部 : " + (inside
                    ? "OK（第一眼就能看到室内）"
                    : "异常！相机跑到墙外了，只会看到外墙和吊顶"));
                sb.AppendLine("  机位与预设一致 : " + (Mathf.Abs(look.magnitude - oc.distance) < 0.2f ? "OK" : "异常！数值与 Transform 不一致"));
            }

            sb.AppendLine("--- 规模 ---");
            sb.AppendLine("Transform 总数      : " + all.Count);
            sb.AppendLine("MeshRenderer 总数   : " + renderers);
            sb.AppendLine("Collider 总数       : " + colliders);
            sb.AppendLine("无材质渲染器        : " + noMat);
            sb.AppendLine("scriptComponents_missing : " + missing);
            if (missingNames.Count > 0)
                foreach (var n in missingNames) sb.AppendLine("    ! " + n);

            // 设备
            var devices = new List<DeviceController>();
            foreach (var t in all)
            {
                var d = t.GetComponent<DeviceController>();
                if (d != null) devices.Add(d);
            }
            int active = 0;
            sb.AppendLine();
            sb.AppendLine("--- 设备（共 " + devices.Count + " 个命中体）---");
            foreach (var d in devices)
            {
                if (!d.passive) active++;
                sb.AppendLine(string.Format("  {0,-16} {1,-20} kind={2,-18} on={3,-5} value={4:0.##} passive={5}",
                    d.id, d.displayName, d.kind, d.initialOn, d.initialValue, d.passive));
            }
            sb.AppendLine("可交互设备数        : " + active);

            // 关键几何自检
            sb.AppendLine();
            sb.AppendLine("--- 几何自检 ---");
            sb.AppendLine("门扇层级            : " + Describe(GameObject.Find("门扇")));
            sb.AppendLine("门扇旋转(yaw)       : " + DoorYaw());

            var panels = new List<Transform>();
            foreach (var t in all)
                if (t.name.StartsWith("帘片_", StringComparison.Ordinal)) panels.Add(t);
            sb.AppendLine("帘片数量            : " + panels.Count + "（应为 8 = 4 组 × 2 片）");
            foreach (var t in panels)
            {
                var r = t.Find("遮光帘");
                if (r == null) { sb.AppendLine("  " + t.name + " → 缺遮光帘"); continue; }
                var b = r.GetComponent<Renderer>().bounds;
                sb.AppendLine(string.Format("  {0,-16} 遮光帘世界X[{1:0.000},{2:0.000}] 宽{3:0.000} 高{4:0.000} 世界Y[{5:0.000},{6:0.000}]",
                    t.name, b.min.x, b.max.x, b.size.x, b.size.y, b.min.y, b.max.y));
            }

            // 窗帘闭合状态下应当正好拼满 4 个窗洞
            sb.AppendLine();
            sb.AppendLine("--- 窗帘闭合拼缝核对（期望覆盖窗洞）---");
            sb.AppendLine("北墙西窗洞 x[-3.55,-1.55] / 北墙东窗洞 x[1.55,3.55]");
            sb.AppendLine("南墙西窗洞 x[-3.55,-1.55] / 南墙东窗洞 x[1.55,3.55]");

            // 浮空检查：所有可移动小件的世界最低点应 >= 0
            sb.AppendLine();
            sb.AppendLine("--- 浮空/穿地检查 ---");
            int below = 0, suspicious = 0;
            foreach (var t in all)
            {
                var r = t.GetComponent<MeshRenderer>();
                if (r == null) continue;
                var b = r.bounds;
                if (b.min.y < -0.06f)
                {
                    below++;
                    if (below <= 12) sb.AppendLine(string.Format("  低于地面: {0} minY={1:0.000}", t.name, b.min.y));
                }
                // 桌面/柜面上方的小件：粗查悬空（底部悬空超过 5cm 且不在灯具/传感器/白名单里）
                string n = t.name;
                bool airborneOk = n.StartsWith("吊", StringComparison.Ordinal) || n.StartsWith("灯", StringComparison.Ordinal)
                    || n.StartsWith("吊线", StringComparison.Ordinal) || n.StartsWith("浮标", StringComparison.Ordinal)
                    || n.StartsWith("烟雾", StringComparison.Ordinal) || n.StartsWith("光照", StringComparison.Ordinal)
                    || n.StartsWith("人流", StringComparison.Ordinal) || n.StartsWith("温湿", StringComparison.Ordinal)
                    || n.StartsWith("CO2", StringComparison.Ordinal) || n.StartsWith("控制屏", StringComparison.Ordinal)
                    || n.StartsWith("入口屏", StringComparison.Ordinal) || n.StartsWith("门禁", StringComparison.Ordinal)
                    || n.StartsWith("白板", StringComparison.Ordinal) || n.StartsWith("圆镜", StringComparison.Ordinal)
                    || n.StartsWith("阴角线", StringComparison.Ordinal) || n.StartsWith("吊轨", StringComparison.Ordinal)
                    || n.StartsWith("窗帘", StringComparison.Ordinal) || n.StartsWith("杆", StringComparison.Ordinal)
                    || n.StartsWith("筒灯", StringComparison.Ordinal) || n.StartsWith("闭门器", StringComparison.Ordinal)
                    || n.StartsWith("门磁", StringComparison.Ordinal) || n.StartsWith("门套", StringComparison.Ordinal)
                    || n.StartsWith("门墙", StringComparison.Ordinal) || n.StartsWith("后墙", StringComparison.Ordinal)
                    || n.StartsWith("北墙", StringComparison.Ordinal) || n.StartsWith("南墙", StringComparison.Ordinal)
                    || n.StartsWith("桌", StringComparison.Ordinal) || n.StartsWith("陈设_", StringComparison.Ordinal)
                    || n.StartsWith("空调", StringComparison.Ordinal) || n.StartsWith("玻璃", StringComparison.Ordinal)
                    || n.StartsWith("窗", StringComparison.Ordinal);
                if (!airborneOk && b.min.y > 0.03f && b.max.y < 2.90f && b.size.y < 1.6f)
                {
                    suspicious++;
                    if (suspicious <= 12) sb.AppendLine(string.Format("  疑似悬空: {0} minY={1:0.000} 尺寸{2}", n, b.min.y, b.size));
                }
            }
            sb.AppendLine("低于地面对象数      : " + below);
            sb.AppendLine("疑似悬空对象数      : " + suspicious);

            // 可移动件的静态标记核查
            sb.AppendLine();
            sb.AppendLine("--- 关键可移动件 BatchingStatic 核查（应全部为 False）---");
            foreach (var nm in new[] { "门扇" })
                CheckStatic(sb, GameObject.Find(nm));
            foreach (var t in panels) CheckStatic(sb, t.gameObject);
            foreach (var d in devices) CheckStatic(sb, d.gameObject);

            // 模型校验：圆盘 / 圆柱面必须朝上（之前出过三角形绕序写反、
            // 整个圆桌面被背面剔除而"看不见"的问题，这里直接从网格数据上把关）
            sb.AppendLine();
            sb.AppendLine("--- 网格法线校验 ---");
            CheckMeshUp(sb, "圆桌面_白橡木", Prim.RoundSlabMesh);
            CheckMeshUp(sb, "小圆几_台面", Prim.RoundSlabMesh);
            CheckMeshDown(sb, "筒灯发光_0", Prim.DiscMeshRef);

            // 空调朝向与穿模核查（正面在本地 -Z，应正对房间中心）
            sb.AppendLine();
            sb.AppendLine("--- 空调朝向 / 穿模核查 ---");
            var acRoot = GameObject.Find("立式空调");
            if (acRoot == null)
            {
                sb.AppendLine("  未找到 立式空调");
            }
            else
            {
                var acPos = acRoot.transform.position;
                var toCenter = (new Vector3(0f, acPos.y, 0f) - acPos);
                toCenter.y = 0f; toCenter.Normalize();
                var front = -(acRoot.transform.rotation * Vector3.forward);
                front.y = 0f; front.Normalize();
                float dot = Vector3.Dot(front, toCenter);
                sb.AppendLine(string.Format("  机身位置 = ({0:0.00}, {1:0.00})   yaw = {2:0.0}°",
                    acPos.x, acPos.z, acRoot.transform.localEulerAngles.y));
                sb.AppendLine(string.Format("  正面·朝向房间中心 点积 = {0:0.000}  → {1}", dot,
                    dot > 0.95f ? "OK（正对房间中心）" : "异常！正面没对准房间中心"));
                // 与绿植的包围盒重叠检查
                var acHit = GameObject.Find("设备_空调");
                int overlaps = 0;
                if (acHit != null && acHit.TryGetComponent<Collider>(out var acCol))
                {
                    foreach (var t in all)
                    {
                        if (!t.name.StartsWith("绿植", StringComparison.Ordinal)) continue;
                        var r = t.GetComponent<Renderer>();
                        if (r == null) continue;
                        if (acCol.bounds.Intersects(r.bounds)) overlaps++;
                    }
                }
                sb.AppendLine("  与绿植重叠的部件数 = " + overlaps + "  → " + (overlaps == 0 ? "OK（不穿模）" : "异常！空调和绿植穿模了"));
            }

            // 联动动画实测：直接把窗帘 / 门推到两个极端，量真实包围盒有没有变
            sb.AppendLine();
            sb.AppendLine("--- 联动动画实测（离开 Play 模式也能验证）---");
            var room = UnityEngine.Object.FindFirstObjectByType<RoomController>();
            if (room == null)
            {
                sb.AppendLine("  未找到 RoomController，跳过");
            }
            else
            {
                room.Initialize();
                room.ApplyCurtainScaleAll();      // 先结算到场景的初始状态

                float openW0 = CurtainWorldWidth(panels);
                float curtainY0 = CurtainWorldMinY(panels);
                float curtainY1 = CurtainWorldMaxY(panels);

                room.ToggleAllCurtains();         // 第一次点：由"展开"切到"收拢"
                room.ApplyCurtainScaleAll();
                float gatheredW = CurtainWorldWidth(panels);

                room.ToggleAllCurtains();         // 再点一次：由"收拢"切回"展开"
                room.ApplyCurtainScaleAll();
                float openW = CurtainWorldWidth(panels);

                sb.AppendLine(string.Format("  初始（展开）8 片总覆盖宽度 : {0:0.000} m（期望 8.000 = 4 组 × 2.00m 窗洞，正好拼满）", openW0));
                sb.AppendLine(string.Format("  收拢后 8 片总覆盖宽度     : {0:0.000} m（期望 1.600 = 8.000 × 20%，收到窗边）", gatheredW));
                sb.AppendLine(string.Format("  再次展开后总覆盖宽度      : {0:0.000} m（期望回到 8.000）", openW));
                sb.AppendLine(string.Format("  窗帘竖向范围             : Y[{0:0.000}, {1:0.000}]（应完全落在窗洞 {2:0.00}–{3:0.00} 内，不拖地）",
                    curtainY0, curtainY1, Dim.WinSill, Dim.WinTop));
                sb.AppendLine("  窗帘开合联动             : " + (openW0 > gatheredW * 3f
                    && Mathf.Abs(openW0 - 8f) < 0.06f && Mathf.Abs(gatheredW - 1.6f) < 0.06f && Mathf.Abs(openW - 8f) < 0.06f
                    ? "OK（展开拼满窗洞 / 收拢到窗边 / 可反复切换）" : "异常！开合没有变化或尺寸不对"));
                sb.AppendLine("  窗帘竖向不出窗洞         : " + (curtainY0 >= Dim.WinSill - 0.02f && curtainY1 <= Dim.WinTop + 0.02f
                    ? "OK" : "异常！帘子拖地或超出窗顶"));

                room.Toggle("door");
                room.ApplyDoorSnap();
                var leaf = GameObject.Find("门扇");
                if (leaf != null)
                {
                    float yaw = leaf.transform.localEulerAngles.y;
                    sb.AppendLine(string.Format("  门开启后 yaw             : {0:0.0}°（期望 ≈ {1:0.0}°）", yaw, Dim.DoorOpenYaw));
                    sb.AppendLine("  门旋转生效               : " + (Mathf.Abs(Mathf.DeltaAngle(yaw, Dim.DoorOpenYaw)) < 3f ? "OK" : "异常！门没转到位"));
                    // 自由端应当转到房间内侧（+X 方向）
                    var b = leaf.GetComponentInChildren<Renderer>();
                    if (b != null)
                        sb.AppendLine(string.Format("  门扇包围盒 X 范围        : [{0:0.000}, {1:0.000}]（开启后应伸向房间内 +X）",
                            b.bounds.min.x, b.bounds.max.x));
                }

                room.ResetAll();
                sb.AppendLine("  已恢复初始状态");
            }

            return sb.ToString();
        }

        /// <summary>校验网格里是否存在朝上的三角面（圆台面必须有）。</summary>
        static void CheckMeshUp(StringBuilder sb, string label, Mesh m)
        {
            if (m == null) { sb.AppendLine("  " + label + " : 网格为空"); return; }
            int up = CountFacing(m, Vector3.up);
            sb.AppendLine(string.Format("  {0,-16} 顶点{1,5}  三角{2,5}  朝上三角 {3,5}  → {4}",
                label, m.vertexCount, m.triangles.Length / 3, up, up > 0 ? "OK（顶面可见）" : "异常！没有朝上的面，会被背面剔除"));
        }

        /// <summary>校验网格里是否存在朝下的三角面（朝下的发光盘必须有）。</summary>
        static void CheckMeshDown(StringBuilder sb, string label, Mesh m)
        {
            if (m == null) { sb.AppendLine("  " + label + " : 网格为空"); return; }
            int dn = CountFacing(m, Vector3.down);
            sb.AppendLine(string.Format("  {0,-16} 顶点{1,5}  三角{2,5}  朝下三角 {3,5}  → {4}",
                label, m.vertexCount, m.triangles.Length / 3, dn, dn > 0 ? "OK（朝下可见）" : "异常！没有朝下的面"));
        }

        static int CountFacing(Mesh m, Vector3 normal)
        {
            var v = m.vertices;
            var tris = m.triangles;
            int n = 0;
            for (int i = 0; i + 2 < tris.Length; i += 3)
            {
                var a = v[tris[i]]; var b = v[tris[i + 1]]; var c = v[tris[i + 2]];
                var fn = Vector3.Cross(b - a, c - a);
                if (fn.sqrMagnitude < 1e-10f) continue;
                if (Vector3.Dot(fn.normalized, normal) > 0.55f) n++;
            }
            return n;
        }

        static float CurtainWorldWidth(List<Transform> panels)        {
            float sum = 0f;
            foreach (var t in panels)
            {
                var r = t.Find("遮光帘");
                if (r != null) sum += r.GetComponent<Renderer>().bounds.size.x;
            }
            return sum;
        }

        static float CurtainWorldMinY(List<Transform> panels)
        {
            float v = float.MaxValue;
            foreach (var t in panels)
            {
                var r = t.Find("遮光帘");
                if (r != null) v = Mathf.Min(v, r.GetComponent<Renderer>().bounds.min.y);
            }
            return v;
        }

        static float CurtainWorldMaxY(List<Transform> panels)
        {
            float v = float.MinValue;
            foreach (var t in panels)
            {
                var r = t.Find("遮光帘");
                if (r != null) v = Mathf.Max(v, r.GetComponent<Renderer>().bounds.max.y);
            }
            return v;
        }

        static void CheckStatic(StringBuilder sb, GameObject go)
        {
            if (go == null) { sb.AppendLine("  缺失: (null)"); return; }
            var flags = GameObjectUtility.GetStaticEditorFlags(go);
            bool batched = (flags & StaticEditorFlags.BatchingStatic) != 0;
            sb.AppendLine(string.Format("  {0,-24} BatchingStatic={1}", go.name, batched));
        }

        static string Describe(GameObject go)
        {
            if (go == null) return "(未找到)";
            var t = go.transform;
            return t.parent != null ? t.parent.name + "/" + t.name + "  子物体 " + t.childCount + " 个" : t.name;
        }

        static string DoorYaw()
        {
            var go = GameObject.Find("门扇");
            if (go == null) return "(未找到)";
            return go.transform.localEulerAngles.y.ToString("0.0") + "°  (闭合 " + Dim.DoorClosedYaw + "° / 开启 " + Dim.DoorOpenYaw + "°)";
        }

        // ================================================================== 截图

        static void CaptureScreenshots()
        {
            Directory.CreateDirectory(ShotDir);
            var cam = Camera.main;
            if (cam == null)
            {
                foreach (var c in UnityEngine.Object.FindObjectsByType<Camera>(FindObjectsSortMode.None))
                    if (c.CompareTag("MainCamera")) { cam = c; break; }
            }
            if (cam == null) { Debug.LogWarning("[NordicRoom] 找不到主相机，跳过截图"); return; }

            var room = UnityEngine.Object.FindFirstObjectByType<RoomController>();
            if (room != null) room.Initialize();   // batchmode 不会自动跑 Awake

            var orbit = cam.GetComponent<OrbitCamera>();
            int w = 1920, h = 1080;
            var rt = new RenderTexture(w, h, 24, RenderTextureFormat.ARGB32) { antiAliasing = 4 };
            var prev = cam.targetTexture;
            cam.targetTexture = rt;

            // 全部取室内视点——相机若在室外，会被外墙和吊顶完全挡住，看不到房间内部。
            // 用「机位点 → 注视点」定义，避免手写 yaw 把朝向搞反。
            var shots = new (string name, Vector3 eye, Vector3 target)[]
            {
                ("01_全景_后墙角",   new Vector3( 3.30f, 1.75f, -1.35f), new Vector3(-1.20f, 1.00f,  1.50f)),
                ("02_正对主位",      new Vector3( 0.00f, 1.55f, -2.10f), new Vector3( 0.00f, 1.30f,  2.60f)),
                ("03_入口视角",      new Vector3(-3.20f, 1.52f, -0.20f), new Vector3( 0.30f, 1.15f,  1.20f)),
                ("04_高角度俯视",    new Vector3( 1.70f, 2.80f, -1.40f), new Vector3(-0.10f, 0.30f,  0.40f)),
                ("05_圆桌近景",      new Vector3( 0.00f, 1.16f,  1.15f), new Vector3( 0.00f, 0.85f, -1.30f)),
                ("06_后墙大窗",      new Vector3(-0.20f, 1.58f, -1.70f), new Vector3( 3.60f, 1.40f,  1.30f)),
                ("07_南墙白板",      new Vector3( 1.70f, 1.72f,  1.60f), new Vector3( 0.60f, 0.95f, -2.80f)),
                ("08_长条柜与圆镜",   new Vector3( 1.50f, 1.62f, -1.30f), new Vector3(-0.90f, 1.50f,  2.70f)),
                ("09_北墙角窗",      new Vector3( 1.90f, 1.62f,  0.90f), new Vector3(-3.20f, 1.45f,  2.30f)),
                ("10_入口与门墙",    new Vector3( 2.60f, 1.60f,  1.60f), new Vector3(-3.60f, 1.30f, -0.30f)),
                ("11_休闲角",       new Vector3(-1.60f, 1.80f, -0.90f), new Vector3( 1.50f, 1.20f, -2.30f)),
                ("12_吊灯仰视",      new Vector3(-0.60f, 1.35f,  0.40f), new Vector3( 0.00f, 2.30f,  0.55f)),
                ("13_立面图_北墙",    new Vector3(-0.40f, 1.55f, -1.60f), new Vector3(-0.40f, 1.45f,  2.90f)),
            };

            foreach (var s in shots)
            {
                if (orbit != null) orbit.SnapLook(s.eye, s.target, true);   // applyNow：batchmode 不走 Update
                cam.Render();
                SavePng(rt, Path.Combine(ShotDir, s.name + ".png"));
            }

            // 正交俯视平面图。
            // 直接把吊顶 / 吊轨 / 吊灯临时收起来再拍——靠相机裁切面去躲吊顶很难调准
            // （正交相机斜穿房间，远端总会带到底面）。
            var hidden = new List<GameObject>();
            foreach (var root in SceneManager.GetActiveScene().GetRootGameObjects())
                CollectPlanHidden(root.transform, hidden);

            var planGo = new GameObject("平面图相机");
            var planCam = planGo.AddComponent<Camera>();
            planCam.orthographic = true;
            planCam.orthographicSize = 4.4f;
            planCam.nearClipPlane = 0.05f;
            planCam.farClipPlane = 40f;
            planCam.clearFlags = CameraClearFlags.SolidColor;
            planCam.backgroundColor = new Color(0.11f, 0.12f, 0.14f);
            planCam.transform.position = new Vector3(0f, 14f, 0f);
            planCam.transform.rotation = Quaternion.Euler(90f, 0f, 0f);
            var prt = new RenderTexture(w, h, 24, RenderTextureFormat.ARGB32) { antiAliasing = 4 };
            planCam.targetTexture = prt;
            planCam.Render();
            SavePng(prt, Path.Combine(ShotDir, "14_平面布局图.png"));
            UnityEngine.Object.DestroyImmediate(planGo);

            foreach (var go in hidden) if (go != null) go.SetActive(true);

            cam.targetTexture = prev;
            rt.Release();
            Debug.Log("[NordicRoom] 截图完成：" + ShotDir);
        }

        static void SavePng(RenderTexture rt, string path)
        {
            var prev = RenderTexture.active;
            RenderTexture.active = rt;
            var tex = new Texture2D(rt.width, rt.height, TextureFormat.RGB24, false);
            tex.ReadPixels(new Rect(0, 0, rt.width, rt.height), 0, 0);
            tex.Apply();
            File.WriteAllBytes(path, tex.EncodeToPNG());
            UnityEngine.Object.DestroyImmediate(tex);
            RenderTexture.active = prev;
        }

        // ================================================================== 报告落盘

        static void WriteStamp(string body)
        {
            try { File.WriteAllText(Path.Combine(ProjectRoot(), StampName), body, new UTF8Encoding(true)); }
            catch (Exception e) { Debug.LogWarning("[NordicRoom] 写报告失败: " + e.Message); }
        }

        static void TryWriteStamp(string body)
        {
            try { File.WriteAllText(Path.Combine(ProjectRoot(), StampName), body, new UTF8Encoding(true)); }
            catch { }
        }

        static string ProjectRoot() => Directory.GetParent(Application.dataPath).FullName;

        static void Collect(Transform t, List<Transform> into)
        {
            into.Add(t);
            for (int i = 0; i < t.childCount; i++) Collect(t.GetChild(i), into);
        }

        /// <summary>平面图需要临时隐藏的顶层物体：吊顶、阴角线、吊轨、吊灯、筒灯。</summary>
        static readonly string[] PlanHidePrefixes =
        {
            "吊顶", "阴角线", "吊轨", "筒灯", "主照明", "烟雾探测器", "光照传感器", "人流传感器", "吊线"
        };

        static void CollectPlanHidden(Transform t, List<GameObject> hidden)
        {
            foreach (var pre in PlanHidePrefixes)
            {
                if (!t.name.StartsWith(pre, StringComparison.Ordinal)) continue;
                if (t.gameObject.activeSelf) { t.gameObject.SetActive(false); hidden.Add(t.gameObject); }
                return;   // 整棵子树一起收起来
            }
            for (int i = 0; i < t.childCount; i++) CollectPlanHidden(t.GetChild(i), hidden);
        }
    }
}
