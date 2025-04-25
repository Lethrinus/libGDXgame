package io.github.ballsofsteel.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/** Altın kesesi – birkaç saniye yerde durur sonra kaybolur. */
public class GoldBag {
    private static final Texture TEX = new Texture(Gdx.files.internal("HUD/gold_bag.png"));
    private static final float SCALE = 1f / 96f;
    private float x, y, life = 6f;                    // saniye

    public GoldBag(float x, float y) { this.x = x; this.y = y; }

    /** @return true → ömrü bitti, listeden sil */
    public boolean update(float dt) { life -= dt; return life <= 0; }

    public void render(SpriteBatch b) {
        float w = TEX.getWidth() * SCALE, h = TEX.getHeight() * SCALE;
        b.draw(TEX, x - w / 2f, y - h / 2f, w, h);
    }
}
