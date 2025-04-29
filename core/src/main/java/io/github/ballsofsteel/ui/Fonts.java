package io.github.ballsofsteel.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

/** Uygulamadaki tek font kaynağı. */
public final class Fonts {

    private Fonts() {}                       // static util

    public static BitmapFont HUD;            // 24–48 px arası
    public static BitmapFont TITLE;          // 48 px (menü başlığı)
    public static BitmapFont BIG;            // 140 px (3-2-1-GO)

    /** create() içinde bir kez çağır. */
    public static void load() {

        FreeTypeFontGenerator gen =
            new FreeTypeFontGenerator(Gdx.files.internal("fonts/Homer_Simpson_Revised.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter prm =
            new FreeTypeFontGenerator.FreeTypeFontParameter();

        // HUD (küçük)
        prm.size = 48;
        prm.minFilter = Texture.TextureFilter.Linear;
        prm.magFilter = Texture.TextureFilter.Linear;
        HUD = gen.generateFont(prm);

        // Menü başlığı
        prm.size = 48;
        TITLE = gen.generateFont(prm);

        // Geri-sayımdaki dev yazı
        prm.size = 140;
        BIG = gen.generateFont(prm);

        gen.dispose();
    }

    /** CoreGame.dispose() içinde çağır. */
    public static void dispose() {
        HUD.dispose();
        TITLE.dispose();
        BIG.dispose();
    }
}
