package io.github.some_example_name;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

/**
 * A simple "Goblin" enemy that patrols within a region.
 */
public class Goblin {

    private enum State { IDLE, PATROL, CHASE, ATTACK }
    private State state = State.PATROL;

    private float x, y;
    private float speed = 2.0f;

    // Patrol area
    private float patrolMinX, patrolMaxX, patrolMinY, patrolMaxY;
    private float patrolTargetX, patrolTargetY;

    // Distances
    private float alertRadius  = 4.0f;
    private float attackRadius = 1.0f;

    // Animations
    private Texture atlas;

    private Animation<TextureRegion> idleAnimation;
    private Animation<TextureRegion> runRight, runLeft;
    private Animation<TextureRegion> attackRight, attackLeft;
    private Animation<TextureRegion> attackUp, attackDown;
    private Animation<TextureRegion> currentAnimation;

    private float stateTime = 0f;
    private boolean isAttacking = false;

    private float scale = 1f / 72f;

    private Player player;
    private OrthographicCamera camera;

    public Goblin(OrthographicCamera camera, Player player,
                  float startX, float startY,
                  float patrolMinX, float patrolMaxX,
                  float patrolMinY, float patrolMaxY) {

        this.camera = camera;
        this.player = player;
        this.x = startX;
        this.y = startY;

        this.patrolMinX = patrolMinX;
        this.patrolMaxX = patrolMaxX;
        this.patrolMinY = patrolMinY;
        this.patrolMaxY = patrolMaxY;

        // 1) Load the atlas
        atlas = new Texture(Gdx.files.internal("Goblin/goblin_animations.png"));

        // 2) Build each animation from its row
        //    e.g. running has 6 frames, idle has 7, etc.
        //    *IMPORTANT*: subregion widths must match the actual row size.

        // running => row(2,2, 1152×190), 6 frames
        TextureRegion runRow = new TextureRegion(atlas, 2, 2, 1152, 190);
        runRight = buildAnimation(runRow, 6, 0.1f, Animation.PlayMode.LOOP);
        runLeft  = mirrorAnimation(runRight);

        // idle => row(2,194, 1344×190), 7 frames
        TextureRegion idleRow = new TextureRegion(atlas, 2, 194, 1344, 190);
        idleAnimation = buildAnimation(idleRow, 7, 0.3f, Animation.PlayMode.LOOP);

        // attack down => (2,386, 1344×190), 6 frames
        TextureRegion attackBRow = new TextureRegion(atlas, 2, 386, 1152, 190);
        attackDown = buildAnimation(attackBRow, 6, 0.1f, Animation.PlayMode.NORMAL);

        // attack right => (2,578, 1152×190), 6 frames
        TextureRegion attackRRow = new TextureRegion(atlas, 2, 578, 1152, 190);
        attackRight = buildAnimation(attackRRow, 6, 0.1f, Animation.PlayMode.NORMAL);
        attackLeft  = mirrorAnimation(attackRight);

        // attack up => (2,770, 1344×190), 6 frames
        TextureRegion attackURow = new TextureRegion(atlas, 2, 770, 1152, 190);
        attackUp = buildAnimation(attackURow, 6, 0.1f, Animation.PlayMode.NORMAL);

        currentAnimation = idleAnimation;
        pickRandomPatrolTarget();
    }

    private Animation<TextureRegion> buildAnimation(TextureRegion rowStrip,
                                                    int numFrames,
                                                    float frameDuration,
                                                    Animation.PlayMode mode) {
        int w = rowStrip.getRegionWidth() / numFrames;
        int h = rowStrip.getRegionHeight();
        TextureRegion[][] tmp = rowStrip.split(w, h);
        TextureRegion[] frames = tmp[0];
        Animation<TextureRegion> anim = new Animation<>(frameDuration, frames);
        anim.setPlayMode(mode);
        return anim;
    }

