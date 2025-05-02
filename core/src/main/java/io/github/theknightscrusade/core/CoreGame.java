package io.github.theknightscrusade.core;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.theknightscrusade.entity.*;
import io.github.theknightscrusade.events.EventBus;
import io.github.theknightscrusade.events.GameEvent;
import io.github.theknightscrusade.events.GameEventListener;
import io.github.theknightscrusade.events.GameEventType;
import io.github.theknightscrusade.factory.GameEntityFactory;
import io.github.theknightscrusade.screen.GameOverScreen;
import io.github.theknightscrusade.ui.*;
import io.github.theknightscrusade.screen.UpgradeMenu;
import io.github.theknightscrusade.screen.PauseManager;

import java.util.*;

// CoreGame
public class CoreGame extends ApplicationAdapter implements GameEventListener {

    /* ---------- render & kamera ---------- */
    private SpriteBatch batch;
    private final OrthographicCamera cam = new OrthographicCamera();
    private TileMapRenderer map;
    private CameraShake cameraShake;
    private Stage overlayStage;
    private CountdownOverlay countdown;
    private PauseManager pauseManager;
    private WaveCounterUI waveUI;
    private WaveManager waveManager;
    /* ---------- varlık listeleri ---------- */
    private Player player;
    private NPC    npc;
    private final List<Goblin>         goblins  = new ArrayList<>();
    private final List<DynamiteGoblin> dynas    = new ArrayList<>();
    private final List<BarrelBomber>   barrels  = new ArrayList<>();
    private final List<GoldBag>        loot     = new ArrayList<>();
    private final GameEntityFactory factory = new GameEntityFactory();

    /* ---------- HUD & upgrade menüsü ---------- */
    private InventoryHUD hud;
    private GoldCounterUI goldUI;
    private UpgradeMenu upgradeMenu;


    // shader program
    private ShaderProgram grayscaleShader;


    // player death
    private boolean playerDead = false;
    private float deathTimer = 0f;
    private float deathAnimDuration = 2.1f; // Set this to your death animation's duration
    private float deathZoomStart = 1.0f;
    private float deathZoomEnd = 1.5f; // How much to zoom in



   // create

