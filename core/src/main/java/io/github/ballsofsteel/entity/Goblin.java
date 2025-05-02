package io.github.ballsofsteel.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import io.github.ballsofsteel.core.CoreGame;
import io.github.ballsofsteel.ui.HealthBarRenderer;

import java.util.ArrayList;
import java.util.List;

// goblin class which extends BaseEnemy
public class Goblin extends BaseEnemy {

    // constants
    private static final float SCALE          = 1f / 72f;
    private static final float SEPARATE_DIST2 = 0.40f;
    private static final float SEPARATE_SPEED = 1.5f;
    private static final float SPEED          = 2.2f;
    private static final float ATTACK_RADIUS  = 1.0f;
    private static final float ATTACK_HIT_T   = 0.15f;
    private static final float ATTACK_DUR     = 0.7f;
    private static final float ATTACK_DMG     = 10f;
    private static final float ATTACK_KB      = 3.5f;
    private static final float ATTACK_CD      = 0.9f;
    private static final float GOLD_SPREAD    = .8f;

    // references
    private final Player        player;
    private final List<Goblin>  crowd;
    private final List<GoldBag> loot;
    private final CoreGame      game;

    // animations
    private final Animation<TextureRegion> runR, runL, idleR, idleL;
    private final Animation<TextureRegion> atkR, atkL, atkU, atkD;
    private final Animation<TextureRegion> deathAnim;

    // state
    private enum ST { MOVE, ATTACK, DIE }
    private ST     st          = ST.MOVE;
    private float  moveT       = 0f;
    private float  atkT        = 0f;
    private float  deathT      = 0f;
    private float  attackCD    = 0f;
    private boolean attackDone = false;
    private boolean facingRight= true;

    // a* path data
    private List<Vector2> path = new ArrayList<>();
    private int   pathIdx      = 0;
    private float pathTimer    = 0f;
    private static final float REPLAN_INTERVAL = .5f;

    /* =================================================== */
    public Goblin(Player player, CoreGame game,
                  List<Goblin> crowd, float sx, float sy,
                  List<GoldBag> loot) {

        super(sx, sy, 50f, game.getMap());
        this.player = player; this.game = game;
        this.crowd = crowd;  this.loot = loot;

        Texture atlas = new Texture(Gdx.files.internal("Goblin/goblin_animations.png"));
        Texture dead  = new Texture(Gdx.files.internal("deadanimation.png"));

        runR  = BaseEnemy.row(atlas, 2,   2, 1152,190,6,.12f);
        runL  = BaseEnemy.mirror(runR);
        idleR = BaseEnemy.row(atlas, 2, 194, 1152,190,6,.25f);
        idleL = BaseEnemy.mirror(idleR);
        atkD  = BaseEnemy.row(atlas, 2, 386, 1152,190,6,.13f);
        atkR  = BaseEnemy.row(atlas, 2, 578, 1152,190,6,.13f);
        atkL  = BaseEnemy.mirror(atkR);
        atkU  = BaseEnemy.row(atlas, 2, 770, 1152,190,6,.13f);
        deathAnim = BaseEnemy.row(dead , 2,   2, 1792,128,14,.10f);
    }

    // update method
    public void update(float dt) {

        baseUpdate(dt);                       // knockback and flash

        if (st == ST.DIE) { deathT += dt; return; }

        separateFromCrowd(crowd, SEPARATE_DIST2, SEPARATE_SPEED, dt);

        float dx = player.getX()-x, dy = player.getY()-y;
        float dist = (float)Math.sqrt(dx*dx+dy*dy);

        switch (st) {
            case MOVE:
                if (dist < ATTACK_RADIUS && attackCD <= 0f) {
                    st = ST.ATTACK; atkT = 0f; attackDone = false;
                } else {
                    steerTowardsPlayer(dt, dx, dy, dist);
                }
                break;

            case ATTACK:
                atkT += dt;
                if (!attackDone && atkT >= ATTACK_HIT_T) {
                    if (dist <= ATTACK_RADIUS) {
                        float ang = MathUtils.atan2(dy, dx)*MathUtils.radiansToDegrees;
                        player.takeDamage(ATTACK_DMG, ATTACK_KB, ang);
                    }
                    attackDone = true;
                }
                if (atkT >= ATTACK_DUR) { st = ST.MOVE; attackCD = ATTACK_CD; }
                break;
        }
        if (attackCD>0f) attackCD-=dt;

        x = MathUtils.clamp(x,.5f, game.getMap().getMapWidth() -.5f);
        y = MathUtils.clamp(y,.5f, game.getMap().getMapHeight()-.5f);
    }

