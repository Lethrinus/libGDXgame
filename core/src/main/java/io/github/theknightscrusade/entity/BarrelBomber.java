package io.github.theknightscrusade.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import io.github.theknightscrusade.core.CoreGame;

import java.util.ArrayList;
import java.util.List;

// barrel bomber instantly explodes
public class BarrelBomber extends BaseEnemy {

    // sprite and animation
    private static final Texture SHEET = new Texture(Gdx.files.internal("Goblin/barrel_atlas.png"));
    private static final Texture BOOM  = new Texture(Gdx.files.internal("Dynamite/explosions_dynamite.png"));
    private static final Animation<TextureRegion>
        runR, runL, prepareA, explodeA;
    static {
        SHEET.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        BOOM .setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        runR     = BaseEnemy.row(SHEET, 2,   2, 768,128,6,.10f);
        runL     = BaseEnemy.mirror(runR);
        prepareA = BaseEnemy.row(SHEET, 2, 262, 384,128,3,.20f);
        explodeA = BaseEnemy.row(BOOM , 2,  68,1728,192,9,.07f);
    }

    // constants
    private static final float SCALE          = 1f/72f;
    private static final float SPEED          = 1.7f;
    private static final float SEPARATE_DIST2 = .45f*.45f*4f;
    private static final float SEPARATE_SPEED = 1.5f;
    private static final float DAMAGE_RADIUS  = 2f;
    private static final float PREP_DIST      = 1.3f;

    // reference  & state
    private final Player player;
    private final CoreGame core;
    private final List<BarrelBomber> crowd;

    private boolean preparing=false, exploding=false, finished=false, facingLeft;
    private float tAnim=0f, preT=0f, boomT=0f;
    private boolean damageApplied=false;
    private float baseY, hop;

    // a* pathfinding
    private List<Vector2> path=new ArrayList<>();
    private int   pathIdx=0;   private float pathTimer=0;
    private static final float REPLAN=.5f;

    public BarrelBomber(Player pl, CoreGame core,
                        List<BarrelBomber> crowd,
                        float sx,float sy){
        super(sx, sy, 1f, core.getMap());

        this.player=pl; this.core=core; this.crowd=crowd;
        baseY=sy;
    }

    // update
    public void update(float dt){

        baseUpdate(dt);          // knockback

        if(preparing){ preT+=dt; if(prepareA.isAnimationFinished(preT)){
            preparing=false; exploding=true; boomT=0;
            core.getCameraShake().shake(.4f,.2f);
        } return; }

        if(exploding){ boomT+=dt;
            if(!damageApplied && boomT>=explodeA.getAnimationDuration()/3f){
                float dx=player.getX()-x, dy=player.getY()-baseY;
                if(dx*dx+dy*dy<DAMAGE_RADIUS*DAMAGE_RADIUS){
                    float ang=MathUtils.atan2(dy,dx)*MathUtils.radiansToDegrees;
                    player.takeDamage(35f,6f,ang);
                } damageApplied=true;
            }
            if(explodeA.isAnimationFinished(boomT)) finished=true;
            return;
        }

        // seperation
        separateFromCrowd(crowd, SEPARATE_DIST2, SEPARATE_SPEED, dt);

        // a* movement
        pathTimer-=dt;
        if(pathTimer<=0||pathIdx>=path.size()){
            path=findPath(core.getMap(),
                (int)Math.floor(x),(int)Math.floor(baseY),
                (int)Math.floor(player.getX()),
                (int)Math.floor(player.getY()));
            pathIdx=0; pathTimer=REPLAN;
        }
        if(pathIdx<path.size()){
            Vector2 g=path.get(pathIdx);
            float gx=g.x-x, gy=g.y-baseY;
            float gd=(float)Math.sqrt(gx*gx+gy*gy);
            if(gd<.1f) pathIdx++;
            else{
                float mv=SPEED*dt;
                float nx=x+(gx/gd)*mv, ny=baseY+(gy/gd)*mv;
                if(canMoveTo(core.getMap(),nx,ny)){ x=nx; baseY=ny; }
            }
        }

        // if close to player, prepare to explode
        float dx=player.getX()-x, dy=player.getY()-baseY;
        facingLeft = dx<0;
        if(Math.sqrt(dx*dx+dy*dy)<PREP_DIST){ preparing=true; preT=0; }

        // hopping
        tAnim+=dt; hop=Math.abs(MathUtils.sin(tAnim*3f))*0.35f;
    }

    // render
    public void render(SpriteBatch b){
        TextureRegion fr =
            preparing ? prepareA.getKeyFrame(preT,false)
                : exploding ? explodeA.getKeyFrame(boomT,false)
                : (facingLeft?runL:runR).getKeyFrame(tAnim,true);
        float w=fr.getRegionWidth()*SCALE, h=fr.getRegionHeight()*SCALE;
        b.draw(fr,x-w/2f, baseY+hop-h/2f, w,h);

        if(flashTimer>0&&!exploding&&!preparing){
            b.setColor(1,0,0,0.3f); b.draw(fr,x-w/2f,baseY+hop-h/2f,w,h); b.setColor(1,1,1,1);
        }
    }

    // cannot be hit


    @Override public boolean isDead ()  { return finished; }
    @Override public boolean isDying()  { return false;    }
    public boolean isFinished(){ return finished; }
    public void dispose(){}
}
