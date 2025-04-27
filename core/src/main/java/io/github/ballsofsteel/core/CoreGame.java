package io.github.ballsofsteel.core;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import io.github.ballsofsteel.entity.*;
import io.github.ballsofsteel.events.EventBus;
import io.github.ballsofsteel.events.GameEvent;
import io.github.ballsofsteel.events.GameEventListener;
import io.github.ballsofsteel.events.GameEventType;
import io.github.ballsofsteel.factory.GameEntityFactory;
import io.github.ballsofsteel.ui.*;
import io.github.ballsofsteel.ui.UpgradeMenu;

import java.util.*;

/**
 * Ana oyun sınıfı – tüm döngü, HUD, dalga ve upgrade menüsü yönetimi.
 */
public class CoreGame extends ApplicationAdapter implements GameEventListener {

    /* ---------- render & kamera ---------- */
    private SpriteBatch batch;
    private final OrthographicCamera cam = new OrthographicCamera();
    private TileMapRenderer map;

    /* ---------- varlık listeleri ---------- */
    private Player player;
    private NPC    npc;
    private final List<Goblin>         goblins  = new ArrayList<>();
    private final List<DynamiteGoblin> dynas    = new ArrayList<>();
    private final List<BarrelBomber>   barrels  = new ArrayList<>();
    private final List<GoldBag>        loot     = new ArrayList<>();

    /* ---------- HUD & upgrade menüsü ---------- */
    private InventoryHUD hud;
    private GoldCounterUI goldUI;
    private UpgradeMenu upgradeMenu;

    /* ---------- factory & dalga yöneticisi ---------- */
    private final GameEntityFactory factory = new GameEntityFactory();
    private WaveManager waveManager;

