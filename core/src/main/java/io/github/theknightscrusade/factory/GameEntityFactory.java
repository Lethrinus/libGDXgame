package io.github.theknightscrusade.factory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import io.github.theknightscrusade.core.CoreGame;
import io.github.theknightscrusade.core.TileMapRenderer;
import io.github.theknightscrusade.entity.*;
import java.util.List;

public class GameEntityFactory implements EntityFactory {

    @Override
    public Player createPlayer(CoreGame        game,
                               OrthographicCamera cam,
                               TileMapRenderer   tileMap,
                               float x,float y){

        Player p = new Player(cam, tileMap, game);
        p.setPosition(x,y);


        Texture meat = new Texture(Gdx.files.internal("HUD/meat.png"));
        p.getInventory().addItem(new MeatItem("Meat", meat, 25f));
        p.getInventory().addItem(new MeatItem("Meat", meat, 25f));

        return p;
    }




    @Override
    public Goblin createGoblin(Player        player,
                               CoreGame game,
                               List<Goblin>  crowd,
                               float         x, float y,
                               List<GoldBag> loot)
    {
        return new Goblin(player,game, crowd, x, y, loot);
    }

    @Override
    public DynamiteGoblin createDynamiteGoblin(Player player,CoreGame game,
                                               List<DynamiteGoblin> crowd,
                                               List<GoldBag> loot,
                                               float x,float y)
    {
        return new DynamiteGoblin(player,game,crowd,loot,x,y);
    }

    public BarrelBomber createBarrelBomber(Player player,
                                           CoreGame game,
                                           List<BarrelBomber> crowd,
                                           float x, float y)
    {
        return new BarrelBomber(player, game, crowd, x, y);
    }

    public NPC createNPC(CoreGame game, float x, float y) {
        return new NPC(game, x, y);
    }
}
