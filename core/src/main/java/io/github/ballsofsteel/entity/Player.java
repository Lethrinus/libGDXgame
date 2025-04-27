package io.github.ballsofsteel.entity;

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
import com.badlogic.gdx.math.Vector2;
import io.github.ballsofsteel.ui.Inventory;
import io.github.ballsofsteel.core.TileMapRenderer;
import io.github.ballsofsteel.core.CoreGame;
import java.util.ArrayList;
import java.util.List;

/**
 * Player class with movement, attacking, dash mechanics, and collision detection.
 */
public class Player {
    private float x, y;
    private float speed = 3.5f;
    private CoreGame core;
    // Timers for movement and attack animations.
    private float movementStateTime = 0f;
    private float attackStateTime = 0f;

    private OrthographicCamera camera;
    private TileMapRenderer tileMapRenderer;
    private Texture atlas;

    // Movement animations.
    private Animation<TextureRegion> idleAnimation, idleAnimationLeft, runningRight, runningLeft;
    // Attack animations.
    private Animation<TextureRegion> attackTop, attackBottom, attackRight, attackLeft;

    private Animation<TextureRegion> healAnimation;
    private Texture healTexture;
    private float healStateTime = 0f;
    private boolean isHealing = false;
    private float healScale = 1f / 64f; // Scale factor for heal effect.
    private float damageMultiplier = 1f;
    private float speedMultiplier = 1f;


    // Currently active animations.
    private Animation<TextureRegion> currentMovementAnim;
    private Animation<TextureRegion> currentAttackAnim;

    private boolean isAttacking = false;

    // Dash fields.
    private boolean isDashing = false;
    private float dashTimeLeft = 0f;
    private float dashSpeed = 10f;
    private float dashDuration = 0.2f;
    private float dashCooldown = 1.0f;
    private float dashCooldownTimer = 0f;

    // Ghost trail constants.
    private static final float GHOST_INITIAL_ALPHA = 0.4f;
    private static final float GHOST_INITIAL_TIME = 0.15f;

    // Ghost trail container.
    private static class GhostFrame {
        float x, y;
        TextureRegion region;
        float alpha;
        float timeToLive;
    }
    private List<GhostFrame> ghosts = new ArrayList<>();

    // Atlas parameters.
    private int frameWidth, frameHeight;
    private float scale = 1.0f / 72f;
    private static final int NUM_FRAMES = 6;

    // Collision box dimensions.
    private static final float COLLISION_W = 0.6f;
    private static final float COLLISION_H = 0.6f;

    // Direction (true = facing right).
    private boolean facingRight = true;

    // Health, knockback, and red flash effect.
    private float maxHealth;
    private float health;
    private float redFlashDuration = 0.2f;
    private float redFlashTimer = 0f;

    // Knockback vector.
    private Vector2 knockback = new Vector2(0, 0);
    private float knockbackDecay = 5f;

