package io.github.some_example_name;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;


/**
 * GameScreen wrapper for Main class to enable proper screen transitions.
 * This class implements Screen interface and delegates functionality to Main.
 */
public class GameScreen implements Screen {
    private final Game game;
    private final Main mainGame;
    private boolean initialized = false;

    public GameScreen(Game game) {
        this.game = game;
        this.mainGame = new Main();
    }

    @Override
    public void show() {
        // Initialize Main when the screen is shown
        mainGame.create();
        initialized = true;
    }

    @Override
    public void render(float delta) {
        if (initialized) {
            mainGame.render();
        }
    }

    @Override
    public void resize(int width, int height) {
        if (initialized) {
            mainGame.resize(width, height);
        }
    }

    @Override
    public void pause() {
        if (initialized) {
            mainGame.pause();
        }
    }

    @Override
    public void resume() {
        if (initialized) {
            mainGame.resume();
        }
    }

    @Override
    public void hide() {
        // Called when this screen is no longer the current screen
    }

    @Override
    public void dispose() {
        if (initialized) {
            mainGame.dispose();
        }
    }
}
