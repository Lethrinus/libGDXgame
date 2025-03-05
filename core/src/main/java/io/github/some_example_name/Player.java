package io.github.some_example_name;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

public class Player {
    private float x, y;
    private float speed = 3.5f;      // Ayarlanmış hız
    private float stateTime = 0f;

    private Texture spriteSheet, idleSheet;
    private Animation<TextureRegion>[] animations;
    private Animation<TextureRegion> currentAnimation;
    private Animation<TextureRegion> idleAnimation;

    private int frameWidth, frameHeight;
    private float scale = 0.042f;    // Ayarlanmış ölçek
    private int FRAME_ROWS = 6;

    public float getX() { return x; }
    public float getY() { return y; }

    public Player() {
        spriteSheet = new Texture(Gdx.files.internal("walkanimation.png"));
        idleSheet = new Texture(Gdx.files.internal("idle.png"));

        int sheetSize = 384;
        int FRAME_COLS = 8;

        frameWidth = sheetSize / FRAME_COLS;
        frameHeight = sheetSize / FRAME_ROWS;

        TextureRegion[][] tmpFrames = TextureRegion.split(spriteSheet, frameWidth, frameHeight);
        TextureRegion[][] idleFrames = TextureRegion.split(idleSheet, 48, 64);

        animations = new Animation[7]; // 7 animasyon: Güney, Güneybatı, Kuzeybatı, Kuzey, Kuzeydoğu, Doğu, Batı
        animations[0] = new Animation<TextureRegion>(0.1f, tmpFrames[0]); // Güney
        animations[1] = new Animation<TextureRegion>(0.1f, tmpFrames[1]); // Güneybatı
        animations[2] = new Animation<TextureRegion>(0.1f, tmpFrames[2]); // Kuzeybatı
        animations[3] = new Animation<TextureRegion>(0.1f, tmpFrames[3]); // Kuzey
        animations[4] = new Animation<TextureRegion>(0.1f, tmpFrames[4]); // Kuzeydoğu
        animations[5] = new Animation<TextureRegion>(0.1f, tmpFrames[5]); // Doğu
        animations[6] = mirrorAnimation(animations[5]);                // Batı (Doğu'nun aynası)

        idleAnimation = new Animation<TextureRegion>(0.2f, idleFrames[0]);
        idleAnimation.setPlayMode(Animation.PlayMode.LOOP);

        currentAnimation = idleAnimation;
        // Başlangıç pozisyonu (0,0); setPosition() ile konumlandırın.
        x = 0;
        y = 0;
    }

    /**
     * Oyuncunun pozisyonunu ayarlar. 32x32 world'de ortalamak için (16,16) kullanılabilir.
     */
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
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

        // Oyuncunun pozisyonunu 0 ile 32 world unit arasında sınırlandır.
        x = MathUtils.clamp(x, 0, 32);
        y = MathUtils.clamp(y, 0, 32);

        currentAnimation = getDirectionAnimation(dx, dy);
        stateTime += delta;
    }

    private Animation<TextureRegion> getDirectionAnimation(float dx, float dy) {
        if (dx == 0 && dy == 0) return idleAnimation;

        if (dy < 0 && dx == 0) return animations[0]; // Güney
        if (dx < 0 && dy < 0)  return animations[1]; // Güneybatı
        if (dx < 0 && dy > 0)  return animations[2]; // Kuzeybatı
        if (dy > 0 && dx == 0) return animations[3]; // Kuzey
        if (dx > 0 && dy > 0)  return animations[4]; // Kuzeydoğu
        if (dx > 0 && dy < 0)  return mirrorAnimation(animations[1]); // Güneydoğu (Güneybatı'nın aynası)
        if (dx > 0 && dy == 0) return animations[5]; // Doğu
        if (dx < 0 && dy == 0) return animations[6]; // Batı

        return animations[0]; // Varsayılan
    }

    private Animation<TextureRegion> mirrorAnimation(Animation<TextureRegion> original) {
        TextureRegion[] mirroredFrames = new TextureRegion[original.getKeyFrames().length];
        for (int i = 0; i < mirroredFrames.length; i++) {
            mirroredFrames[i] = new TextureRegion(original.getKeyFrames()[i]);
            mirroredFrames[i].flip(true, false);
        }
        return new Animation<TextureRegion>(original.getFrameDuration(), mirroredFrames);
    }

    public void render(SpriteBatch batch) {
        TextureRegion frame = currentAnimation.getKeyFrame(stateTime, true);
        // Render, oyuncunun (x,y) pozisyonunu merkez alacak şekilde yapılır.
        batch.draw(frame, x - (frameWidth * scale) / 2f, y - (frameHeight * scale) / 2f, frameWidth * scale, frameHeight * scale);
    }

    public void dispose() {
        spriteSheet.dispose();
        idleSheet.dispose();
    }
}
