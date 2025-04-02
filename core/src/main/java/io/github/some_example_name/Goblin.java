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
 * Goblin enemy class with AI behavior refactored using the Strategy Pattern.
 * It supports patrol, chase, and attack behaviors.
 */
public class Goblin {
    // Position and movement
    private float x, y;
    private float speed = 2.0f;

    // Patrol boundaries and target
    private float patrolMinX, patrolMaxX, patrolMinY, patrolMaxY;
    private float patrolTargetX, patrolTargetY;

    // Distance thresholds.
    private float alertRadius = 4.0f;
    private float attackRadius = 1.0f;

    // Animations.
    private Texture atlas;
    private Animation<TextureRegion> idleAnimation, runRight, runLeft, attackRight, attackLeft, attackUp, attackDown;
    private Animation<TextureRegion> currentMovementAnim, currentAttackAnim;

    private float movementStateTime = 0f;
    private float attackStateTime = 0f;
    private boolean isAttacking = false;

    private float scale = 1f / 72f;

    // Reference to the player
    private Player player;

    // Health and knockback.
    private float maxHealth = 50f;
    private float health = maxHealth;

    private Vector2 knockback = new Vector2(0, 0);
    private float knockbackDecay = 5f;

    private boolean attackExecuted = false;
    private float attackHitTime = 0.15f;
    private float attackDuration = 0.7f;
    private float attackDamage = 10f;
    private float attackKnockbackForce = 3f;

    // Attack cooldown.
    private float attackCooldownTimer = 0f;
    private float attackCooldownDuration = 0.8f;
    private float minAttackDistance = 0.8f;

    // Patrol waypoints.
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

    // Death disappearance delay.
    private float deathWaitTime = 0f;
    private float deathDisappearDelay = 5f;

    // Strategy Pattern for AI behavior.
    private GoblinState currentState;

    /**
     * Constructs a Goblin enemy.
     *
     * @param player       Reference to the player.
     * @param startX       Initial X position.
     * @param startY       Initial Y position.
     * @param patrolMinX   Minimum X boundary for patrol.
     * @param patrolMaxX   Maximum X boundary for patrol.
     * @param patrolMinY   Minimum Y boundary for patrol.
     * @param patrolMaxY   Maximum Y boundary for patrol.
     */
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

        // Build running animations.
        TextureRegion runRow = new TextureRegion(atlas, 2, 2, 1152, 190);
        runRight = buildAnimation(runRow, 6, 0.1f, Animation.PlayMode.LOOP);
        runLeft = mirrorAnimation(runRight);

        // Build idle animation.
        TextureRegion idleRow = new TextureRegion(atlas, 2, 194, 1344, 190);
        idleAnimation = buildAnimation(idleRow, 7, 0.1f, Animation.PlayMode.LOOP);
        currentMovementAnim = idleAnimation;

        // Build attack animations.
        TextureRegion attackBRow = new TextureRegion(atlas, 2, 386, 1152, 190);
        attackDown = buildAnimation(attackBRow, 6, 0.13f, Animation.PlayMode.NORMAL);
        TextureRegion attackRRow = new TextureRegion(atlas, 2, 578, 1152, 190);
        attackRight = buildAnimation(attackRRow, 6, 0.13f, Animation.PlayMode.NORMAL);
        attackLeft = mirrorAnimation(attackRight);
        TextureRegion attackURow = new TextureRegion(atlas, 2, 770, 1152, 190);
        attackUp = buildAnimation(attackURow, 6, 0.13f, Animation.PlayMode.NORMAL);

        // Build death animation.
        deathTexture = new Texture(Gdx.files.internal("deadanimation.png"));
        TextureRegion dead1 = new TextureRegion(deathTexture, 2, 2, 1792, 128);
        deathAnimation = buildAnimation(dead1, 14, 0.1f, Animation.PlayMode.NORMAL);

        pickRandomPatrolTarget();

