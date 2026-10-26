using System.Globalization;
using UnityEngine;

namespace NordicRoom
{
    /// <summary>
    /// 把实测温度画到落地空调那块"数显"面板上。
    ///
    /// 为什么不用 TextMesh：数显面板只有 0.16m × 0.084m，字号极小时 TextMesh 会糊且依赖字体资源；
    /// 这里直接用七段数码管的方式在纹理上画，任何分辨率下都清晰，也不依赖字体。
    ///
    /// 用法：RoomController 在拿到第一帧数据后 new 一个实例，之后每次温度变化调 SetValue。
    /// </summary>
    public class AcDigitDisplay
    {
        const int TW = 160;   // 纹理宽
        const int TH = 64;    // 纹理高

        readonly Color32 _bg = new Color32(8, 10, 12, 255);
        readonly Color32 _on = new Color32(70, 235, 255, 255);
        readonly Color32 _off = new Color32(78, 82, 88, 255);

        readonly Color32[] _buf = new Color32[TW * TH];
        readonly Texture2D _tex;
        readonly Material _mat;

        float _lastTemp = float.NaN;
        bool _lastOn;
        bool _drawn;

        public AcDigitDisplay(MeshRenderer renderer)
        {
            _tex = new Texture2D(TW, TH, TextureFormat.RGBA32, false)
            {
                name = "AC_DigitTex",
                filterMode = FilterMode.Point,
                wrapMode = TextureWrapMode.Clamp,
                hideFlags = HideFlags.DontSave
            };

            _mat = new Material(renderer.sharedMaterial)
            {
                name = "M_AC_DigitDisplay"
            };

            // 文字既做基础色（不发光环境也看得见），也做自发光贴图（暗光下像真的数显）
            _mat.SetTexture("_BaseMap", _tex);
            _mat.SetColor("_BaseColor", Color.white);
            _mat.SetTexture("_EmissionMap", _tex);
            _mat.EnableKeyword("_EMISSION");
            _mat.SetColor("_EmissionColor", new Color(0.32f, 0.95f, 1f) * 1.5f);
            _mat.globalIlluminationFlags = MaterialGlobalIlluminationFlags.RealtimeEmissive;
            _mat.SetFloat("_Smoothness", 0.2f);
            _mat.SetFloat("_Metallic", 0f);

            // 用 renderer.material 赋值会得到属于该渲染器的实例，不会污染 MatLib 缓存里的共享材质
            renderer.material = _mat;
            _mat = renderer.material;
        }

        /// <summary>更新显示。温度变化小于 0.05℃ 且开关状态没变时直接返回，避免每帧重画纹理。</summary>
        public void SetValue(float temperature, bool deviceOn)
        {
            if (_drawn && deviceOn == _lastOn && !float.IsNaN(_lastTemp) && Mathf.Abs(temperature - _lastTemp) < 0.05f)
                return;

            _lastTemp = temperature;
            _lastOn = deviceOn;
            _drawn = true;

            Draw(temperature, deviceOn);
        }

        // ================================================================= 绘制

        void Draw(float temperature, bool deviceOn)
        {
            for (int i = 0; i < _buf.Length; i++) _buf[i] = _bg;

            string text = temperature.ToString("0.0", CultureInfo.InvariantCulture);
            if (text.Length > 5) text = text.Substring(0, 5);

            Color32 fg = deviceOn ? _on : _off;

            // 版面：数码管高 44，宽 24，间隔 5，小数点占 8
            const int digitH = 44;
            const int digitW = 24;
            const int gap = 5;
            const int dotW = 7;
            const int topMargin = 10;   // 纹理坐标系是"左右下角"为原点，这里表示离顶部的距离

            int total = 0;
            foreach (char c in text) total += (c == '.' || c == '-') ? (dotW + gap) : (digitW + gap);
            total -= gap;
            int x = Mathf.Max(3, (TW - total) / 2);
            int yBot = TH - topMargin - digitH;

            foreach (char c in text)
            {
                if (c == '.')
                {
                    Fill(x, yBot + 2, dotW - 1, 7, fg);
                    x += dotW + gap;
                    continue;
                }

                if (c == '-')
                {
                    Fill(x, yBot + digitH / 2 - 2, dotW + 1, 5, fg);
                    x += dotW + gap;
                    continue;
                }

                int d = c - '0';
                if (d < 0 || d > 9)
                {
                    x += digitW + gap;
                    continue;
                }

                DrawDigit(x, yBot, digitW, digitH, d, fg);
                x += digitW + gap;
            }

            _tex.SetPixels32(_buf);
            _tex.Apply(false, false);
        }

        /// <summary>七段数码管的段码表：顺序 a b c d e f g。</summary>
        static readonly bool[,] Segments =
        {
            // a     b      c      d      e      f      g
            { true,  true,  true,  true,  true,  true,  false }, // 0
            { false, true,  true,  false, false, false, false }, // 1
            { true,  true,  false, true,  true,  false, true  }, // 2
            { true,  true,  true,  true,  false, false, true  }, // 3
            { false, true,  true,  false, false, true,  true  }, // 4
            { true,  false, true,  true,  false, true,  true  }, // 5
            { true,  false, true,  true,  true,  true,  true  }, // 6
            { true,  true,  true,  false, false, false, false }, // 7
            { true,  true,  true,  true,  true,  true,  true  }, // 8
            { true,  true,  true,  true,  false, true,  true  }  // 9
        };

        void DrawDigit(int x, int y, int w, int h, int digit, Color32 c)
        {
            int t = Mathf.Max(3, w / 6);          // 笔画粗细
            int half = (h - t) / 2;

            if (Segments[digit, 0]) Fill(x + t, y + h - t, w - 2 * t, t, c);   // a 上
            if (Segments[digit, 1]) Fill(x + w - t, y + half + t, t, half - t, c); // b 右上
            if (Segments[digit, 2]) Fill(x + w - t, y + t, t, half - t, c);   // c 右下
            if (Segments[digit, 3]) Fill(x + t, y, w - 2 * t, t, c);          // d 下
            if (Segments[digit, 4]) Fill(x, y + t, t, half - t, c);           // e 左下
            if (Segments[digit, 5]) Fill(x, y + half + t, t, half - t, c);    // f 左上
            if (Segments[digit, 6]) Fill(x + t, y + half, w - 2 * t, t, c);   // g 中
        }

        /// <summary>用底部为原点的坐标填充一个矩形，自动裁剪到纹理范围内。</summary>
        void Fill(int x, int y, int w, int h, Color32 c)
        {
            int x0 = Mathf.Clamp(x, 0, TW);
            int x1 = Mathf.Clamp(x + w, 0, TW);
            int y0 = Mathf.Clamp(y, 0, TH);
            int y1 = Mathf.Clamp(y + h, 0, TH);

            for (int yy = y0; yy < y1; yy++)
            {
                int row = yy * TW;
                for (int xx = x0; xx < x1; xx++)
                    _buf[row + xx] = c;
            }
        }
    }
}
