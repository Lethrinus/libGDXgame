package io.github.some_example_name;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

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
    private float dashSpeed = 10f;       // dash movement speed
    private float dashDuration = 0.2f;   // dash lasts 0.2s
    private float dashCooldown = 1.0f;   // 1s cooldown between dashes
    private float dashCooldownTimer = 0f;

    // Ghost trail
    private List<GhostFrame> ghosts = new ArrayList<>();

    // Atlas frames
    private int frameWidth, frameHeight;
    private float scale = 1.0f / 72f;
    private static final int NUM_FRAMES = 6;

    // bounding box for collision
    private static final float COLLISION_W = 0.6f;
    private static final float COLLISION_H = 0.6f;

    public float getX() { return x; }
    public float getY() { return y; }

    public Player(OrthographicCamera camera, TileMapRenderer tileMapRenderer) {
        this.camera = camera;
        this.tileMapRenderer = tileMapRenderer;

        // Load atlas
        atlas = new Texture(Gdx.files.internal("Player/knight_atlas.png"));

        // Single-row subregions
        TextureRegion idleRegion         = new TextureRegion(atlas, 2,   2,   1152, 195);
        TextureRegion attackTopRegion    = new TextureRegion(atlas, 2,  199,  1152, 195);
        TextureRegion attackBottomRegion = new TextureRegion(atlas, 2,  396,  1152, 195);
        TextureRegion attackRightRegion  = new TextureRegion(atlas, 2,  593,  1152, 195);
        TextureRegion runRightRegion     = new TextureRegion(atlas, 2,  790,  1152, 195);

        frameWidth  = 1152 / NUM_FRAMES; // 192
        frameHeight = 195;

        idleAnimation   = buildAnimation(idleRegion,        0.4f,  Animation.PlayMode.LOOP);
        attackTop       = buildAnimation(attackTopRegion,   0.05f, Animation.PlayMode.NORMAL);
        attackBottom    = buildAnimation(attackBottomRegion,0.05f, Animation.PlayMode.NORMAL);
        attackRight     = buildAnimation(attackRightRegion, 0.05f, Animation.PlayMode.NORMAL);
        attackLeft      = mirrorAnimation(attackRight);
        runningRight    = buildAnimation(runRightRegion,    0.1f,  Animation.PlayMode.LOOP);
        runningLeft     = mirrorAnimation(runningRight);

        currentAnimation = idleAnimation;

        // Start position
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
        TextureRegion[] mirrored   = new TextureRegion[origFrames.length];
        for(int i=0; i<origFrames.length; i++){
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

        // 1) Update dash cooldown
        if(dashCooldownTimer > 0f){
            dashCooldownTimer -= delta;
            if(dashCooldownTimer < 0f) dashCooldownTimer = 0f;
        }

        // 2) Check for dash input if not dashing & off cooldown
        if(!isDashing && dashCooldownTimer <= 0f) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
                // Start dash
                startDash();
            }
        }

        // 3) If currently dashing, update dash time
        if (isDashing) {
            dashTimeLeft -= delta;
            if (dashTimeLeft <= 0f) {
                // dash ended
                isDashing = false;
                dashCooldownTimer = dashCooldown;
            } else {
                // spawn a ghost trail each frame
                spawnGhostImage();
            }
        }

        // 4) Decide movement speed
        float moveSpeed = isDashing ? dashSpeed : speed;

        // Attack anim in progress?
        if (isAttacking && !currentAnimation.isAnimationFinished(stateTime)) {
            stateTime += delta;
            return;
        }
        if (isAttacking && currentAnimation.isAnimationFinished(stateTime)) {
            isAttacking = false;
        }

        // 5) Movement input (WASD)
        float dx = 0, dy = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) dy += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) dy -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= 1;

        // normalize diagonal
        if(dx != 0 && dy != 0) {
            dx *= 0.7071f;
            dy *= 0.7071f;
        }

        float oldX = x;
        float oldY = y;

        // 6) Apply movement speed
        x += dx * moveSpeed * delta;
        y += dy * moveSpeed * delta;

        // 7) Collisions with bounding box
        if (checkCollisionRect(x - COLLISION_W/2f, y - COLLISION_H/2f,
            COLLISION_W, COLLISION_H)) {
            // blocked => revert
            x = oldX;
            y = oldY;
        }

        // 8) Attack with left click
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            isAttacking = true;
            stateTime = 0f;

            Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(mousePos);

            float angleDeg = MathUtils.atan2(mousePos.y - y, mousePos.x - x)
                * MathUtils.radiansToDegrees;

            if (angleDeg >= -45 && angleDeg < 45) {
                currentAnimation = attackRight;
            }
            else if (angleDeg >= 45 && angleDeg < 135) {
                currentAnimation = attackTop;
            }
            else if (angleDeg >= -135 && angleDeg < -45) {
                currentAnimation = attackBottom;
            }
            else {
                currentAnimation = attackLeft;
            }
        }
        else {
            // Movement animation vs. idle
            if(dx != 0 || dy != 0){
                if(dx < 0)      currentAnimation = runningLeft;
                else if(dx > 0) currentAnimation = runningRight;
                else            currentAnimation = runningRight;
            }
            else {
                currentAnimation = idleAnimation;
            }
        }

        // 9) Update ghosts
        updateGhosts(delta);

        // 10) Advance time
        stateTime += delta;
    }

    /**
     * Start a dash: set isDashing, dashTime, spawn an initial ghost if wanted.
     */
    private void startDash(){
        isDashing = true;
        dashTimeLeft = dashDuration;
        spawnGhostImage();
    }

    /**
     * Spawn a "ghost" with the current frame, position.
     */
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

    /**
     * Fade out ghost frames, remove them when fully faded.
     */
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

    public void render(SpriteBatch batch) {
        // 1) Draw ghost frames behind the player
        for (GhostFrame ghost : ghosts) {
            float w = ghost.region.getRegionWidth()  * scale;
            float h = ghost.region.getRegionHeight() * scale;
            batch.setColor(1f, 1f, 1f, ghost.alpha);
            batch.draw(ghost.region, ghost.x - w/2f, ghost.y - h/2f, w, h);
        }
        batch.setColor(1f,1f,1f,1f);

        // 2) Draw the player
        TextureRegion frame = currentAnimation.getKeyFrame(stateTime);
        float drawW = frame.getRegionWidth()  * scale;
        float drawH = frame.getRegionHeight() * scale;
        batch.draw(frame, x - drawW/2f, y - drawH/2f, drawW, drawH);
    }

    /**
     * Collisions: check if (rx, ry, rw, rh) covers any "blocked" tile in layer=1.
     */
    private boolean checkCollisionRect(float rx, float ry, float rw, float rh) {
        int startX = (int)Math.floor(rx);
        int endX   = (int)Math.floor(rx + rw);
        int startY = (int)Math.floor(ry);
        int endY   = (int)Math.floor(ry + rh);

        for(int ty=startY; ty<=endY; ty++){
            for(int tx=startX; tx<=endX; tx++){
                if(tileMapRenderer.isCellBlocked(tx, ty)){
                    return true;
                }
            }
        }
        return false;
    }

    // simple struct for "ghost" frames
    private static class GhostFrame {
        float x, y;
        TextureRegion region;
        float alpha;
        float timeToLive;
    }
}
