using UnityEngine;
using UnityEngine.InputSystem;

namespace NordicRoom
{
    /// <summary>
    /// 轨道相机：左键拖拽旋转、右键/中键拖拽平移、滚轮缩放。
    /// 水平方向不限制（方便拉远看整体），只防止相机钻到地面以下。
    /// （与"暖木会议室"的 WarmRoom/OrbitCamera.cs 一致，只加了一个 SnapLook 便捷方法。）
    /// </summary>
    [DisallowMultipleComponent]
    public class OrbitCamera : MonoBehaviour
    {
        public Vector3 pivot = new Vector3(0f, 1.15f, 0f);
        public float distance = 9.8f;
        public float yaw = -38f;
        public float pitch = 25f;

        public float minDistance = 1.2f;
        public float maxDistance = 24f;
        public float orbitSpeed = 0.22f;
        public float panSpeed = 0.0016f;
        public float zoomSpeed = 0.9f;
        public float smoothing = 14f;
        public float minHeight = 0.22f;
        public float minPitch = -32f;
        public float maxPitch = 84f;

        Camera _cam;
        Vector3 _pivotT, _pivotC;
        float _distT, _distC, _yawT, _yawC, _pitchT, _pitchC;

        /// <summary>鼠标被界面占用时不响应相机操作。</summary>
        public bool blocked;

        void Awake()
        {
            _cam = GetComponent<Camera>();
            _pivotT = _pivotC = pivot;
            _distT = _distC = distance;
            _yawT = _yawC = yaw;
            _pitchT = _pitchC = pitch;
        }

        public void Update()
        {
            var mouse = Mouse.current;
            if (mouse == null)
            {
                Apply(true);   // 编辑器非运行模式：直接摆到位
                return;
            }

            var delta = mouse.delta.ReadValue();
            float scroll = mouse.scroll.ReadValue().y;
            bool orbiting = mouse.leftButton.isPressed;
            bool panning = mouse.rightButton.isPressed || mouse.middleButton.isPressed;

            if (!blocked)
            {
                if (orbiting && !panning)
                {
                    _yawT += delta.x * orbitSpeed * 2.4f;
                    _pitchT -= delta.y * orbitSpeed * 2.4f;
                    _pitchT = Mathf.Clamp(_pitchT, minPitch, maxPitch);
                }
                else if (panning)
                {
                    float fovScale = _distC * Mathf.Tan(_cam.fieldOfView * 0.5f * Mathf.Deg2Rad) * 2f;
                    var right = transform.right;
                    var up = Vector3.ProjectOnPlane(transform.up, Vector3.up).normalized;
                    if (up.sqrMagnitude < 0.01f) up = Vector3.forward;
                    _pivotT += (-right * delta.x + -up * delta.y) * panSpeed * fovScale;
                }

                if (Mathf.Abs(scroll) > 0.001f)
                {
                    _distT -= Mathf.Sign(scroll) * zoomSpeed * Mathf.Max(0.6f, _distC * 0.12f);
                    _distT = Mathf.Clamp(_distT, minDistance, maxDistance);
                }
            }

            _pivotT = new Vector3(_pivotT.x, Mathf.Clamp(_pivotT.y, 0.30f, 2.35f), _pivotT.z);
            _distT = Mathf.Clamp(_distT, minDistance, maxDistance);
            Apply(false);
        }

        void Apply(bool immediate)
        {
            if (immediate)
            {
                _pivotC = _pivotT; _distC = _distT; _yawC = _yawT; _pitchC = _pitchT;
            }
            else
            {
                float dt = Mathf.Max(Time.unscaledDeltaTime, 1e-4f);
                float k = 1f - Mathf.Exp(-smoothing * dt);
                _pivotC = Vector3.Lerp(_pivotC, _pivotT, k);
                _distC = Mathf.Lerp(_distC, _distT, k);
                _yawC = Mathf.Lerp(_yawC, _yawT, k);
                _pitchC = Mathf.Lerp(_pitchC, _pitchT, k);
            }

            var rot = Quaternion.Euler(_pitchC, _yawC, 0f);
            var pos = _pivotC - rot * Vector3.forward * _distC;
            if (pos.y < minHeight) pos.y = minHeight;
            transform.position = pos;
            transform.rotation = rot;
        }

        /// <summary>
        /// 跳到指定机位（截图 / 相机预设）。
        /// applyNow=true 时立刻写 Transform：batchmode 截图不会走 Update，
        /// 只设目标值的话连拍出来的画面会全部停在旧机位。
        /// </summary>
        public void SnapTo(float newYaw, float newPitch, float newDist, Vector3 newPivot, bool applyNow = false)
        {
            _yawT = _yawC = newYaw;
            _pitchT = _pitchC = Mathf.Clamp(newPitch, minPitch, maxPitch);
            _distT = _distC = Mathf.Clamp(newDist, minDistance, maxDistance);
            _pivotT = _pivotC = new Vector3(newPivot.x, Mathf.Clamp(newPivot.y, 0.30f, 2.35f), newPivot.z);
            if (applyNow) Apply(true);
        }

        /// <summary>
        /// 从「机位点 eye」看向「注视点 target」摆机位。
        /// 直接写 yaw/pitch 太容易把朝向写反，用点算没有歧义：
        /// 相机观察方向 = rot * Vector3.forward，所以 yaw = atan2(dir.x, dir.z)。
        /// </summary>
        public void SnapLook(Vector3 eye, Vector3 target, bool applyNow = false)
        {
            var d = target - eye;
            float dist = Mathf.Max(d.magnitude, 0.3f);
            d /= dist;
            float yaw = Mathf.Atan2(d.x, d.z) * Mathf.Rad2Deg;
            float pitch = -Mathf.Asin(Mathf.Clamp(d.y, -1f, 1f)) * Mathf.Rad2Deg;
            SnapTo(yaw, pitch, dist, target, applyNow);
        }

        /// <summary>立刻把目标值写到 Transform。</summary>
        public void ApplyNow() => Apply(true);
    }
}