    /* ---------------------------------------------------------------------- */
    /*  create                                                                */
    /* ---------------------------------------------------------------------- */
    @Override
    public void create() {

        batch = new SpriteBatch();
        cam.setToOrtho(false, 16, 9);

        map    = new TileMapRenderer(cam, "maps/tileset2.tmx");
        player = factory.createPlayer(this, cam, map, 8, 4.5f);

        /* NPC – diyalog & yükseltme tetikleyici */
        npc = factory.createNPC(this,5, 5, new String[]{
            "Hello, brave hero!",
            "I am the Goblin Slayer!",
            "I can help you upgrade your skills.",
            "Just press 'E' to talk to me.",
            "I can also give you some gold."
        });

        /* HUD */
        hud = new InventoryHUD();
        hud.initializeCamera(1200, 800);
        hud.loadTextures();
        goldUI = new GoldCounterUI(hud.getCamera());

        /* Upgrade menüsü & dalga yöneticisi */
        upgradeMenu = new UpgradeMenu();
        waveManager = new WaveManager(this, factory);

        /* Event dinleme */
        EventBus.register(this);

        /* Scroll ile HUD slot geçişi */
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override public boolean scrolled(float dx, float dy) {
                if (dy > 0) hud.nextSlot(); else if (dy < 0) hud.prevSlot();
                return true;
            }
        });

        Gdx.gl.glClearColor(0, 0, 0, 1);
    }

    /* ---------------------------------------------------------------------- */
    /*  render (ana döngü)                                                    */
    /* ---------------------------------------------------------------------- */
    @Override
    public void render() {

        float dt = Gdx.graphics.getDeltaTime();

        handleInput(dt);

        /* Menü açıkken oyun güncellenmez (pause) */
        if (!upgradeMenu.isVisible()) {

            /* --- güncelleme --- */
            player.update(dt);
            npc.update(dt, new Vector2(player.getX(), player.getY()));

            goblins.forEach(g -> g.update(dt));
            dynas  .removeIf(d -> { d.update(dt); return d.isDead();     });
            barrels.removeIf(b -> { b.update(dt); return b.isFinished(); });

            updateLoot();
            waveManager.update(dt);
            updateCamera(dt);
        }

        /* --- çizim --- */
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderWorld();
        renderHUD();
        upgradeMenu.render(batch);   // menü daima en üstte
    }

    /* ---------------------------------------------------------------------- */
    /*  handleInput                                                           */
    /* ---------------------------------------------------------------------- */
    private void handleInput(float dt) {

        upgradeMenu.handleInput();            // menü tuşları
        if (upgradeMenu.isVisible()) return;  // menü açıksa başka giriş yok

        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) hud.nextSlot();
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT )) hud.prevSlot();

        for (int i = 0; i < 4; i++)
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i))
                hud.setSelectedSlot(i);

        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) useSelectedItem();
    }

    /* ---------------------------------------------------------------------- */
    /*  EventBus callback                                                     */
    /* ---------------------------------------------------------------------- */
    @Override
    public void onEvent(GameEvent e) {

        GameEventType t = e.getType();

        if (t == GameEventType.UPGRADE_MENU_REQUEST) {         // NPC → menü
            upgradeMenu.show(player);

        } else if (t == GameEventType.UPGRADE_SELECTED) {      // seçim bitti
            waveManager.startNextWave();
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Dünya & HUD çizimi yardımcıları                                       */
    /* ---------------------------------------------------------------------- */
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

        /* zemin + çarpışma katmanları */
        map.renderBaseLayers(new int[]{0, 1});

        /* çalı gölgesi */
        if (player.isInBush()) map.renderBushWithShader(player, 50f);
        else                    map.renderBushNoShader();

        /* goblin (ağaç altı) */
        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        goblins.forEach(g -> g.render(batch));
        batch.end();

        /* ağaç tepeleri */
        if (map.isCellTreeTop((int) player.getX(), (int) player.getY()))
            map.renderTreeTopWithShader(player, 90f);
        else map.renderTreeTopNoShader();

        /* oyuncu + diğer varlıklar */
        batch.begin();
        player.render(batch);
        npc.render(batch);
        barrels.forEach(b -> b.render(batch));
        dynas  .forEach(d -> d.render(batch));
        loot   .forEach(g -> g.render(batch));
        batch.end();
    }

    private void renderHUD() {

        hud.drawHUD(player);
        goldUI.draw();

        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        /* oyuncu can barı */
        HealthBarRenderer.drawBar(batch,
            player.getX(),
            player.getY() + player.getSpriteHeight()/2f + .25f,
            player.getHealth() / player.getMaxHealth(),
            false);

        /* goblin can barları */
        for (Goblin g : goblins) {
            if (!g.isDead() && !g.isDying())
                HealthBarRenderer.drawBar(batch,
                    g.getX(), g.getY() + .8f,
                    g.getHealth() / g.getMaxHealth(), true);
        }
        batch.end();
    }

    /* ---------------------------------------------------------------------- */
    /*  Envanter kullanımı                                                    */
    /* ---------------------------------------------------------------------- */
    private void useSelectedItem() {
        int idx = hud.getSelectedSlot();
        if (idx < player.getInventory().getItems().size()) {
            player.getInventory().getItems().get(idx).use(player);
            if (idx >= player.getInventory().getItems().size())
                hud.setSelectedSlot(Math.max(0, player.getInventory().getItems().size() - 1));
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  dispose                                                               */
    /* ---------------------------------------------------------------------- */
    @Override
    public void dispose() {
        upgradeMenu.dispose();
        EventBus.unregister(this);

        batch.dispose();
        map.dispose();
        hud.dispose();
        goldUI.dispose();

        player.dispose();
        npc.dispose();
        goblins .forEach(Goblin        ::dispose);
        dynas   .forEach(DynamiteGoblin::dispose);
        barrels .forEach(BarrelBomber  ::dispose);
    }

    /* ---------------------------------------------------------------------- */
    /*  Getter’lar                                                            */
    /* ---------------------------------------------------------------------- */
    public List<Goblin>         getGoblins()  { return goblins;  }
    public List<DynamiteGoblin> getDynaList() { return dynas;    }
    public List<BarrelBomber>   getBarrels()  { return barrels;  }
    public List<GoldBag>        getLoot()     { return loot;     }
    public Player               getPlayer()   { return player;   }
    public TileMapRenderer      getMap()      { return map;      }

    public void addGoblin(Goblin g) { goblins.add(g); }
}
