#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;
uniform sampler2D textureSampler;
uniform sampler2D shadowMap;
uniform bool useTexture;
uniform bool useBlinnPhong;
uniform bool useShadow;
uniform float biasMax;

// 0.0 = terkena cahaya, 1.0 = dalam bayangan penuh
float calcShadow(vec4 fragPosLightSpace, vec3 norm, vec3 lightDir) {
    vec3 proj = fragPosLightSpace.xyz / fragPosLightSpace.w;
    proj = proj * 0.5 + 0.5;                       // NDC [-1,1] -> [0,1]
    if (proj.z > 1.0) return 0.0;                  // di luar far plane cahaya

    // Bias dinamis berdasarkan kemiringan permukaan terhadap cahaya
    float bias = max(biasMax * (1.0 - dot(norm, lightDir)), biasMax * 0.1);

    // PCF 3x3: rata-rata 9 sampel untuk tepi bayangan yang halus
    float shadow = 0.0;
    vec2 texelSize = 1.0 / vec2(textureSize(shadowMap, 0));
    for (int x = -1; x <= 1; ++x) {
        for (int y = -1; y <= 1; ++y) {
            float closest = texture(shadowMap, proj.xy + vec2(x, y) * texelSize).r;
            shadow += (proj.z - bias > closest) ? 1.0 : 0.0;
        }
    }
    return shadow / 9.0;
}

void main() {
    // 1. Ambient
    float ambientStrength = 0.2;
    vec3 ambient = ambientStrength * lightColor;

    // 2. Diffuse (Lambertian)
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(lightPos - FragPos);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    // 3. Specular (Phong vs Blinn-Phong)
    float specularStrength = 0.8;
    vec3 viewDir = normalize(viewPos - FragPos);
    float spec = 0.0;
    if (useBlinnPhong) {
        vec3 halfwayDir = normalize(lightDir + viewDir);
        spec = pow(max(dot(norm, halfwayDir), 0.0), 32.0);
    } else {
        vec3 reflectDir = reflect(-lightDir, norm);
        spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0);
    }
    vec3 specular = specularStrength * spec * lightColor;

    // 4. Bayangan: ambient tetap, diffuse dan specular hilang di area bayangan
    float shadow = useShadow ? calcShadow(FragPosLightSpace, norm, lightDir) : 0.0;

    vec3 baseColor = useTexture ? texture(textureSampler, TexCoords).rgb : objectColor;
    vec3 result = (ambient + (1.0 - shadow) * (diffuse + specular)) * baseColor;
    FragColor = vec4(result, 1.0);
}