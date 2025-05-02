package io.github.theknightscrusade.screen;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.ScreenUtils;

public class EndScreen implements Screen {
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();
    private final Runnable onExit;
    private float alpha = 0f;
    private float timer = 0f;

    public EndScreen(Runnable onExit) {
        this.onExit = onExit;
        font.getData().setScale(2f);
        font.setColor(1, 1, 1, 0);
    }

    public EndScreen(Game game) {
        this(() -> game.setScreen(null));
    }

    @Override
    public void render(float delta) {
        timer += delta;
        alpha = Math.min(1f, timer / 2f);
        font.setColor(1, 1, 1, alpha);

        ScreenUtils.clear(0, 0, 0, 1);
        batch.begin();
        String title = "THE END";
        String thanks = "Thanks for playing!";
        String prompt = "[ESC] Exit    |    [ENTER] Restart";

        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        GlyphLayout layout = new GlyphLayout();
        layout.setText(font, title);
        font.draw(batch, layout, (width - layout.width) / 2, height * 0.7f);
        layout.setText(font, thanks);
        font.draw(batch, layout, (width - layout.width) / 2, height * 0.55f);
        layout.setText(font, prompt);
        font.draw(batch, layout, (width - layout.width) / 2, height * 0.4f);
        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            onExit.run();
        }
    }

    @Override public void resize(int width, int height) {}
    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
