package io.github.ballsofsteel.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import io.github.ballsofsteel.core.GridPathfinder;
import io.github.ballsofsteel.core.TileMapRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Enemy üst-sınıfı – tüm yaratıkların ortak ihtiyaçlarını tutar:
 *
 * • Pozisyon (x / y)
 * • HP, knock-back, kırmızı flaş geri bildirimi
 * • Hasar alma {@link #takeDamage}
 * • Knock-back ve flaş zamanlayıcısını yürüten {@link #baseUpdate}
 *
 * • Sprite-sheet’ten satır animasyonu oluşturan {@link #row} ve yansıtma {@link #mirror}
 * • Kalabalıktan uzaklaştırma  {@link #separateFromCrowd}
 * • Basit çarpışma kontrolü    {@link #canMoveTo}
 * • Tek seferlik A* yol üretimi {@link #findPath}
 *
 * **Kullanım (alt sınıflarda):**
 *   –  Ctor’da super(x, y, maxHp) çağır.
 *   –  update(dt) içinde ilk satırda baseUpdate(dt) çağır.
 *   –  Hasar aldığında super.takeDamage(d,k,a) kullan.
 *   –  İhtiyaca göre yardımcı metodları çağır.
 */
public abstract class BaseEnemy {

    /* ------------------------------------------------------------------ */
    /*  TEMEL ALANLAR                                                     */
    /* ------------------------------------------------------------------ */
    protected float x, y;                 // dünya koordinatı (merkez)
    protected final float maxHp;
    protected float hp;
    private final Polygon bbox = new Polygon(new float[]{
            -0.3f,-0.3f, 0.3f,-0.3f, 0.3f,0.3f, -0.3f,0.3f});
    /* Knock-back ivmesi (her karede azalacak) */
    protected final Vector2 knock = new Vector2();


    protected TileMapRenderer map;


    /* Hasar sonrası kırmızı flaş */
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

    /* ------------------------------------------------------------------ */
    /** Alt sınıfın update(dt) fonksiyonunun *başında* çağrılmalı. */
    protected void baseUpdate(float dt){

        /* güvenli knock-back */
        if(knock.len2()>.0001f){
            float nx = x + knock.x*dt;
            float ny = y + knock.y*dt;
            bbox.setPosition(nx,ny);
            if(!map.isCellBlocked((int)nx,(int)ny,bbox)){   // geçilebiliyorsa uygula
                x = nx; y = ny;
            } else knock.setZero();                         // aksi hâlde dur
            knock.scl(1 - dt*4f);
        }
        if(flashTimer>0) flashTimer-=dt;
    }

    /* ------------------------------------------------------------------ */
    /** Ortak hasar alma – alt sınıflar super() çağırıp ek işlem yapabilir. */
    public void takeDamage(float dmg, float knockForce, float angleDeg) {
        hp -= dmg;
        flashTimer = FLASH_TIME;

        float rad = angleDeg * MathUtils.degreesToRadians;
        knock.add(MathUtils.cos(rad) * knockForce,
                MathUtils.sin(rad) * knockForce);
    }

    /* ------------------------------------------------------------------ */
    /*  ANİMASYON YARDIMCILARI                                            */
    /* ------------------------------------------------------------------ */

    /**
     * Sprite-sheet’in tek satırından Animation üretir.
     *
     * @param tex   Sprite sheet Texture
     * @param x,y   Satırın sol-üst px koordinatı
     * @param w,h   Satırın genişlik / yükseklik px
     * @param n     Kare sayısı
     * @param dur   Bir kare süresi (saniye)
     */
    public static Animation<TextureRegion> row(
            Texture tex, int x, int y,
            int w, int h, int n, float dur) {

        TextureRegion src = new TextureRegion(tex, x, y, w, h);
        TextureRegion[] frames = src.split(w / n, h)[0];
        return new Animation<>(dur, frames);
    }

    /** Verilen animasyonun soldan sağa yansıtılmış kopyası. */
    public static Animation<TextureRegion> mirror(Animation<TextureRegion> src) {
        TextureRegion[] orig = src.getKeyFrames();
        TextureRegion[] mirr = new TextureRegion[orig.length];
        for (int i = 0; i < orig.length; i++) {
            mirr[i] = new TextureRegion(orig[i]);
            mirr[i].flip(true, false);
        }
        return new Animation<>(src.getFrameDuration(), mirr);
    }

    /* ------------------------------------------------------------------ */
    /*  KALABALIKTAN UZAKLAŞTIRMA                                         */
    /* ------------------------------------------------------------------ */

    /**
     * Aynı türden yaratık listesini iterek çakışmayı engeller.
     *
     * @param crowd    Bu düşman dâhil *tüm* düşmanlar
     * @param minDist2 İstenilen minimum mesafe² (d²)
     * @param speed    Ayırma hızı (dünya-birimi/sn)
     * @param dt       Delta-time
     */
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
                x += dx / d * dt * speed;
                y += dy / d * dt * speed;
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /*  BASİT ÇARPIŞMA KONTROLÜ                                           */
    /* ------------------------------------------------------------------ */

    /**
     * Verilen hedef koordinata gidilebilir mi?
     *  – 0.3 × 0.3f’lik kare polygon kullanır
     *  – Yalnızca TileMapRenderer.isCellBlocked(…) çağırır
     */
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

    /* ------------------------------------------------------------------ */
    /*  BASİT YOL BULMA (A*)                                              */
    /* ------------------------------------------------------------------ */

    /**
     * GridPathfinder üzerinden yeni yol döner.
     * Başlangıç hücresi merkez noktası listede kalırsa ilk eleman silinir,
     * böylece yaratık geriye adım atmaz.
     *
     * @return Başından sonuna düğüm listesi (hücre merkezi koordinatları)
     */
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

    /* ------------------------------------------------------------------ */
    /*  BASİT GETTER’LAR                                                  */
    /* ------------------------------------------------------------------ */
    public float  getX()         { return x; }
    public float  getY()         { return y; }
    public float  getHealth()    { return hp; }
    public float  getMaxHealth() { return maxHp; }

    /** Death animasyonu bittiğinde true dönmeli (alt sınıf override). */
    public boolean isDead()  { return hp <= 0f; }

    /** Death animasyonu oynarken true (varsayılan: hiç oynamaz). */
    public boolean isDying() { return false; }
}