    @Override
    public void create() {
        Fonts.load();
        grayscaleShader = ShaderManager.createGrayscaleShader();
        batch = new SpriteBatch();
        cam.setToOrtho(false, 21, 12.35f);
        map    = new TileMapRenderer(cam, "maps/tileset2.tmx");
        player = factory.createPlayer(this, cam, map, 31, 45);


        npc = factory.createNPC(this, 26, 45);

        npc.setDialoguesForWave(-1, new String[]{
            "Welcome, brave knight.",
            "We are in dire need.",
            "You are our only hope!",
            "Goblins destroyed our...",
            "...village out in the woods!",
            "They have built their...",
            "...camps there.",
            "They have been attacking...",
            "...the town since then.",
            "All our guards deserted...",
            "...their posts!",
            "We have sent word to...",
            "...the king.",
            "But it will take time...",
            "...until the help comes!",
            "You must defend...",
            "...the town as long as...",
            "...you can!"
        });
        npc.setDialoguesForWave(0, new String[]{
            "Here they come! Prepare yourself!"
        });
        npc.setDialoguesForWave(1, new String[]{
            "Good job surviving that.",
            "I can help with your gear if you have gold to pay!"
        });
        npc.setDialoguesForWave(2, new String[]{
            "You're holding strong!"
        });
        npc.setDialoguesForWave(3, new String[]{
            "They cannot stand your strength!.",
            "Time for your final upgrade."
        });
        npc.setDialoguesForWave(4, new String[]{
            "Reinforcements almost arrived, you must buy us a little more time!",
            "Give it your all!"
        });
        npc.setDialoguesForWave(5, new String[]{
            "You've done it. King's army has came. We are saved.",
            "Thank you, brave knight."
        });



        // hud , ui , wave manager,  upgrade menu
        hud = new InventoryHUD();
        hud.initializeCamera(1200, 800);
        hud.loadTextures();
        goldUI = new GoldCounterUI(hud.getCamera());
        waveUI = new WaveCounterUI(hud.getCamera());
        cameraShake = new CameraShake(cam);
        upgradeMenu = new UpgradeMenu();
        waveManager = new WaveManager(this, factory);


        overlayStage = new Stage(new ScreenViewport());   // firstly create stage
        BitmapFont bigFont = new BitmapFont();
        bigFont.getData().setScale(1f);
        countdown = new CountdownOverlay(0.7f);
        overlayStage.addActor(countdown);


        TextureAtlas pauseAtlas = new TextureAtlas("UI/mainmenu_ui.atlas");
        pauseManager = new PauseManager((Game) Gdx.app.getApplicationListener(), new InputMultiplexer(), pauseAtlas);

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(pauseManager.getStage());
        multiplexer.addProcessor(overlayStage);
        multiplexer.addProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float dx, float dy) {
                if (dy > 0) hud.nextSlot(); else if (dy < 0) hud.prevSlot();
                return true;
            }
        });
        multiplexer.addProcessor(upgradeMenu.getStage());
        Gdx.input.setInputProcessor(multiplexer);

        // event listener
        EventBus.register(this);

        // slot changing with scroll
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override public boolean scrolled(float dx, float dy) {
                if (dy > 0) hud.nextSlot(); else if (dy < 0) hud.prevSlot();
                return true;
            }
        });

        Gdx.gl.glClearColor(0, 0, 0, 1);
    }


    // render loop

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();

        pauseManager.update();

        if (pauseManager.isPaused()) {
            renderPausedState();
            pauseManager.render();
            return;
        }

        //alive or death logic//
        player.update(dt);

        if (!playerDead && player.getHealth() <= 0f) {
            playerDead     = true;
            deathTimer     = 0f;
            deathZoomStart = cam.zoom;
            deathZoomEnd   = 0.5f;
        }

        if (playerDead) {
            deathTimer += dt;
            float t = MathUtils.clamp(deathTimer / deathAnimDuration, 0f, 1f);
            cam.zoom = MathUtils.lerp(deathZoomStart, deathZoomEnd, t);
            cam.update();

            renderDeathState(dt);

            if (deathTimer >= deathAnimDuration) {
                Pixmap pix = ScreenUtils.getFrameBufferPixmap(0, 0,
                    Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                Texture snap = new Texture(pix); pix.dispose();
                snap.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

                Game g = (Game)Gdx.app.getApplicationListener();
                g.setScreen(new GameOverScreen(g, snap));
            }
            return;
        }


        handleInput(dt);
        if (!upgradeMenu.isVisible()) {
            npc.update(dt, new Vector2(player.getX(), player.getY()));
            goblins .removeIf(e -> { e.update(dt); return e.isDead(); });
            dynas   .removeIf(d -> { d.update(dt); return d.isDead(); });
            barrels .removeIf(b -> { b.update(dt); return b.isFinished(); });
            updateLoot();
            waveManager.update(dt);
            updateCamera(dt);
            cameraShake.update(dt);
        }

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        renderWorld();
        renderHUD();
        upgradeMenu.render(batch);
    }


    private void renderPausedState() {
        // clear first
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 1) bind grayscale shader and set uniform to full (1.0)
        grayscaleShader.begin();
        grayscaleShader.setUniformf("u_grayscaleFactor", 1f);

        // 2) draw map with shader
        ShaderProgram oldShader = map.renderer.getBatch().getShader();
        map.renderer.getBatch().setShader(grayscaleShader);
        map.renderBase();
        map.renderTreeTop(player, 90f, map.isCellTreeTop(
            (int)player.getX(), (int)player.getY()));
        map.renderer.getBatch().setShader(oldShader);

        // 3) draw entities with shader
        SpriteBatch sb = batch;
        sb.setShader(grayscaleShader);
        sb.setProjectionMatrix(cam.combined);
        sb.begin();
        goblins .forEach(g -> g.render(sb));
        dynas   .forEach(d -> d.render(sb));
        barrels .forEach(b -> b.render(sb));
        loot    .forEach(l -> l.render(sb));
        npc     .render(sb, new Vector2(player.getX(), player.getY()));
        player  .render(sb);
        sb.end();

        // 4) unbind
        grayscaleShader.end();
        sb.setShader(null);
    }


    private void renderDeathState(float dt) {
        deathTimer += dt;
        float grayscaleIntensity = Math.min(deathTimer / deathAnimDuration, 1f);

        // 1) bind & set the uniform
        grayscaleShader.begin();
        grayscaleShader.setUniformf("u_grayscaleFactor", grayscaleIntensity);

        // 2) draw your map + entities
        map.renderer.getBatch().setShader(grayscaleShader);
        map.renderBase();
        map.renderTreeTop(player, 90f, map.isCellTreeTop((int)player.getX(), (int)player.getY()));

        batch.setProjectionMatrix(cam.combined);
        batch.setShader(grayscaleShader);
        batch.begin();
        goblins.forEach(g -> g.render(batch));
        // ... etc ...
        player.render(batch);
        batch.end();

        // 3) unbind
        grayscaleShader.end();
        batch.setShader(null);
    }


    // input handling

    private void handleInput(float dt) {

        upgradeMenu.handleInput();
        if (upgradeMenu.isVisible()) return;

        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) hud.nextSlot();
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT )) hud.prevSlot();

        for (int i = 0; i < 4; i++)
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i))
                hud.setSelectedSlot(i);

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) useSelectedItem();
    }

   // eventbus callback
    @Override
    public void onEvent(GameEvent e) {
        GameEventType t = e.getType();
        switch (t) {
            case WAVE_COUNTDOWN:
                npc.setPointerVisible(false);
                int n = e.getPayload();
                countdown.showNumber(n);
                break;
            case UPGRADE_MENU_REQUEST:
                int wave = 0;
                if (e.getPayload() != null) wave = e.getPayload();
                if (wave >= 1 && wave < 7 && !upgradeMenu.isVisible()) {
                    upgradeMenu.show(player, wave);
                }
                break;
            case UPGRADE_SELECTED:
                waveManager.startNextWave();
                break;
            case WAVE_START_REQUEST:
                waveManager.startNextWave();
                break;
        }
    }


      // world and hud printing helpers // =======
    private void updateLoot() {
        for (Iterator<GoldBag> it = loot.iterator(); it.hasNext();) {
            GoldBag g = it.next();
            if (g.isCollectedBy(player)) { player.addGold(1); it.remove(); }
        }
        goldUI.setGold(player.getGold());
    }

    private void updateCamera(float dt) {
        float lerp = .1f;
        cam.position.x = MathUtils.lerp(cam.position.x, player.getX(), lerp);
        cam.position.y = MathUtils.lerp(cam.position.y, player.getY(), lerp);

        float hw = cam.viewportWidth / 2f,
            hh = cam.viewportHeight / 2f;
        cam.position.x = MathUtils.clamp(cam.position.x, hw, map.getMapWidth()  - hw);
        cam.position.y = MathUtils.clamp(cam.position.y, hh, map.getMapHeight() - hh);
        cam.update();
    }

    private void renderWorld() {
        map.renderBase(); // layer printing

        //bushes
        map.renderBush(player, 50f, player.isInBush());

        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        // render queue
        loot.forEach(g -> g.render(batch));
        goblins.forEach(g -> g.render(batch));
        dynas.forEach(d -> d.render(batch));
        barrels.forEach(b -> b.render(batch));


        npc.render(batch, new Vector2(player.getX(), player.getY()));

        player.render(batch);

        batch.end();

        map.renderTreeTop(player, 90f, map.isCellTreeTop((int)player.getX(), (int)player.getY()));

    }


    private void renderHUD() {
        float dt = Gdx.graphics.getDeltaTime();
        hud.drawHUD(player);
        goldUI.draw();
        waveUI.setWave(waveManager.getCurrentWave() + 1);
        waveUI.draw();
        batch.setProjectionMatrix(cam.combined);
        batch.begin();


        HealthBarRenderer.drawBar(batch,
            player.getX(),
            player.getY() + player.getSpriteHeight()/2f + .25f,
            player.getHealth() / player.getMaxHealth(),
            false);

        // goblin health bars
        for (Goblin g : goblins) {
            if (!g.isDead() && !g.isDying())
                HealthBarRenderer.drawBar(batch,
                    g.getX(), g.getY() + .8f,
                    g.getHealth() / g.getMaxHealth(), true);
        }


        overlayStage.act(dt);
        overlayStage.draw();

        batch.end();
    }


    private void useSelectedItem() {
        int idx = hud.getSelectedSlot();
        if (idx < player.getInventory().getItems().size()) {
            player.getInventory().getItems().get(idx).use(player);
            if (idx >= player.getInventory().getItems().size())
                hud.setSelectedSlot(Math.max(0, player.getInventory().getItems().size() - 1));
        }
    }

    //dispose
    @Override
    public void dispose() {
        upgradeMenu.dispose();
        EventBus.unregister(this);

        batch.dispose();
        map.dispose();
        hud.dispose();
        goldUI.dispose();
        waveUI.dispose();

        player.dispose();
        npc.dispose();
        goblins .forEach(Goblin        ::dispose);
        dynas   .forEach(DynamiteGoblin::dispose);
        barrels .forEach(BarrelBomber  ::dispose);

        if (grayscaleShader != null) grayscaleShader.dispose();
        Fonts.dispose();
    }


    // Getters

    public List<Goblin>         getGoblins()  { return goblins;  }
    public List<DynamiteGoblin> getDynaList() { return dynas;    }
    public List<BarrelBomber>   getBarrels()  { return barrels;  }
    public List<GoldBag>        getLoot()     { return loot;     }
    public Player               getPlayer()   { return player;   }
    public NPC                  getNpc()        { return npc; }
    public TileMapRenderer      getMap()      { return map;      }
    public CameraShake getCameraShake()       { return cameraShake;}
    public boolean isIntervalActive() { return WaveManager.isIntervalActive();}
    public WaveManager getWaveManager() {return waveManager;}
    public UpgradeMenu getUpgradeMenu() {return upgradeMenu;}
}

