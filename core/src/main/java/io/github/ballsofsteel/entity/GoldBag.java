package io.github.ballsofsteel.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;

public class GoldBag {

   // atlas && animation
    private static final Texture SPAWN_TEX =
        new Texture(Gdx.files.internal("goldbag_spawn.png"));   // 896×128
    private static final Texture ICON_TEX  =
        new Texture(Gdx.files.internal("HUD/gold_bag.png"));

    private static final float SCALE = 1f / 96f;
    private static final float R     = 0.5f;

    private static final Animation<TextureRegion> SPAWN_ANIM;

    static {
        SPAWN_TEX.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        TextureRegion src = new TextureRegion(SPAWN_TEX, 2, 2, 896, 128);   // bounds
        TextureRegion[] frames = src.split(896 / 7, 128)[0];                // 7 kare
        SPAWN_ANIM = new Animation<>(0.15f, frames);
        SPAWN_ANIM.setPlayMode(Animation.PlayMode.NORMAL);
    }

    /* -------------------------------------------------- */
    private final float x, y;
    private float t = 0f;                     // animation time

    public GoldBag(float x, float y){ this.x = x; this.y = y; }

    public boolean isCollectedBy(Player p){
        float dx = p.getX()-x, dy = p.getY()-y;
        return dx*dx+dy*dy < R*R;
    }

    // render
    public void render(SpriteBatch b){

        t += Gdx.graphics.getDeltaTime();

        TextureRegion fr = SPAWN_ANIM.isAnimationFinished(t)
            ? new TextureRegion(ICON_TEX)
            : SPAWN_ANIM.getKeyFrame(t);

        float w = fr.getRegionWidth()  * SCALE;
        float h = fr.getRegionHeight() * SCALE;

        b.draw(fr, x - w/2f, y - h/2f, w, h);
    }
}
