/*  src/io/github/ballsofsteel/entity/Goblin.java  */
package io.github.ballsofsteel.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import io.github.ballsofsteel.ui.HealthBarRenderer;

import java.util.List;

public class Goblin {

    /* ---------- sabitler ---------- */
    private static final float SCALE          = 1f / 72f;
    private static final float SEPARATE_DIST2 = 0.40f;       // kare mesafe^2
    private static final float SPEED          = 2.2f;
    private static final float ALERT_RADIUS   = 5.0f;
    private static final float ATTACK_RADIUS  = 1.0f;
    private static final float ATTACK_HIT_T   = 0.15f;
    private static final float ATTACK_DUR     = 0.7f;
    private static final float ATTACK_DMG     = 10f;
    private static final float ATTACK_KB      = 3.5f;
    private static final float ATTACK_CD      = 0.9f;
    private static final float MAX_HP         = 50f;
    private static final float FLASH_TIME     = 0.2f;
    private static final float GOLD_SPREAD    = .4f;

    /* ---------- referanslar ---------- */
    private final Player player;
    private final List<Goblin> crowd;
    private final List<GoldBag> loot;

    /* ---------- konum / yön / anim ---------- */
    private float x, y;
    private boolean facingRight = true;
    private final Texture atlas;
    private final Animation<TextureRegion> runR, runL, idleR, idleL;
    private final Animation<TextureRegion> atkR, atkL, atkU, atkD;
    private final Animation<TextureRegion> deathAnim;
    private float moveT = 0f, atkT = 0f, deathT = 0f;

    /* ---------- durum ---------- */
    private enum ST {MOVE, ATTACK, DIE}
    private ST st = ST.MOVE;
    private boolean attackDone = false;
    private float attackCD = 0f;
    private float hp = MAX_HP;
    private Vector2 knock = new Vector2();
    private float redFlash = 0f;

    public Goblin(Player player, List<Goblin> crowd,
                  float startX, float startY, List<GoldBag> loot) {
        this.player = player;
        this.crowd  = crowd;
        this.loot   = loot;
        this.x = startX; this.y = startY;

        atlas = new Texture(Gdx.files.internal("Goblin/goblin_animations.png"));
        TextureRegion runRow  = new TextureRegion(atlas, 2,   2, 1152, 190);
        TextureRegion idleRow = new TextureRegion(atlas, 2, 194, 1152, 190);
        TextureRegion atkDRow = new TextureRegion(atlas, 2, 386, 1152, 190);
        TextureRegion atkRRow = new TextureRegion(atlas, 2, 578, 1152, 190);
        TextureRegion atkURow = new TextureRegion(atlas, 2, 770, 1152, 190);
        TextureRegion deadRow = new TextureRegion(
            new Texture(Gdx.files.internal("deadanimation.png")),
            2, 2, 1792, 128);

        runR  = build(runRow ,6,0.12f,true);
        runL  = mirror(runR);
        idleR = build(idleRow,6,0.25f,true);
        idleL = mirror(idleR);
        atkR  = build(atkRRow,6,0.13f,false);
        atkL  = mirror(atkR);
        atkD  = build(atkDRow,6,0.13f,false);
        atkU  = build(atkURow,6,0.13f,false);
        deathAnim = build(deadRow,14,0.1f,false);
    }
    private Animation<TextureRegion> build(TextureRegion src,
                                           int frames,float dur,boolean loop){
        int w = src.getRegionWidth()/frames, h = src.getRegionHeight();
        Animation<TextureRegion> a =
            new Animation<>(dur, src.split(w,h)[0]);
        a.setPlayMode(loop? Animation.PlayMode.LOOP
            : Animation.PlayMode.NORMAL);
        return a;
    }
    private Animation<TextureRegion> mirror(Animation<TextureRegion> src){
        TextureRegion[] k = src.getKeyFrames(), m=new TextureRegion[k.length];
        for(int i=0;i<k.length;i++){ m[i]=new TextureRegion(k[i]); m[i].flip(true,false);}
        Animation<TextureRegion> a =
            new Animation<>(src.getFrameDuration(),m);
        a.setPlayMode(src.getPlayMode());
        return a;
    }

