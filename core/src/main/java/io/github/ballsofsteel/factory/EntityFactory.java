package io.github.ballsofsteel.factory;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import io.github.ballsofsteel.core.TileMapRenderer;
import io.github.ballsofsteel.entity.Goblin;
import io.github.ballsofsteel.entity.NPC;
import io.github.ballsofsteel.entity.Player;

import java.util.List;

/**
 * Interface defining entity creation operations
 */
public interface EntityFactory {
    Player createPlayer(OrthographicCamera camera, TileMapRenderer tileMapRenderer, float x, float y);
    Goblin createGoblin(Player player, float x, float y, float patrolMinX, float patrolMaxX, float patrolMinY, float patrolMaxY);
    Goblin createPatrollingGoblin(Player player, float x, float y, float patrolMinX, float patrolMaxX, float patrolMinY, float patrolMaxY, List<Vector2> waypoints);
    NPC createNPC(float x, float y, String[] dialogueLines);
}
