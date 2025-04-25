/*  GoldCounterUI – sağ üstte net altın sayacı  */
package io.github.ballsofsteel.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public final class GoldCounterUI {

    /* ── yerleşim sabitleri (px) ───────────────────────── */
    private static final float ICON_PX = 128f;   // ekranda ~0.5 cm
    private static final float PAD_PX  = 20f;   // kenar boşluğu
    private static final float GAP_PX  = 3f;    // ikon–yazı arası

    /* ── referanslar & durum ───────────────────────────── */
    private final OrthographicCamera cam;          // HUD kamerası
    private final SpriteBatch        batch;
    private final Texture            icon;
    private final BitmapFont         font;
    private final GlyphLayout        layout = new GlyphLayout();

    private int gold = 0;

    /* ── ctor ──────────────────────────────────────────── */
    public GoldCounterUI(OrthographicCamera hudCamera) {

        cam   = hudCamera;
        batch = new SpriteBatch();
        icon  = new Texture("HUD/gold_bag.png");

        /* pikselsiz TrueType font */
        FreeTypeFontGenerator gen =
            new FreeTypeFontGenerator(Gdx.files.internal("fonts/Homer_Simpson_Revised.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter prm = new FreeTypeFontGenerator.FreeTypeFontParameter();
        prm.size      = 48;                               // 24 px
        prm.minFilter = Texture.TextureFilter.Linear;
        prm.magFilter = Texture.TextureFilter.Linear;
        font = gen.generateFont(prm);
        font.setColor(1,1,1,1);
        gen.dispose();
    }

    /* ── altın işlemleri ───────────────────────────────── */
    public void setGold(int v){ gold = v; }
    public void addGold(int v){ gold += v; }
    public int  getGold()     { return gold; }

    /* ── çiz ──────────────────────────────────────────── */
    public void draw() {

        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        /* ikon konumu */
        float xI = cam.viewportWidth  - PAD_PX - ICON_PX;
        float yI = cam.viewportHeight - PAD_PX - ICON_PX;

        /* metin ölçümü */
        String txt = String.valueOf(gold);
        layout.setText(font, txt);

        float xT = xI - GAP_PX - layout.width;
        float yT = yI + ICON_PX/2f + layout.height/4f;

        /* render */
        batch.draw(icon, xI, yI, ICON_PX, ICON_PX);
        font.draw(batch, layout, xT, yT);

        batch.end();
    }

    /* ── temizle ──────────────────────────────────────── */
    public void dispose(){
        batch.dispose();
        icon .dispose();
        font .dispose();
    }
}
