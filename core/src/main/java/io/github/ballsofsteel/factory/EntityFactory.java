package io.github.ballsofsteel.factory;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import io.github.ballsofsteel.core.CoreGame;
import io.github.ballsofsteel.core.TileMapRenderer;
import io.github.ballsofsteel.entity.*;

import java.util.List;

/** Oyun varlıklarını üretmek için ortak fabrika arayüzü. */
public interface EntityFactory {

    /* -------- oyuncu -------- */
    Player createPlayer(CoreGame game,
                        OrthographicCamera cam,
                        TileMapRenderer   tileMap,
                        float x, float y);

    /* -------- goblin (normal & patrolden) -------- */
    Goblin createGoblin(Player        player,
                        List<Goblin>  crowd,
                        float         x, float y,
                        List<GoldBag> sharedLoot);

    /* -------- dinamit goblini -------- */
    DynamiteGoblin createDynamiteGoblin(Player       player,
                                        List<DynamiteGoblin> crowd,
                                        List<GoldBag>       sharedLoot,
                                        float x,float y);

    /* -------- varil bombacısı -------- */
    BarrelBomber  createBarrelBomber   (Player         player,
                                        List<BarrelBomber> crowd,
                                        float x,float y);

    /* -------- NPC -------- */
    NPC createNPC(CoreGame game,float x,float y,String[] lines);
}
