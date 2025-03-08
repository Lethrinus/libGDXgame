package io.github.some_example_name;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;

public class Player {
    private float x, y;
    private float speed = 3.5f;

    // Separate timers for movement and attack animations
    private float movementStateTime = 0f;
    private float attackStateTime = 0f;

    private OrthographicCamera camera;
    private TileMapRenderer tileMapRenderer;
    private Texture atlas;

    // Movement animations
    private Animation<TextureRegion> idleAnimation;
    private Animation<TextureRegion> idleAnimationLeft;
    private Animation<TextureRegion> runningRight;
    private Animation<TextureRegion> runningLeft;
    // Attack animations
    private Animation<TextureRegion> attackTop;
    private Animation<TextureRegion> attackBottom;
    private Animation<TextureRegion> attackRight;
    private Animation<TextureRegion> attackLeft;

    // Currently active animations
    private Animation<TextureRegion> currentMovementAnim;
    private Animation<TextureRegion> currentAttackAnim;

    private boolean isAttacking = false;

    // Dashing fields
    private boolean isDashing = false;
    private float dashTimeLeft = 0f;
    private float dashSpeed = 10f;
    private float dashDuration = 0.2f;
    private float dashCooldown = 1.0f;
    private float dashCooldownTimer = 0f;

    // Ghost trail constants
    private static final float GHOST_INITIAL_ALPHA = 0.8f;
    private static final float GHOST_INITIAL_TIME = 0.4f;

    // Ghost trail container
    private static class GhostFrame {
        float x, y;
        TextureRegion region;
        float alpha;
        float timeToLive;
    }
    private java.util.List<GhostFrame> ghosts = new java.util.ArrayList<>();

    // Atlas parameters
    private int frameWidth, frameHeight;
    private float scale = 1.0f / 72f;
    private static final int NUM_FRAMES = 6;

    // Collision box dimensions
    private static final float COLLISION_W = 0.6f;
    private static final float COLLISION_H = 0.6f;

    // Direction (true = facing right)
    private boolean facingRight = true;

    // Health, knockback and red flash effect
    private float maxHealth = 100f;
    private float health = maxHealth;
    private Texture healthBarTexture;

    private float redFlashDuration = 0.2f;
    private float redFlashTimer = 0f;

    // Knockback vector (increased multiplier)
    private Vector2 knockback = new Vector2(0, 0);
    private float knockbackDecay = 5f;

    // Attack-related fields
    // Target goblin (should be set from Main using setTargetGoblin)
    private Goblin targetGoblin;
    private boolean attackExecuted = false;
    private float attackHitTime = 0.15f;   // Time when damage is applied
    private float attackDuration = 0.4f;   // Total duration of the attack animation
    private float attackDamage = 20f;
    // Increased knockback force for attack (4f)
    private float attackKnockbackForce = 4f;
    private float attackRange = 1.5f;

    public float getX() { return x; }
    public float getY() { return y; }

