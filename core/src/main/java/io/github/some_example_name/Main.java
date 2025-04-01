package io.github.some_example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Player player;
    private Goblin goblin;
    private NPC npc;
    private TileMapRenderer tileMapRenderer;
    private OrthographicCamera camera;
    private InventoryHUD hud;

    // Health bar renderers for player and goblin.
    private HealthBarRenderer healthBarRendererPlayer;
    // Use enemy mode for goblin so that its health bar uses a green-to-yellow gradient.
    private HealthBarRenderer healthBarRendererGoblin;

    // Other fields (textures, overlay, etc.) are omitted for brevity.

    @Override
    public void create() {
        batch = new SpriteBatch();

        // Set up camera: 16x9 world units.
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 16, 9);
        camera.update();

        // Load tile map.
        tileMapRenderer = new TileMapRenderer(camera, "maps/tileset.tmx");

        // Initialize the entity factory (using Factory Pattern already implemented).
        EntityFactory entityFactory = new GameEntityFactory();

        // Create player using the factory.
        player = entityFactory.createPlayer(camera, tileMapRenderer, 8, 4.5f);

        // Set up HUD.
        hud = new InventoryHUD();
        hud.initializeCamera(1200, 800);
        hud.loadTextures();

        // Create goblin using the factory (which now uses Strategy Pattern for its AI).
        ArrayList<Vector2> waypoints = new ArrayList<>();
        waypoints.add(new Vector2(5, 5));
        waypoints.add(new Vector2(5, 10));
        waypoints.add(new Vector2(10, 10));
        waypoints.add(new Vector2(10, 5));
        goblin = entityFactory.createPatrollingGoblin(player, 11, 4.5f, 8, 12, 3, 6, waypoints);
        player.setTargetGoblin(goblin);

        // Create NPC using the factory.
        String[] npcLines = { "hello adventurer!", "be careful out there.", "press e to talk." };
        npc = entityFactory.createNPC(5, 5, npcLines);

        // Initialize health bar renderers.
        healthBarRendererPlayer = new HealthBarRenderer(camera);
        // For the goblin, use the enemy mode to avoid red colors.
        healthBarRendererGoblin = new HealthBarRenderer(camera, true);

        // Optional: Set input processor for HUD slot switching.
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

        // Other initialization (overlay textures, whiteTexture, etc.) would go here.
        Gdx.gl.glClearColor(0, 0, 0, 1);
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        handleInput();

        // Update game objects.
        player.update(delta);
        goblin.update(delta);
        npc.update(delta, new Vector2(player.getX(), player.getY()));
        updateCamera(delta);

        // Clear screen.
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Render world layers and entities.
        renderWorld();

        // Render HUD elements.
        renderHUD(delta);
    }

    private void renderHUD(float delta) {
        // Render inventory HUD.
        hud.drawHUD(player);

        // Render dash cooldown (code omitted for brevity).
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        renderDashCooldown(batch);
        batch.end();

        // Render health bars.
        renderHealthBars(delta);
    }

    private void renderHealthBars(float delta) {
        // Render player health bar.
        healthBarRendererPlayer.render(
            player.getX(),
            player.getY() + player.getSpriteHeight() / 2f + 0.2f,
            1f, 0.1f,
            player.getHealth(),
            player.getMaxHealth(),
            delta
        );

        // Render goblin health bar only if the goblin is alive (using new strategy-based getters).
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

    private void renderDashCooldown(SpriteBatch batch) {
        // Your dash cooldown rendering code goes here.
    }

    private void renderWorld() {
        // Render base layers.
        tileMapRenderer.renderBaseLayers(new int[]{0, 1});

        // Render bush layers (with or without shader based on player state).
        if (player.isInBush()) {
            tileMapRenderer.renderBushWithShader(player, 50f);
        } else {
            tileMapRenderer.renderBushNoShader();
        }

        // Render goblin.
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        goblin.render(batch);
        batch.end();

        // Render tree tops.
        if (tileMapRenderer.isCellTreeTop((int) player.getX(), (int) player.getY())) {
            tileMapRenderer.renderTreeTopWithShader(player, 90f);
        } else {
            tileMapRenderer.renderTreeTopNoShader();
        }

        // Render player and NPC.
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        player.render(batch);
        npc.render(batch);
        batch.end();

        // Optional: Render bush overlay if applicable.
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
        // Handle inventory selection.
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            hud.nextSlot();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            hud.prevSlot();
        }
        // Directly select inventory slots with number keys.
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
        // Use the selected item.
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            useSelectedItem();
        }
    }

    private void useSelectedItem() {
        int slotIndex = hud.getSelectedSlot();
        if (slotIndex < player.getInventory().getItems().size()) {
            Item item = player.getInventory().getItems().get(slotIndex);
            if (item != null) {
                item.use(player);
                if (hud.getSelectedSlot() >= player.getInventory().getItems().size()) {
                    hud.setSelectedSlot(Math.max(0, player.getInventory().getItems().size() - 1));
                }
            }
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        hud.dispose();
        player.dispose();
        goblin.dispose();
        npc.dispose();
        tileMapRenderer.dispose();
        healthBarRendererPlayer.dispose();
        healthBarRendererGoblin.dispose();
    }
}
