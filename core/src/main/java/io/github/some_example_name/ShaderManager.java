package io.github.some_example_name;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * A helper class to provide the circle-fade shader
 * (per-pixel transparency around the player's center).
 */
public class ShaderManager {

    /**
     * Creates and returns the circle shader.
     * If distance(worldPos, playerPos) < radius => alpha=0.5, else alpha=1.0
     */
    public static ShaderProgram createCircleShader() {
        // Vertex: pass "world position" to fragment
        final String VERT =
            "attribute vec4 a_position;\n" +
                "attribute vec2 a_texCoord0;\n" +
                "\n" +
                "uniform mat4 u_projTrans;\n" +
                "\n" +
                "varying vec2 v_texCoord;\n" +
                "varying vec2 v_worldPos;\n" +
                "\n" +
                "void main(){\n" +
                "    v_texCoord = a_texCoord0;\n" +
                "    // a_position is in world coords since we use OrthographicCamera + unitScale\n" +
                "    v_worldPos = a_position.xy;\n" +
                "    gl_Position = u_projTrans * a_position;\n" +
                "}\n";

        // Fragment: fade inside circle => alpha=0.5, outside => alpha=1.0
        final String FRAG =
            "#ifdef GL_ES\n" +
                "precision mediump float;\n" +
                "#endif\n" +
                "\n" +
                "uniform sampler2D u_texture;\n" +
                "uniform vec2 u_playerPos;\n" +
                "uniform float u_radius;\n" +
                "\n" +
                "varying vec2 v_texCoord;\n" +
                "varying vec2 v_worldPos;\n" +
                "\n" +
                "void main(){\n" +
                "    vec4 c = texture2D(u_texture, v_texCoord);\n" +
                "    float dist = distance(v_worldPos, u_playerPos);\n" +
                "    float alpha = 1.0;\n" +
                "    if(dist < u_radius){\n" +
                "       alpha = 0.5;\n" +
                "    }\n" +
                "    gl_FragColor = vec4(c.rgb, c.a * alpha);\n" +
                "}\n";

        ShaderProgram shader = new ShaderProgram(VERT, FRAG);
        if (!shader.isCompiled()) {
            throw new IllegalStateException("CircleShader compile error:\n" + shader.getLog());
        }
        return shader;
    }

    private ShaderManager() {
        // Prevent instantiation
    }
}
