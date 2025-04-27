/*  tam dosya – Dinamitçi (kaç / fırlat / patla, red-flash, knock-back, loot) */
package io.github.ballsofsteel.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import io.github.ballsofsteel.ui.HealthBarRenderer;

import java.util.*;

public class DynamiteGoblin {

    /* ───────── sprite & animasyon ───────── */
    private static final Texture SHEET  = new Texture(Gdx.files.internal("Goblin/dynamite_goblin.png"));
    private static final Texture BOOMS  = new Texture(Gdx.files.internal("Dynamite/explosions_dynamite.png"));
    private static final Texture DEADTX = new Texture(Gdx.files.internal("deadanimation.png"));
    private static final Animation<TextureRegion>
        runR, runL, throwR, throwL,
        dynamiteA, explosionA,
        deathA;

    static {
        SHEET .setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        BOOMS .setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        DEADTX.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        runR   = row(SHEET , 2,  2,1152,192,6,.10f);  runL   = mirror(runR);
        throwR = row(SHEET , 2,196,1344,192,7,.08f);  throwL = mirror(throwR);

        dynamiteA  = row(BOOMS , 2,  2, 384, 64,6,.06f);
        explosionA = row(BOOMS , 2, 68,1728,192,9,.06f);
        deathA     = row(DEADTX, 2,  2,1792,128,14,.07f);
    }
    private static Animation<TextureRegion> row(Texture t,int x,int y,int w,int h,int n,float d){
        return new Animation<>(d,new TextureRegion(t,x,y,w,h).split(w/n,h)[0]);
    }
    private static Animation<TextureRegion> mirror(Animation<TextureRegion> src){
        TextureRegion[] k=src.getKeyFrames(), m=new TextureRegion[k.length];
        for(int i=0;i<k.length;i++){ m[i]=new TextureRegion(k[i]); m[i].flip(true,false); }
        return new Animation<>(src.getFrameDuration(),m);
    }

    /* ───────── sabitler ───────── */
    private static final float SCALE = 1f/72f;          // tüm karelerde ortak ölçek
    private static final float KEEP_DIST  = 2.8f;       // fazla yakınsa kaçar
    private static final float THROW_DIST = 4.2f;       // bu mesafede durur/fırlatır
    private static final float MAX_HP     = 60f;
    private static final float FLASH_TIME = .15f;       // kırmızı parıltı süresi
    private static final float THROW_COOLDOWN = 1.2f;   // eski değeri ~0.8


    /* ───────── referanslar ───────── */
    private final Player player;
    private final List<DynamiteGoblin> crowd;
    private final List<GoldBag> loot;

    /* ───────── durum ───────── */
    private float x,y, hp=MAX_HP;
    private float flashTimer=0f;                       // red-flash sayacı
    private final Vector2 knock = new Vector2();       // geri tepme
    private float throwCD = 0f;                         // alan ekle


    private enum ST{MOVE,THROW,DIE} private ST st=ST.MOVE;
    private float tThrow,tState; private boolean released;

    private final ArrayList<Dynamite> bombs = new ArrayList<>();

    public DynamiteGoblin(Player p,List<DynamiteGoblin> same,List<GoldBag> loot,
                          float sx,float sy){
        player=p; crowd=same; this.loot=loot; x=sx; y=sy;
    }

    /* ───────── ana update ───────── */
    public void update(float dt){
        bombs.removeIf(b->b.update(dt));
        if (throwCD>0) throwCD-=dt;
        if(st==ST.DIE){ tState+=dt; return; }

        /* basit separation */
        for(DynamiteGoblin g:crowd){
            if(g==this || g.st==ST.DIE) continue;
            float dx=x-g.x, dy=y-g.y, d2=dx*dx+dy*dy;
            if(d2<.4f){
                float d=(float)Math.sqrt(d2);
                x+=dx/d*dt*1.5f; y+=dy/d*dt*1.5f;
            }
        }

        float dx = player.getX()-x, dy = player.getY()-y;
        float dist = (float)Math.sqrt(dx*dx+dy*dy);

        if(st==ST.THROW){
            tThrow+=dt;
            if(!released && tThrow>=.32f){
                bombs.add(new Dynamite(x,y, dx/dist*3.5f, dy/dist*3.5f));
                released=true;
            }
            if(tThrow>=throwR.getAnimationDuration()) st=ST.MOVE;
        }else{
            // MOVE
            if(dist<KEEP_DIST){             // kaç – uzaklaş
                x-=dx/dist*dt*2f; y-=dy/dist*dt*2f;
            }else if(dist>THROW_DIST){      // yaklaş
                x+=dx/dist*dt*1.7f; y+=dy/dist*dt*1.7f;
            }else{                          // atış mesafesi
                if(throwCD <= 0f)  {
                    st = ST.THROW;
                 tThrow=0; released=false;
                throwCD = THROW_COOLDOWN;}
            }
        }

        /* knock-back sönümü */
        if(knock.len2()>.0001f){
            x+=knock.x*dt; y+=knock.y*dt;
            knock.scl(1-dt*4f);
        }

        if(flashTimer>0) flashTimer-=dt;
        tState+=dt;
    }

