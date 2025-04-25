/*  CoreGame.java – ana oyun döngüsü
    · GoldCounterUI yalnızca sağ-üstte küçük sayaç çiziyor ―
      eski icon+font kodu kaldırıldı.
    · BarrelBomber kalabalık ayrışması için liste referansı veriliyor.
*/
package io.github.ballsofsteel.core;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import io.github.ballsofsteel.entity.*;
import io.github.ballsofsteel.factory.GameEntityFactory;
import io.github.ballsofsteel.ui.GoldCounterUI;
import io.github.ballsofsteel.ui.HealthBarRenderer;
import io.github.ballsofsteel.ui.InventoryHUD;

import java.util.*;

public class CoreGame extends ApplicationAdapter {

    /* ── render/kamera ─────────────────────────────────────────── */
    private SpriteBatch batch;
    private final OrthographicCamera cam = new OrthographicCamera();
    private TileMapRenderer map;

    /* ── varlıklar ─────────────────────────────────────────────── */
    private Player player;
    private Goblin patrolGoblin;
    private NPC    npc;
    private final List<DynamiteGoblin> dynas   = new ArrayList<>();
    private final List<BarrelBomber>   barrels = new ArrayList<>();
    private final List<GoldBag>        loot    = new ArrayList<>();

    /* ── HUD & sayaçlar ───────────────────────────────────────── */
    private InventoryHUD hud;
    private GoldCounterUI goldUI;

    /* ── factory ──────────────────────────────────────────────── */
    private final GameEntityFactory factory = new GameEntityFactory();

    /* ── getter’lar (Player erişimi için) ─────────────────────── */
    public List<DynamiteGoblin> getDynaList() { return dynas; }
    public Goblin              getMainGoblin(){ return patrolGoblin; }

    /* ── başlangıç ────────────────────────────────────────────── */
    @Override public void create() {

        batch = new SpriteBatch();
        cam.setToOrtho(false, 16, 9);

        map    = new TileMapRenderer(cam, "maps/tileset.tmx");
        player = factory.createPlayer(this, cam, map, 8, 4.5f);

        /* yollanan devriye goblini */
        ArrayList<Vector2> wp = new ArrayList<>(Arrays.asList(
            new Vector2(5,5), new Vector2(5,10),
            new Vector2(10,10), new Vector2(10,5)));
        patrolGoblin = factory.createPatrollingGoblin(
            player, 11,4.5f, 8,12, 3,6, wp, loot);

        npc = factory.createNPC(5,5, new String[]{
            "hello adventurer!", "be careful out there.", "press e to talk."});

        /* HUD */
        hud = new InventoryHUD();
        hud.initializeCamera(1200,800);
        hud.loadTextures();

        goldUI = new GoldCounterUI(hud.getCamera());   // InventoryHUD → HUD kamerası

        /* scroll ile slot geçişi */
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override public boolean scrolled(float dx, float dy) {
                if (dy > 0) hud.nextSlot(); else if (dy < 0) hud.prevSlot();
                return true;
            }
        });
        Gdx.gl.glClearColor(0,0,0,1);
    }

    /* ── ana loop ─────────────────────────────────────────────── */
    @Override public void render() {

        float dt = Gdx.graphics.getDeltaTime();
        handleInput();

        player.update(dt);
        patrolGoblin.update(dt);
        npc.update(dt, new Vector2(player.getX(), player.getY()));
        updateSpawns(dt);
        updateCamera(dt);

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderWorld();
        renderHUD();
    }

    /* ── düşman / loot güncellemeleri ─────────────────────────── */
    private void updateSpawns(float dt) {

        /* spawn tuşları */
        if (Gdx.input.isKeyJustPressed(Input.Keys.T))
            dynas.add(factory.createDynamiteGoblin(
                player, dynas, loot,
                player.getX() + 3f, player.getY()));

        if (Gdx.input.isKeyJustPressed(Input.Keys.B))
            barrels.add(factory.createBarrelBomber(
                player, barrels,
                player.getX() + 4f, player.getY()));

        dynas   .removeIf(d -> { d.update(dt); return d.isDead();     });
        barrels .removeIf(b -> { b.update(dt); return b.isFinished(); });

        for (Iterator<GoldBag> it = loot.iterator(); it.hasNext();) {
            GoldBag g = it.next();
            if (g.isCollectedBy(player)) {         // çanta alındı mı?
                player.addGold(1);
                it.remove();
            }
        }
        goldUI.setGold(player.getGold());          // sayacı güncelle
    }

    /* ── kamera takip ─────────────────────────────────────────── */
    private void updateCamera(float dt) {
        float lerp = .1f;
        cam.position.x = MathUtils.lerp(cam.position.x, player.getX(), lerp);
        cam.position.y = MathUtils.lerp(cam.position.y, player.getY(), lerp);

        float hw = cam.viewportWidth  / 2f,
            hh = cam.viewportHeight / 2f;
        cam.position.x = MathUtils.clamp(cam.position.x, hw, map.getMapWidth()  - hw);
        cam.position.y = MathUtils.clamp(cam.position.y, hh, map.getMapHeight() - hh);
        cam.update();
    }

    /* ── dünya çizimi ─────────────────────────────────────────── */
    private void renderWorld() {

        map.renderBaseLayers(new int[]{0,1});
        if (player.isInBush()) map.renderBushWithShader(player, 50f);
        else                    map.renderBushNoShader();

        /* goblin ağaç altı katmanında */
        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        patrolGoblin.render(batch);
        batch.end();

        /* ağaç tepeleri */
        if (map.isCellTreeTop((int)player.getX(), (int)player.getY()))
            map.renderTreeTopWithShader(player, 90f);
        else map.renderTreeTopNoShader();

        /* oyuncu + diğer varlıklar */
        batch.begin();
        player.render(batch);
        barrels.forEach (b -> b.render(batch));
        loot   .forEach (g -> g.render(batch));
        dynas  .forEach (d -> d.render(batch));
        npc.render(batch);
        batch.end();
    }

    /* ── HUD çizimi ───────────────────────────────────────────── */
    private void renderHUD() {

        hud.drawHUD(player);          // envanter
        goldUI.draw();                // sağ-üst altın sayacı

        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        /* sağlık barları */
        HealthBarRenderer.drawBar(batch,
            player.getX(), player.getY() + player.getSpriteHeight()/2f + .25f,
            player.getHealth()/player.getMaxHealth(), false);

        if (!patrolGoblin.isDead() && !patrolGoblin.isDying())
            HealthBarRenderer.drawBar(batch,
                patrolGoblin.getX(), patrolGoblin.getY() + .8f,
                patrolGoblin.getHealth()/patrolGoblin.getMaxHealth(), true);

        batch.end();
    }

    /* ── input / eylemler ────────────────────────────────────── */
    private void handleInput() {

        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) hud.nextSlot();
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT )) hud.prevSlot();

        for (int i = 0; i < 4; i++)
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) hud.setSelectedSlot(i);

        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) useSelectedItem();
    }
    private void useSelectedItem() {
        int idx = hud.getSelectedSlot();
        if (idx < player.getInventory().getItems().size()) {
            player.getInventory().getItems().get(idx).use(player);
            if (idx >= player.getInventory().getItems().size())
                hud.setSelectedSlot(Math.max(0, player.getInventory().getItems().size() - 1));
        }
    }

    /* ── temizle ─────────────────────────────────────────────── */
    @Override public void dispose() {
        batch.dispose();
        player.dispose();
        patrolGoblin.dispose();
        npc.dispose();
        hud.dispose();
        map.dispose();
        goldUI.dispose();
    }
}