        // Initialize AI state to Patrol.
        currentState = new PatrolState();
    }

    // --------------------- Animation Helpers ---------------------
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

    // --------------------- Patrol Helpers ---------------------
    private void pickRandomPatrolTarget() {
        patrolTargetX = MathUtils.random(patrolMinX, patrolMaxX);
        patrolTargetY = MathUtils.random(patrolMinY, patrolMaxY);
    }

    private void moveToNextWaypoint() {
        currentWaypointIndex = (currentWaypointIndex + 1) % patrolWaypoints.size();
        isPaused = true;
        currentPauseTime = 0f;
    }

    private void moveTowards(float tx, float ty, float moveSpeed, float delta) {
        float dx = tx - x;
        float dy = ty - y;
        float d2 = dx * dx + dy * dy;
        if (d2 < 0.0001f) return;
        float dist = (float) Math.sqrt(d2);
        float nx = dx / dist;
        float ny = dy / dist;
        x += nx * moveSpeed * delta;
        y += ny * moveSpeed * delta;
    }

    // --------------------- Public Methods ---------------------

    /**
     * Sets patrol waypoints for the goblin.
     *
     * @param waypoints A list of Vector2 positions for patrolling.
     */
    public void setPatrolWaypoints(List<Vector2> waypoints) {
        if (waypoints != null && !waypoints.isEmpty()) {
            this.patrolWaypoints = new ArrayList<>(waypoints);
            this.currentWaypointIndex = 0;
            this.usingWaypoints = true;
        }
    }

    /**
     * Applies damage, knockback, and triggers the red flash effect.
     *
     * @param damage         Amount of damage.
     * @param knockbackForce Knockback force.
     * @param angleDegrees   Direction of knockback in degrees.
     */
    public void takeDamage(float damage, float knockbackForce, float angleDegrees) {
        health -= damage;
        if (health < 0) health = 0;
        float angleRad = MathUtils.degreesToRadians * angleDegrees;
        knockback.set(knockbackForce * MathUtils.cos(angleRad) * 1.5f,
            knockbackForce * MathUtils.sin(angleRad) * 1.5f);
        redFlashTimer = redFlashDuration;
    }

    // --------------------- Update & Render ---------------------

    /**
     * Updates the goblin behavior using the current AI strategy.
     *
     * Note: If the goblin is already attacking, we do not change its state until the attack animation completes.
     *
     * @param delta Time elapsed since last frame.
     */
    public void update(float delta) {
        if (isDead) return;

        if (isDying) {
            deathStateTime += delta;
            if (deathAnimation.isAnimationFinished(deathStateTime)) {
                deathWaitTime += delta;
                if (deathWaitTime >= deathDisappearDelay) {
                    isDead = true;
                }
            }
            return;
        }

        if (attackCooldownTimer > 0) {
            attackCooldownTimer -= delta;
        }

        if (health <= 0 && !isDying) {
            isDying = true;
            deathStateTime = 0f;
            return;
        }

        float dxP = player.getX() - x;
        float dyP = player.getY() - y;
        float distToPlayer = (float) Math.sqrt(dxP * dxP + dyP * dyP);

        // Only change state if not already in an attack animation.
        if (!isAttacking) {
            if (player.isInBush()) {
                currentState = new PatrolState();
            } else if (distToPlayer < attackRadius && attackCooldownTimer <= 0) {
                currentState = new AttackState();
            } else if (distToPlayer < alertRadius) {
                currentState = new ChaseState();
            } else {
                currentState = new PatrolState();
            }
        }

        // Delegate behavior update to the current state.
        currentState.update(this, delta);

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

    /**
     * Renders the goblin.
     *
     * @param batch The SpriteBatch used for drawing.
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

    // --------------------- Getters ---------------------
    public float getX() { return x; }
    public float getY() { return y; }
    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }
    public boolean isDead() { return isDead; }
    public boolean isDying() { return isDying; }

    // --------------------- Strategy Pattern States ---------------------

    /**
     * GoblinState interface for AI behavior.
     */
    private interface GoblinState {
        void update(Goblin goblin, float delta);
    }

    /**
     * PatrolState: The goblin patrols using either waypoints or random targets.
     */
    private class PatrolState implements GoblinState {
        @Override
        public void update(Goblin goblin, float delta) {
            if (usingWaypoints) {
                if (isPaused) {
                    currentPauseTime += delta;
                    lookAroundTimer += delta;
                    if (lookAroundTimer >= 0.8f) {
                        lookDirection *= -1;
                        lookAroundTimer = 0f;
                    }
                    currentMovementAnim = idleAnimation;
                    if (currentPauseTime >= waypointPauseDuration) {
                        isPaused = false;
                    }
                } else {
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
                float pdx = patrolTargetX - x;
                float pdy = patrolTargetY - y;
                if (Math.abs(pdx) < 0.1f && Math.abs(pdy) < 0.1f) {
                    pickRandomPatrolTarget();
                }
                moveTowards(patrolTargetX, patrolTargetY, speed, delta);
                currentMovementAnim = (pdx < 0) ? runLeft : runRight;
            }
        }
    }

    /**
     * ChaseState: The goblin chases the player when within alert range.
     */
    private class ChaseState implements GoblinState {
        @Override
        public void update(Goblin goblin, float delta) {
            float dx = player.getX() - x;
            float dy = player.getY() - y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist > minAttackDistance) {
                moveTowards(player.getX(), player.getY(), speed * 1.4f, delta);
            }
            currentMovementAnim = (dx < 0) ? runLeft : runRight;
        }
    }

    /**
     * AttackState: The goblin attacks the player.
     * Once an attack starts, the full attack animation plays out.
     * At the hit moment, damage is applied only if the player is within range.
     * After the animation finishes, the goblin transitions to chase (if within alert range) or patrol.
     */
    private class AttackState implements GoblinState {
        @Override
        public void update(Goblin goblin, float delta) {
            // Start the attack animation if not already started.
            if (!isAttacking) {
                isAttacking = true;
                attackStateTime = 0f;
                attackExecuted = false;
                float dx = player.getX() - x;
                float dy = player.getY() - y;
                float angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
                if (angle >= -45 && angle < 45) {
                    currentAttackAnim = attackRight;
                } else if (angle >= 45 && angle < 135) {
                    currentAttackAnim = attackUp;
                } else if (angle >= -135 && angle < -45) {
                    currentAttackAnim = attackDown;
                } else {
                    currentAttackAnim = attackLeft;
                }
            }
            attackStateTime += delta;
            // At the hit moment, if the player is still in range, apply damage.
            if (!attackExecuted && attackStateTime >= attackHitTime) {
                float dx = player.getX() - x;
                float dy = player.getY() - y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist <= attackRadius) {
                    float angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
                    player.takeDamage(attackDamage, attackKnockbackForce, angle);
                }
                attackExecuted = true;
            }
            // Once the full attack animation completes, reset attack state and transition.
            if (attackStateTime >= attackDuration) {
                isAttacking = false;
                attackExecuted = false;
                attackCooldownTimer = attackCooldownDuration;
                float dx = player.getX() - x;
                float dy = player.getY() - y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist < alertRadius) {
                    currentState = new ChaseState();
                } else {
                    currentState = new PatrolState();
                }
            }
        }
    }
}
