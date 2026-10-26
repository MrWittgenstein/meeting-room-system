using UnityEngine;

namespace SmartRoom
{
    /// <summary>
    /// 程序化建模图元工具：所有家具都由这些盒体/圆柱/圆角盒拼出。
    /// </summary>
    public static class Prim
    {
        static Mesh _roundedBox;
        static Mesh _disc;
        static Mesh _ring;

        /// <summary>创建一个盒体。size 为实际尺寸（米），不是 Unity 默认 Cube 的 1 单位缩放。</summary>
        public static GameObject Box(string name, Transform parent, Vector3 center, Vector3 size, Material mat,
            Quaternion? rot = null, bool collider = false)
        {
            var go = GameObject.CreatePrimitive(PrimitiveType.Cube);
            go.name = name;
            go.transform.SetParent(parent, false);
            go.transform.localPosition = center;
            go.transform.localRotation = rot ?? Quaternion.identity;
            go.transform.localScale = size;

            var col = go.GetComponent<Collider>();
            if (!collider && col != null) Object.DestroyImmediate(col);

            var r = go.GetComponent<MeshRenderer>();
            r.sharedMaterial = mat;
            r.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.On;
            r.receiveShadows = true;
            return go;
        }

        /// <summary>以两个角点定义一个盒体（便于和墙体坐标对齐）。</summary>
        public static GameObject BoxBetween(string name, Transform parent, Vector3 min, Vector3 max, Material mat,
            bool collider = false)
        {
            var center = (min + max) * 0.5f;
            var size = new Vector3(Mathf.Abs(max.x - min.x), Mathf.Abs(max.y - min.y), Mathf.Abs(max.z - min.z));
            return Box(name, parent, center, size, mat, null, collider);
        }

        public static GameObject Cylinder(string name, Transform parent, Vector3 center, float radius, float height,
            Material mat, Quaternion? rot = null, int segments = 24, bool collider = false)
        {
            var go = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            go.name = name;
            go.transform.SetParent(parent, false);
            go.transform.localPosition = center;
            // Unity 圆柱默认高 2 单位、直径 1 单位
            go.transform.localRotation = rot ?? Quaternion.identity;
            go.transform.localScale = new Vector3(radius * 2f, height * 0.5f, radius * 2f);

            var col = go.GetComponent<Collider>();
            if (!collider && col != null) Object.DestroyImmediate(col);

            go.GetComponent<MeshRenderer>().sharedMaterial = mat;
            return go;
        }

        public static GameObject Sphere(string name, Transform parent, Vector3 center, float radius, Material mat,
            bool collider = false)
        {
            var go = GameObject.CreatePrimitive(PrimitiveType.Sphere);
            go.name = name;
            go.transform.SetParent(parent, false);
            go.transform.localPosition = center;
            go.transform.localScale = Vector3.one * (radius * 2f);

            var col = go.GetComponent<Collider>();
            if (!collider && col != null) Object.DestroyImmediate(col);

            go.GetComponent<MeshRenderer>().sharedMaterial = mat;
            return go;
        }

        public static GameObject Quad(string name, Transform parent, Vector3 center, Vector2 size, Material mat,
            Quaternion? rot = null)
        {
            var go = GameObject.CreatePrimitive(PrimitiveType.Quad);
            go.name = name;
            go.transform.SetParent(parent, false);
            go.transform.localPosition = center;
            go.transform.localRotation = rot ?? Quaternion.identity;
            go.transform.localScale = new Vector3(size.x, size.y, 1f);
            Object.DestroyImmediate(go.GetComponent<Collider>());
            go.GetComponent<MeshRenderer>().sharedMaterial = mat;
            return go;
        }

        // -------------------------------------------------------------- 圆角盒（桌板、坐垫）

