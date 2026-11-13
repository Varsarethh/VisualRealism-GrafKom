# Visual Realism - Grafika Komputer Minggu 11

Implementasi Java (LWJGL 3, JOML, GLSL 3.3 core) untuk lima indikator:
Phong/Blinn-Phong, Flat/Smooth Shading, Hidden Surface Removal,
Texture Mapping, dan Shadow Mapping.

## Kontrol
- B : Phong / Blinn-Phong
- F : Flat / Smooth
- Z : Z-Buffer on/off
- C : Back-Face Culling on/off
- T : Tekstur on/off
- N : Nearest / Bilinear+Mipmap
- H : Bayangan on/off
- [] : Turunkan / naikkan shadow bias
- V : VSync on/off
- Mouse kiri (seret) : putar kamera
- Scroll : zoom
- Esc : keluar

## Cara menjalankan
Buka project dengan NetBeans (Maven project), lakukan Reload POM,
Clean and Build, lalu Run.
Kebutuhan: JDK 17 atau lebih baru dan GPU yang mendukung OpenGL 3.3.

## Struktur singkat
- src/main/java/com/grafkom/viewing3d : kelas Java
- src/main/resources/shaders : GLSL vertex dan fragment shader
- src/main/resources/textures : gambar tekstur

## Sumber tekstur
[Bricks075 dari ambientCG]

