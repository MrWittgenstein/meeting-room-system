using UnityEngine;

namespace NordicRoom
{
    /// <summary>
    /// 程序化建模图元。
    /// 只保留经过验证可靠的网格（盒体 / 圆柱 / 球 / 圆盘 / 圆角盒）；
    /// 之前那个自写的"椭圆板"网格在 URP 下光照异常，这里不再使用。
    /// </summary>
    public static class Prim
    {
        static Mesh _rounded, _disc, _cyl;

        /// <summary>圆台面网格 / 圆盘网格（暴露给编辑器自检，用来校验法线朝向）。</summary>
        public static Mesh RoundSlabMesh { get { if (_cyl == null) _cyl = BuildCylinderMesh(72); return _cyl; } }
        public static Mesh DiscMeshRef => DiscMesh();

        // ---------------------------------------------------------------- 盒 / 柱 / 球

        public static GameObject Box(string name, Transform parent, Vector3 center, Vector3 size,
            Material mat, Quaternion? rot = null, bool collider = false)
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

        public static GameObject BoxBetween(string name, Transform parent, Vector3 min, Vector3 max,
            Material mat, bool collider = false)
        {
            var c = (min + max) * 0.5f;
            var s = new Vector3(Mathf.Abs(max.x - min.x), Mathf.Abs(max.y - min.y), Mathf.Abs(max.z - min.z));
            return Box(name, parent, c, s, mat, null, collider);
        }

        /// <summary>圆柱：Unity 默认圆柱高 2、直径 1，这里按"半径 + 实际高度"换算。</summary>
        public static GameObject Cylinder(string name, Transform parent, Vector3 center, float radius,
            float height, Material mat, Quaternion? rot = null, bool collider = false, int segs = 0)
        {
            var go = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            go.name = name;
            go.transform.SetParent(parent, false);
            go.transform.localPosition = center;
            go.transform.localRotation = rot ?? Quaternion.identity;
            go.transform.localScale = new Vector3(radius * 2f, height * 0.5f, radius * 2f);
            var col = go.GetComponent<Collider>();
            if (!collider && col != null) Object.DestroyImmediate(col);
            go.GetComponent<MeshRenderer>().sharedMaterial = mat;
            return go;
        }

        public static GameObject Sphere(string name, Transform parent, Vector3 center, float radius,
            Material mat, bool collider = false)
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

        // ---------------------------------------------------------------- 圆角盒

        public static Mesh RoundedMesh()
        {
            if (_rounded != null) return _rounded;
            const int seg = 18, rings = 11;
            const float p = 6.0f;
            var verts = new System.Collections.Generic.List<Vector3>();
            var norms = new System.Collections.Generic.List<Vector3>();
            var uvs = new System.Collections.Generic.List<Vector2>();
            var tris = new System.Collections.Generic.List<int>();

            for (int j = 0; j <= rings; j++)
            {
                float v = (float)j / rings, theta = v * Mathf.PI;
                for (int i = 0; i <= seg; i++)
                {
                    float u = (float)i / seg, phi = u * Mathf.PI * 2f;
                    float sx = Mathf.Sin(theta) * Mathf.Cos(phi);
                    float sy = Mathf.Cos(theta);
                    float sz = Mathf.Sin(theta) * Mathf.Sin(phi);
                    float den = Mathf.Pow(Mathf.Pow(Mathf.Abs(sx), p) + Mathf.Pow(Mathf.Abs(sy), p)
                                        + Mathf.Pow(Mathf.Abs(sz), p), 1f / p);
                    if (den < 1e-5f) den = 1e-5f;
                    verts.Add(new Vector3(sx, sy, sz) / den * 0.5f);
                    norms.Add(new Vector3(sx, sy, sz).normalized);
                    uvs.Add(new Vector2(u, 1f - v));
                }
            }
            for (int j = 0; j < rings; j++)
                for (int i = 0; i < seg; i++)
                {
                    int a = j * (seg + 1) + i, b = a + seg + 1;
                    tris.Add(a); tris.Add(b); tris.Add(a + 1);
                    tris.Add(a + 1); tris.Add(b); tris.Add(b + 1);
                }
            _rounded = new Mesh { name = "RoundedBox" };
            _rounded.SetVertices(verts); _rounded.SetNormals(norms); _rounded.SetUVs(0, uvs);
            _rounded.SetTriangles(tris, 0); _rounded.RecalculateBounds();
            return _rounded;
        }

        public static GameObject RoundedBox(string name, Transform parent, Vector3 center, Vector3 size,
            Material mat, Quaternion? rot = null)
        {
            var go = new GameObject(name);
            go.transform.SetParent(parent, false);
            go.transform.localPosition = center;
            go.transform.localRotation = rot ?? Quaternion.identity;
            go.transform.localScale = size;
            go.AddComponent<MeshFilter>().sharedMesh = RoundedMesh();
            var r = go.AddComponent<MeshRenderer>();
            r.sharedMaterial = mat;
            r.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.On;
            r.receiveShadows = true;
            return go;
        }

