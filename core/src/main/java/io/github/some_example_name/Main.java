package io.github.some_example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Main game class with:
 *  - Gradual fade overlay when player is in a bush
 *  - Conditional tree top shader if player is under tree tile
 *  - Bigger dash bar in orange
 */
public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Player player;
    private Goblin goblin;
    private NPC npc;
    private TileMapRenderer tileMapRenderer;
    private OrthographicCamera camera;
    private InventoryHUD hud;
    // Overlay for bush dim effect
    private Texture overlayTexture;
    private float overlayAlpha = 0f;          // current alpha
    private final float overlayTarget = 0.3f; // max alpha
    private final float overlayFadeSpeed = 1f; // how fast it fades in/out

    @Override
    public void create() {
        batch = new SpriteBatch();

        // Optional custom cursor
        Pixmap cursorPixmap = new Pixmap(Gdx.files.internal("HUD/cursor.png"));
        Cursor customCursor = Gdx.graphics.newCursor(cursorPixmap, 0, 0);
        Gdx.graphics.setCursor(customCursor);
        cursorPixmap.dispose();
        //hud
        hud = new InventoryHUD();
        hud.initializeCamera(1200, 800);
        hud.loadTextures();
        // Camera: 16x9 world units
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 16, 9);
        camera.update();

        // Load tile map
        tileMapRenderer = new TileMapRenderer(camera, "maps/tileset.tmx");

        // Create player at center
        player = new Player(camera, tileMapRenderer);
        player.setPosition(8, 4.5f);
        Texture meatTexture = new Texture("HUD/meat.png");
        player.getInventory().addItem(new MeatItem("Meat",meatTexture, 25f));
        player.getInventory().addItem(new MeatItem("Meat",meatTexture, 25f));


        // Create goblin
        goblin = new Goblin(camera, player, 11, 4.5f, 8, 12, 3, 6);
        player.setTargetGoblin(goblin);

        String[] npcLines = {
            "hello adventurer!",
            "be careful out there.",
            "press e to talk."
        };
        npc = new NPC(5, 5, npcLines);

        // Create overlay texture for bush fade
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        overlayTexture = new Texture(pixmap);
        pixmap.dispose();

        Gdx.gl.glClearColor(0, 0, 0, 1);
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        handleInput();

        // Update logic
        player.update(delta);
        goblin.update(delta);
        npc.update(delta, new Vector2(player.getX(), player.getY()));
        updateCamera(delta);

        // Clear screen
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 1) Render base layers (Ground, Collision)
        tileMapRenderer.renderBaseLayers(new int[]{0, 1});

        // 2) Render Bush layer (shader if in bush, else normal)
        if (player.isInBush()) {
            tileMapRenderer.renderBushWithShader(player, 50f);
        } else {
            tileMapRenderer.renderBushNoShader();
        }

        // Gradual fade for bush overlay
        if (player.isInBush()) {
            overlayAlpha = Math.min(overlayAlpha + overlayFadeSpeed * delta, overlayTarget);
        } else {
            overlayAlpha = Math.max(overlayAlpha - overlayFadeSpeed * delta, 0f);
        }

        // 3) Render Goblin first (so tree top can cover it)
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        goblin.render(batch);
        batch.end();

        // 4) Check if player is under tree top tile
        if (tileMapRenderer.isCellTreeTop((int)player.getX(), (int)player.getY())) {
            // Use shader
            tileMapRenderer.renderTreeTopWithShader(player, 90f);
        } else {
            // Render tree top normally
            tileMapRenderer.renderTreeTopNoShader();
        }
        //hud
        if(Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            if(!player.getInventory().getItems().isEmpty()) {
                Item firstItem = player.getInventory().getItems().get(0);
                firstItem.use(player);
            }
        }
        // 5) Then render player and NPC
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        player.render(batch);
        npc.render(batch);
        batch.end();

        // 6) If overlayAlpha > 0, draw dim overlay for bush effect
        if (overlayAlpha > 0f) {
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            batch.setColor(0, 0, 0, overlayAlpha);
            batch.draw(overlayTexture,
                camera.position.x - camera.viewportWidth / 2,
                camera.position.y - camera.viewportHeight / 2,
                camera.viewportWidth,
                camera.viewportHeight);
            batch.setColor(Color.WHITE);
            batch.end();
        }

        // 7) Draw UI (health bars, dash bar)
        batch.setProjectionMatrix(camera.combined);
        hud.drawHUD(player);
        batch.begin();
        goblin.renderHealthBar(batch);
        player.renderHealthBar(batch);
        renderDashCooldown(batch);

        batch.end();
    }

    /**
     * Renders a bigger dash cooldown bar in orange.
     */
    private void renderDashCooldown(SpriteBatch batch) {
        float cooldown = player.getDashCooldown();
        float timer = player.getDashCooldownTimer();
        if (cooldown <= 0) return;

        float percent = 1f - (timer / cooldown);
        if (percent < 0f) percent = 0f;

        // Make it bigger and orange
        float barW = 2f;
        float barH = 0.2f;
        float barX = camera.position.x - camera.viewportWidth / 2f + 0.2f;
        float barY = camera.position.y + camera.viewportHeight / 2f - 0.4f;

        // Background in gray
        batch.setColor(Color.GRAY);
        batch.draw(player.getHealthBarTexture(), barX, barY, barW, barH);

        // Fill portion in orange
        batch.setColor(Color.MAROON);
        batch.draw(player.getHealthBarTexture(), barX, barY, barW * percent, barH);

        batch.setColor(Color.WHITE);
    }

    private void updateCamera(float delta) {
        float smoothing = 0.1f;
        float targetX = player.getX();
        float targetY = player.getY();
        camera.position.x = MathUtils.lerp(camera.position.x, targetX, smoothing);
        camera.position.y = MathUtils.lerp(camera.position.y, targetY, smoothing);

        float halfW = camera.viewportWidth / 2f;
        float halfH = camera.viewportHeight / 2f;
        camera.position.x = MathUtils.clamp(camera.position.x, halfW, tileMapRenderer.getMapWidth() - halfW);
        camera.position.y = MathUtils.clamp(camera.position.y, halfH, tileMapRenderer.getMapHeight() - halfH);

        camera.update();
    }
    private void handleInput() {
        // Sağ/sol ok tuşlarıyla slot değiştir
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            hud.nextSlot();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            hud.prevSlot();
        }

        // 1,2,3,4 tuşlarıyla doğrudan slot seçimi
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            hud.setSelectedSlot(0);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            hud.setSelectedSlot(1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            hud.setSelectedSlot(2);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
            hud.setSelectedSlot(3);
        }

        // E tuşuna basılırsa seçili slottaki item'i kullan (yemek)
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            useSelectedItem();
        }
    }

    private void useSelectedItem() {
        int slotIndex = hud.getSelectedSlot();
        // Envanterde yeterince item var mı diye bak
        if (slotIndex < player.getInventory().getItems().size()) {
            Item item = player.getInventory().getItems().get(slotIndex);
            if (item != null) {
                item.use(player);
                // MeatItem ise can artıracak ve kendini remove edecektir.
            }
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        overlayTexture.dispose();
        goblin.dispose();
        npc.dispose();
        player.dispose();
        hud.dispose();
        tileMapRenderer.dispose();
    }
}
