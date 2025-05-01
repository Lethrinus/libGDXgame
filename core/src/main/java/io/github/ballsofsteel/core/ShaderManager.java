package io.github.ballsofsteel.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class ShaderManager {

    private static final String VERT =
        "attribute vec4 a_position;\n" +
            "attribute vec4 a_color;\n" +
            "attribute vec2 a_texCoord0;\n" +
            "uniform mat4 u_projTrans;\n" +
            "varying vec4 v_color;\n" +
            "varying vec2 v_texCoord;\n" +
            "void main(){\n" +
            "    v_color = a_color;\n" +
            "    v_texCoord = a_texCoord0;\n" +
            "    gl_Position = u_projTrans * a_position;\n" +
            "}";

    private static final String FRAG =
        "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "varying vec4 v_color;\n" +
            "varying vec2 v_texCoords;\n" +
            "uniform sampler2D u_texture;\n" +
            "void main(){\n" +
            "    vec4 color = texture2D(u_texture, v_texCoords) * v_color;\n" +
            "    gl_FragColor = color;\n" +
            "}";

    private static final String CIRCLE_SHADER_NAME = "circle-fade";

    // Circle fade shader source
    private static final String CIRCLE_VERT = VERT;

    private static final String CIRCLE_FRAG =
        "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "varying vec4 v_color;\n" +
            "varying vec2 v_texCoord;\n" +
            "uniform sampler2D u_texture;\n" +
            "uniform vec2 u_playerScreenPos;\n" +
            "uniform float u_radius;\n" +
            "void main(){\n" +
            "    vec4 color = texture2D(u_texture, v_texCoord) * v_color;\n" +
            "    float dist = distance(gl_FragCoord.xy, u_playerScreenPos);\n" +
            "    float factor = 1.0 - smoothstep(u_radius, u_radius + 30.0, dist);\n" +
            "    vec4 darkened = color * mix(1.0, 0.9, factor);\n" +
            "    gl_FragColor = darkened;\n" +
            "}";

    // Grayscale shader source (fixed to include u_grayscaleFactor)
    private static final String GRAYSCALE_VERT = VERT;

    private static final String GRAYSCALE_FRAG =
        "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "varying vec4 v_color;\n" +
            "varying vec2 v_texCoord;\n" +
            "uniform sampler2D u_texture;\n" +
            // Added uniform to control amount of grayscale
            "uniform float u_grayscaleFactor;\n" +
            "void main() {\n" +
            "    vec4 color = texture2D(u_texture, v_texCoord) * v_color;\n" +
            "    float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));\n" +
            "    vec3 mixed = mix(color.rgb, vec3(gray), u_grayscaleFactor);\n" +
            "    gl_FragColor = vec4(mixed, color.a);\n" +
            "}";

    private ShaderManager() {}

    public static ShaderProgram createCircleShader() {
        ShaderProgram shader = new ShaderProgram(CIRCLE_VERT, CIRCLE_FRAG);
        if (!shader.isCompiled()) {
            Gdx.app.error("ShaderManager", "Could not compile circle shader: " + shader.getLog());
        }
        return shader;
    }

    public static ShaderProgram createGrayscaleShader() {
        ShaderProgram shader = new ShaderProgram(GRAYSCALE_VERT, GRAYSCALE_FRAG);
        if (!shader.isCompiled()) {
            Gdx.app.error("ShaderManager", "Could not compile grayscale shader: " + shader.getLog());
        }
        return shader;
    }
}
