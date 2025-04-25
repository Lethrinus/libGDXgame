package io.github.ballsofsteel.factory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import io.github.ballsofsteel.core.CoreGame;
import io.github.ballsofsteel.core.TileMapRenderer;
import io.github.ballsofsteel.entity.*;

import java.util.ArrayList;
import java.util.List;

public class GameEntityFactory implements EntityFactory {

    @Override
    public Player createPlayer(CoreGame        game,
                               OrthographicCamera cam,
                               TileMapRenderer   tileMap,
                               float x,float y){

        Player p = new Player(cam, tileMap, game);   //  ← 3 argüman
        p.setPosition(x,y);

        /* starter item örneği */
        Texture meat = new Texture(Gdx.files.internal("HUD/meat.png"));
        p.getInventory().addItem(new MeatItem("Meat", meat, 25f));
        p.getInventory().addItem(new MeatItem("Meat", meat, 25f));

        return p;
    }



    @Override
    public Goblin createGoblin(Player player,
                               float x,float y,
                               float minX,float maxX,
                               float minY,float maxY,
                               List<GoldBag> loot){
        return new Goblin(player,x,y,minX,maxX,minY,maxY,loot);
    }

    @Override
    public Goblin createPatrollingGoblin(Player player,
                                         float x,float y,
                                         float minX,float maxX,
                                         float minY,float maxY,
                                         List<Vector2> wp,
                                         List<GoldBag> loot){
        Goblin g = createGoblin(player,x,y,minX,maxX,minY,maxY,loot);
        if(wp!=null && !wp.isEmpty()) g.setPatrolWaypoints(new ArrayList<>(wp));
        return g;
    }


    @Override
    public DynamiteGoblin createDynamiteGoblin(Player player,
                                               List<DynamiteGoblin> crowd,
                                               List<GoldBag> loot,
                                               float x,float y){
        return new DynamiteGoblin(player,crowd,loot,x,y);
    }

    public BarrelBomber createBarrelBomber(Player p,
                                           List<BarrelBomber> crowd,
                                           float x,float y){
        return new BarrelBomber(p,crowd,x,y);
    }

    @Override public NPC createNPC(float x,float y,String[] lines){
        return new NPC(x,y,lines);
    }
}
