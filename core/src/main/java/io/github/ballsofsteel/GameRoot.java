package io.github.ballsofsteel;

import com.badlogic.gdx.Game;
import io.github.ballsofsteel.screen.MenuScreen;
import io.github.ballsofsteel.ui.Fonts;

public class GameRoot extends Game {
    private StoryManager storyManager;

    @Override
    public void create() {
        Fonts.load();
        storyManager = new StoryManager(this);
        setScreen(new MenuScreen(this));
    }
}