        /// <summary>圆角盒网格：比 Cube 更像真实家具，用于桌板/坐垫等。</summary>
        public static Mesh RoundedBoxMesh()
        {
            if (_roundedBox != null) return _roundedBox;

            // 用一个细分球体做"膨胀"，形成圆角；这里采用简化的倒角盒：8 角球 + 棱柱
            // 实现方式：以球面坐标生成一个"超椭球"，p 越大越接近立方体。
            const int seg = 20;
            const int rings = 12;
            const float p = 6.5f; // 形状指数：越大越方

            var verts = new System.Collections.Generic.List<Vector3>();
            var norms = new System.Collections.Generic.List<Vector3>();
            var tris = new System.Collections.Generic.List<int>();
            var uvs = new System.Collections.Generic.List<Vector2>();

            for (int j = 0; j <= rings; j++)
            {
                float v = (float)j / rings;
                float theta = v * Mathf.PI;           // 0..PI 从上到下
                for (int i = 0; i <= seg; i++)
                {
                    float u = (float)i / seg;
                    float phi = u * Mathf.PI * 2f;

                    float sx = Mathf.Sin(theta) * Mathf.Cos(phi);
                    float sy = Mathf.Cos(theta);
                    float sz = Mathf.Sin(theta) * Mathf.Sin(phi);

                    // 超椭球：|x|^p+|y|^p+|z|^p = 1
                    float denom = Mathf.Pow(Mathf.Pow(Mathf.Abs(sx), p) + Mathf.Pow(Mathf.Abs(sy), p) + Mathf.Pow(Mathf.Abs(sz), p), 1f / p);
                    if (denom < 1e-5f) denom = 1e-5f;
                    var dir = new Vector3(sx, sy, sz) / denom;

                    verts.Add(dir * 0.5f);
                    norms.Add(new Vector3(sx, sy, sz).normalized);
                    uvs.Add(new Vector2(u, 1f - v));
                }
            }

            for (int j = 0; j < rings; j++)
            {
                for (int i = 0; i < seg; i++)
                {
                    int a = j * (seg + 1) + i;
                    int b = a + seg + 1;
                    tris.Add(a); tris.Add(b); tris.Add(a + 1);
                    tris.Add(a + 1); tris.Add(b); tris.Add(b + 1);
                }
            }

            _roundedBox = new Mesh { name = "RoundedBox" };
            _roundedBox.SetVertices(verts);
            _roundedBox.SetNormals(norms);
            _roundedBox.SetUVs(0, uvs);
            _roundedBox.SetTriangles(tris, 0);
            _roundedBox.RecalculateBounds();
            return _roundedBox;
        }

        /// <summary>圆角盒物体。size 为实际尺寸，corner 为圆角半径。</summary>
        public static GameObject RoundedBox(string name, Transform parent, Vector3 center, Vector3 size,
            Material mat, Quaternion? rot = null)
        {
            var go = new GameObject(name);
            go.transform.SetParent(parent, false);
            go.transform.localPosition = center;
            go.transform.localRotation = rot ?? Quaternion.identity;
            go.transform.localScale = size;

            var mf = go.AddComponent<MeshFilter>();
            mf.sharedMesh = RoundedBoxMesh();
            var mr = go.AddComponent<MeshRenderer>();
            mr.sharedMaterial = mat;
            mr.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.On;
            mr.receiveShadows = true;
            return go;
        }

        // -------------------------------------------------------------- 平面的圆盘/圆环

        public static Mesh DiscMesh()
        {
            if (_disc != null) return _disc;
            const int seg = 48;
            var verts = new System.Collections.Generic.List<Vector3> { Vector3.zero };
            var norms = new System.Collections.Generic.List<Vector3> { Vector3.up };
            var uvs = new System.Collections.Generic.List<Vector2> { new Vector2(0.5f, 0.5f) };
            for (int i = 0; i <= seg; i++)
            {
                float a = (float)i / seg * Mathf.PI * 2f;
                verts.Add(new Vector3(Mathf.Cos(a) * 0.5f, 0f, Mathf.Sin(a) * 0.5f));
                norms.Add(Vector3.up);
                uvs.Add(new Vector2(Mathf.Cos(a) * 0.5f + 0.5f, Mathf.Sin(a) * 0.5f + 0.5f));
            }
            var tris = new System.Collections.Generic.List<int>();
            for (int i = 1; i <= seg; i++) { tris.Add(0); tris.Add(i + 1); tris.Add(i); }
            _disc = new Mesh { name = "Disc" };
            _disc.SetVertices(verts); _disc.SetNormals(norms); _disc.SetUVs(0, uvs);
            _disc.SetTriangles(tris, 0); _disc.RecalculateBounds();
            return _disc;
        }

        public static GameObject Disc(string name, Transform parent, Vector3 center, float radius, Material mat,
            Quaternion? rot = null)
        {
            var go = new GameObject(name);
            go.transform.SetParent(parent, false);
            go.transform.localPosition = center;
            go.transform.localRotation = rot ?? Quaternion.identity;
            go.transform.localScale = new Vector3(radius * 2f, 1f, radius * 2f);
            go.AddComponent<MeshFilter>().sharedMesh = DiscMesh();
            var mr = go.AddComponent<MeshRenderer>();
            mr.sharedMaterial = mat;
            mr.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;
            mr.receiveShadows = false;
            return go;
        }

        /// <summary>带碰撞体的盒体（可交互设备用，供鼠标射线命中）。</summary>
        public static GameObject BoxCollidable(string name, Transform parent, Vector3 center, Vector3 size,
            Material mat, Quaternion? rot = null)
        {
            return Box(name, parent, center, size, mat, rot, true);
        }
    }
}