    public Player(OrthographicCamera camera, TileMapRenderer tileMapRenderer) {
        this.camera = camera;
        this.tileMapRenderer = tileMapRenderer;

        atlas = new Texture(Gdx.files.internal("Player/knight_atlas.png"));

        // Define subregions (positions and sizes assumed)
        TextureRegion idleRegion         = new TextureRegion(atlas, 2, 2, 1152, 195);
        TextureRegion attackTopRegion    = new TextureRegion(atlas, 2, 199, 1152, 195);
        TextureRegion attackBottomRegion = new TextureRegion(atlas, 2, 396, 1152, 195);
        TextureRegion attackRightRegion  = new TextureRegion(atlas, 2, 593, 1152, 195);
        TextureRegion runRightRegion     = new TextureRegion(atlas, 2, 790, 1152, 195);

        frameWidth  = 1152 / NUM_FRAMES; // 192
        frameHeight = 195;

        idleAnimation     = buildAnimation(idleRegion, 0.25f, Animation.PlayMode.LOOP);
        idleAnimationLeft = mirrorAnimation(idleAnimation);
        attackTop         = buildAnimation(attackTopRegion, 0.05f, Animation.PlayMode.NORMAL);
        attackBottom      = buildAnimation(attackBottomRegion, 0.05f, Animation.PlayMode.NORMAL);
        attackRight       = buildAnimation(attackRightRegion, 0.05f, Animation.PlayMode.NORMAL);
        attackLeft        = mirrorAnimation(attackRight);
        runningRight      = buildAnimation(runRightRegion, 0.1f, Animation.PlayMode.LOOP);
        runningLeft       = mirrorAnimation(runningRight);

        currentMovementAnim = idleAnimation;

        // Starting position
        x = 16;
        y = 9;

        // Create a simple white texture for health bar rendering
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        healthBarTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    private Animation<TextureRegion> buildAnimation(TextureRegion region, float frameDuration, Animation.PlayMode mode) {
        TextureRegion[][] tmp = region.split(frameWidth, frameHeight);
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

    // Set the target goblin (call from Main)
    public void setTargetGoblin(Goblin goblin) {
        this.targetGoblin = goblin;
    }

    // Apply damage to the player along with knockback and red flash effect
    public void takeDamage(float damage, float knockbackForce, float angleDegrees) {
        health -= damage;
        if (health < 0) health = 0;
        float angleRad = MathUtils.degreesToRadians * angleDegrees;
        // Increased knockback multiplier (1.5x)
        knockback.set(knockbackForce * MathUtils.cos(angleRad) * 1.5f,
            knockbackForce * MathUtils.sin(angleRad) * 1.5f);
        redFlashTimer = redFlashDuration;
    }

    public void update(float delta) {
        // Handle dashing cooldown and duration
        if (dashCooldownTimer > 0f) {
            dashCooldownTimer -= delta;
            if (dashCooldownTimer < 0f) dashCooldownTimer = 0f;
        }
        if (!isDashing && dashCooldownTimer <= 0f && Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            isDashing = true;
            dashTimeLeft = dashDuration;
        }
        if (isDashing) {
            dashTimeLeft -= delta;
            if (dashTimeLeft <= 0f) {
                isDashing = false;
                dashCooldownTimer = dashCooldown;
            } else {
                spawnGhostImage();
            }
        }

        // Handle attack input and processing
        if (!isAttacking && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            isAttacking = true;
            attackStateTime = 0f;
            attackExecuted = false;

            Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(mousePos);
            float angleDeg = MathUtils.atan2(mousePos.y - y, mousePos.x - x) * MathUtils.radiansToDegrees;
            if (angleDeg >= -45 && angleDeg < 45) {
                currentAttackAnim = attackRight;
                facingRight = true;
            } else if (angleDeg >= 45 && angleDeg < 135) {
                currentAttackAnim = attackTop;
            } else if (angleDeg >= -135 && angleDeg < -45) {
                currentAttackAnim = attackBottom;
            } else {
                currentAttackAnim = attackLeft;
                facingRight = false;
            }
        }
        if (isAttacking) {
            attackStateTime += delta;
            if (!attackExecuted && attackStateTime >= attackHitTime) {
                if (targetGoblin != null) {
                    float dx = targetGoblin.getX() - x;
                    float dy = targetGoblin.getY() - y;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    if (dist <= attackRange) {
                        float angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
                        targetGoblin.takeDamage(attackDamage, attackKnockbackForce, angle);
                        attackExecuted = true;
                    }
                }
            }
            if (attackStateTime >= attackDuration) {
                isAttacking = false;
            }
            // During attack, do not process movement
            return;
        }

        // Process movement input
        float dx = 0, dy = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) dy += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) dy -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= 1;
        if (dx != 0 && dy != 0) {
            dx *= 0.7071f;
            dy *= 0.7071f;
        }
        float oldX = x, oldY = y;
        float moveSpeed = isDashing ? dashSpeed : speed;
        x += dx * moveSpeed * delta;
        y += dy * moveSpeed * delta;

        // Simple collision check
        Rectangle playerRect = new Rectangle(x - COLLISION_W / 2f, y - COLLISION_H / 2f, COLLISION_W, COLLISION_H);
        if (tileMapRenderer.isCellBlocked((int)Math.floor(x), (int)Math.floor(y), createRectanglePolygon(playerRect))) {
            x = oldX;
            y = oldY;
        }

        // Update movement animation based on input
        if (dx != 0 || dy != 0) {
            if (dx < 0) {
                currentMovementAnim = runningLeft;
                facingRight = false;
            } else {
                currentMovementAnim = runningRight;
                facingRight = true;
            }
        } else {
            currentMovementAnim = facingRight ? idleAnimation : idleAnimationLeft;
        }

        // Apply knockback effect if any
        if (knockback.len() > 0.01f) {
            x += knockback.x * delta;
            y += knockback.y * delta;
            knockback.scl(1 - knockbackDecay * delta);
            if (knockback.len() < 0.01f) {
                knockback.set(0, 0);
            }
        }
        movementStateTime += delta;

        if (redFlashTimer > 0) redFlashTimer -= delta;

        // Update ghost trail frames
        updateGhosts(delta);
    }

