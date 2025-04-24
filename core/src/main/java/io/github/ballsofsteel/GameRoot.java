package io.github.ballsofsteel;

import com.badlogic.gdx.Game;
import io.github.ballsofsteel.screen.MenuScreen;

public class GameRoot extends Game {
    private StoryManager storyManager;

    @Override
    public void create() {
        storyManager = new StoryManager(this);
        setScreen(new MenuScreen(this));
    }
}
