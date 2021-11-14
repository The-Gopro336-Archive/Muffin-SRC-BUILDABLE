
// Inputs
in vec3 position; // vertex pos in eye coords
in vec3 color;
in vec3 normal;   // vertex normal in eye-coords
in vec2 texCoord;

// Uniforms
uniform sampler2D textureUnit;
uniform mat4 V;
uniform bool texture_loaded;

// Outputs
layout(location = 0) out vec4 fragColor;

// Light source uniforms
struct Light {
    vec3 position;
    vec3 intensity;
};
uniform Light light;

// Material uniforms
struct Material {
    vec3 Ka;            // Ambient reflectivity
    vec3 Kd;            // Diffuse reflectivity
    vec3 Ks;            // Specular reflectivity
    float shininess;    // Specular Shininess
};
uniform Material material;

vec3 lightModel()
{
    // Normalized light source vector
    vec4 l = vec4( light.position, 1);

    // Renormalize the interpolated normal
    vec3 n = normalize(normal);

    // Normalized light source vector
    vec3 s = normalize( l.xyz - position.xyz);

    // Reflected vector
    vec3 r = reflect( -s, n);

    // View vector
    vec3 v = normalize( -position.xyz );

    // Ambient
    vec3 ambientContribution = material.Ka;

    // Diffuse
    float diffuse = max( dot( s, n), 0.0);
    vec3 diffuseContribution = material.Kd * diffuse;

    // Specular
    float specular = pow( max( dot(r, v), 0.0), max( material.shininess, 0));
    vec3 specularContribution = material.Ks * specular;

    // Calculate final color
    return light.intensity * (ambientContribution + diffuseContribution + specularContribution);
}


void main(void)
{
    vec3 light = lightModel();

    if (texCoord.s < 0 || texCoord.t < 0 || !texture_loaded)
        fragColor = vec4(light * color, 0.5);
    else
        fragColor = vec4(light * texture2D(textureUnit, texCoord.st).xyz,1.0);
}