    /* ---------- update ---------- */
    public void update(float dt){
        if (st == ST.DIE){
            deathT += dt;
            return;
        }

        /* separation */
        for (Goblin g : crowd){
            if (g==this || g.st==ST.DIE) continue;
            float dx = x-g.x, dy = y-g.y, d2 = dx*dx+dy*dy;
            if (d2 < SEPARATE_DIST2){
                float d=(float)Math.sqrt(d2);
                if (d>0){ x+=dx/d*dt*1.5f; y+=dy/d*dt*1.5f; }
            }
        }

        /* knockback */
        if (knock.len2()>.0001f){
            x+=knock.x*dt; y+=knock.y*dt;
            knock.scl(1-dt*4f);
        }

        float dx = player.getX()-x, dy = player.getY()-y;
        float dist = (float)Math.sqrt(dx*dx+dy*dy);

        switch (st){

            case MOVE:
                if (dist < ATTACK_RADIUS && attackCD <= 0f) {
                    st = ST.ATTACK; atkT = 0f; attackDone = false;
                } else {
                    x += dx / dist * SPEED * dt;
                    y += dy / dist * SPEED * dt;
                    facingRight = dx > 0;
                }
                moveT += dt;
                break;

            case ATTACK:
                atkT += dt;
                if (!attackDone && atkT >= ATTACK_HIT_T){
                    if (dist <= ATTACK_RADIUS){
                        float ang = MathUtils.atan2(dy,dx)*MathUtils.radiansToDegrees;
                        player.takeDamage(ATTACK_DMG, ATTACK_KB, ang);
                    }
                    attackDone = true;
                }
                if (atkT >= ATTACK_DUR){
                    st = ST.MOVE; attackCD = ATTACK_CD; atkT=0f;
                }
                break;
        }
        if (attackCD>0f) attackCD-=dt;
        if (redFlash>0f) redFlash-=dt;
    }

    /* ---------- render ---------- */
    public void render(SpriteBatch b){
        TextureRegion fr;
        if (st == ST.DIE){
            fr = deathAnim.getKeyFrame(deathT);
        } else if (st == ST.ATTACK){
            float angle = MathUtils.atan2(player.getY()-y,
                player.getX()-x) * MathUtils.radiansToDegrees;
            if      (angle>=-45 && angle<45)   fr = facingRight? atkR.getKeyFrame(atkT): atkL.getKeyFrame(atkT);
            else if (angle>=45 && angle<135)   fr = atkU.getKeyFrame(atkT);
            else if (angle>=-135 && angle<-45) fr = atkD.getKeyFrame(atkT);
            else                               fr = facingRight? atkL.getKeyFrame(atkT): atkR.getKeyFrame(atkT);
        } else {
            fr = facingRight? runR.getKeyFrame(moveT,true)
                : runL.getKeyFrame(moveT,true);
        }

        float w = fr.getRegionWidth()*SCALE, h = fr.getRegionHeight()*SCALE;
        b.draw(fr, x-w/2f, y-h/2f, w, h);

        if (redFlash>0f && st!=ST.DIE){
            b.setColor(1,0,0,0.35f);
            b.draw(fr, x-w/2f, y-h/2f, w, h);
            b.setColor(1,1,1,1);
        }
        if (st!=ST.DIE)
            HealthBarRenderer.drawBar(b, x, y+0.8f, hp/MAX_HP, true);
    }

    /* ---------- damage / ölüm ---------- */
    public void takeDamage(float dmg,float kb,float angDeg){
        if (st==ST.DIE) return;
        hp -= dmg; redFlash = FLASH_TIME;
        float rad = angDeg*MathUtils.degreesToRadians;
        knock.add(MathUtils.cos(rad)*kb, MathUtils.sin(rad)*kb);
        if (hp<=0){
            st = ST.DIE; deathT=0f;
            if (MathUtils.randomBoolean(1f))
                loot.add(new GoldBag(
                    x+MathUtils.random(-GOLD_SPREAD,GOLD_SPREAD),
                    y+MathUtils.random(-GOLD_SPREAD,GOLD_SPREAD)));
        }
    }

    /* ---------- getter ---------- */
    public float getX(){ return x; }  public float getY(){ return y; }
    public float getHealth(){ return hp; }  public float getMaxHealth(){ return MAX_HP; }
    public boolean isDead(){ return st==ST.DIE && deathAnim.isAnimationFinished(deathT); }
    public boolean isDying(){ return st==ST.DIE && !isDead(); }

    /* ---------- dispose (boş) ---------- */
    public void dispose(){}
}
