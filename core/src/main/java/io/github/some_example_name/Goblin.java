package io.github.some_example_name;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

/**
 * A basic Goblin enemy with patrol, chase, and attack states.
 * After the death animation finishes, the goblin disappears after a delay.
 */
public class Goblin {
    private enum State { IDLE, PATROL, CHASE, ATTACK }
    private State state = State.PATROL;

    private float x, y;
    private float speed = 2.0f;

    // Patrol boundaries.
    private float patrolMinX, patrolMaxX, patrolMinY, patrolMaxY, patrolTargetX, patrolTargetY;

    // Distance thresholds.
    private float alertRadius = 4.0f;
    private float attackRadius = 1.0f;

    // Animations.
    private Texture atlas;
    private Animation<TextureRegion> idleAnimation, runRight, runLeft, attackRight, attackLeft, attackUp, attackDown, currentMovementAnim, currentAttackAnim;

    private float movementStateTime = 0f;
    private float attackStateTime = 0f;
    private boolean isAttacking = false;

    private float scale = 1f / 72f;

    private Player player;

    // Health and knockback.
    private float maxHealth = 50f;
    private float health = maxHealth;

    private Vector2 knockback = new Vector2(0, 0);
    private float knockbackDecay = 5f;

    private boolean attackExecuted = false;
    private float attackHitTime = 0.15f;
    private float attackDuration = 0.4f;
    private float attackDamage = 10f;
    private float attackKnockbackForce = 3f;


    // Attack cooldown
    private float attackCooldownTimer = 0f;
    private float attackCooldownDuration = 0.6f; // Time between attacks
    private float minAttackDistance = 0.8f; // Minimum distance to maintain when attacking
    // Add these fields to the Goblin class
    private List<Vector2> patrolWaypoints = new ArrayList<>();
    private int currentWaypointIndex = 0;
    private boolean usingWaypoints = false;
    private float waypointPauseDuration = 2.0f;
    private float currentPauseTime = 0f;
    private boolean isPaused = false;
    private float lookAroundTimer = 0f;
    private float lookDirection = 1f; // 1 for right, -1 for left

    // Red flash effect.
    private float redFlashDuration = 0.2f;
    private float redFlashTimer = 0f;

    // Death animation.
    private Animation<TextureRegion> deathAnimation;
    private float deathStateTime = 0f;
    private boolean isDying = false;
    private boolean isDead = false;
    private Texture deathTexture;

    // Time after death animation finishes.
    private float deathWaitTime = 0f;
    private float deathDisappearDelay = 5f; // seconds.

