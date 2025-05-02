package io.github.ballsofsteel.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import io.github.ballsofsteel.core.GridPathfinder;
import io.github.ballsofsteel.core.TileMapRenderer;
import java.util.List;


public abstract class BaseEnemy {

    // basis fields
    protected float x, y;                 // world coordinate *(center)*
    protected final float maxHp;
    protected float hp;
    private final Polygon bbox = new Polygon(new float[]{
            -0.3f,-0.3f, 0.3f,-0.3f, 0.3f,0.3f, -0.3f,0.3f});

    // knockback acceleration
    protected final Vector2 knock = new Vector2();


    protected TileMapRenderer map;


    // red flash after taking damage
    protected static final float FLASH_TIME = .15f;
    protected float flashTimer = 0f;

    /* ------------------------------------------------------------------ */
    public BaseEnemy(float sx, float sy, float maxHp, TileMapRenderer map) {
        this.x     = sx;
        this.y     = sy;
        this.maxHp = maxHp;
        this.hp    = maxHp;
        this.map   = map;               // <— ekledik
    }


    protected void baseUpdate(float dt){

        // safe knockback
        if(knock.len2()>.0001f){
            float nx = x + knock.x*dt;
            float ny = y + knock.y*dt;
            bbox.setPosition(nx,ny);
            if(!map.isCellBlocked((int)nx,(int)ny,bbox)){   // if passable apply
                x = nx; y = ny;
            } else knock.setZero();                         // stop instead
            knock.scl(1 - dt*4f);
        }
        if(flashTimer>0) flashTimer-=dt;
    }


    public void takeDamage(float dmg, float knockForce, float angleDeg) {
        hp -= dmg;
        flashTimer = FLASH_TIME;

        float rad = angleDeg * MathUtils.degreesToRadians;
        knock.add(MathUtils.cos(rad) * knockForce,
                MathUtils.sin(rad) * knockForce);
    }

    // animation helpers
    public static Animation<TextureRegion> row(
            Texture tex, int x, int y,
            int w, int h, int n, float dur) {

        TextureRegion src = new TextureRegion(tex, x, y, w, h);
        TextureRegion[] frames = src.split(w / n, h)[0];
        return new Animation<>(dur, frames);
    }

    public static Animation<TextureRegion> mirror(Animation<TextureRegion> src) {
        TextureRegion[] orig = src.getKeyFrames();
        TextureRegion[] mirr = new TextureRegion[orig.length];
        for (int i = 0; i < orig.length; i++) {
            mirr[i] = new TextureRegion(orig[i]);
            mirr[i].flip(true, false);
        }
        return new Animation<>(src.getFrameDuration(), mirr);
    }

    // seperating from crowd
    protected void separateFromCrowd(List<? extends BaseEnemy> crowd,
                                     float minDist2,
                                     float speed,
                                     float dt) {
        for (BaseEnemy other : crowd) {
            if (other == this) continue;
            float dx = x - other.x;
            float dy = y - other.y;
            float d2 = dx * dx + dy * dy;
            if (d2 < minDist2 && d2 > 0f) {
                float d = (float) Math.sqrt(d2);
                float pushX = x + dx / d * dt * speed;
                float pushY = y + dy / d * dt * speed;
                // ◄◄ İleri gitmeden önce kontrol et
                if (canMoveTo(map, pushX, pushY)) {
                    x = pushX;
                    y = pushY;
                }
            }
        }
    }

   // basic collision control
    protected boolean canMoveTo(TileMapRenderer map,
                                float targetX, float targetY) {

        Polygon poly = new Polygon(new float[]{
                -0.3f, -0.3f,
                0.3f, -0.3f,
                0.3f,  0.3f,
                -0.3f,  0.3f});
        poly.setPosition(targetX, targetY);

        return !map.isCellBlocked((int) targetX,
                (int) targetY,
                poly);
    }


   // basic pathfinding a* algorithm
    protected List<Vector2> findPath(TileMapRenderer map,
                                     int sx, int sy,   // start tile
                                     int tx, int ty) { // target tile

        GridPathfinder pf = new GridPathfinder(map);
        List<Vector2> raw = pf.findPath(sx, sy, tx, ty);

        if (!raw.isEmpty()) {
            Vector2 first = raw.get(0);
            if (Math.abs(first.x - (sx + .5f)) < .01f &&
                    Math.abs(first.y - (sy + .5f)) < .01f) {
                raw.remove(0);
            }
        }
        return raw;
    }

    // getters

    public float  getX()         { return x; }
    public float  getY()         { return y; }
    public float  getHealth()    { return hp; }
    public float  getMaxHealth() { return maxHp; }
    public boolean isDead()  { return hp <= 0f; }
    public boolean isDying() { return false; }

}