    // Attack-related fields.
    private Goblin targetGoblin;
    private boolean attackExecuted = false;
    private float attackHitTime = 0.15f;
    private float attackDuration = 0.4f;
    private float attackDamage = 20f;
    private float attackKnockbackForce = 4f;
    private float attackRange = 1.5f;
    float dmg = attackDamage * damageMultiplier;
    float moveSpee = (isDashing? dashSpeed : speed*speedMultiplier);
    public float getX() { return x; }
    public float getY() { return y; }
    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }
    public float getDashCooldown() {
        return dashCooldown;
    }
    public float getDashCooldownTimer() {
        return dashCooldownTimer;
    }

    private Inventory inventory;

    public Player(OrthographicCamera camera, TileMapRenderer tileMapRenderer, CoreGame core) {
        this.camera = camera;
        this.core = core;
        this.tileMapRenderer = tileMapRenderer;
        this.health = 100f;
        this.maxHealth = 100f;
        this.inventory = new Inventory();
        atlas = new Texture(Gdx.files.internal("Player/knight_atlas.png"));

        // Initialize heal animation.
        healTexture = new Texture(Gdx.files.internal("Player/heal_animation.png"));
        TextureRegion healRegion = new TextureRegion(healTexture, 2, 2, 512, 512);
        int rows = 4, cols = 4;
        TextureRegion[][] healFrames2D = healRegion.split(512 / cols, 512 / rows);
        TextureRegion[] healFrames = new TextureRegion[rows * cols];
        int index = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                healFrames[index++] = healFrames2D[i][j];
            }
        }
        healAnimation = new Animation<TextureRegion>(0.1f, healFrames);
        healAnimation.setPlayMode(Animation.PlayMode.NORMAL);

        // Define subregions for animations.
        TextureRegion idleRegion         = new TextureRegion(atlas, 2, 2, 1152, 195);
        TextureRegion attackTopRegion    = new TextureRegion(atlas, 2, 199, 1152, 195);
        TextureRegion attackBottomRegion = new TextureRegion(atlas, 2, 396, 1152, 195);
        TextureRegion attackRightRegion  = new TextureRegion(atlas, 2, 593, 1152, 195);
        TextureRegion runRightRegion     = new TextureRegion(atlas, 2, 790, 1152, 195);

        frameWidth  = 1152 / NUM_FRAMES; // 192 pixels per frame.
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

        // Set starting position.
        x = 8;
        y = 4.5f;
    }

    /**
     * Triggers the healing effect animation.
     */
    public void triggerHealEffect() {
        isHealing = true;
        healStateTime = 0f;
    }
    // ARTTIRICI METODLAR
    public void increaseAttackDamage(float mult){ damageMultiplier *= mult; }
    public void increaseMoveSpeed   (float mult){ speedMultiplier  *= mult; }

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

    public void increaseHealth(float amount) {
        health += amount;
        if (health > maxHealth) {
            health = maxHealth;
        }
    }

    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Sets the player's position.
     *
     * @param x New x-coordinate.
     * @param y New y-coordinate.
     */
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Sets the target goblin for attack interactions.
     *
     * @param goblin The target Goblin.
     */
    public void setTargetGoblin(Goblin goblin) {
        this.targetGoblin = goblin;
    }

    /**
     * Updates the TileMapRenderer reference (used when changing maps).
     *
     * @param tileMapRenderer The new TileMapRenderer.
     */
    public void setTileMapRenderer(TileMapRenderer tileMapRenderer) {
        this.tileMapRenderer = tileMapRenderer;
    }

    /**
     * Checks if the player is currently in a bush cell.
     *
     * @return true if in a bush cell; false otherwise.
     */
    public boolean isInBush() {
        int tileX = (int) Math.floor(x);
        int tileY = (int) Math.floor(y);
        return tileMapRenderer.isCellBush(tileX, tileY);
    }

    private int gold;
    public void addGold(int n){ gold += n; }
    public int  getGold(){ return gold; }

    /**
     * Applies damage, knockback, and red flash effect to the player.
     *
     * @param damage         Damage amount.
     * @param knockbackForce Force of the knockback.
     * @param angleDegrees   Direction of the knockback in degrees.
     */
    public void takeDamage(float damage, float knockbackForce, float angleDegrees) {
        health -= damage;
        if (health < 0) health = 0;
        float angleRad = MathUtils.degreesToRadians * angleDegrees;
        knockback.set(knockbackForce * MathUtils.cos(angleRad) * 1.5f,
            knockbackForce * MathUtils.sin(angleRad) * 1.5f);
        redFlashTimer = redFlashDuration;
        io.github.ballsofsteel.events.EventBus.get()
            .post(new io.github.ballsofsteel.events.GameEvent(
                io.github.ballsofsteel.events.GameEventType.PLAYER_HEALTH_CHANGED, this));
    }
    public float getSpriteHeight() {
        // Use the current movement animation frame.
        TextureRegion currentFrame = currentMovementAnim.getKeyFrame(movementStateTime);
        return currentFrame.getRegionHeight() * scale;
    }
    public void update(float delta) {
        // Handle dash cooldown.
        if (dashCooldownTimer > 0f) {
            dashCooldownTimer -= delta;
            if (dashCooldownTimer < 0f) dashCooldownTimer = 0f;
        }
        if (isHealing) {
            healStateTime += delta;
            if (healAnimation.isAnimationFinished(healStateTime)) {
                isHealing = false;
            }
        }
        // Process movement input.
        float dx = 0, dy = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) dy += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) dy -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= 1;

        // Normalize diagonal movement.
        if (dx != 0 && dy != 0) {
            dx *= 0.7071f;
            dy *= 0.7071f;
        }

        // Handle dash input (only if moving).
        if (!isDashing && dashCooldownTimer <= 0f && Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            if (dx != 0 || dy != 0) {
                isDashing = true;
                dashTimeLeft = dashDuration;
            }
        }

        // If dashing, decrement time and spawn ghost images.
        if (isDashing) {
            dashTimeLeft -= delta;
            if (dashTimeLeft <= 0f) {
                isDashing = false;
                dashCooldownTimer = dashCooldown;
            } else {
                spawnGhostImage();
            }
        }

        // Handle attack input.
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

        // Attack logic.
        if (isAttacking) {
            attackStateTime += delta;

            if (!attackExecuted && attackStateTime >= attackHitTime) {
                /* -- Dinamitçiler */
                for (DynamiteGoblin dg : core.getDynaList()) {
                    float ddx = dg.getX() - x, ddy = dg.getY() - y;      // ← isimler değiştirildi
                    if (ddx*ddx + ddy*ddy <= attackRange*attackRange) {
                        float ang = MathUtils.atan2(ddy, ddx) * MathUtils.radiansToDegrees;
                        dg.takeDamage(attackDamage, attackKnockbackForce, ang);
                        attackExecuted = true;
                    }
                }

                /* -- Ana Goblin */
                /* -------- goblinler -------- */
                if(!attackExecuted){
                    for(Goblin g : core.getGoblins()){
                        float gx=g.getX()-x, gy=g.getY()-y;
                        if(gx*gx+gy*gy <= attackRange*attackRange){
                            float ang = MathUtils.atan2(gy,gx)*MathUtils.radiansToDegrees;
                            g.takeDamage(attackDamage, attackKnockbackForce, ang);
                            attackExecuted = true;   // tek darbe → tek goblin
                            break;
                        }
                    }
                }

            }
            if (attackStateTime >= attackDuration) isAttacking = false;
        }
         else {
            // Store old position.
            float oldX = x, oldY = y;
            float moveSpeed = isDashing ? dashSpeed : speed;
            x += dx * moveSpeed * delta;
            y += dy * moveSpeed * delta;

            // Collision check: create rectangle and convert to polygon.
            Rectangle playerRect = new Rectangle(x - COLLISION_W / 2f, y - COLLISION_H / 2f, COLLISION_W, COLLISION_H);
            Polygon poly = createRectanglePolygon(playerRect);
            if (tileMapRenderer.isCellBlocked((int) Math.floor(x), (int) Math.floor(y), poly)) {
                x = oldX;
                y = oldY;
            }

            // Update movement animation based on input.
            if (dx != 0 || dy != 0) {
                if (dx != 0) {
                    if (dx < 0) {
                        currentMovementAnim = runningLeft;
                        facingRight = false;
                    } else {
                        currentMovementAnim = runningRight;
                        facingRight = true;
                    }
                } else {
                    // Only vertical movement.
                    // If vertical running animations are available, use them.
                    // Otherwise, use the horizontal running animation as a fallback.
                    currentMovementAnim = facingRight ? runningRight : runningLeft;
                }
            } else {
                currentMovementAnim = facingRight ? idleAnimation : idleAnimationLeft;
            }
        }

        // Apply knockback.
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
        updateGhosts(delta);
    }

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

    private void updateGhosts(float delta) {
        float fadeRate = GHOST_INITIAL_ALPHA / GHOST_INITIAL_TIME;
        for (int i = ghosts.size() - 1; i >= 0; i--) {
            GhostFrame g = ghosts.get(i);
            g.timeToLive -= delta;
            g.alpha -= fadeRate * delta;
            if (g.timeToLive <= 0f || g.alpha <= 0f) {
                ghosts.remove(i);
            }
        }
    }

    private Polygon createRectanglePolygon(Rectangle rect) {
        float[] vertices = new float[] {
            rect.x, rect.y,
            rect.x + rect.width, rect.y,
            rect.x + rect.width, rect.y + rect.height,
            rect.x, rect.y + rect.height
        };
        return new Polygon(vertices);
    }


    public void render(SpriteBatch batch) {
        // Determine current frame based on whether attacking or moving.
        TextureRegion frame = isAttacking ? currentAttackAnim.getKeyFrame(attackStateTime)
            : currentMovementAnim.getKeyFrame(movementStateTime);
        float drawW = frame.getRegionWidth() * scale;
        float drawH = frame.getRegionHeight() * scale;

        // Render ghost trail.
        for (GhostFrame ghost : ghosts) {
            float w = ghost.region.getRegionWidth() * scale;
            float h = ghost.region.getRegionHeight() * scale;
            batch.setColor(1f, 1f, 1f, ghost.alpha);
            batch.draw(ghost.region, ghost.x - w / 2f, ghost.y - h / 2f, w, h);
        }
        batch.setColor(1, 1, 1, 1);

        // Render the player sprite.
        batch.draw(frame, x - drawW / 2f, y - drawH / 2f, drawW, drawH);

        // Render heal effect centered on the player.
        if (isHealing) {
            TextureRegion healFrame = healAnimation.getKeyFrame(healStateTime);
            float healW = healFrame.getRegionWidth() * healScale;
            float healH = healFrame.getRegionHeight() * healScale;
            batch.draw(healFrame, x - healW / 2f, y - healH / 2f, healW, healH);
        }

        // Render red flash effect if damaged.
        if (redFlashTimer > 0) {
            batch.setColor(1, 0, 0, 0.3f);
            batch.draw(frame, x - drawW / 2f, y - drawH / 2f, drawW, drawH);
            batch.setColor(1, 1, 1, 1);
        }
    }

    // Getter methods for dash cooldown UI.



    /**
     * Disposes of player resources.
     */
    public void dispose() {
        atlas.dispose();
        healTexture.dispose();
    }
}
