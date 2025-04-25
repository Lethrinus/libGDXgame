/*  tam dosya – ana oyun (loop + spawn + HUD). */
package io.github.ballsofsteel.core;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import io.github.ballsofsteel.entity.*;
import io.github.ballsofsteel.factory.GameEntityFactory;
import io.github.ballsofsteel.ui.HealthBarRenderer;
import io.github.ballsofsteel.ui.InventoryHUD;

import java.util.*;

public class CoreGame extends ApplicationAdapter {

    /* ---------- render/cam ---------- */
    private SpriteBatch batch;
    private final OrthographicCamera cam = new OrthographicCamera();
    private TileMapRenderer map;

    /* ---------- entities ---------- */
    private Player player;
    private Goblin patrolGoblin;
    private NPC npc;
    private final List<DynamiteGoblin> dynas = new ArrayList<>();
    private final List<BarrelBomber> barrels = new ArrayList<>();
    private final List<GoldBag> loot        = new ArrayList<>();

    /* ---------- HUD & factory ---------- */
    private InventoryHUD hud;
    private final GameEntityFactory factory = new GameEntityFactory();

    /* ---------- getters (Player ihtiyaç duyuyor) ---------- */
    public List<DynamiteGoblin> getDynaList(){ return dynas; }
    public Goblin getMainGoblin(){ return patrolGoblin; }

    @Override public void create() {

        batch = new SpriteBatch();
        cam.setToOrtho(false,16,9);

        map = new TileMapRenderer(cam,"maps/tileset.tmx");

        player = factory.createPlayer(this, cam, map, 8, 4.5f);

        ArrayList<Vector2> wp = new ArrayList<>(Arrays.asList(
            new Vector2(5,5),new Vector2(5,10),
            new Vector2(10,10),new Vector2(10,5)));
        patrolGoblin = factory.createPatrollingGoblin(
            player,11,4.5f,8,12,3,6, wp, loot);

        npc = factory.createNPC(5,5,new String[]{
            "hello adventurer!","be careful out there.","press e to talk."});

        hud = new InventoryHUD();
        hud.initializeCamera(1200,800); hud.loadTextures();

        Gdx.input.setInputProcessor(new InputAdapter(){
            @Override public boolean scrolled(float dx,float dy){
                if(dy>0) hud.nextSlot(); else if(dy<0) hud.prevSlot();
                return true;
            }
        });
        Gdx.gl.glClearColor(0,0,0,1);
    }

    @Override public void render() {

        float dt = Gdx.graphics.getDeltaTime();
        handleInput();

        player.update(dt);
        patrolGoblin.update(dt);
        npc.update(dt,new Vector2(player.getX(),player.getY()));
        updateSpawns(dt);

        updateCam(dt);

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderWorld();
        renderHud();
    }

    /* ---------- spawn / enemy update ---------- */
    private void updateSpawns(float dt){
        if(Gdx.input.isKeyJustPressed(Input.Keys.T))
            dynas.add(factory.createDynamiteGoblin(player,dynas,loot,
                player.getX()+3f,player.getY()));

        if(Gdx.input.isKeyJustPressed(Input.Keys.B))
            barrels.add(factory.createBarrelBomber(player,
                player.getX()+4f,player.getY()));

        dynas.removeIf(d->{ d.update(dt); return d.isDead(); });
        barrels.removeIf(b->{ b.update(dt); return b.isFinished(); });
        loot.removeIf(g->g.update(dt));
    }

    /* ---------- camera ---------- */
    private void updateCam(float dt){
        float s=.1f;
        cam.position.x=MathUtils.lerp(cam.position.x,player.getX(),s);
        cam.position.y=MathUtils.lerp(cam.position.y,player.getY(),s);

        float hw=cam.viewportWidth/2f, hh=cam.viewportHeight/2f;
        cam.position.x=MathUtils.clamp(cam.position.x,hw,map.getMapWidth()-hw);
        cam.position.y=MathUtils.clamp(cam.position.y,hh,map.getMapHeight()-hh);
        cam.update();
    }

    /* ---------- render helpers ---------- */
    private void renderWorld(){

        map.renderBaseLayers(new int[]{0,1});
        if(player.isInBush()) map.renderBushWithShader(player,50f);
        else                  map.renderBushNoShader();

        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        patrolGoblin.render(batch);
        batch.end();

        if(map.isCellTreeTop((int)player.getX(),(int)player.getY()))
            map.renderTreeTopWithShader(player,90f);
        else map.renderTreeTopNoShader();

        batch.begin();
        player.render(batch);
        for(BarrelBomber b:barrels) b.render(batch);
        for(GoldBag g:loot)         g.render(batch);
        for(DynamiteGoblin d:dynas) d.render(batch);
        npc.render(batch);
        batch.end();
    }

    private void renderHud(){

        hud.drawHUD(player);

        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        HealthBarRenderer.drawBar(batch,
            player.getX(), player.getY()+player.getSpriteHeight()/2f+.25f,
            player.getHealth()/player.getMaxHealth(), false);

        if(!patrolGoblin.isDead() && !patrolGoblin.isDying())
            HealthBarRenderer.drawBar(batch,
                patrolGoblin.getX(), patrolGoblin.getY()+.8f,
                patrolGoblin.getHealth()/patrolGoblin.getMaxHealth(),true);
        batch.end();
    }

    /* ---------- input ---------- */
    private void handleInput(){
        if(Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) hud.nextSlot();
        if(Gdx.input.isKeyJustPressed(Input.Keys.LEFT )) hud.prevSlot();

        for(int i=0;i<4;i++)
            if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_1+i)) hud.setSelectedSlot(i);

        if(Gdx.input.isKeyJustPressed(Input.Keys.E)) useSelected();
    }
    private void useSelected(){
        int idx=hud.getSelectedSlot();
        if(idx<player.getInventory().getItems().size()){
            player.getInventory().getItems().get(idx).use(player);
            if(idx>=player.getInventory().getItems().size())
                hud.setSelectedSlot(Math.max(0,player.getInventory().getItems().size()-1));
        }
    }

    /* ---------- dispose ---------- */
    @Override public void dispose(){
        batch.dispose(); player.dispose(); patrolGoblin.dispose();
        npc.dispose(); hud.dispose(); map.dispose();
    }
}
