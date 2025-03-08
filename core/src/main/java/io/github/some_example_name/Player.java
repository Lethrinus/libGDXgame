package io.github.some_example_name;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Polygon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Player {
    private float x, y;
    private float speed = 3.5f;
    private float stateTime = 0f;

    private OrthographicCamera camera;
    private TileMapRenderer tileMapRenderer;
    private Texture atlas;

    // Animations
    private Animation<TextureRegion> idleAnimation;
    private Animation<TextureRegion> idleAnimationLeft; // Idle when facing left
    private Animation<TextureRegion> runningRight;
    private Animation<TextureRegion> runningLeft;
    private Animation<TextureRegion> attackTop;
    private Animation<TextureRegion> attackBottom;
    private Animation<TextureRegion> attackRight;
    private Animation<TextureRegion> attackLeft;

    private Animation<TextureRegion> currentAnimation;
    private boolean isAttacking = false;

    // Dashing fields
    private boolean isDashing = false;
    private float dashTimeLeft = 0f;
    private float dashSpeed = 10f;       // Dash movement speed
    private float dashDuration = 0.2f;   // Dash lasts 0.2 seconds
    private float dashCooldown = 1.0f;   // 1 second cooldown between dashes
    private float dashCooldownTimer = 0f;

    // Ghost trail
    private List<GhostFrame> ghosts = new ArrayList<>();

    // Atlas frame parameters
    private int frameWidth, frameHeight;
    private float scale = 1.0f / 72f;
    private static final int NUM_FRAMES = 6;

    // Collision bounding box dimensions
    private static final float COLLISION_W = 0.6f;
    private static final float COLLISION_H = 0.6f;

    // Field to keep track of facing direction (true = facing right, false = facing left)
    private boolean facingRight = true;

    public float getX() { return x; }
    public float getY() { return y; }

    public Player(OrthographicCamera camera, TileMapRenderer tileMapRenderer) {
        this.camera = camera;
        this.tileMapRenderer = tileMapRenderer;

        // Load the atlas texture
        atlas = new Texture(Gdx.files.internal("Player/knight_atlas.png"));

        // Single row subregions
        TextureRegion idleRegion         = new TextureRegion(atlas, 2,   2,   1152, 195);
        TextureRegion attackTopRegion    = new TextureRegion(atlas, 2,  199,  1152, 195);
        TextureRegion attackBottomRegion = new TextureRegion(atlas, 2,  396,  1152, 195);
        TextureRegion attackRightRegion  = new TextureRegion(atlas, 2,  593,  1152, 195);
        TextureRegion runRightRegion     = new TextureRegion(atlas, 2,  790,  1152, 195);

        frameWidth  = 1152 / NUM_FRAMES; // 192
        frameHeight = 195;

        idleAnimation     = buildAnimation(idleRegion, 0.25f, Animation.PlayMode.LOOP);
        idleAnimationLeft = mirrorAnimation(idleAnimation); // Mirrored idle for facing left
        attackTop         = buildAnimation(attackTopRegion, 0.05f, Animation.PlayMode.NORMAL);
        attackBottom      = buildAnimation(attackBottomRegion, 0.05f, Animation.PlayMode.NORMAL);
        attackRight       = buildAnimation(attackRightRegion, 0.05f, Animation.PlayMode.NORMAL);
        attackLeft        = mirrorAnimation(attackRight);
        runningRight      = buildAnimation(runRightRegion, 0.1f, Animation.PlayMode.LOOP);
        runningLeft       = mirrorAnimation(runningRight);

        currentAnimation = idleAnimation;

        // Starting position
        x = 16;
        y = 9;
    }

    private Animation<TextureRegion> buildAnimation(TextureRegion rowStrip,
                                                    float frameDuration,
                                                    Animation.PlayMode mode) {
        TextureRegion[][] tmp = rowStrip.split(frameWidth, frameHeight);
        TextureRegion[] frames = tmp[0];
        Animation<TextureRegion> anim = new Animation<>(frameDuration, frames);
        anim.setPlayMode(mode);
        return anim;
    }

    private Animation<TextureRegion> mirrorAnimation(Animation<TextureRegion> original) {
        TextureRegion[] origFrames = original.getKeyFrames();
        TextureRegion[] mirrored = new TextureRegion[origFrames.length];
        for (int i = 0; i < origFrames.length; i++) {
            TextureRegion tmp = new TextureRegion(origFrames[i]);
            tmp.flip(true, false);
            mirrored[i] = tmp;
        }
        Animation<TextureRegion> anim = new Animation<>(original.getFrameDuration(), mirrored);
        anim.setPlayMode(original.getPlayMode());
        return anim;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void update(float delta) {
        // 1) Update dash cooldown timer
        if (dashCooldownTimer > 0f) {
            dashCooldownTimer -= delta;
            if (dashCooldownTimer < 0f) dashCooldownTimer = 0f;
        }

        // 2) Check for dash input
        if (!isDashing && dashCooldownTimer <= 0f) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
                startDash();
            }
        }

        // 3) Update dash duration and spawn ghost images
        if (isDashing) {
            dashTimeLeft -= delta;
            if (dashTimeLeft <= 0f) {
                isDashing = false;
                dashCooldownTimer = dashCooldown;
            } else {
                spawnGhostImage();
            }
        }

        // 4) If an attack animation is playing, wait until it finishes
        if (isAttacking && !currentAnimation.isAnimationFinished(stateTime)) {
            stateTime += delta;
            return;
        }
        if (isAttacking && currentAnimation.isAnimationFinished(stateTime)) {
            isAttacking = false;
        }

        // 5) Movement input (W, A, S, D)
        float dx = 0, dy = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) dy += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) dy -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= 1;

        // Normalize diagonal movement
        if (dx != 0 && dy != 0) {
            dx *= 0.7071f;
            dy *= 0.7071f;
        }

        float oldX = x;
        float oldY = y;

        float moveSpeed = isDashing ? dashSpeed : speed;
        x += dx * moveSpeed * delta;
        y += dy * moveSpeed * delta;

        // 6) Collision check using the player's collision polygon
        if (checkCollisionRect(x - COLLISION_W / 2f, y - COLLISION_H / 2f, COLLISION_W, COLLISION_H)) {
            x = oldX;
            y = oldY;
        }

        // 7) Attack on left mouse click
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            isAttacking = true;
            stateTime = 0f;

            Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(mousePos);

            float angleDeg = MathUtils.atan2(mousePos.y - y, mousePos.x - x)
                * MathUtils.radiansToDegrees;

            if (angleDeg >= -45 && angleDeg < 45) {
                currentAnimation = attackRight;
                facingRight = true;
            } else if (angleDeg >= 45 && angleDeg < 135) {
                currentAnimation = attackTop;
            } else if (angleDeg >= -135 && angleDeg < -45) {
                currentAnimation = attackBottom;
            } else {
                currentAnimation = attackLeft;
                facingRight = false;
            }
        } else {
            // 8) Set movement animation based on input direction.
            // If there is any movement input, play the running animation.
            if (dx != 0 || dy != 0) {
                if (dx < 0) {
                    currentAnimation = runningLeft;
                    facingRight = false;
                } else if (dx > 0) {
                    currentAnimation = runningRight;
                    facingRight = true;
                } else {
                    // If only vertical movement, choose running animation based on last facing direction
                    currentAnimation = facingRight ? runningRight : runningLeft;
                }
            } else {
                currentAnimation = facingRight ? idleAnimation : idleAnimationLeft;
            }
        }

        // 9) Update ghost images
        updateGhosts(delta);

        // 10) Advance the animation state time
        stateTime += delta;
    }

    private void startDash() {
        isDashing = true;
        dashTimeLeft = dashDuration;
        spawnGhostImage();
    }

    private void spawnGhostImage() {
        TextureRegion frame = currentAnimation.getKeyFrame(stateTime);
        GhostFrame gf = new GhostFrame();
        gf.x = x;
        gf.y = y;
        gf.region = new TextureRegion(frame);
        gf.alpha = 0.6f;
        gf.timeToLive = 0.3f;
        ghosts.add(gf);
    }

    private void updateGhosts(float delta) {
        Iterator<GhostFrame> it = ghosts.iterator();
        while (it.hasNext()) {
            GhostFrame g = it.next();
            g.timeToLive -= delta;
            float fadeRate = 1f / 0.1f;
            g.alpha -= fadeRate * delta;
            if (g.timeToLive <= 0f || g.alpha <= 0f) {
                it.remove();
            }
        }
    }

    /**
     * Creates a collision polygon from the player's bounding rectangle and checks collision
     * against the collision shapes defined in the tile.
     */
    private boolean checkCollisionRect(float rx, float ry, float rw, float rh) {
        Rectangle playerRect = new Rectangle(rx, ry, rw, rh);
        Polygon playerPoly = createRectanglePolygon(playerRect);

        int startX = (int) Math.floor(rx);
        int endX = (int) Math.floor(rx + rw);
        int startY = (int) Math.floor(ry);
        int endY = (int) Math.floor(ry + rh);

        for (int ty = startY; ty <= endY; ty++) {
            for (int tx = startX; tx <= endX; tx++) {
                if (tileMapRenderer.isCellBlocked(tx, ty, playerPoly)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Helper method to convert a rectangle to a polygon.
    private Polygon createRectanglePolygon(Rectangle rect) {
        float[] vertices = new float[]{
            rect.x, rect.y,
            rect.x + rect.width, rect.y,
            rect.x + rect.width, rect.y + rect.height,
            rect.x, rect.y + rect.height
        };
        return new Polygon(vertices);
    }

    public void render(SpriteBatch batch) {
        // Draw ghost images behind the player
        for (GhostFrame ghost : ghosts) {
            float w = ghost.region.getRegionWidth() * scale;
            float h = ghost.region.getRegionHeight() * scale;
            batch.setColor(1f, 1f, 1f, ghost.alpha);
            batch.draw(ghost.region, ghost.x - w / 2f, ghost.y - h / 2f, w, h);
        }
        batch.setColor(1f, 1f, 1f, 1f);

        // Draw the player character
        TextureRegion frame = currentAnimation.getKeyFrame(stateTime);
        float drawW = frame.getRegionWidth() * scale;
        float drawH = frame.getRegionHeight() * scale;
        batch.draw(frame, x - drawW / 2f, y - drawH / 2f, drawW, drawH);
    }

    // Helper class to store ghost frame data.
    private static class GhostFrame {
        float x, y;
        TextureRegion region;
        float alpha;
        float timeToLive;
    }
}
