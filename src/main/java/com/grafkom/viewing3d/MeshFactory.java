package com.grafkom.viewing3d;

import org.joml.Vector3f;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MeshFactory {

    public static Mesh createSphere(float radius, int stacks, int sectors, boolean smooth) {
        int vCount = (stacks + 1) * (sectors + 1);
        float[] pos = new float[vCount * 3];
        float[] uv = new float[vCount * 2];

        int p = 0, t = 0;
        for (int i = 0; i <= stacks; i++) {
            float phi = (float) Math.PI * i / stacks;          // 0 (kutub atas) sampai PI (kutub bawah)
            for (int j = 0; j <= sectors; j++) {
                float theta = 2f * (float) Math.PI * j / sectors;
                pos[p++] = radius * (float) (Math.sin(phi) * Math.cos(theta));
                pos[p++] = radius * (float) Math.cos(phi);
                pos[p++] = radius * (float) (Math.sin(phi) * Math.sin(theta));
                uv[t++] = (float) j / sectors;
                uv[t++] = 1f - (float) i / stacks;
            }
        }

        int[] idx = new int[stacks * sectors * 6];
        int k = 0;
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < sectors; j++) {
                int a = i * (sectors + 1) + j;
                int b = a + sectors + 1;
                int c = a + 1;
                int d = b + 1;
                // urutan CCW jika dilihat dari luar bola
                idx[k++] = a; idx[k++] = c; idx[k++] = b;
                idx[k++] = c; idx[k++] = d; idx[k++] = b;
            }
        }
        return build(pos, uv, idx, smooth);
    }

    // Bidang datar pada y = 0, membentang -half sampai +half; tile = jumlah pengulangan tekstur
    public static Mesh createPlane(float half, float tile) {
        float[] pos = {
            -half, 0f,  half,
             half, 0f,  half,
             half, 0f, -half,
            -half, 0f, -half
        };
        float[] uv = {
            0f, 0f,
            tile, 0f,
            tile, tile,
            0f, tile
        };
        int[] idx = { 0, 1, 2, 0, 2, 3 };   // CCW jika dilihat dari atas
        return build(pos, uv, idx, true);
    }

    private static Mesh build(float[] pos, float[] uv, int[] idx, boolean smooth) {
        if (smooth) {
            float[] normals = computeSmoothNormals(pos, idx);
            int vCount = pos.length / 3;
            float[] data = new float[vCount * 8];
            for (int v = 0; v < vCount; v++) {
                data[v * 8]     = pos[v * 3];
                data[v * 8 + 1] = pos[v * 3 + 1];
                data[v * 8 + 2] = pos[v * 3 + 2];
                data[v * 8 + 3] = normals[v * 3];
                data[v * 8 + 4] = normals[v * 3 + 1];
                data[v * 8 + 5] = normals[v * 3 + 2];
                data[v * 8 + 6] = uv[v * 2];
                data[v * 8 + 7] = uv[v * 2 + 1];
            }
            return new Mesh(data, idx);
        }

        // Flat shading: setiap segitiga memakai vertex sendiri dengan normal face
        int triCount = idx.length / 3;
        float[] data = new float[triCount * 3 * 8];
        int[] indices = new int[triCount * 3];
        int n = 0;

        for (int tri = 0; tri < triCount; tri++) {
            int[] corner = { idx[tri * 3], idx[tri * 3 + 1], idx[tri * 3 + 2] };
            Vector3f a = vertex(pos, corner[0]);
            Vector3f b = vertex(pos, corner[1]);
            Vector3f c = vertex(pos, corner[2]);
            Vector3f fn = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
            if (fn.lengthSquared() < 1e-12f) continue;   // segitiga degenerat (di kutub)
            fn.normalize();

            for (int k = 0; k < 3; k++) {
                int v = corner[k];
                int o = n * 8;
                data[o]     = pos[v * 3];
                data[o + 1] = pos[v * 3 + 1];
                data[o + 2] = pos[v * 3 + 2];
                data[o + 3] = fn.x;
                data[o + 4] = fn.y;
                data[o + 5] = fn.z;
                data[o + 6] = uv[v * 2];
                data[o + 7] = uv[v * 2 + 1];
                indices[n] = n;
                n++;
            }
        }
        return new Mesh(Arrays.copyOf(data, n * 8), Arrays.copyOf(indices, n));
    }

    // Vertex normal averaging: jumlahkan normal semua face pada posisi yang sama, lalu normalisasi
    private static float[] computeSmoothNormals(float[] pos, int[] idx) {
        Map<String, Vector3f> acc = new HashMap<>();
        for (int tri = 0; tri < idx.length / 3; tri++) {
            int i0 = idx[tri * 3], i1 = idx[tri * 3 + 1], i2 = idx[tri * 3 + 2];
            Vector3f a = vertex(pos, i0);
            Vector3f b = vertex(pos, i1);
            Vector3f c = vertex(pos, i2);
            Vector3f fn = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));  // belum dinormalisasi (berbobot luas)
            acc.computeIfAbsent(key(pos, i0), k -> new Vector3f()).add(fn);
            acc.computeIfAbsent(key(pos, i1), k -> new Vector3f()).add(fn);
            acc.computeIfAbsent(key(pos, i2), k -> new Vector3f()).add(fn);
        }

        float[] out = new float[pos.length];
        for (int v = 0; v < pos.length / 3; v++) {
            Vector3f n = new Vector3f(acc.get(key(pos, v)));
            if (n.lengthSquared() > 1e-12f) n.normalize(); else n.set(0f, 1f, 0f);
            out[v * 3] = n.x;
            out[v * 3 + 1] = n.y;
            out[v * 3 + 2] = n.z;
        }
        return out;
    }

    private static Vector3f vertex(float[] pos, int i) {
        return new Vector3f(pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2]);
    }

    // Kunci posisi (dibulatkan) agar vertex di sambungan UV dan di kutub dihitung sebagai satu titik
    private static String key(float[] pos, int i) {
        return Math.round(pos[i * 3] * 10000) + "," + Math.round(pos[i * 3 + 1] * 10000) + ","
             + Math.round(pos[i * 3 + 2] * 10000);
    }
}