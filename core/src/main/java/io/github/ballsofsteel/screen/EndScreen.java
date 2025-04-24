// src/io/github/some_example_name/EndScreen.java
package io.github.ballsofsteel.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class EndScreen implements Screen {
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font  = new BitmapFont();
    private final Runnable onExit;
    public EndScreen(Runnable onExit) { this.onExit = onExit; font.getData().setScale(2); }
    public EndScreen(com.badlogic.gdx.Game game){ this(() -> game.setScreen(null)); }
    @Override public void render(float delta){
        Gdx.gl.glClearColor(0,0,0,1); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin(); font.setColor(Color.WHITE);
        font.draw(batch,"THE END\n\nThanks for playing!", 200, 400);
        font.draw(batch,"Press ESC to exit", 220, 320); batch.end();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            Gdx.app.exit();
    }
    @Override public void resize(int w,int h){}
    @Override public void show(){}
    @Override public void hide(){}
    @Override public void pause(){}
    @Override public void resume(){}
    @Override public void dispose(){ batch.dispose(); font.dispose(); }
}