    private Animation<TextureRegion> mirrorAnimation(Animation<TextureRegion> original) {
        TextureRegion[] origFrames = original.getKeyFrames();
        TextureRegion[] mirrored   = new TextureRegion[origFrames.length];
        for (int i = 0; i < origFrames.length; i++) {
            TextureRegion tmp = new TextureRegion(origFrames[i]);
            tmp.flip(true, false);
            mirrored[i] = tmp;
        }
        Animation<TextureRegion> anim =
            new Animation<>(original.getFrameDuration(), mirrored);
        anim.setPlayMode(original.getPlayMode());
        return anim;
    }

    private void pickRandomPatrolTarget() {
        patrolTargetX = MathUtils.random(patrolMinX, patrolMaxX);
        patrolTargetY = MathUtils.random(patrolMinY, patrolMaxY);
    }

    public void update(float delta) {
        // If mid-attack not done
        if (isAttacking && !currentAnimation.isAnimationFinished(stateTime)) {
            stateTime += delta;
            return;
        }
        // Attack just finished
        if (isAttacking && currentAnimation.isAnimationFinished(stateTime)) {
            isAttacking = false;
        }

        // distance to player
        float dxP = player.getX() - x;
        float dyP = player.getY() - y;
        float distToPlayer = (float) Math.sqrt(dxP*dxP + dyP*dyP);

        // pick state
        if (distToPlayer < attackRadius) {
            state = State.ATTACK;
        }
        else if (distToPlayer < alertRadius) {
            state = State.CHASE;
        }
        else {
            state = State.PATROL;
        }

        switch (state) {
            case PATROL:
                float pdx = patrolTargetX - x;
                float pdy = patrolTargetY - y;
                if (Math.abs(pdx) < 0.1f && Math.abs(pdy) < 0.1f) {
                    pickRandomPatrolTarget();
                }
                moveTowards(patrolTargetX, patrolTargetY, speed, delta);
                // choose left/right anim based on direction
                if (pdx < 0) currentAnimation = runLeft;
                else         currentAnimation = runRight;
                break;

            case CHASE:
                moveTowards(player.getX(), player.getY(), speed * 1.4f, delta);
                if (dxP < 0) currentAnimation = runLeft;
                else         currentAnimation = runRight;
                break;

            case ATTACK:
                isAttacking = true;
                stateTime = 0f;
                float angle = MathUtils.atan2(dyP, dxP) * MathUtils.radiansToDegrees;
                if (angle >= -45 && angle < 45) {
                    currentAnimation = attackRight;
                }
                else if (angle >= 45 && angle < 135) {
                    currentAnimation = attackUp;
                }
                else if (angle >= -135 && angle < -45) {
                    currentAnimation = attackDown;
                }
                else {
                    currentAnimation = attackLeft;
                }
                break;

            case IDLE:
            default:
                currentAnimation = idleAnimation;
                break;
        }

        stateTime += delta;
    }

    private void moveTowards(float tx, float ty, float moveSpeed, float delta) {
        float dx = tx - x;
        float dy = ty - y;
        float d2 = dx*dx + dy*dy;
        if (d2 < 0.0001f) return;

        float dist = (float) Math.sqrt(d2);
        float nx = dx/dist;
        float ny = dy/dist;

        x += nx * moveSpeed * delta;
        y += ny * moveSpeed * delta;
    }

    public void render(SpriteBatch batch) {
        TextureRegion frame = currentAnimation.getKeyFrame(stateTime);

        // no more fixed FRAME_WIDTH—just use the actual region's width
        float drawW = frame.getRegionWidth()  * scale;
        float drawH = frame.getRegionHeight() * scale;

        batch.draw(frame, x - drawW/2f, y - drawH/2f, drawW, drawH);
    }

    public void dispose() {
        atlas.dispose();
    }

    // getters
    public float getX(){ return x; }
    public float getY(){ return y; }
}