        /// <summary>圆柱形台面（圆桌用）：一个扁圆柱，顶面朝上受光正常。</summary>
        public static GameObject RoundSlab(string name, Transform parent, Vector3 center, float radius,
            float thickness, Material mat)
        {
            if (_cyl == null) _cyl = BuildCylinderMesh(72);
            var go = new GameObject(name);
            go.transform.SetParent(parent, false);
            go.transform.localPosition = center;
            go.transform.localScale = new Vector3(radius * 2f, thickness, radius * 2f);
            go.AddComponent<MeshFilter>().sharedMesh = _cyl;
            var r = go.AddComponent<MeshRenderer>();
            r.sharedMaterial = mat;
            r.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.On;
            r.receiveShadows = true;
            return go;
        }

        static Mesh BuildCylinderMesh(int seg)
        {
            var verts = new System.Collections.Generic.List<Vector3>();
            var norms = new System.Collections.Generic.List<Vector3>();
            var uvs = new System.Collections.Generic.List<Vector2>();
            var tris = new System.Collections.Generic.List<int>();

            // 顶面（注意绕序：从上方看必须是逆时针，否则会被背面剔除而整个面不可见）
            verts.Add(new Vector3(0f, 0.5f, 0f)); norms.Add(Vector3.up); uvs.Add(new Vector2(0.5f, 0.5f));
            for (int i = 0; i <= seg; i++)
            {
                float a = (float)i / seg * Mathf.PI * 2f;
                float x = Mathf.Cos(a) * 0.5f, z = Mathf.Sin(a) * 0.5f;
                verts.Add(new Vector3(x, 0.5f, z)); norms.Add(Vector3.up);
                uvs.Add(new Vector2(x + 0.5f, z + 0.5f));
            }
            for (int i = 1; i <= seg; i++) { tris.Add(0); tris.Add(i + 1); tris.Add(i); }

            // 底面
            int bc = verts.Count;
            verts.Add(new Vector3(0f, -0.5f, 0f)); norms.Add(Vector3.down); uvs.Add(new Vector2(0.5f, 0.5f));
            int bs = verts.Count;
            for (int i = 0; i <= seg; i++)
            {
                float a = (float)i / seg * Mathf.PI * 2f;
                float x = Mathf.Cos(a) * 0.5f, z = Mathf.Sin(a) * 0.5f;
                verts.Add(new Vector3(x, -0.5f, z)); norms.Add(Vector3.down);
                uvs.Add(new Vector2(x + 0.5f, z + 0.5f));
            }
            for (int i = 1; i <= seg; i++) { tris.Add(bc); tris.Add(bs + i); tris.Add(bs + i + 1); }

            // 侧壁
            for (int i = 0; i < seg; i++)
            {
                float a0 = (float)i / seg * Mathf.PI * 2f, a1 = (float)(i + 1) / seg * Mathf.PI * 2f;
                var n0 = new Vector3(Mathf.Cos(a0), 0f, Mathf.Sin(a0));
                var n1 = new Vector3(Mathf.Cos(a1), 0f, Mathf.Sin(a1));
                int baseIdx = verts.Count;
                verts.Add(new Vector3(n0.x * 0.5f, -0.5f, n0.z * 0.5f)); norms.Add(n0); uvs.Add(new Vector2((float)i / seg, 0f));
                verts.Add(new Vector3(n1.x * 0.5f, -0.5f, n1.z * 0.5f)); norms.Add(n1); uvs.Add(new Vector2((float)(i + 1) / seg, 0f));
                verts.Add(new Vector3(n1.x * 0.5f, 0.5f, n1.z * 0.5f)); norms.Add(n1); uvs.Add(new Vector2((float)(i + 1) / seg, 1f));
                verts.Add(new Vector3(n0.x * 0.5f, 0.5f, n0.z * 0.5f)); norms.Add(n0); uvs.Add(new Vector2((float)i / seg, 1f));
                tris.Add(baseIdx); tris.Add(baseIdx + 1); tris.Add(baseIdx + 2);
                tris.Add(baseIdx); tris.Add(baseIdx + 2); tris.Add(baseIdx + 3);
            }

            var m = new Mesh { name = "CylinderSlab" };
            m.SetVertices(verts); m.SetNormals(norms); m.SetUVs(0, uvs);
            m.SetTriangles(tris, 0); m.RecalculateBounds();
            return m;
        }

        // ---------------------------------------------------------------- 圆盘 / 命中盒

