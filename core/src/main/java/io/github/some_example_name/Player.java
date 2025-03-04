package io.github.some_example_name;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Player {
    private float x, y;
    private float speed = 200f;
    private float stateTime = 0f;

    private Texture spriteSheet, idleSheet;
    private Animation<TextureRegion>[] animations;
    private Animation<TextureRegion> currentAnimation;
    private Animation<TextureRegion> idleAnimation;

    private int frameWidth, frameHeight;
    private float scale = 4f;
    private int FRAME_ROWS = 6;

    public Player() {
        spriteSheet = new Texture(Gdx.files.internal("walkanimation.png"));
        idleSheet = new Texture(Gdx.files.internal("idle.png"));

        int sheetSize = 384;
        int FRAME_COLS = 8;

        frameWidth = sheetSize / FRAME_COLS;
        frameHeight = sheetSize / FRAME_ROWS;

        TextureRegion[][] tmpFrames = TextureRegion.split(spriteSheet, frameWidth, frameHeight);
        TextureRegion[][] idleFrames = TextureRegion.split(idleSheet, 48, 64);

        animations = new Animation[7]; // 7 animasyon var: South, SW, NW, North, NE, East, West
        animations[0] = new Animation<>(0.1f, tmpFrames[0]); // South (Aşağı)
        animations[1] = new Animation<>(0.1f, tmpFrames[1]); // Southwest (Sol Aşağı)
        animations[2] = new Animation<>(0.1f, tmpFrames[2]); // Northwest (Sol Yukarı)
        animations[3] = new Animation<>(0.1f, tmpFrames[3]); // North (Yukarı)
        animations[4] = new Animation<>(0.1f, tmpFrames[4]); // Northeast (Sağ Yukarı)
        animations[5] = new Animation<>(0.1f, tmpFrames[5]); // East (Sağ)
        animations[6] = mirrorAnimation(animations[5]); // West (Sol) = East (Sağ) aynalanmış hali

        idleAnimation = new Animation<>(0.2f, idleFrames[0]);
        idleAnimation.setPlayMode(Animation.PlayMode.LOOP);

        currentAnimation = idleAnimation;
        x = 100;
        y = 100;
    }

    public void update(float delta) {
        float dx = 0, dy = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) dy++;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) dy--;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx++;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx--;

        if (dx != 0 && dy != 0) {
            dx *= 0.7071f;
            dy *= 0.7071f;
        }

        x += dx * speed * delta;
        y += dy * speed * delta;

        currentAnimation = getDirectionAnimation(dx, dy);
        stateTime += delta;
    }

    private Animation<TextureRegion> getDirectionAnimation(float dx, float dy) {
        if (dx == 0 && dy == 0) return idleAnimation; // Idle animasyon

        if (dy < 0 && dx == 0) return animations[0]; // South (Aşağı)
        if (dx < 0 && dy < 0)  return animations[1]; // Southwest (Sol Aşağı)
        if (dx < 0 && dy > 0)  return animations[2]; // Northwest (Sol Yukarı)
        if (dy > 0 && dx == 0) return animations[3]; // North (Yukarı)
        if (dx > 0 && dy > 0)  return animations[4]; // Northeast (Sağ Yukarı)
        if (dx > 0 && dy < 0)  return mirrorAnimation(animations[1]); // Southeast (Sağ Aşağı) = Southwest'in aynalanmış hali
        if (dx > 0 && dy == 0) return animations[5]; // East (Sağ)
        if (dx < 0 && dy == 0) return animations[6]; // **West (Sol)**

        return animations[0]; // **Fallback**
    }

    private Animation<TextureRegion> mirrorAnimation(Animation<TextureRegion> original) {
        TextureRegion[] mirroredFrames = new TextureRegion[original.getKeyFrames().length];

        for (int i = 0; i < mirroredFrames.length; i++) {
            mirroredFrames[i] = new TextureRegion(original.getKeyFrames()[i]);
            mirroredFrames[i].flip(true, false); // X ekseninde aynalama (sağa çevirme)
        }

        return new Animation<>(original.getFrameDuration(), mirroredFrames);
    }

    public void render(SpriteBatch batch) {
        TextureRegion frame = currentAnimation.getKeyFrame(stateTime, true);
        batch.draw(frame, x, y, frameWidth * scale, frameHeight * scale);
    }

    public void dispose() {
        spriteSheet.dispose();
        idleSheet.dispose();
    }
}
