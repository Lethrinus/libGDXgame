package io.github.some_example_name;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Enemy {
    private Texture texture;
    private float x, y;
    private EnemyStrategy strategy;

    public Enemy(Texture texture, float x, float y, EnemyStrategy strategy) {
        this.texture = texture;
        this.x = x;
        this.y = y;
        this.strategy = strategy;
    }

    public void update(Player player) {
        // Execute the current behavior strategy
        strategy.execute(this, player);
    }

    public void render(SpriteBatch batch) {
        batch.draw(texture, x, y);
    }

    public void setX(float x) {
        this.x = x;
    }
    public void setY(float y) {
        this.y = y;
    }
    public float getX() {
        return x;
    }
    public float getY() {
        return y;
    }

    public void dispose() {
        texture.dispose();
    }
}
