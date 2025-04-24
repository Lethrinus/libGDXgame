
package io.github.ballsofsteel.screen;

import com.badlogic.gdx.*;
import io.github.ballsofsteel.core.CoreGame;

public class PlayScreen implements Screen {
    private final CoreGame core = new CoreGame();

    public PlayScreen(Game game) {
    }

    @Override public void show() { core.create(); }
    @Override public void render(float delta) { core.render(); }
    @Override public void resize(int w,int h){ core.resize(w,h); }
    @Override public void pause(){ core.pause(); }
    @Override public void resume(){ core.resume(); }
    @Override public void hide(){}
    @Override public void dispose(){ core.dispose(); }
}
