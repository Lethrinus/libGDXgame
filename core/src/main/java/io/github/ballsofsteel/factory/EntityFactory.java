package io.github.ballsofsteel.factory;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import io.github.ballsofsteel.core.CoreGame;
import io.github.ballsofsteel.core.TileMapRenderer;
import io.github.ballsofsteel.entity.*;

import java.util.List;

public interface EntityFactory {
    Player createPlayer(CoreGame core, OrthographicCamera cam, TileMapRenderer map, float x, float y);
    Goblin createGoblin(Player p,float x,float y,float minX,float maxX,float minY,float maxY,List<GoldBag> loot);
    Goblin createPatrollingGoblin(Player p,float x,float y,float minX,float maxX,float minY,float maxY,
                                  List<Vector2> waypoints,List<GoldBag> loot);
    DynamiteGoblin createDynamiteGoblin(Player p,List<DynamiteGoblin> crowd,
                                        List<GoldBag> loot,float x,float y);
    BarrelBomber createBarrelBomber(Player p,float x,float y);
    NPC createNPC(float x,float y,String[] lines);
}
