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

/** Dinamitçi goblin – kaçarken engelden “sörf” yapmaz, duvara takılmaz. */
public class DynamiteGoblin extends BaseEnemy {

    /* ── sabitler ── */
    private static final float SCALE=1f/72f, KEEP=2.8f, THROW=4.2f,
        SPEED=1.7f, MAX_HP=60f, CD=1.2f;

    /* ── sprite / anim ── */
    private static final Texture SHEET = new Texture("Goblin/dynamite_goblin.png");
    private static final Texture BOOMS = new Texture("Dynamite/explosions_dynamite.png");
    private static final Texture DEAD  = new Texture("deadanimation.png");
    private static final Animation<TextureRegion>
        runR,runL,throwR,throwL,dynaA,boomA,deathA;
    static{
        SHEET.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        runR  = BaseEnemy.row(SHEET,2,  2,1152,192,6,.10f);
        runL  = BaseEnemy.mirror(runR);
        throwR= BaseEnemy.row(SHEET,2,196,1344,192,7,.1f);
        throwL= BaseEnemy.mirror(throwR);
        dynaA = BaseEnemy.row(BOOMS,2,2,384,64,6,.06f);
        boomA = BaseEnemy.row(BOOMS,2,68,1728,192,9,.06f);
        deathA= BaseEnemy.row(DEAD ,2,2,1792,128,14,.07f);
    }

    /* ── referans & durum ── */
    private final Player player; private final CoreGame game;
    private final List<DynamiteGoblin> crowd; private final List<GoldBag> loot;
    private enum ST{MOVE,THROW,DIE} private ST st=ST.MOVE;
    private float tThrow,stateT,cd=0; private boolean released;
    private final ArrayList<Dynamite> bombs=new ArrayList<>();

    /* A* */
    private List<Vector2> path=new ArrayList<>(); private int idx=0;
    private float timer=0; private static final float REPLAN=.4f;

    public DynamiteGoblin(Player p,CoreGame g,List<DynamiteGoblin> same,
                          List<GoldBag> loot,float sx,float sy){
        super(sx,sy,MAX_HP,g.getMap());           //  <-- map’i ilet
        player=p; game=g; crowd=same; this.loot=loot;
    }

    /* ───────────────── update ───────────────── */
    public void update(float dt){

        baseUpdate(dt); bombs.removeIf(b->b.update(dt));
        if(cd>0) cd-=dt; stateT+=dt; if(st==ST.DIE) return;

        separateFromCrowd(crowd,.4f,1.5f,dt);

        float dx=player.getX()-x, dy=player.getY()-y;
        float dist=(float)Math.sqrt(dx*dx+dy*dy);

        if(st==ST.THROW){
            tThrow+=dt;
            if(!released && tThrow>=.32f){
                bombs.add(new Dynamite(x,y, dx/dist*3.5f, dy/dist*3.5f));
                released=true;
            }
            if(tThrow>=.5f){ st=ST.MOVE; }
            return;
        }

        /* ----- hareket ----- */
        if(dist<KEEP){                        // kaç
            tryMove(x - dx/dist*SPEED*dt*0.8f,
                y - dy/dist*SPEED*dt*0.8f, dt);

        } else if(dist>THROW){                // yaklaş
            movePath(player.getX(),player.getY(),dt);

        } else if(cd<=0){                     // fırlat
            st=ST.THROW; tThrow=0; released=false; cd=CD;
        }
    }

    /* güvenli adım */
    private void tryMove(float nx,float ny,float dt){
        if(!map.isCellBlocked((int)nx,(int)ny,null)){ x=nx; y=ny; }
    }

    /* A* takip */
    private void movePath(float tx,float ty,float dt){
        timer-=dt;
        if(timer<=0||idx>=path.size()){
            path=findPath(map,(int)x,(int)y,(int)tx,(int)ty);
            idx=0; timer=REPLAN;
        }
        if(idx<path.size()){
            Vector2 g=path.get(idx);
            float gx=g.x-x, gy=g.y-y, d=(float)Math.sqrt(gx*gx+gy*gy);
            if(d<.1f) idx++; else tryMove(x+gx/d*SPEED*dt, y+gy/d*SPEED*dt, dt);
        }
    }

    /* ───────────────── render / damage vs ───────────────── */
    public void render(SpriteBatch b){
        bombs.forEach(d->d.render(b));
        boolean left=player.getX()<x;
        boolean flee= (player.getX()-x)*(player.getX()-x)+
            (player.getY()-y)*(player.getY()-y) < KEEP*KEEP;
        TextureRegion fr=
            st==ST.DIE  ? deathA.getKeyFrame(stateT,false)
                : st==ST.THROW? (left?throwL:throwR).getKeyFrame(tThrow,false)
                :               (flee?(left?runR:runL):(left?runL:runR)).getKeyFrame(stateT,true);
        float w=fr.getRegionWidth()*SCALE, h=fr.getRegionHeight()*SCALE;
        b.draw(fr,x-w/2f,y-h/2f,w,h);
        if(flashTimer>0){ b.setColor(1,0,0,0.3f); b.draw(fr,x-w/2f,y-h/2f,w,h); b.setColor(1,1,1,1); }
        if(st!=ST.DIE) HealthBarRenderer.drawBar(b,x,y+1f,hp/maxHp,true);
    }

    @Override public void takeDamage(float d,float k,float a){
        if(st==ST.DIE) return; super.takeDamage(d,k,a);
        if(hp<=0){ st=ST.DIE; stateT=0;
            if(MathUtils.randomBoolean(1f))
                loot.add(new GoldBag(x+MathUtils.random(-.4f,.4f),
                    y+MathUtils.random(-.4f,.4f)));
        }
    }
    @Override public boolean isDead(){ return st==ST.DIE&&deathA.isAnimationFinished(stateT); }
    @Override public boolean isDying(){ return st==ST.DIE&&!isDead(); }

    /* ───────── iç sınıf: Dinamit ───────── */
    private class Dynamite{
        float rotation = 0f;
        Vector2 pos=new Vector2(), vel=new Vector2(); float fuse=1.1f,boomT;
        boolean exploding;
        Dynamite(float sx,float sy,float vx,float vy){ pos.set(sx,sy); vel.set(vx,vy); }
        boolean update(float dt){
            rotation += 720f * dt;
            if(exploding){ boomT+=dt; return boomA.isAnimationFinished(boomT); }
            fuse-=dt; pos.mulAdd(vel,dt);
            if(fuse<=0){ exploding=true; boomT=0;
                float dx=player.getX()-pos.x, dy=player.getY()-pos.y;
                if(dx*dx+dy*dy<2.25f){
                    float ang=MathUtils.atan2(dy,dx)*MathUtils.radiansToDegrees;
                    player.takeDamage(25f,4f,ang);
                }
            } return false;
        }
        void render(SpriteBatch b) {
            TextureRegion fr = exploding ? boomA.getKeyFrame(boomT)
                : dynaA.getKeyFrame(fuse, true);
            float s = 1f / 64f;
            float w = fr.getRegionWidth() * s;
            float h = fr.getRegionHeight() * s;

            if (exploding) {
                // Patlama sabit çizilir (dönmez)
                b.draw(fr,
                    pos.x - w / 2f, pos.y - h / 2f,
                    w, h);
            } else {
                // Sadece uçan dinamit döner
                b.draw(fr,
                    pos.x - w / 2f, pos.y - h / 2f,
                    w / 2f, h / 2f,   // origin
                    w, h,
                    1f, 1f,
                    rotation);
            }
        }
    }
    public void dispose(){}
}
