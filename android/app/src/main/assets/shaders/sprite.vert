#version 300 es
layout(location = 0) in vec2 aPos;
layout(location = 1) in vec2 aUV;
layout(location = 2) in vec3 aColor;
layout(location = 3) in float aTexIndex;
uniform mat4 uProj;
out vec2 vUV;
out vec4 vColor;
out flat int vTexIndex;
void main() {
    vUV = aUV;
    vColor = vec4(aColor, 1.0);
    vTexIndex = int(aTexIndex + 0.5);
    gl_Position = uProj * vec4(aPos, 0.0, 1.0);
}
