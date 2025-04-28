package io.github.ballsofsteel.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;
import java.util.List;

public class BarrelBomber {

    /* ---------- statik animasyonlar ---------- */
    private static final Texture SHEET = new Texture(Gdx.files.internal("Goblin/barrel_atlas.png"));
    private static final Texture BOOM  = new Texture(Gdx.files.internal("Dynamite/explosions_dynamite.png"));

    private static final Animation<TextureRegion> runR, runL, prepareExplodeA, explodeA;
    static {
        SHEET.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        BOOM.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        runR            = row(SHEET, 2, 2, 768, 128, 6, .10f);
        runL            = mirror(runR);
        prepareExplodeA = row(SHEET, 2, 262, 384, 128, 3, .2f);
        explodeA        = row(BOOM , 2, 68, 1728, 192, 9, .07f);
    }

    private static Animation<TextureRegion> row(Texture t, int x, int y, int w, int h, int n, float d) {
        return new Animation<>(d, new TextureRegion(t, x, y, w, h).split(w / n, h)[0]);
    }

    private static Animation<TextureRegion> mirror(Animation<TextureRegion> src) {
        TextureRegion[] original = src.getKeyFrames();
        TextureRegion[] mirrored = new TextureRegion[original.length];
        for (int i = 0; i < original.length; i++) {
            mirrored[i] = new TextureRegion(original[i]);
            mirrored[i].flip(true, false);
        }
        return new Animation<>(src.getFrameDuration(), mirrored);
    }

    /* ---------- sabitler ---------- */
    private static final float SCALE = 1f / 72f;
    private static final float RADIUS = .45f;
    private static final float DAMAGE_RADIUS = 2f;

    /* ---------- referanslar ve state ---------- */
    private final Player player;
    private final List<BarrelBomber> crowd;

    private float x, y, baseY, hop;
    private float t, preBoomT, boomT;
    private boolean preparing, exploding, finished, facingLeft;
    private boolean damageApplied; // sadece bir kez damage vereceğiz

    public BarrelBomber(Player player, List<BarrelBomber> crowd, float startX, float startY) {
        this.player = player;
        this.crowd = crowd;
        this.x = startX;
        this.baseY = this.y = startY;
    }

    public void update(float dt) {
        if (preparing) {
            preBoomT += dt;
            if (prepareExplodeA.isAnimationFinished(preBoomT)) {
                preparing = false;
                exploding = true;
                boomT = 0;
            }
            return;
        }

        if (exploding) {
            boomT += dt;

            if (!damageApplied && boomT >= explodeA.getAnimationDuration() / 3f) { // patlama ortasında hasar uygula
                float dx = player.getX() - x;
                float dy = player.getY() - baseY;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist < DAMAGE_RADIUS) {
                    float angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
                    player.takeDamage(35f, 6f, angle);
                }
                damageApplied = true;
            }

            if (explodeA.isAnimationFinished(boomT)) {
                finished = true;
            }
            return;
        }

        /* ---------- birbirinden uzaklaştırma ---------- */
        for (BarrelBomber b : crowd) {
            if (b == this || b.exploding || b.preparing) continue;
            float dx = x - b.x, dy = baseY - b.baseY;
            float d2 = dx * dx + dy * dy;
            if (d2 < RADIUS * RADIUS * 4) {
                float d = (float) Math.sqrt(d2);
                if (d > 0) {
                    x += dx / d * dt * 1.5f;
                    baseY += dy / d * dt * 1.5f;
                }
            }
        }

        /* ---------- oyuncuya doğru koş ve şişmeye başla ---------- */
        float dx = player.getX() - x;
        float dy = player.getY() - baseY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        facingLeft = dx < 0;

        if (dist > 1.3f) {
            x += dx / dist * dt * 1.7f;
            baseY += dy / dist * dt * 1.7f;
        } else {
            preparing = true;
            preBoomT = 0;
        }

        t += dt;
        hop = Math.abs(MathUtils.sin(t * 3f)) * 0.35f;
    }

    public void render(SpriteBatch batch) {
        TextureRegion frame;
        if (preparing) {
            frame = prepareExplodeA.getKeyFrame(preBoomT, false);
            draw(batch, frame, 1f);
        } else if (exploding) {
            frame = explodeA.getKeyFrame(boomT, false);
            draw(batch, frame, 1f);
        } else {
            frame = (facingLeft ? runL : runR).getKeyFrame(t, true);
            draw(batch, frame, 1f, hop);
        }
    }

    private void draw(SpriteBatch batch, TextureRegion frame, float scale) {
        draw(batch, frame, scale, 0);
    }

    private void draw(SpriteBatch batch, TextureRegion frame, float scale, float yOffset) {
        float width = frame.getRegionWidth() * scale * SCALE;
        float height = frame.getRegionHeight() * scale * SCALE;
        batch.draw(frame, x - width / 2f, baseY + yOffset - height / 2f, width, height);
    }

    public boolean isFinished() {
        return finished;
    }

    public void dispose() {
    }
}
