package io.github.theknightscrusade.ui;

import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;

/** 3-2-1-GO geri-sayımlarını çizen basit actor. */
public class CountdownOverlay extends Group {

    private final BitmapFont font = Fonts.BIG;    // büyük ve keskin
    private final GlyphLayout lay = new GlyphLayout();
    private final float yRatio;                   // ekran yüksekliği (%)

    private String text = "";

    public CountdownOverlay(float yRatio) {
        this.yRatio = yRatio;
        setTransform(false);
        setColor(1, 1, 1, 0);      // başlangıçta görünmez
    }

    /** WaveManager’den her sayı geldiğinde çağrılır. */
    public void showNumber(int n) {
        text = (n > 0) ? Integer.toString(n) : "GO!";
        setColor(1, 1, 1, 1);      // alfa tekrar 1
        clearActions();
        addAction(Actions.fadeOut(.5f));
    }

    @Override public void draw(Batch batch, float parentAlpha) {
        if (getColor().a <= 0f) return;

        lay.setText(font, text);
        float w = getStage().getViewport().getWorldWidth();
        float h = getStage().getViewport().getWorldHeight();
        float x = w / 2f - lay.width  / 2f;
        float y = h * yRatio + lay.height / 2f;

        font.setColor(1, 1, 1, getColor().a);
        font.draw(batch, lay, x, y);
    }
}
