using System.IO;
using System.Text;
using UnityEditor;
using UnityEngine;

namespace SmartRoom.EditorTools
{
    /// <summary>
    /// 用 Unity 自己的 API 校验场景里所有组件的脚本引用是否有效。
    /// 不再靠解析 YAML（Unity 6 对 Assembly-CSharp 的脚本用本地 fileID 引用，解析会误报）。
    /// </summary>
    public static class VerifyScripts
    {
        public static void Run()
        {
            const string scenePath = "Assets/Scenes/MeetingRoom.unity";
            if (!File.Exists(scenePath))
            {
                Debug.LogError("[Verify] 场景不存在: " + scenePath);
                return;
            }
            UnityEditor.SceneManagement.EditorSceneManager.OpenScene(scenePath);

            var sb = new StringBuilder();
            int ok = 0, broken = 0;

            foreach (var go in Object.FindObjectsByType<GameObject>(FindObjectsSortMode.None))
            {
                foreach (var c in go.GetComponents<Component>())
                {
                    if (c == null)
                    {
                        // 这就是"missing script"：组件存在但脚本解析失败
                        broken++;
                        sb.AppendLine("BROKEN  " + go.name + "  <null component>");
                        continue;
                    }
                    var ms = MonoScript.FromMonoBehaviour(c as MonoBehaviour);
                    if (c is MonoBehaviour)
                    {
                        if (ms == null)
                        {
                            broken++;
                            sb.AppendLine("BROKEN  " + go.name + "  " + c.GetType().Name + " (MonoScript 为空)");
                        }
                        else
                        {
                            ok++;
                            sb.AppendLine("OK      " + go.name + "  -> " + ms.GetClass()?.FullName
                                + "  [" + AssetDatabase.GetAssetPath(ms) + "]");
                        }
                    }
                }
            }

            // 单独统计 null 组件（missing script 的直接证据）
            int nullComps = 0;
            foreach (var go in Object.FindObjectsByType<GameObject>(FindObjectsSortMode.None))
                foreach (var c in go.GetComponents<Component>())
                    if (c == null) nullComps++;

            sb.AppendLine();
            sb.AppendLine("汇总: 有效脚本组件=" + ok + "  丢失脚本组件=" + nullComps);

            var outPath = Path.Combine(Application.dataPath, "../dsh_verify_scripts.txt");
            File.WriteAllText(outPath, sb.ToString(), Encoding.UTF8);
            Debug.Log("[Verify] " + outPath);
        }
    }
}
