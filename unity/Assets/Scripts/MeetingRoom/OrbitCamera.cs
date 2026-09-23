using UnityEngine;
using UnityEngine.InputSystem;

namespace SmartRoom
{
    /// <summary>
    /// 轨道相机：左键拖拽旋转、右键/中键拖拽平移、滚轮缩放。
    /// 相机被限制在房间内部，避免穿到墙外或钻进地下。
    /// </summary>
    [DisallowMultipleComponent]
    public class OrbitCamera : MonoBehaviour
    {
        public Vector3 pivot = new Vector3(0f, 1.15f, 0f);
        public float distance = 12.5f;
        public float yaw = 38f;
        public float pitch = 26f;

        public float minDistance = 1.2f;
        public float maxDistance = 24f;
        public float orbitSpeed = 0.22f;
        public float panSpeed = 0.0016f;
        public float zoomSpeed = 0.9f;
        public float smoothing = 14f;

        // 只限制视点的高度范围：太高会看到吊顶背面，太低会钻到地板下面。
        // 水平方向不限制，这样用户可以自由拉远看整体，不会被"卡"在墙里。
        public Vector3 pivotMin = new Vector3(-40f, 0.30f, -40f);
        public Vector3 pivotMax = new Vector3(40f, 2.35f, 40f);
        public float minHeight = 0.22f;

        Camera _cam;

        Vector3 _pivotTarget, _pivotCurrent;
        float _distTarget, _distCurrent;
        float _yawTarget, _yawCurrent;
        float _pitchTarget, _pitchCurrent;

        /// <summary>鼠标是否正被 HUD 占用（此时不做相机操作）。</summary>
        public bool blocked;

        void Awake()
        {
            _cam = GetComponent<Camera>();
            _pivotTarget = _pivotCurrent = pivot;
            _distTarget = _distCurrent = distance;
            _yawTarget = _yawCurrent = yaw;
            _pitchTarget = _pitchCurrent = pitch;
        }

        public void Update()
        {
            var mouse = Mouse.current;
            if (mouse == null)
            {
                // 编辑器非运行模式（出图）下没有输入设备，仍然把相机摆到目标位置
                ApplyTransform(true);
                return;
            }

            float dt = Mathf.Max(Time.unscaledDeltaTime, 1e-4f);
            var delta = mouse.delta.ReadValue();
            float scroll = mouse.scroll.ReadValue().y;
            bool orbiting = mouse.leftButton.isPressed;
            bool panning = mouse.rightButton.isPressed || mouse.middleButton.isPressed;

            if (!blocked)
            {
                if (orbiting && !panning)
                {
                    _yawTarget += delta.x * orbitSpeed * 2.4f;
                    _pitchTarget -= delta.y * orbitSpeed * 2.4f;
                    _pitchTarget = Mathf.Clamp(_pitchTarget, -32f, 84f);
                }
                else if (panning)
                {
                    // 按当前朝向做屏幕空间平移
                    float fovScale = _distCurrent * Mathf.Tan(_cam.fieldOfView * 0.5f * Mathf.Deg2Rad) * 2f;
                    var right = transform.right;
                    var up = Vector3.ProjectOnPlane(transform.up, Vector3.up).normalized;
                    if (up.sqrMagnitude < 0.01f) up = Vector3.forward;
                    var move = (-right * delta.x + -up * delta.y) * panSpeed * fovScale;
                    _pivotTarget += move;
                }

                if (Mathf.Abs(scroll) > 0.001f)
                {
                    _distTarget -= Mathf.Sign(scroll) * zoomSpeed * Mathf.Max(0.6f, _distCurrent * 0.12f);
                    _distTarget = Mathf.Clamp(_distTarget, minDistance, maxDistance);
                }
            }

            _pivotTarget = Clamp(_pivotTarget, pivotMin, pivotMax);
            _distTarget = Mathf.Clamp(_distTarget, minDistance, maxDistance);

            ApplyTransform(false);
        }

        /// <summary>把内部目标值落到 Transform 上。immediate=true 时不做平滑，直接到位。</summary>
        void ApplyTransform(bool immediate)
        {
            if (immediate)
            {
                _pivotCurrent = _pivotTarget;
                _distCurrent = _distTarget;
                _yawCurrent = _yawTarget;
                _pitchCurrent = _pitchTarget;
            }
            else
            {
                float dt = Mathf.Max(Time.unscaledDeltaTime, 1e-4f);
                float k = 1f - Mathf.Exp(-smoothing * dt);
                _pivotCurrent = Vector3.Lerp(_pivotCurrent, _pivotTarget, k);
                _distCurrent = Mathf.Lerp(_distCurrent, _distTarget, k);
                _yawCurrent = Mathf.Lerp(_yawCurrent, _yawTarget, k);
                _pitchCurrent = Mathf.Lerp(_pitchCurrent, _pitchTarget, k);
            }

            var rot = Quaternion.Euler(_pitchCurrent, _yawCurrent, 0f);
            var pos = _pivotCurrent - rot * Vector3.forward * _distCurrent;
            // 只防止相机钻到地板以下（房间里没有墙体碰撞，允许自由拉远看整体）
            if (pos.y < minHeight) pos.y = minHeight;
            transform.position = pos;
            transform.rotation = rot;
        }

        static Vector3 Clamp(Vector3 v, Vector3 min, Vector3 max)
        {
            return new Vector3(Mathf.Clamp(v.x, min.x, max.x), Mathf.Clamp(v.y, min.y, max.y), Mathf.Clamp(v.z, min.z, max.z));
        }

        /// <summary>直接跳到某个预设机位（用于截图和"相机预设"）。</summary>
        public void SnapTo(float newYaw, float newPitch, float newDist, Vector3 newPivot)
        {
            _yawTarget = _yawCurrent = newYaw;
            _pitchTarget = _pitchCurrent = newPitch;
            _distTarget = _distCurrent = Mathf.Clamp(newDist, minDistance, maxDistance);
            _pivotTarget = _pivotCurrent = Clamp(newPivot, pivotMin, pivotMax);
        }
    }
}