    // Spawn a ghost image of the current movement frame
    private void spawnGhostImage() {
        TextureRegion frame = currentMovementAnim.getKeyFrame(movementStateTime);
        GhostFrame gf = new GhostFrame();
        gf.x = x;
        gf.y = y;
        gf.region = new TextureRegion(frame);
        gf.alpha = GHOST_INITIAL_ALPHA;
        gf.timeToLive = GHOST_INITIAL_TIME;
        ghosts.add(gf);
    }

    // Update ghost frames: fade them out linearly over their lifetime
    private void updateGhosts(float delta) {
        java.util.Iterator<GhostFrame> it = ghosts.iterator();
        // Calculate fade rate so that alpha reaches 0 when timeToLive expires
        float fadeRate = GHOST_INITIAL_ALPHA / GHOST_INITIAL_TIME;
        while (it.hasNext()) {
            GhostFrame g = it.next();
            g.timeToLive -= delta;
            g.alpha -= fadeRate * delta;
            if (g.timeToLive <= 0f || g.alpha <= 0f) {
                it.remove();
            }
        }
    }

    // Helper: create a polygon from a rectangle
    private Polygon createRectanglePolygon(Rectangle rect) {
        float[] vertices = new float[] {
            rect.x, rect.y,
            rect.x + rect.width, rect.y,
            rect.x + rect.width, rect.y + rect.height,
            rect.x, rect.y + rect.height
        };
        return new Polygon(vertices);
    }

    // Render the player sprite and ghost trail
    public void render(SpriteBatch batch) {
        TextureRegion frame;
        if (isAttacking) {
            frame = currentAttackAnim.getKeyFrame(attackStateTime);
        } else {
            frame = currentMovementAnim.getKeyFrame(movementStateTime);
        }
        float drawW = frame.getRegionWidth() * scale;
        float drawH = frame.getRegionHeight() * scale;

        // Render ghost trail behind the player
        for (GhostFrame ghost : ghosts) {
            float w = ghost.region.getRegionWidth() * scale;
            float h = ghost.region.getRegionHeight() * scale;
            batch.setColor(1f, 1f, 1f, ghost.alpha);
            batch.draw(ghost.region, ghost.x - w / 2f, ghost.y - h / 2f, w, h);
        }
        batch.setColor(1, 1, 1, 1);

        batch.draw(frame, x - drawW / 2f, y - drawH / 2f, drawW, drawH);

        // Draw red flash overlay on the player when hit
        if (redFlashTimer > 0) {
            batch.setColor(1, 0, 0, 0.3f);
            batch.draw(frame, x - drawW / 2f, y - drawH / 2f, drawW, drawH);
            batch.setColor(1, 1, 1, 1);
        }
    }

    // Render the player's health bar (should be drawn on top in a separate batch)
    public void renderHealthBar(SpriteBatch batch) {
        float barWidth = 1f;
        float barHeight = 0.1f;
        float healthPercentage = health / maxHealth;
        batch.setColor(1, 0, 0, 1);
        batch.draw(healthBarTexture, x - barWidth / 2, y + (barWidth / 2f) + 0.2f, barWidth * healthPercentage, barHeight);
        batch.setColor(1, 1, 1, 1);
    }

    public void dispose() {
        atlas.dispose();
        healthBarTexture.dispose();
    }
}
