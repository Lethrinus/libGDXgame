package io.github.theknightscrusade;

import com.badlogic.gdx.Game;
import io.github.theknightscrusade.events.*;
import io.github.theknightscrusade.screen.EndScreen;

public class StoryManager implements GameEventListener {
    private final Game game;
    private boolean endingQueued;

    public StoryManager(Game game) {
        this.game = game;
        EventBus.get().register(this);
    }
    @Override public void onEvent(GameEvent e) {
        switch (e.getType()) {
            case ENEMY_DEFEATED:
                // ileride daha çok şey yapılabilir (puan, diyalog …)
                break;
            case GAME_COMPLETED:
                game.setScreen(new EndScreen(game));
                break;
        }
    }
}
