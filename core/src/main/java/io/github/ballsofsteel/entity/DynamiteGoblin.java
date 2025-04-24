package io.github.ballsofsteel.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Enemy that runs toward the player; within attackRange it stops,
 * plays a 7-frame throw animation and launches a dynamite that
 * travels with its own 6-frame animation.  After fuse expires the
 * dynamite explodes using a 9-frame animation and deals AoE damage.
 *
 *  - idle_dynamite   : 6 frames  1344×192
 *  - running_tnt     : 6 frames  1344×192
 *  - throwing_tnt    : 7 frames  1344×192
 *
 *  - dynamite row    : 6 frames   384×64   (each 64×64)
 *  - explosion row   : 9 frames  1728×192  (each 192×192)
 */
public class DynamiteGoblin {

    // ─────────────────── static assets ──────────────────────────────
    private static Texture enemyTex, boomTex;
    private static Animation<TextureRegion> idleR, runR, throwR;
    private static Animation<TextureRegion> idleL, runL, throwL;
    private static Animation<TextureRegion> dynamiteAnim;
    private static Animation<TextureRegion> explosionAnim;

    private static void loadAssets() {
        if (enemyTex != null) return; // already loaded
        enemyTex = new Texture(Gdx.files.internal("Goblin/dynamite_goblin.png"));
        boomTex  = new Texture(Gdx.files.internal("Dynamite/explosions_dynamite.png"));
        enemyTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        boomTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        idleR   = buildRow(enemyTex,   2,390, 1152,192, 6, 0.15f, Animation.PlayMode.LOOP);
        runR    = buildRow(enemyTex,   2,  2, 1152,192, 6, 0.2f, Animation.PlayMode.LOOP);
        throwR  = buildRow(enemyTex,   2,196, 1344,192, 7, 0.08f, Animation.PlayMode.NORMAL);

        idleL  = mirrorAnimation(idleR);
        runL   = mirrorAnimation(runR);
        throwL = mirrorAnimation(throwR);

        dynamiteAnim  = buildRow(boomTex, 2,2,   384,64,  6, 0.06f, Animation.PlayMode.LOOP);
        explosionAnim = buildRow(boomTex, 2,68, 1728,192, 9, 0.06f, Animation.PlayMode.NORMAL);
    }

    private static Animation<TextureRegion> buildRow(Texture tex,
                                                     int x,int y,int w,int h,int frameCount,float frameDur,Animation.PlayMode mode){
        TextureRegion row=new TextureRegion(tex,x,y,w,h);
        TextureRegion[][] split=row.split(w/frameCount,h);
        Animation<TextureRegion> a=new Animation<>(frameDur,split[0]);
        a.setPlayMode(mode);
        return a;
    }

    /** Goblin & Player’dakiyle aynı util  */
    private static Animation<TextureRegion> mirrorAnimation(Animation<TextureRegion> original){
        TextureRegion[] orig=original.getKeyFrames();
        TextureRegion[] mirr=new TextureRegion[orig.length];
        for(int i=0;i<orig.length;i++){
            TextureRegion t=new TextureRegion(orig[i]);
            t.flip(true,false);
            mirr[i]=t;
        }
        Animation<TextureRegion> out=new Animation<>(original.getFrameDuration(),mirr);
        out.setPlayMode(original.getPlayMode());
        return out;
    }

    // ─────────────────── instance fields ────────────────────────────
    private final Player player;   // referans
    private float x,y;
    private float speed = 1.5f;
    private float hp    = 60f;

    private Animation<TextureRegion> currentAnim;
    private float animTime;

    // attack
    private float attackRange = 4.2f;
    private float cooldown = 2.7f;
    private float cdTimer  = 0f;
    private boolean throwing;
    private boolean bombReleased;
    private float throwReleaseTime = 0.40f; // 0.08f*5th frame approx
    private float keepDistance = 3.0f;
    private float maxChaseRange = 8.0f;
    private List<Dynamite> bombs = new ArrayList<>();

    // sprite scale (textures 192 px wide; world unit ≈ 1)
    private final float scale = 1f/72f;

    public DynamiteGoblin(Player player,float startX,float startY){
        loadAssets();
        this.player=player;
        this.x=startX;
        this.y=startY;
        currentAnim=idleR;
    }