    // chase & pathfinding
    private void steerTowardsPlayer(float dt,
                                    float dx,float dy,float dist){

        // path refreshing
        pathTimer -= dt;
        if (pathTimer <= 0f || pathIdx >= path.size()) {
            path = findPath(game.getMap(),
                (int)Math.floor(x),  (int)Math.floor(y),
                (int)Math.floor(player.getX()),
                (int)Math.floor(player.getY()));
            pathIdx   = 0;
            pathTimer = REPLAN_INTERVAL;
        }

        // move along with A*
        if (pathIdx < path.size()) {
            Vector2 goal = path.get(pathIdx);
            float gx = goal.x - x, gy = goal.y - y;
            float gdist = (float)Math.sqrt(gx*gx+gy*gy);

            if (gdist < 0.1f) { pathIdx++; }
            else {
                float mv = SPEED * dt;
                float nx = x + (gx/gdist)*mv;
                float ny = y + (gy/gdist)*mv;
                if (canMoveTo(game.getMap(), nx, ny)) { x=nx; y=ny; }
                facingRight = gx > 0;   moveT += dt;
            }
        } else if (dist > ATTACK_RADIUS*0.8f) {      // run straight to player
            float mv = SPEED*dt;
            float nx = x + (dx/dist)*mv;
            float ny = y + (dy/dist)*mv;
            if (canMoveTo(game.getMap(), nx, ny)) { x=nx; y=ny; facingRight = dx>0; moveT+=dt; }
        }
    }

    // render method
    public void render(SpriteBatch b){
        TextureRegion fr;
        if (st==ST.DIE) fr = deathAnim.getKeyFrame(deathT);
        else if (st==ST.ATTACK){
            float ang = MathUtils.atan2(player.getY()-y, player.getX()-x)*MathUtils.radiansToDegrees;
            if      (ang>=-45 && ang<45)   fr = facingRight? atkR.getKeyFrame(atkT): atkL.getKeyFrame(atkT);
            else if (ang>=45 && ang<135)   fr = atkU.getKeyFrame(atkT);
            else if (ang>=-135&&ang<-45)   fr = atkD.getKeyFrame(atkT);
            else                           fr = facingRight? atkL.getKeyFrame(atkT): atkR.getKeyFrame(atkT);
        } else fr = facingRight? runR.getKeyFrame(moveT,true)
            : runL.getKeyFrame(moveT,true);

        float w = fr.getRegionWidth()*SCALE, h = fr.getRegionHeight()*SCALE;
        b.draw(fr, x-w/2f, y-h/2f, w, h);

        if (flashTimer>0f && st!=ST.DIE){
            b.setColor(1,0,0,0.35f); b.draw(fr,x-w/2f,y-h/2f,w,h); b.setColor(1,1,1,1);
        }
        if (st!=ST.DIE)
            HealthBarRenderer.drawBar(b, x, y+.8f, hp/maxHp, true);
    }

    // taking damage
    @Override public void takeDamage(float dmg,float kb,float angDeg){
        if(st==ST.DIE) return;
        super.takeDamage(dmg,kb,angDeg);
        if(hp<=0f){
            st=ST.DIE; deathT=0f;
            if(MathUtils.randomBoolean(1f))
                loot.add(new GoldBag(
                    x+MathUtils.random(-GOLD_SPREAD,GOLD_SPREAD),
                    y+MathUtils.random(-GOLD_SPREAD,GOLD_SPREAD)));
        }
    }
    @Override public boolean isDead (){ return st==ST.DIE && deathAnim.isAnimationFinished(deathT); }
    @Override public boolean isDying(){ return st==ST.DIE && !isDead(); }

    public void dispose(){}
}
