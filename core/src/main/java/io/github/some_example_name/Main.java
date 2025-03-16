package io.github.some_example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

/**
 * Main game class.
 * Features:
 *  - Gradual fade overlay when the player is in a bush.
 *  - Conditional tree top shader if the player is under a tree tile.
 *  - A larger dash cooldown bar in orange.
 *  - Enhanced health bar rendering.
 */
public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Player player;
    private Goblin goblin;
    private NPC npc;
    private TileMapRenderer tileMapRenderer;
    private OrthographicCamera camera;
    private InventoryHUD hud;
    // Overlay for bush dim effect.
    private Texture overlayTexture;
    private float overlayAlpha = 0f;          // Current alpha value.
    private final float overlayTarget = 0.3f;   // Maximum alpha value.
    private final float overlayFadeSpeed = 1f;  // Fade speed.

    // Health bar renderers for player and goblin.
    private HealthBarRenderer healthBarRendererPlayer;
    private HealthBarRenderer healthBarRendererGoblin;
    // White texture used for dash cooldown bar.
    private Texture whiteTexture;

    @Override
    public void create() {
        batch = new SpriteBatch();

        // Optional custom cursor.
        Pixmap cursorPixmap = new Pixmap(Gdx.files.internal("HUD/cursor.png"));
        Cursor customCursor = Gdx.graphics.newCursor(cursorPixmap, 0, 0);
        Gdx.graphics.setCursor(customCursor);
        cursorPixmap.dispose();

        // Initialize HUD.
        hud = new InventoryHUD();
        hud.initializeCamera(1200, 800);
        hud.loadTextures();


        // Set up camera: 16x9 world units.
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 16, 9);
        camera.update();

        // Load tile map.
        tileMapRenderer = new TileMapRenderer(camera, "maps/tileset.tmx");

        // Create player at center.
        player = new Player(camera, tileMapRenderer);
        player.setPosition(8, 4.5f);
        // Add items to player's inventory.
        Texture meatTexture = new Texture("HUD/meat.png");
        player.getInventory().addItem(new MeatItem("Meat", meatTexture, 25f));
        player.getInventory().addItem(new MeatItem("Meat", meatTexture, 25f));

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                if (amountY > 0) {
                    hud.nextSlot();
                } else if (amountY < 0) {
                    hud.prevSlot();
                }
                return true;
            }
        });

        // Create goblin enemy.
        goblin = new Goblin(player, 11, 4.5f, 8, 12, 3, 6);
        player.setTargetGoblin(goblin);
        // goblin waypoints
        ArrayList<Vector2> waypoints = new ArrayList<>();
        waypoints.add(new Vector2(5, 5));
        waypoints.add(new Vector2(5, 10));
        waypoints.add(new Vector2(10, 10));
        waypoints.add(new Vector2(10, 5));
        goblin.setPatrolWaypoints(waypoints);
        this.goblin = goblin;

        String[] npcLines = {
            "hello adventurer!",
            "be careful out there.",
            "press e to talk."
        };
        npc = new NPC(5, 5, npcLines);

        // Create overlay texture for bush fade effect.
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        overlayTexture = new Texture(pixmap);
        pixmap.dispose();

        // Initialize health bar renderers.
        healthBarRendererPlayer = new HealthBarRenderer(camera);
        healthBarRendererGoblin = new HealthBarRenderer(camera);

        // Create a white texture for dash cooldown bar.
        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(Color.WHITE);
        pix.fill();
        whiteTexture = new Texture(pix);
        pix.dispose();

        Gdx.gl.glClearColor(0, 0, 0, 1);
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        handleInput();

        // Update game logic.
        player.update(delta);
        goblin.update(delta);
        npc.update(delta, new Vector2(player.getX(), player.getY()));
        updateCamera(delta);

        // Clear screen.
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Render map layers and entities
        renderWorld();

        // Render HUD elements
        renderHUD(delta);
    }

    private void renderWorld() {
        // 1) Render base layers (e.g., ground, collision layers).
        tileMapRenderer.renderBaseLayers(new int[]{0, 1});

        // 2) Render bush layer (with shader if player is in a bush, else normally).
        if (player.isInBush()) {
            tileMapRenderer.renderBushWithShader(player, 50f);
        } else {
            tileMapRenderer.renderBushNoShader();
        }

        // Gradual fade for bush overlay.
        if (player.isInBush()) {
            overlayAlpha = Math.min(overlayAlpha + overlayFadeSpeed * Gdx.graphics.getDeltaTime(), overlayTarget);
        } else {
            overlayAlpha = Math.max(overlayAlpha - overlayFadeSpeed * Gdx.graphics.getDeltaTime(), 0f);
        }

        // 3) Render goblin (so that tree tops can cover it).
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        goblin.render(batch);
        batch.end();

        // 4) Check if the player is under a tree top tile and render accordingly.
        if (tileMapRenderer.isCellTreeTop((int)player.getX(), (int)player.getY())) {
            tileMapRenderer.renderTreeTopWithShader(player, 90f);
        } else {
            tileMapRenderer.renderTreeTopNoShader();
        }

        // 5) Render player and NPC.
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        player.render(batch);
        npc.render(batch);
        batch.end();

        // 6) Render bush overlay if applicable.
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
    }

    private void renderHUD(float delta) {
        // Render inventory HUD
        hud.drawHUD(player);

        // Render dash cooldown
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        renderDashCooldown(batch);
        batch.end();

        // Render health bars
        renderHealthBars(delta);
    }

    private void renderHealthBars(float delta) {
        // Player health bar
        healthBarRendererPlayer.render(
            player.getX(),
            player.getY() + player.getSpriteHeight() / 2f + 0.2f,
            1f, 0.1f,
            player.getHealth(),
            player.getMaxHealth(),
            delta
        );

        // Goblin health bar - only if alive and not dying
        if (!goblin.isDead() && !goblin.isDying()) {
            healthBarRendererGoblin.render(
                goblin.getX(),
                goblin.getY() + 0.7f,
                1f, 0.1f,
                goblin.getHealth(),
                goblin.getMaxHealth(),
                delta
            );
        }
    }

    /**
     * Renders a larger dash cooldown bar in orange.
     *
     * @param batch The SpriteBatch used for rendering.
     */
    private void renderDashCooldown(SpriteBatch batch) {
        float cooldown = player.getDashCooldown();
        float timer = player.getDashCooldownTimer();
        if (cooldown <= 0) return;

        float percent = 1f - (timer / cooldown);
        if (percent < 0f) percent = 0f;

        // Dash bar dimensions and position.
        float barW = 2f;
        float barH = 0.2f;
        float barX = camera.position.x - camera.viewportWidth / 2f + 0.2f;
        float barY = camera.position.y + camera.viewportHeight / 2f - 0.4f;

        // Background in gray.
        batch.setColor(Color.GRAY);
        batch.draw(whiteTexture, barX, barY, barW, barH);

        // Filled portion in maroon.
        batch.setColor(Color.MAROON);
        batch.draw(whiteTexture, barX, barY, barW * percent, barH);

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
        // Change inventory slot using left/right arrow keys.
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            hud.nextSlot();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            hud.prevSlot();
        }

        // Directly select inventory slot using number keys 1-4.
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

        // Use the item in the selected slot when the E key is pressed (e.g., food).
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            useSelectedItem();
        }
    }

    private void useSelectedItem() {
        int slotIndex = hud.getSelectedSlot();
        List<Item> items = player.getInventory().getItems();
        if (slotIndex < items.size()) {
            Item item = items.get(slotIndex);
            if (item != null) {
                item.use(player);
                // Adjust the selected slot if needed after using an item.
                if (hud.getSelectedSlot() >= items.size()) {
                    hud.setSelectedSlot(Math.max(0, items.size() - 1));
                }
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
        healthBarRendererPlayer.dispose();
        healthBarRendererGoblin.dispose();
        whiteTexture.dispose();
    }
}
