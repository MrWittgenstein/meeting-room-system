using UnityEngine;

namespace NordicRoom
{
    /// <summary>
    /// 挂在可交互设备上的控制器。状态由 RoomController 统一驱动。
    /// （本文件照搬"暖木会议室"的 WarmRoom/DeviceController.cs，只改命名空间与描边配色。）
    ///
    /// 注意：这里**刻意不保存任何 Transform / GameObject 引用**。
    /// 之前的版本在 DeviceController 上放了一个 `public Transform visualTarget`，
    /// 结果带非空引用的组件在场景重载后整体解析失败（Unity 报 "referenced script is missing"），
    /// 甚至导致无法进入 Play 模式。所有视觉对象改为运行时按层级查找。
    /// </summary>
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

        /// <summary>被动对象：只作展示，不参与开关控制（座椅 / 白板 / 矮柜等）。</summary>
        public bool passive = false;

        /// <summary>悬停描边相对本体的偏移。</summary>
        public Vector3 outlineOffset = Vector3.zero;

        GameObject _outline;
        bool _hovered;

        void Awake()
        {
            on = initialOn;
            value = initialValue;
        }

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

            var size = new Vector3(0.3f, 0.3f, 0.3f);
            var bc = GetComponent<BoxCollider>();
            var mr = GetComponent<MeshRenderer>();
            if (bc != null) size = Vector3.Scale(bc.size, transform.lossyScale);
            else if (mr != null) size = mr.bounds.size;

            // 北欧明亮风：描边用清亮的青蓝色（暖橙会和木色糊在一起）
            var mat = Mat.Emissive("n_outline", new Color(0.35f, 0.80f, 0.95f), 2.4f);
            size *= 1.04f;
            const float t = 0.013f;

            // 12 条棱
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
