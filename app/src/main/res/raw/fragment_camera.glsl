#extension GL_OES_EGL_image_external:require
precision mediump float;

uniform samplerExternalOES u_TextureSampler;
uniform float identity;

varying vec2 vTextureCoord;

void main() {
    vec4 color = texture2D(u_TextureSampler, vTextureCoord);
    float gray = 0.299 * color.r + 0.587 * color.g + 0.114 * color.b;
    vec4 newColor = vec4(gray, gray, gray, color.a);
    gl_FragColor = mix(color, newColor, identity);
}