        /// <summary>
        /// 发光圆盘。默认旋转 Rx(+90°) 后法线朝 **下**（筒灯 / 吊灯 / 探头用），
        /// 所以法线就按 -Y 写。
        /// </summary>
        public static Mesh DiscMesh()
        {
            if (_disc != null) return _disc;
            const int seg = 56;
            var verts = new System.Collections.Generic.List<Vector3> { Vector3.zero };
            var norms = new System.Collections.Generic.List<Vector3> { Vector3.down };
            var uvs = new System.Collections.Generic.List<Vector2> { new Vector2(0.5f, 0.5f) };
            for (int i = 0; i <= seg; i++)
            {
                float a = (float)i / seg * Mathf.PI * 2f;
                verts.Add(new Vector3(Mathf.Cos(a) * 0.5f, 0f, Mathf.Sin(a) * 0.5f));
                norms.Add(Vector3.down);
                uvs.Add(new Vector2(Mathf.Cos(a) * 0.5f + 0.5f, Mathf.Sin(a) * 0.5f + 0.5f));
            }
            var tris = new System.Collections.Generic.List<int>();
            // 绕序：法线朝上，从 +Y 看必须逆时针
            for (int i = 1; i <= seg; i++) { tris.Add(0); tris.Add(i); tris.Add(i + 1); }
            _disc = new Mesh { name = "Disc" };
            _disc.SetVertices(verts); _disc.SetNormals(norms); _disc.SetUVs(0, uvs);
            _disc.SetTriangles(tris, 0); _disc.RecalculateBounds();
            return _disc;
        }

        public static GameObject Disc(string name, Transform parent, Vector3 center, float radius,
            Material mat, Quaternion? rot = null)
        {
            var go = new GameObject(name);
            go.transform.SetParent(parent, false);
            go.transform.localPosition = center;
            go.transform.localRotation = rot ?? Quaternion.Euler(90f, 0f, 0f);
            go.transform.localScale = new Vector3(radius * 2f, radius * 2f, 1f);
            go.AddComponent<MeshFilter>().sharedMesh = DiscMesh();
            var r = go.AddComponent<MeshRenderer>();
            r.sharedMaterial = mat;
            r.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;
            r.receiveShadows = true;
            return go;
        }

        /// <summary>
        /// 开口圆锥台（灯罩专用）：只有侧壁、没有底盖，法线严格朝外。
        /// 用两段圆柱去叠灯罩会留下朝下的内表面和阶梯缝，渲出来就是一块死黑的"锥子"，
        /// 所以这里改成一次性生成的锥台网格。
        /// </summary>
        public static Mesh ConeBandMesh(float rTop, float rBot, float h, int seg)
        {
            var verts = new System.Collections.Generic.List<Vector3>();
            var norms = new System.Collections.Generic.List<Vector3>();
            var uvs = new System.Collections.Generic.List<Vector2>();
            var tris = new System.Collections.Generic.List<int>();

            float dr = rTop - rBot;
            float nl = Mathf.Sqrt(h * h + dr * dr);
            float ky = nl > 1e-6f ? dr / nl : 0f;
            float kx = nl > 1e-6f ? h / nl : 1f;

            for (int i = 0; i <= seg; i++)
            {
                float a = (float)i / seg * Mathf.PI * 2f;
                float cx = Mathf.Cos(a), cz = Mathf.Sin(a);
                var n = new Vector3(cx * kx, ky, cz * kx).normalized;

                verts.Add(new Vector3(cx * rBot, 0f, cz * rBot)); norms.Add(n); uvs.Add(new Vector2((float)i / seg, 0f));
                verts.Add(new Vector3(cx * rTop, h, cz * rTop)); norms.Add(n); uvs.Add(new Vector2((float)i / seg, 1f));
            }
            for (int i = 0; i < seg; i++)
            {
                int b = i * 2;
                tris.Add(b); tris.Add(b + 1); tris.Add(b + 2);
                tris.Add(b); tris.Add(b + 2); tris.Add(b + 3);
            }

            var m = new Mesh { name = "ConeBand" };
            m.SetVertices(verts); m.SetNormals(norms); m.SetUVs(0, uvs);
            m.SetTriangles(tris, 0); m.RecalculateBounds();
            return m;
        }

        public static GameObject ConeBand(string name, Transform parent, Vector3 baseCenter, float rTop, float rBot,
            float height, Material mat, int seg = 48)
        {
            var go = new GameObject(name);
            go.transform.SetParent(parent, false);
            go.transform.localPosition = baseCenter;
            go.AddComponent<MeshFilter>().sharedMesh = ConeBandMesh(rTop, rBot, height, seg);
            var r = go.AddComponent<MeshRenderer>();
            r.sharedMaterial = mat;
            r.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;
            r.receiveShadows = true;
            return go;
        }

        /// <summary>只带碰撞体的透明命中盒（用于鼠标点选设备）。</summary>
        public static GameObject Hitbox(string name, Transform parent, Vector3 center, Vector3 size)
        {
            var go = new GameObject(name);
            go.transform.SetParent(parent, false);
            go.transform.localPosition = center;
            var bc = go.AddComponent<BoxCollider>();
            bc.size = size;
            return go;
        }
    }
}
