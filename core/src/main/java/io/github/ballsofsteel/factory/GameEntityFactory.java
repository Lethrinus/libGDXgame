package io.github.ballsofsteel.factory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import io.github.ballsofsteel.core.TileMapRenderer;
import io.github.ballsofsteel.entity.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete implementation of EntityFactory that creates game entities.
 */
public class GameEntityFactory implements EntityFactory {

    @Override
    public Player createPlayer(OrthographicCamera camera, TileMapRenderer tileMapRenderer, float x, float y) {
        // Create player
        Player player = new Player(camera, tileMapRenderer);
        player.setPosition(x, y);

        // Add initial items to inventory
        addInitialPlayerItems(player);

        return player;
    }

    /**
     * Adds starter items to the player's inventory
     */
    private void addInitialPlayerItems(Player player) {
        Texture meatTexture = new Texture(Gdx.files.internal("HUD/meat.png"));
        player.getInventory().addItem(new MeatItem("Meat", meatTexture, 25f));
        player.getInventory().addItem(new MeatItem("Meat", meatTexture, 25f));



    }

    @Override
    public Goblin createGoblin(Player player, float x, float y, float patrolMinX, float patrolMaxX, float patrolMinY, float patrolMaxY) {
        Goblin goblin = new Goblin(player, x, y, patrolMinX, patrolMaxX, patrolMinY, patrolMaxY);
        return goblin;
    }

    @Override
    public Goblin createPatrollingGoblin(Player player, float x, float y, float patrolMinX, float patrolMaxX, float patrolMinY, float patrolMaxY, List<Vector2> waypoints) {
        Goblin goblin = createGoblin(player, x, y, patrolMinX, patrolMaxX, patrolMinY, patrolMaxY);

        if (waypoints != null && !waypoints.isEmpty()) {
            goblin.setPatrolWaypoints(new ArrayList<>(waypoints));
        }

        return goblin;
    }

    public DynamiteGoblin createDynamiteGoblin(Player player, float x, float y) {
        return new DynamiteGoblin(player, x, y);
    }
    public BarrelBomber createBarrelBomber(Player p,float x,float y){
        return new BarrelBomber(p,x,y);
    }
    @Override
    public NPC createNPC(float x, float y, String[] dialogueLines) {
        return new NPC(x, y, dialogueLines);
    }
}
