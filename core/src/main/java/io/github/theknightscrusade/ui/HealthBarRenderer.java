package io.github.theknightscrusade.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/** Ortak, statik sağlık çubuğu — SpriteBatch.begin()–end() arasına GİRMEZ! */
public final class HealthBarRenderer {

    private static final ShapeRenderer SR = new ShapeRenderer();
    private HealthBarRenderer() {}

    /**
     * @param batch  Aktif SpriteBatch (begin–end dışında çağrılmalı)
     * @param cx     Dünya ekseninde merkez X
     * @param cy     Dünya ekseninde taban Y
     * @param ratio  0–1 arası sağlık yüzdesi
     * @param enemy  Düşman mı? (kırmızı yerine yeşil→sarı skala)
     */
    public static void drawBar(SpriteBatch batch,
                               float cx, float cy,
                               float ratio,
                               boolean enemy) {

        batch.end();
        SR.setProjectionMatrix(batch.getProjectionMatrix());
        SR.begin(ShapeRenderer.ShapeType.Filled);

        /* arka plan */
        SR.setColor(Color.DARK_GRAY);
        SR.rect(cx - .5f, cy, 1f, .12f);

        /* dolu kısım */
        Color c = enemy
            ? new Color(MathUtils.lerp(0f, 1f, 1 - ratio), 1f, 0f, 1f)  // yeşil→sarı
            : (ratio > .5f ? Color.LIME : Color.RED);                  // yeşil / sarı
        SR.setColor(c);
        SR.rect(cx - .49f, cy + .01f, .98f * ratio, .10f);

        SR.end();
        batch.begin();
    }
}
