package io.github.theknightscrusade.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

// font source
public final class Fonts {

    private Fonts() {}                       // static util

    public static BitmapFont HUD;            // 24–48 px between
    public static BitmapFont TITLE;          // 48 px (menu title)
    public static BitmapFont BIG;           // 140 px (3-2-1-GO)
    public static BitmapFont NPC_FONT;
    public static BitmapFont HUD_COMPACT;


    public static void load() {

        FreeTypeFontGenerator gen =
            new FreeTypeFontGenerator(Gdx.files.internal("fonts/Homer_Simpson_Revised.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter prm =
            new FreeTypeFontGenerator.FreeTypeFontParameter();

        // HUD (small)
        prm.size = 48;
        prm.minFilter = Texture.TextureFilter.Linear;
        prm.magFilter = Texture.TextureFilter.Linear;
        HUD = gen.generateFont(prm);

        // Menu title
        prm.size = 48;
        TITLE = gen.generateFont(prm);

        prm.size = 82;
        prm.borderWidth = 0.2f;
        prm.color = com.badlogic.gdx.graphics.Color.BLACK;
        prm.borderColor = com.badlogic.gdx.graphics.Color.WHITE;
        NPC_FONT = gen.generateFont(prm);

        // countdown font
        prm.size = 140;
        prm.color = Color.WHITE;
        BIG = gen.generateFont(prm);

        // HUD_COMPACT (less spacing)
        prm.size = 48;
        prm.characters = FreeTypeFontGenerator.DEFAULT_CHARS;
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
