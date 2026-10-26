using UnityEngine;

namespace WarmRoom
{
    /// <summary>程序化建模图元：盒体 / 圆柱 / 球 / 圆盘 / 圆角盒 / 椭圆板。</summary>
    public static class Prim
    {
        static Mesh _rounded, _disc, _ellipse, _cylinder;

        // ---------------------------------------------------------------- 基本体

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

        public static GameObject Cylinder(string name, Transform parent, Vector3 center, float radius,
            float height, Material mat, Quaternion? rot = null, bool collider = false)
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
            const float p = 6.0f;   // 形状指数，越大越方
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

        // ---------------------------------------------------------------- 圆盘 / 椭圆

        public static Mesh DiscMesh()
        {
            if (_disc != null) return _disc;
            const int seg = 56;
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
        /// 带倒角的长椭圆板（会议桌桌面）。
        /// 用超椭圆参数曲线生成：|x/a|^n + |z/b|^n = 1，n 越大越接近矩形。
        /// 相对普通 Cube 更像"船形/长椭圆实木会议桌"。
        /// </summary>
        public static Mesh EllipseMesh(int seg = 72, float n = 3.2f)
        {
            string key = "ellipse_" + seg + "_" + n.ToString("0.0");
            if (Cache.TryGetValue(key, out var m) && m != null) return m;

            var verts = new System.Collections.Generic.List<Vector3>();
            var norms = new System.Collections.Generic.List<Vector3>();
            var uvs = new System.Collections.Generic.List<Vector2>();
            var tris = new System.Collections.Generic.List<int>();

            // 顶面中心 + 边缘
            verts.Add(new Vector3(0f, 0.5f, 0f));
            norms.Add(Vector3.up);
            uvs.Add(new Vector2(0.5f, 0.5f));
            for (int i = 0; i <= seg; i++)
            {
                float t = (float)i / seg * Mathf.PI * 2f;
                float ct = Mathf.Cos(t), st = Mathf.Sin(t);
                float x = Mathf.Sign(ct) * Mathf.Pow(Mathf.Abs(ct), 2f / n) * 0.5f;
                float z = Mathf.Sign(st) * Mathf.Pow(Mathf.Abs(st), 2f / n) * 0.5f;
                verts.Add(new Vector3(x, 0.5f, z));
                norms.Add(Vector3.up);
                uvs.Add(new Vector2(x + 0.5f, z + 0.5f));
            }
            for (int i = 1; i <= seg; i++) { tris.Add(0); tris.Add(i); tris.Add(i + 1); }

            int bottomCenter = verts.Count;
            verts.Add(new Vector3(0f, -0.5f, 0f));
            norms.Add(Vector3.down);
            uvs.Add(new Vector2(0.5f, 0.5f));
            int bottomStart = verts.Count;
            for (int i = 0; i <= seg; i++)
            {
                float t = (float)i / seg * Mathf.PI * 2f;
                float ct = Mathf.Cos(t), st = Mathf.Sin(t);
                float x = Mathf.Sign(ct) * Mathf.Pow(Mathf.Abs(ct), 2f / n) * 0.5f;
                float z = Mathf.Sign(st) * Mathf.Pow(Mathf.Abs(st), 2f / n) * 0.5f;
                verts.Add(new Vector3(x, -0.5f, z));
                norms.Add(Vector3.down);
                uvs.Add(new Vector2(x + 0.5f, z + 0.5f));
            }
            for (int i = 1; i <= seg; i++)
            {
                tris.Add(bottomCenter); tris.Add(bottomStart + i + 1); tris.Add(bottomStart + i);
            }

            // 侧壁
            for (int i = 0; i < seg; i++)
            {
                int t0 = 1 + i, t1 = 1 + i + 1;
                int b0 = bottomStart + i, b1 = bottomStart + i + 1;
                var dir = ((verts[t0] + verts[t1]) * 0.5f - (verts[b0] + verts[b1]) * 0.5f).normalized;
                var nrm = new Vector3(dir.x, 0f, dir.z).normalized;
                int baseIdx = verts.Count;
                verts.Add(verts[b0]); norms.Add(nrm); uvs.Add(new Vector2((float)i / seg, 0f));
                verts.Add(verts[b1]); norms.Add(nrm); uvs.Add(new Vector2((float)(i + 1) / seg, 0f));
                verts.Add(verts[t1]); norms.Add(nrm); uvs.Add(new Vector2((float)(i + 1) / seg, 1f));
                verts.Add(verts[t0]); norms.Add(nrm); uvs.Add(new Vector2((float)i / seg, 1f));
                tris.Add(baseIdx); tris.Add(baseIdx + 1); tris.Add(baseIdx + 2);
                tris.Add(baseIdx); tris.Add(baseIdx + 2); tris.Add(baseIdx + 3);
            }

            m = new Mesh { name = "EllipseSlab" };
            m.SetVertices(verts); m.SetNormals(norms); m.SetUVs(0, uvs);
            m.SetTriangles(tris, 0); m.RecalculateBounds();
            Cache[key] = m;
            return m;
        }

        static readonly System.Collections.Generic.Dictionary<string, Mesh> Cache =
            new System.Collections.Generic.Dictionary<string, Mesh>();

        /// <summary>长椭圆板物体：size = (X 长, Y 厚, Z 宽)。</summary>
        public static GameObject EllipseSlab(string name, Transform parent, Vector3 center, Vector3 size,
            Material mat, float n = 3.2f)
        {
            var go = new GameObject(name);
            go.transform.SetParent(parent, false);
            go.transform.localPosition = center;
            go.transform.localScale = size;
            go.AddComponent<MeshFilter>().sharedMesh = EllipseMesh(72, n);
            var r = go.AddComponent<MeshRenderer>();
            r.sharedMaterial = mat;
            r.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.On;
            r.receiveShadows = true;
            return go;
        }

        /// <summary>带碰撞体的盒体（可交互命中区用）。</summary>
        public static GameObject BoxCollidable(string name, Transform parent, Vector3 center, Vector3 size,
            Material mat, Quaternion? rot = null)
            => Box(name, parent, center, size, mat, rot, true);

        /// <summary>只留碰撞体、去掉渲染体的透明命中盒。</summary>
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
