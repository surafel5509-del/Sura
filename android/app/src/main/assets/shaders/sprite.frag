#version 300 es
precision mediump float;
in vec2 vUV;
in vec4 vColor;
in flat int vTexIndex;
uniform sampler2D uTex[8];
out vec4 fragColor;
void main() {
    int idx = clamp(vTexIndex, 0, 7);
    fragColor = texture(uTex[idx], vUV) * vColor;
}
