package io.github.theknightscrusade;

import com.badlogic.gdx.Game;
import io.github.theknightscrusade.screen.MenuScreen;
import io.github.theknightscrusade.ui.Fonts;

public class GameRoot extends Game {

    @Override
    public void create() {
        Fonts.load();
        new StoryManager(this);
        setScreen(new MenuScreen(this));
    }
}
