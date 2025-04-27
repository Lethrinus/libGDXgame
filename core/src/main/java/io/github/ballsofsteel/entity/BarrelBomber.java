package io.github.ballsofsteel.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;
import java.util.List;

/** BarrelBomber v3 – çarpışma önleme + büyük patlama. */
public class BarrelBomber {

    /* ---------- statik animasyon ---------- */
    private static final Texture SHEET = new Texture(Gdx.files.internal("Goblin/barrel_atlas.png"));
    private static final Texture BOOM  = new Texture(Gdx.files.internal("Dynamite/explosions_dynamite.png"));
    private static final Animation<TextureRegion> runR, runL, explodeA;
    static{
        SHEET.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        BOOM .setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        runR    = row(SHEET,2,2, 768,128,6,.10f);   runL = mirror(runR);
        explodeA= row(BOOM ,2,68,1728,192,9,.07f);
    }
    private static Animation<TextureRegion> row(Texture t,int x,int y,int w,int h,int n,float d){
        return new Animation<>(d,new TextureRegion(t,x,y,w,h).split(w/n,h)[0]);
    }
    private static Animation<TextureRegion> mirror(Animation<TextureRegion> src){
        TextureRegion[] k = src.getKeyFrames(), m=new TextureRegion[k.length];
        for(int i=0;i<k.length;i++){ m[i]=new TextureRegion(k[i]); m[i].flip(true,false);}
        return new Animation<>(src.getFrameDuration(),m);
    }

    /* ---------- sabitler ---------- */
    private static final float SCALE=1f/72f, BODY=192*SCALE, RADIUS=.45f;

    /* ---------- ref / crowd ---------- */
    private final Player player;
    private final List<BarrelBomber> crowd;

    /* ---------- state ---------- */
    private float x,y, baseY, hop;
    private float t, boomT;
    private boolean exploding, facingLeft;

    public BarrelBomber(Player p, List<BarrelBomber> all, float sx,float sy){
        player=p; crowd=all; x=sx; baseY=y=sy;
    }

    public void update(float dt){

        if(exploding){ boomT+=dt; return; }

        /* ---------- crowd separation ---------- */
        for(BarrelBomber b:crowd){
            if(b==this || b.exploding) continue;
            float dx = x-b.x, dy = baseY-b.baseY, d2=dx*dx+dy*dy;
            if(d2 < RADIUS*RADIUS*4){
                float d=(float)Math.sqrt(d2);
                if(d>0){ x+=dx/d*dt*1.5f; baseY+=dy/d*dt*1.5f; }
            }
        }

        /* ---------- basit AI (oyuncuya koş-patla) ---------- */
        float dx = player.getX()-x, dy = player.getY()-baseY;
        float dist=(float)Math.sqrt(dx*dx+dy*dy);
        facingLeft = dx<0;

        if(dist>1.3f){
            x+=dx/dist*dt*1.7f; baseY+=dy/dist*dt*1.7f;
        }else{ exploding=true; boomT=0;
            if(dx*dx+dy*dy<1.5f*1.5f){
                float ang=MathUtils.atan2(dy,dx)*MathUtils.radiansToDegrees;
                player.takeDamage(35f,4f,ang);
            }
        }

        t+=dt; hop = Math.abs(MathUtils.sin(t*3f))*0.35f;
    }

    public void render(SpriteBatch b){
        if(exploding){
            TextureRegion f=explodeA.getKeyFrame(boomT);
            draw(b,f,1.5f);
            return;
        }
        TextureRegion f=(facingLeft?runL:runR).getKeyFrame(t,true);
        draw(b,f,1f, hop);
    }

    private void draw(SpriteBatch b,TextureRegion f,float scl){ draw(b,f,scl,0); }
    private void draw(SpriteBatch b,TextureRegion f,float scl,float yOff){
        float w=f.getRegionWidth()*scl*SCALE, h=f.getRegionHeight()*scl*SCALE;
        b.draw(f,x-w/2f,baseY+yOff-h/2f,w,h);
    }

    public boolean isFinished(){ return exploding && explodeA.isAnimationFinished(boomT); }

    public void dispose() {
    }
}