    /* ───────── render ───────── */
    public void render(SpriteBatch b){
        bombs.forEach(d->d.render(b));

        // animasyon karesi + yön belirleme
        boolean playerLeft = player.getX() < x;
        boolean fleeing    = (st==ST.MOVE &&
            (player.getX()-x)*(player.getX()-x)+
                (player.getY()-y)*(player.getY()-y) < KEEP_DIST*KEEP_DIST);

        TextureRegion fr =
            st==ST.DIE   ? deathA.getKeyFrame(tState,false)
                : st==ST.THROW ? (player.getX()<x ? throwL : throwR).getKeyFrame(tThrow,false)
                : /* MOVE */    (fleeing
                ? (player.getX()<x ? runR : runL).getKeyFrame(tState,true)
                : (player.getX()<x ? runL : runR).getKeyFrame(tState,true));

        float w = fr.getRegionWidth()  * SCALE;
        float h = fr.getRegionHeight() * SCALE;
        b.draw(fr, x-w/2f, y-h/2f, w, h);

        // red-flash SADECE hayattayken
        if(flashTimer>0 && st!=ST.DIE){
            b.setColor(1,0,0,0.3f);
            b.draw(fr, x-w/2f, y-h/2f, w, h);
            b.setColor(1,1,1,1);
        }

        if(st!=ST.DIE)
            HealthBarRenderer.drawBar(b, x, y+1f, hp/MAX_HP, true);
    }

    /* ───────── damage ───────── */
    public void takeDamage(float dmg,float kb,float angDeg){
        if(st==ST.DIE) return;
        hp-=dmg; flashTimer=FLASH_TIME;

        float rad = angDeg*MathUtils.degreesToRadians;
        knock.add(MathUtils.cos(rad)*kb, MathUtils.sin(rad)*kb);

        if(hp<=0){
            st=ST.DIE; tState=0;
            if(MathUtils.randomBoolean(1f))
                loot.add(new GoldBag(x + MathUtils.random(-.4f,.4f),
                    y + MathUtils.random(-.4f,.4f)));
        }
    }
    public boolean isDead(){ return st==ST.DIE && deathA.isAnimationFinished(tState); }

    /* ───────── iç sınıf: uçan dinamit ───────── */
    private class Dynamite{
        final Vector2 pos=new Vector2(), vel=new Vector2();
        float fuse=1.1f, boomT; boolean exploding;
        Dynamite(float sx,float sy,float vx,float vy){ pos.set(sx,sy); vel.set(vx,vy); }
        boolean update(float dt){
            if(exploding){ boomT+=dt; return explosionA.isAnimationFinished(boomT); }

            fuse-=dt; pos.mulAdd(vel,dt);
            if(fuse<=0){
                exploding=true; boomT=0;
                float dx=player.getX()-pos.x, dy=player.getY()-pos.y;
                if(dx*dx+dy*dy<1.5f*1.5f){
                    float ang=MathUtils.atan2(dy,dx)*MathUtils.radiansToDegrees;
                    player.takeDamage(25f,4f,ang);
                }
            }
            return false;
        }
        void render(SpriteBatch b){
            TextureRegion fr = exploding ? explosionA.getKeyFrame(boomT)
                : dynamiteA .getKeyFrame(fuse,true);
            float s=1f/64f, w=fr.getRegionWidth()*s, h=fr.getRegionHeight()*s;
            b.draw(fr, pos.x-w/2f, pos.y-h/2f, w, h);
        }
    }
    public void dispose() {
    }
    /* ───────── getter’lar ───────── */
    public float getX(){ return x; }
    public float getY(){ return y; }
}
