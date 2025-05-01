package io.github.ballsofsteel.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

/** Uygulamadaki tek font kaynağı. */
public final class Fonts {

    private Fonts() {}                       // static util

    public static BitmapFont HUD;            // 24–48 px arası
    public static BitmapFont TITLE;          // 48 px (menü başlığı)
    public static BitmapFont BIG;           // 140 px (3-2-1-GO)
    public static BitmapFont NPC_FONT;
    public static BitmapFont HUD_COMPACT;


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

        prm.size = 82;
        prm.borderWidth = 0.2f;
        prm.color = com.badlogic.gdx.graphics.Color.BLACK;
        prm.borderColor = com.badlogic.gdx.graphics.Color.WHITE;
        NPC_FONT = gen.generateFont(prm);

        // Geri-sayımdaki dev yazı
        prm.size = 140;
        prm.color = Color.WHITE;
        BIG = gen.generateFont(prm);

        // HUD_COMPACT (less spacing)
        prm.size = 48;
        prm.characters = FreeTypeFontGenerator.DEFAULT_CHARS;
        prm.spaceX = -2; // Reduce horizontal spacing between letters
        HUD_COMPACT = gen.generateFont(prm);

        gen.dispose();
    }

    public static void dispose() {
        HUD.dispose();
        TITLE.dispose();
        BIG.dispose();
        NPC_FONT.dispose();
        HUD_COMPACT.dispose();
    }
}