    // ─────────────────── update ────────────────────────────────
    public void update(float dt){
        // 1) update bombs
        Iterator<Dynamite> it=bombs.iterator();
        while(it.hasNext()){
            Dynamite d=it.next();
            if(d.update(dt)) it.remove();
        }

        // 2) dead?
        if(hp<=0) return;

        // 3) attack logic
        float dx=player.getX()-x, dy=player.getY()-y;
        float dist = (float)Math.sqrt(dx*dx+dy*dy);

        if(throwing){
            animTime+=dt;
            if(!bombReleased && animTime>=throwReleaseTime){
                spawnBomb(dx,dy,dist);
                bombReleased=true;
            }
            if(animTime>=currentAnim.getAnimationDuration()){
                throwing=false; bombReleased=false;
                cdTimer=cooldown;
            }
            return; // no movement while throwing
        }

        if(dist < keepDistance){
            float nx = dx / dist, ny = dy / dist;
            x -= nx * speed * dt;
            y -= ny * speed * dt;
            currentAnim = dx < 0 ? runL : runR;
        } else if (dist > attackRange && dist < maxChaseRange) {
            float nx = dx / dist, ny = dy / dist;
            x += nx * speed * dt;
            y += ny * speed * dt;
            currentAnim = dx < 0 ? runL : runR;

        }
        else {
            currentAnim = dx < 0 ? idleL : idleR;
            if (cdTimer <= 0) startThrow(dx);
        }
        if (cdTimer > 0) cdTimer -= dt;
        animTime += dt;

        // move or attack
        if(dist>attackRange){
            float nx=dx/dist, ny=dy/dist;
            x+=nx*speed*dt;
            y+=ny*speed*dt;
            currentAnim = dx<0? runL : runR;
        }else{
            // in range
            if(cdTimer<=0){
                startThrow(dx);
            }else{
                currentAnim = dx<0? idleL:idleR;
            }
        }
        if(cdTimer>0) cdTimer-=dt;

        animTime+=dt;
    }

    private void startThrow(float dx){
        throwing=true;
        animTime=0;
        bombReleased=false;
        currentAnim = dx<0? throwL : throwR;
    }

    private void spawnBomb(float dx,float dy,float dist){
        float speed = 3.8f;
        float vx = (dx/dist)*speed;
        float vy = (dy/dist)*speed;
        bombs.add(new Dynamite(x, y, vx, vy));

    }

    // ─────────────────── render ────────────────────────────────
    public void render(SpriteBatch batch){
        // bombs
        for(Dynamite d:bombs) d.render(batch);

        // self
        TextureRegion frame=currentAnim.getKeyFrame(animTime, !throwing);
        float w=frame.getRegionWidth()*scale, h=frame.getRegionHeight()*scale;
        batch.draw(frame, x-w/2f, y-h/2f, w,h);
    }

    // ─────────────────── damage api (optional) ─────────────────
    public void takeDamage(float dmg){
        hp-=dmg;
        if(hp<0) hp=0;
    }
    public boolean isDead(){return hp<=0;}

    // ─────────────────── inner Dynamite projectile ─────────────
    private class Dynamite{
        Vector2 pos = new Vector2();
        Vector2 vel = new Vector2();
        float fuse = 1.1f;            // seconds until explosion
        float animT;                  // for flying animation
        float rotation;
        boolean exploding;
        float boomT;
        float radius = 1.15f;         // damage radius
        float damage = 30f;

        Dynamite(float sx,float sy,float vx,float vy){
            pos.set(sx,sy);
            vel.set(vx,vy);
        }

        /** @return true when finished */
        boolean update(float dt){
            if(exploding){
                boomT+=dt;
                return explosionAnim.isAnimationFinished(boomT);
            }
            fuse-=dt;
            pos.mulAdd(vel,dt);
            rotation += 540f * dt;
            animT+=dt;
            if(fuse<=0){
                explode();
            }
            return false;
        }

        void explode(){
            exploding=true; boomT=0;

            // damage player if inside radius
            float d2=pos.dst2(player.getX(),player.getY());
            if(d2<radius*radius){
                float angle = MathUtils.atan2(player.getY()-pos.y, player.getX()-pos.x)*MathUtils.radiansToDegrees;
                player.takeDamage(damage,4f,angle); // mevcut Player API’nı kullan
            }
            // (kamp ateşi varsa burada da hasar verebilirsin)
        }

        void render(SpriteBatch b){
            if(!exploding){
                TextureRegion f=dynamiteAnim.getKeyFrame(animT,true);
                float w = 0.5f, h = 0.5f;
                b.draw(f, pos.x-w/2f,pos.y-h/2f,w/2f, h/2f, w,h,1.7f,1.7f,rotation);
            }else{
                TextureRegion f=explosionAnim.getKeyFrame(boomT);
                float w=f.getRegionWidth()/64f, h=f.getRegionHeight()/64f;
                b.draw(f,pos.x-w/2f,pos.y-h/2f,w,h);
            }
        }
    }
}
