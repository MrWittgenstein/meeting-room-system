using UnityEngine;

namespace SmartRoom
{
    /// <summary>挂在可交互设备上的控制器。状态由 RoomController 统一驱动。</summary>
    [DisallowMultipleComponent]
    public class DeviceController : MonoBehaviour
    {
        public string id = "device";
        public string displayName = "设备";
        public DeviceKind kind = DeviceKind.MainLight;

        public bool initialOn = true;
        public float initialValue = 1f;

        [HideInInspector] public bool on = true;
        [HideInInspector] public float value = 1f;

        /// <summary>外描边相对本体的偏移，避免描边被本体完全包住。</summary>
        public Vector3 outlineOffset = Vector3.zero;

        // 注意：这里曾经放过一个 `public Transform visualTarget` 字段，用来直接绑定门扇/幕布。
        // 结果发现：**带非空 Transform 引用的 DeviceController 在场景重载后会整体解析失败**
        // （Unity 报 "the referenced script (Unknown) is missing"，组件变 null）。
        // 同一场景里 visualTarget 为 0 的设备则完全正常，规律非常明确。
        // 因此改为运行时按层级查找（见 RoomController.ResolveVisualRefs），不再序列化组件引用。

        GameObject _outline;
        bool _hovered;

        void Awake()
        {
            on = initialOn;
            value = initialValue;
        }

        /// <summary>构建期调用：把初始状态写入。</summary>
        public void ApplyInitial()
        {
            on = initialOn;
            value = initialValue;
        }

        public void SetHover(bool hovered)
        {
            if (_hovered == hovered) return;
            _hovered = hovered;
            if (hovered && _outline == null) CreateOutline();
            if (_outline != null) _outline.SetActive(hovered);
        }

        void CreateOutline()
        {
            _outline = new GameObject("选中描边");
            _outline.transform.SetParent(transform, false);
            _outline.transform.localPosition = outlineOffset;
            _outline.transform.localRotation = Quaternion.identity;

            // 从渲染体推算包围盒尺寸
            var size = new Vector3(0.3f, 0.3f, 0.3f);
            var mf = GetComponent<MeshFilter>();
            if (mf != null && mf.sharedMesh != null)
            {
                var b = mf.sharedMesh.bounds;
                size = Vector3.Scale(b.size, transform.lossyScale);
            }

            var mat = MatLib.Emissive("m_outline", new Color(0.25f, 0.80f, 1f), 2.2f);
            size *= 1.06f;
            const float t = 0.012f;

            // 12 条棱（比整体放大的实心盒更"描边"，且从任何角度看都成立）
            AddBar(new Vector3(0f, size.y * 0.5f, size.z * 0.5f), new Vector3(size.x + t, t, t), mat);
            AddBar(new Vector3(0f, size.y * 0.5f, -size.z * 0.5f), new Vector3(size.x + t, t, t), mat);
            AddBar(new Vector3(0f, -size.y * 0.5f, size.z * 0.5f), new Vector3(size.x + t, t, t), mat);
            AddBar(new Vector3(0f, -size.y * 0.5f, -size.z * 0.5f), new Vector3(size.x + t, t, t), mat);
            AddBar(new Vector3(size.x * 0.5f, 0f, size.z * 0.5f), new Vector3(t, size.y + t, t), mat);
            AddBar(new Vector3(-size.x * 0.5f, 0f, size.z * 0.5f), new Vector3(t, size.y + t, t), mat);
            AddBar(new Vector3(size.x * 0.5f, 0f, -size.z * 0.5f), new Vector3(t, size.y + t, t), mat);
            AddBar(new Vector3(-size.x * 0.5f, 0f, -size.z * 0.5f), new Vector3(t, size.y + t, t), mat);
            AddBar(new Vector3(size.x * 0.5f, size.y * 0.5f, 0f), new Vector3(t, t, size.z + t), mat);
            AddBar(new Vector3(-size.x * 0.5f, size.y * 0.5f, 0f), new Vector3(t, t, size.z + t), mat);
            AddBar(new Vector3(size.x * 0.5f, -size.y * 0.5f, 0f), new Vector3(t, t, size.z + t), mat);
            AddBar(new Vector3(-size.x * 0.5f, -size.y * 0.5f, 0f), new Vector3(t, t, size.z + t), mat);
        }

        void AddBar(Vector3 localPos, Vector3 size, Material mat)
        {
            var go = GameObject.CreatePrimitive(PrimitiveType.Cube);
            go.name = "边";
            go.transform.SetParent(_outline.transform, false);
            go.transform.localPosition = localPos;
            go.transform.localScale = size;
            Destroy(go.GetComponent<Collider>());
            var r = go.GetComponent<MeshRenderer>();
            r.sharedMaterial = mat;
            r.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;
            r.receiveShadows = false;
        }
    }
}
