package io.github.theknightscrusade.ui;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;

public final class GoldCounterUI {

    private static final float ICON_PX = 128f;
    private static final float PAD_PX  = 10f;
    private static final float GAP_PX  = 4f;

    private final OrthographicCamera cam;
    private final SpriteBatch        batch;
    private final Texture            icon;
    private final BitmapFont         font = Fonts.HUD;
    private final GlyphLayout        layout = new GlyphLayout();

    private int gold = 0;

    public GoldCounterUI(OrthographicCamera hudCamera) {
        this.cam   = hudCamera;
        this.batch = new SpriteBatch();
        this.icon  = new Texture("HUD/gold_bag.png");
    }

    public void setGold(int v){ gold = v; }

    public void draw() {
        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        float xI = cam.viewportWidth  - PAD_PX - ICON_PX;
        float yI = cam.viewportHeight - PAD_PX - ICON_PX;

        String txt = String.valueOf(gold);
        layout.setText(font, txt);

        float xT = xI - GAP_PX - layout.width;
        float yT = yI + ICON_PX/2f + layout.height/4f;

        batch.draw(icon, xI, yI, ICON_PX, ICON_PX);
        font.draw(batch, layout, xT, yT);

        batch.end();
    }

    public void dispose(){
        batch.dispose();
        icon .dispose();   // HUD & BIG font’lar Fonts.dispose() içinde yok edilecek
    }
}
