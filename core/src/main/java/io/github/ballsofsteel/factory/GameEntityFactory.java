package io.github.ballsofsteel.factory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import io.github.ballsofsteel.core.CoreGame;
import io.github.ballsofsteel.core.TileMapRenderer;
import io.github.ballsofsteel.entity.*;

import java.util.List;

public class GameEntityFactory implements EntityFactory {

    @Override
    public Player createPlayer(CoreGame core, OrthographicCamera cam, TileMapRenderer map, float x, float y){
        Player p = new Player(cam, map, core);
        p.getInventory().addItem(new MeatItem("Meat",
            new Texture(Gdx.files.internal("HUD/meat.png")), 25f));
        return p;
    }

    @Override
    public Goblin createGoblin(Player p,float x,float y,float minX,float maxX,float minY,float maxY,
                               List<GoldBag> loot){
        return new Goblin(p,x,y,minX,maxX,minY,maxY,loot);
    }

    @Override
    public Goblin createPatrollingGoblin(Player p,float x,float y,float minX,float maxX,float minY,float maxY,
                                         List<Vector2> wps,List<GoldBag> loot){
        Goblin g = createGoblin(p,x,y,minX,maxX,minY,maxY,loot);
        g.setPatrolWaypoints(wps);
        return g;
    }

    @Override
    public DynamiteGoblin createDynamiteGoblin(Player p,List<DynamiteGoblin> crowd,
                                               List<GoldBag> loot,float x,float y){
        return new DynamiteGoblin(p,crowd,loot,x,y);
    }

    @Override public BarrelBomber createBarrelBomber(Player p,float x,float y){
        return new BarrelBomber(p,x,y);
    }

    @Override public NPC createNPC(float x,float y,String[] lines){
        return new NPC(x,y,lines);
    }
}
