package io.github.ballsofsteel.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import io.github.ballsofsteel.entity.Player;
import io.github.ballsofsteel.events.EventBus;
import io.github.ballsofsteel.events.GameEvent;
import io.github.ballsofsteel.events.GameEventListener;
import io.github.ballsofsteel.events.GameEventType;

public final class UpgradeMenu implements GameEventListener {

    private boolean visible = false;
    private final BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();
    private Player player;

    private enum Upgrade { HEALTH, DAMAGE, SPEED }

    public UpgradeMenu() {
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        EventBus.register(this);         // (şimdilik gerekmiyor ama hazır)
    }

    public void show(Player p) { visible = true;  player = p; }
    public void hide()         { visible = false; }
    public boolean isVisible() { return visible; }

    /* tuşlar 1-2-3 */
    public void handleInput() {
        if (!visible) return;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) apply(Upgrade.HEALTH);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) apply(Upgrade.DAMAGE);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) apply(Upgrade.SPEED );
    }

    private void apply(Upgrade u) {
        switch (u) {
            case HEALTH: player.increaseHealth(30f);        break;
            case DAMAGE: player.increaseAttackDamage(1.2f); break;
            case SPEED : player.increaseMoveSpeed (1.1f);   break;
        }
        EventBus.post(new GameEvent(GameEventType.UPGRADE_SELECTED, u.name()));
        hide();
    }

    public void render(SpriteBatch b) {
        if (!visible) return;

        /* --- batch begin/end güvenliği --- */
        boolean openedHere = !b.isDrawing();
        if (openedHere) b.begin();

        /* --- projeksiyonu geçici olarak HUD (piksel) kamerasına al --- */
        Matrix4 oldProj = b.getProjectionMatrix();                 // yedekle
        b.setProjectionMatrix(
            new com.badlogic.gdx.math.Matrix4()
                .setToOrtho2D(0, 0,
                    Gdx.graphics.getWidth(),
                    Gdx.graphics.getHeight()));

        /* --- metni çiz --- */
        String txt =
            "=== UPGRADE ===\n" +
                "1) +30 HEALTH\n" +
                "2) +20% DAMAGE\n" +
                "3) +10% MOVE SPEED\n" +
                "\nPress 1  2  3";

        layout.setText(font, txt);
        float x = (Gdx.graphics.getWidth()  - layout.width ) / 2f;
        float y = (Gdx.graphics.getHeight() + layout.height) / 2f;
        font.draw(b, txt, x, y);

        /* --- eski projeksiyonu geri koy --- */
        b.setProjectionMatrix(oldProj);

        if (openedHere) b.end();
    }


    public void dispose() { font.dispose(); }

    @Override public void onEvent(GameEvent e) { /* şimdilik boş */ }
}
