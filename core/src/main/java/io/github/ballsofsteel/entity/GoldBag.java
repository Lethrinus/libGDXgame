package io.github.ballsofsteel.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/** Oyuncu üzerinden geçtiğinde toplanan altın kesesi (süre sınırı yok). */
public class GoldBag {

    private static final Texture TEX   = new Texture(Gdx.files.internal("HUD/gold_bag.png"));
    private static final float   SCALE = 1f / 96f;
    private static final float   R     = 0.5f;              // toplanma yarıçapı

    private final float x, y;
    public GoldBag(float x, float y){ this.x = x; this.y = y; }

    /** Oyuncu (px,py) konumundaysa true döner (toplandı). */
    public boolean checkCollected(float px, float py){
        float dx = px - x, dy = py - y;
        return dx*dx + dy*dy <= R*R;
    }

    public void render(SpriteBatch b){
        float w = TEX.getWidth()*SCALE, h = TEX.getHeight()*SCALE;
        b.draw(TEX, x-w/2f, y-h/2f, w, h);
    }
    public boolean isCollectedBy(Player p){
        float dx = p.getX()-x, dy = p.getY()-y;
        return dx*dx+dy*dy < 0.5f*0.5f;                // ~½ birim mesafe
    }
}