    public float getX() { return x; }
    public float getY() { return y; }
    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }
    public boolean isDead() {
        return isDead;
    }
    public Goblin(Player player,
                  float startX, float startY,
                  float patrolMinX, float patrolMaxX,
                  float patrolMinY, float patrolMaxY) {
        this.player = player;
        this.x = startX;
        this.y = startY;
        this.patrolMinX = patrolMinX;
        this.patrolMaxX = patrolMaxX;
        this.patrolMinY = patrolMinY;
        this.patrolMaxY = patrolMaxY;

        atlas = new Texture(Gdx.files.internal("Goblin/goblin_animations.png"));

        // Build running animation.
        TextureRegion runRow = new TextureRegion(atlas, 2, 2, 1152, 190);
        runRight = buildAnimation(runRow, 6, 0.1f, Animation.PlayMode.LOOP);
        runLeft = mirrorAnimation(runRight);

        // Build idle animation.
        TextureRegion idleRow = new TextureRegion(atlas, 2, 194, 1344, 190);
        idleAnimation = buildAnimation(idleRow, 7, 0.1f, Animation.PlayMode.LOOP);
        currentMovementAnim = idleAnimation;

        // Build attack animations.
        TextureRegion attackBRow = new TextureRegion(atlas, 2, 386, 1152, 190);
        attackDown = buildAnimation(attackBRow, 6, 0.1f, Animation.PlayMode.NORMAL);
        TextureRegion attackRRow = new TextureRegion(atlas, 2, 578, 1152, 190);
        attackRight = buildAnimation(attackRRow, 6, 0.1f, Animation.PlayMode.NORMAL);
        attackLeft = mirrorAnimation(attackRight);
        TextureRegion attackURow = new TextureRegion(atlas, 2, 770, 1152, 190);
        attackUp = buildAnimation(attackURow, 6, 0.1f, Animation.PlayMode.NORMAL);

        // Build death animation.
        deathTexture = new Texture(Gdx.files.internal("deadanimation.png"));
        TextureRegion dead1 = new TextureRegion(deathTexture, 2, 2, 1792, 128);
        deathAnimation = buildAnimation(dead1, 14, 0.1f, Animation.PlayMode.NORMAL);

        pickRandomPatrolTarget();
    }

    private Animation<TextureRegion> buildAnimation(TextureRegion region, int numFrames, float frameDuration, Animation.PlayMode mode) {
        int w = region.getRegionWidth() / numFrames;
        int h = region.getRegionHeight();
        TextureRegion[][] tmp = region.split(w, h);
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

    private void pickRandomPatrolTarget() {
        patrolTargetX = MathUtils.random(patrolMinX, patrolMaxX);
        patrolTargetY = MathUtils.random(patrolMinY, patrolMaxY);
    }

    public void setPatrolWaypoints(List<Vector2> waypoints) {
        if (waypoints != null && !waypoints.isEmpty()) {
            this.patrolWaypoints = new ArrayList<>(waypoints);
            this.currentWaypointIndex = 0;
            this.usingWaypoints = true;
        }
    }

    private void moveToNextWaypoint() {
        currentWaypointIndex = (currentWaypointIndex + 1) % patrolWaypoints.size();
        isPaused = true;
        currentPauseTime = 0f;
    }

    public void takeDamage(float damage, float knockbackForce, float angleDegrees) {
        health -= damage;
        if (health < 0) health = 0;
        float angleRad = MathUtils.degreesToRadians * angleDegrees;
        knockback.set(knockbackForce * MathUtils.cos(angleRad) * 1.5f,
            knockbackForce * MathUtils.sin(angleRad) * 1.5f);
        redFlashTimer = redFlashDuration;
    }

    public void update(float delta) {
        if (isDead) return;

        // If dying, play death animation.
        if (isDying) {
            deathStateTime += delta;
            if (deathAnimation.isAnimationFinished(deathStateTime)) {
                // After death animation, wait for a delay before marking as dead.
                deathWaitTime += delta;
                if (deathWaitTime >= deathDisappearDelay) {
                    isDead = true;
                }
            }
            return;
        }

        // Update attack cooldown timer
        if (attackCooldownTimer > 0) {
            attackCooldownTimer -= delta;
        }

        // Trigger death if health is depleted.
        if (health <= 0 && !isDying) {
            isDying = true;
            deathStateTime = 0f;
            return;
        }

        if (isAttacking) {
            // Handle attack animation timing.
            attackStateTime += delta;
            if (!attackExecuted && attackStateTime >= attackHitTime) {
                float dx = player.getX() - x;
                float dy = player.getY() - y;
                float dist = (float)Math.sqrt(dx * dx + dy * dy);
                if (dist <= attackRadius) {
                    float angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
                    player.takeDamage(attackDamage, attackKnockbackForce, angle);
                    attackExecuted = true;
                }
            }
            if (attackStateTime >= attackDuration) {
                isAttacking = false;
                attackExecuted = false;
                // Reset attack cooldown when attack finishes
                attackCooldownTimer = attackCooldownDuration;
            }
            return;
        }

        // Calculate distance to player.
        float dxP = player.getX() - x;
        float dyP = player.getY() - y;
        float distToPlayer = (float)Math.sqrt(dxP * dxP + dyP * dyP);

        // Bush mechanic: if player is in a bush, goblin continues patrolling.
        if (player.isInBush()) {
            state = State.PATROL;
        } else if (distToPlayer < attackRadius && attackCooldownTimer <= 0) {
            // Only initiate attack if cooldown has expired
            isAttacking = true;
            attackStateTime = 0f;
            attackExecuted = false;
            float angle = MathUtils.atan2(dyP, dxP) * MathUtils.radiansToDegrees;
            if (angle >= -45 && angle < 45) currentAttackAnim = attackRight;
            else if (angle >= 45 && angle < 135) currentAttackAnim = attackUp;
            else if (angle >= -135 && angle < -45) currentAttackAnim = attackDown;
            else currentAttackAnim = attackLeft;
        } else if (distToPlayer < alertRadius) {
            // Chase the player.
            state = State.CHASE;
        } else {
            // Otherwise, patrol.
            state = State.PATROL;
        }

        switch (state) {
            case PATROL:
                if (usingWaypoints) {
                    // Waypoint-based patrol
                    if (isPaused) {
                        // Pause at waypoint
                        currentPauseTime += delta;
                        lookAroundTimer += delta;

                        // Look around while paused, but use idle animation
                        if (lookAroundTimer >= 0.8f) {
                            lookDirection *= -1;
                            lookAroundTimer = 0f;
                        }

                        // Use idle animation when paused instead of run animation
                        currentMovementAnim = idleAnimation;

                        // When pause is over, continue to next waypoint
                        if (currentPauseTime >= waypointPauseDuration) {
                            isPaused = false;
                        }
                    } else {
                        // Move to current waypoint
                        Vector2 waypoint = patrolWaypoints.get(currentWaypointIndex);
                        float wpDx = waypoint.x - x;
                        float wpDy = waypoint.y - y;

                        if (Math.abs(wpDx) < 0.1f && Math.abs(wpDy) < 0.1f) {
                            moveToNextWaypoint();
                        } else {
                            moveTowards(waypoint.x, waypoint.y, speed, delta);
                            currentMovementAnim = (wpDx < 0) ? runLeft : runRight;
                        }
                    }
                } else {
                    // Original random patrol behavior
                    float pdx = patrolTargetX - x;
                    float pdy = patrolTargetY - y;
                    if (Math.abs(pdx) < 0.1f && Math.abs(pdy) < 0.1f) {
                        pickRandomPatrolTarget();
                    }
                    moveTowards(patrolTargetX, patrolTargetY, speed, delta);
                    currentMovementAnim = (pdx < 0) ? runLeft : runRight;
                }
                break;
            case CHASE:
                // Only move closer if beyond minimum attack distance
                if (distToPlayer > minAttackDistance) {
                    moveTowards(player.getX(), player.getY(), speed * 1.4f, delta);
                }
                currentMovementAnim = (dxP < 0) ? runLeft : runRight;
                break;
            case IDLE:
            default:
                currentMovementAnim = idleAnimation;
                break;
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
    }

    private void moveTowards(float tx, float ty, float moveSpeed, float delta) {
        float dx = tx - x;
        float dy = ty - y;
        float d2 = dx * dx + dy * dy;
        if (d2 < 0.0001f) return;
        float dist = (float)Math.sqrt(d2);
        float nx = dx / dist;
        float ny = dy / dist;
        x += nx * moveSpeed * delta;
        y += ny * moveSpeed * delta;
    }

    public boolean isDying() {
        return isDying;
    }

    /**
     * Renders the goblin.
     *
     * @param batch The SpriteBatch used for rendering.
     */
    public void render(SpriteBatch batch) {
        if (isDead) return;

        TextureRegion frame;
        if (isDying) {
            frame = deathAnimation.getKeyFrame(deathStateTime);
        } else if (isAttacking) {
            frame = currentAttackAnim.getKeyFrame(attackStateTime);
        } else {
            frame = currentMovementAnim.getKeyFrame(movementStateTime);
        }
        float drawW = frame.getRegionWidth() * scale;
        float drawH = frame.getRegionHeight() * scale;
        batch.draw(frame, x - drawW / 2f, y - drawH / 2f, drawW, drawH);

        if (!isDying && redFlashTimer > 0) {
            batch.setColor(1, 0, 0, 0.3f);
            batch.draw(frame, x - drawW / 2f, y - drawH / 2f, drawW, drawH);
            batch.setColor(1, 1, 1, 1);
        }
    }

    /**
     * Disposes of goblin resources.
     */
    public void dispose() {
        if (deathTexture != null) {
            deathTexture.dispose();
        }
        atlas.dispose();
    }
}
