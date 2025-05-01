package io.github.ballsofsteel.ui;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import io.github.ballsofsteel.ui.Fonts;

public final class WaveCounterUI {

    private static final float ICON_PX = 96f;
    private static final float PAD_PX  = 25f;
    private static final float GAP_PX  = 10f;

    private final OrthographicCamera cam;
    private final SpriteBatch        batch;
    private final Texture            icon;
    private final BitmapFont         font = Fonts.HUD;
    private final GlyphLayout        layout = new GlyphLayout();

    private int wave = 0;

    public WaveCounterUI(OrthographicCamera hudCamera) {
        this.cam   = hudCamera;
        this.batch = new SpriteBatch();
        this.icon  = new Texture("HUD/wave_icon.png");  // ikon dosyasını sen belirle (128x128 önerilir)
    }

    public void setWave(int wave) {
        this.wave = Math.max(1, wave + 1); // Always show at least 1
    }
    public void draw() {
        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        float xI = PAD_PX;
        float yI = cam.viewportHeight - PAD_PX - ICON_PX;

        String txt = "Wave " + (wave + 1);
        layout.setText(font, txt);

        float xT = xI + ICON_PX + GAP_PX;
        float yT = yI + ICON_PX / 2f + layout.height / 4f;
        batch.draw(icon, xI, yI, ICON_PX, ICON_PX);
        font.draw(batch, layout, xT, yT);

        batch.end();
    }
    public void dispose() {
        batch.dispose();
        icon.dispose();
    }
}
