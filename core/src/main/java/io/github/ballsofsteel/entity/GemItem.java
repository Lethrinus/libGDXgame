// src/io/github/some_example_name/GemItem.java
package io.github.ballsofsteel.entity;

import com.badlogic.gdx.graphics.Texture;
import io.github.ballsofsteel.events.*;

public class GemItem extends Item {
    public GemItem(Texture icon){ super("Emerald Core", icon); }
    @Override public void use(Player p){
        // zümrüt alınınca oyunu bitir
        EventBus.get().post(new GameEvent(GameEventType.GAME_COMPLETED, p));
    }
}
