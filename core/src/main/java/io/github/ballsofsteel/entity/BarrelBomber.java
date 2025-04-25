package io.github.ballsofsteel.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;


public class BarrelBomber {

    /* ───── static assets ───── */
    private static Texture barrelSheet, boomSheet;
    private static Animation<TextureRegion> runR, runL;
    private static Animation<TextureRegion> fuseAnim;     // 3-frame “tnt_explosion”
    private static Animation<TextureRegion> explodeAnim;  // 9-frame big boom

    private static void load() {
        if (barrelSheet != null) return;

        barrelSheet = new Texture(Gdx.files.internal("Goblin/barrel_atlas.png"));
        boomSheet   = new Texture(Gdx.files.internal("Dynamite/explosions_dynamite.png"));
        barrelSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        boomSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        runR      = row(barrelSheet, 2,   2, 768,128, 6, 0.35f);
        runL      = mirror(runR);
        fuseAnim  = row(barrelSheet, 2, 262, 384,128, 3, 0.3f);
        explodeAnim = row(boomSheet, 2, 68, 1728,192, 9, 0.06f);
    }
    private static Animation<TextureRegion> row(Texture tex,int x,int y,int w,int h,
                                                int n,float dur){
        TextureRegion[][] sp = new TextureRegion(tex,x,y,w,h).split(w/n,h);
        return new Animation<>(dur, sp[0]);
    }
    private static Animation<TextureRegion> mirror(Animation<TextureRegion> src){
        TextureRegion[] k = src.getKeyFrames();
        TextureRegion[] m = new TextureRegion[k.length];
        for(int i=0;i<k.length;i++){ m[i]=new TextureRegion(k[i]); m[i].flip(true,false); }
        return new Animation<>(src.getFrameDuration(), m);
    }

    /* ───── runtime fields ───── */
    private enum State { RUNNING, FUSE, EXPLODING }
    private State state = State.RUNNING;

    private final Player player;
    private float x, baseY;
    private float hopPhase;
    private boolean facingLeft;

    private float fuseT, boomT;

    private static final float SPEED = 2f;
    private static final float ATTACK_RANGE = 1.3f;
    private static final float DAMAGE_R     = 1.5f;
    private static final float DAMAGE_AMT   = 35f;
    private static final float SCALE_BODY   = 1f/72f;
    private static final float SCALE_BOOM   = 1.5f * SCALE_BODY;

    public BarrelBomber(Player p,float sx,float sy){
        load();
        player = p; x=sx; baseY=sy;
    }

    /* ───── UPDATE ───── */
    public void update(float dt){
        if(state==State.RUNNING)   updateRunning(dt);
        else if(state==State.FUSE) updateFuse(dt);
        else if(state==State.EXPLODING) boomT+=dt;
    }

    private void updateRunning(float dt){
        float dx = player.getX()-x, dy = player.getY()-baseY;
        float dist = (float)Math.sqrt(dx*dx+dy*dy);

        facingLeft = dx<0;
        if(dist>ATTACK_RANGE){
            float nx=dx/dist, ny=dy/dist;
            x     += nx*SPEED*dt;
            baseY += ny*SPEED*dt;
        } else {
            state = State.FUSE; fuseT = 0;
        }
        hopPhase += dt*2f;
    }
    private void updateFuse(float dt){
        fuseT += dt;
        if(fuseAnim.isAnimationFinished(fuseT)){
            startExplosion();
        }
    }
    private void startExplosion(){
        state = State.EXPLODING; boomT = 0;
        float dx = player.getX()-x, dy = player.getY()-baseY;
        if(dx*dx+dy*dy < DAMAGE_R*DAMAGE_R){
            float ang = MathUtils.atan2(dy,dx)*MathUtils.radiansToDegrees;
            player.takeDamage(DAMAGE_AMT,4f,ang);
        }
    }

    /* ───── RENDER ───── */
    public void render(SpriteBatch b){
        if(state==State.RUNNING){
            TextureRegion f=(facingLeft?runL:runR).getKeyFrame(hopPhase,true);
            float hop=Math.abs(MathUtils.sin(hopPhase))*0.35f;
            draw(b,f,SCALE_BODY,hop);
        }else if(state==State.FUSE){
            draw(b, fuseAnim.getKeyFrame(fuseT), SCALE_BODY,0);
        }else{ // EXPLODING
            draw(b, explodeAnim.getKeyFrame(boomT), SCALE_BOOM,0);
        }
    }

    private void draw(SpriteBatch b,TextureRegion tex,float scl,float yOff){
        float w=tex.getRegionWidth()*scl, h=tex.getRegionHeight()*scl;
        b.draw(tex, x-w/2f, baseY+yOff-h/2f, w,h);
    }

    /* — remove flag — */
    public boolean isFinished(){
        return state==State.EXPLODING && explodeAnim.isAnimationFinished(boomT);
    }
}
