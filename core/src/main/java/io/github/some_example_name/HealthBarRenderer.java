package io.github.some_example_name;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * Renders a rounded, animated health bar.
 */
public class HealthBarRenderer {
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;
    private float displayedHealthPercent;

    public HealthBarRenderer(OrthographicCamera camera) {
        this.camera = camera;
        shapeRenderer = new ShapeRenderer();
        shapeRenderer.setAutoShapeType(true);
        shapeRenderer.setProjectionMatrix(camera.combined);
        displayedHealthPercent = 1f;
    }

    /**
     * Renders the health bar.
     *
     * @param centerX       Horizontal center position of the bar
     * @param centerY       Vertical position of the bar (typically above the character)
     * @param width         Total width of the bar
     * @param height        Height of the bar
     * @param currentHealth Current health value
     * @param maxHealth     Maximum health value
     * @param delta         Delta time (for smooth animation)
     */
    public void render(float centerX, float centerY, float width, float height, float currentHealth, float maxHealth, float delta) {
        float healthPercent = currentHealth / maxHealth;
        // Smooth animation: gradually adjust displayedHealthPercent toward the actual health percentage.
        displayedHealthPercent = MathUtils.lerp(displayedHealthPercent, healthPercent, delta * 10f);

        float x = centerX - width / 2f;
        float y = centerY - height / 2f;
        float cornerRadius = height / 2f;
        float padding = 0.02f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw background: dark gray (for border and empty area)
        shapeRenderer.setColor(Color.DARK_GRAY);
        drawRoundedRect(x, y, width, height, cornerRadius);

        // Draw fill: gradient color based on health percentage (Red -> Yellow -> Green)
        float fillWidth = (width - padding * 2) * displayedHealthPercent;
        float fillX = x + padding;
        float fillY = y + padding;
        float fillHeight = height - padding * 2;

        shapeRenderer.setColor(getHealthColor(displayedHealthPercent));
        if (fillWidth > 0) {
            drawRoundedRect(fillX, fillY, fillWidth, fillHeight, cornerRadius * 0.8f);
        }

        shapeRenderer.end();
    }

    /**
     * Calculates the color based on health percentage.
     * 0% → Red, 50% → Yellow, 100% → Green.
     */
    private Color getHealthColor(float percent) {
        if (percent > 0.5f) {
            // Interpolate from yellow to green.
            return new Color(MathUtils.lerp(1f, 0f, (percent - 0.5f) * 2), 1f, 0f, 1f);
        } else {
            // Interpolate from red to yellow.
            return new Color(1f, MathUtils.lerp(0f, 1f, percent * 2), 0f, 1f);
        }
    }

    /**
     * Draws a rounded rectangle.
     *
     * @param x The bottom-left x-coordinate.
     * @param y The bottom-left y-coordinate.
     * @param w Width of the rectangle.
     * @param h Height of the rectangle.
     * @param r Radius for the rounded corners.
     */
    private void drawRoundedRect(float x, float y, float w, float h, float r) {
        int segments = 16;
        // Draw horizontal center rectangle.
        shapeRenderer.rect(x + r, y, w - 2 * r, h);
        // Draw vertical center rectangle.
        shapeRenderer.rect(x, y + r, w, h - 2 * r);
        // Draw corner arcs.
        shapeRenderer.arc(x + r, y + r, r, 180f, 90f, segments);
        shapeRenderer.arc(x + w - r, y + r, r, 270f, 90f, segments);
        shapeRenderer.arc(x + w - r, y + h - r, r, 0f, 90f, segments);
        shapeRenderer.arc(x + r, y + h - r, r, 90f, 90f, segments);
    }

    /**
     * Disposes of the ShapeRenderer resource.
     */
    public void dispose() {
        shapeRenderer.dispose();
    }
}
