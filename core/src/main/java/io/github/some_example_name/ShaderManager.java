package io.github.some_example_name;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Utility class for creating the circle fade shader.
 * This shader darkens fragments based on their distance from the player's screen position,
 * but inverted: when the player is near (inside a bush/tree), the layer is dimmed,
 * and when far, it is rendered normally.
 */
public class ShaderManager {
    private static final String VERT =
        "attribute vec4 a_position;\n" +
            "attribute vec2 a_texCoord0;\n" +
            "uniform mat4 u_projTrans;\n" +
            "varying vec2 v_texCoord;\n" +
            "void main(){\n" +
            "    v_texCoord = a_texCoord0;\n" +
            "    gl_Position = u_projTrans * a_position;\n" +
            "}";

    private static final String FRAG =
        "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "varying vec2 v_texCoord;\n" +
            "uniform sampler2D u_texture;\n" +
            "uniform vec2 u_playerScreenPos;\n" +
            "uniform float u_radius;\n" +
            "void main(){\n" +
            "    vec4 color = texture2D(u_texture, v_texCoord);\n" +
            "    float dist = distance(gl_FragCoord.xy, u_playerScreenPos);\n" +
            "    // Invert the smoothstep factor so that near the player, factor=1 (dark) and far, factor=0 (normal)\n" +
            "    float factor = 1.0 - smoothstep(u_radius, u_radius + 50.0, dist);\n" +
            "    // Mix between full brightness and 0.3 brightness based on factor\n" +
            "    vec4 darkened = color * mix(1.0, 0.3, factor);\n" +
            "    gl_FragColor = darkened;\n" +
            "}";

    public static ShaderProgram createCircleShader() {
        ShaderProgram shader = new ShaderProgram(VERT, FRAG);
        if (!shader.isCompiled()) {
            Gdx.app.error("ShaderManager", "Could not compile shader: " + shader.getLog());
        }
        return shader;
    }
}
