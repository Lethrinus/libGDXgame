package io.github.theknightscrusade.factory;

import com.badlogic.gdx.graphics.OrthographicCamera;
import io.github.theknightscrusade.core.CoreGame;
import io.github.theknightscrusade.core.TileMapRenderer;
import io.github.theknightscrusade.entity.*;

import java.util.List;


public interface EntityFactory {

    Player createPlayer(CoreGame game,
                        OrthographicCamera cam,
                        TileMapRenderer   tileMap,
                        float x, float y);


    Goblin createGoblin(Player        player,
                        CoreGame       game,
                        List<Goblin>  crowd,
                        float         x, float y,
                        List<GoldBag> sharedLoot);

    DynamiteGoblin createDynamiteGoblin(Player       player, CoreGame game,
                                        List<DynamiteGoblin> crowd,
                                        List<GoldBag>       sharedLoot,
                                        float x,float y);

    BarrelBomber createBarrelBomber(Player player,
                                    CoreGame game,
                                    List<BarrelBomber> crowd,
                                    float x, float y);

    NPC createNPC(CoreGame game, float x, float y);

}
