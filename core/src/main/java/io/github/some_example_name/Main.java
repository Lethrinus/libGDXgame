package io.github.some_example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Player player;
    private Goblin goblin;
    private TileMapRenderer tileMapRenderer;
    private OrthographicCamera camera;

    // World dimensions
    private float mapWidth, mapHeight;

    // Radius for the partial fade effect on the TreeTop layer
    private static final float TREE_FADE_RADIUS = 1.2f;

    @Override
    public void create() {
        batch = new SpriteBatch();

        Pixmap pixmap = new Pixmap(Gdx.files.internal("cursor.png"));
        int hotspotX = 0;
        int hotspotY = 0;
        Cursor customCursor = Gdx.graphics.newCursor(pixmap, hotspotX, hotspotY);
        Gdx.graphics.setCursor(customCursor);

        // Create a 16x9 aspect ratio camera
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 16, 9);
        camera.update();

        // Load the tile map
        tileMapRenderer = new TileMapRenderer(camera);
        mapWidth = tileMapRenderer.getMapWidth();
        mapHeight = tileMapRenderer.getMapHeight();

        // Create the player and set its starting position (center of the map)
        player = new Player(camera, tileMapRenderer);
        player.setPosition(16, 9);

        // Create a goblin that patrols within a designated area
        goblin = new Goblin(camera, player,
            10, 10,  // starting position
            8, 12, 8, 12); // patrol boundaries
        pixmap.dispose();
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        // Update game objects
        player.update(delta);
        goblin.update(delta);
        updateCamera(delta);

        // Clear the screen
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Render base layers (Ground and Collision layers: indices 0 and 1)
        tileMapRenderer.renderBaseLayers(new int[]{0, 1});

        // Render the player and goblin
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        player.render(batch);
        goblin.render(batch);
        batch.end();

        // Render the TreeTop layer with a circle fade shader (layer index 2)
        tileMapRenderer.renderTreeTopWithShader(player, TREE_FADE_RADIUS);
    }

    private void updateCamera(float delta) {
        float smoothing = 0.03f;
        float targetX = player.getX();
        float targetY = player.getY();

        camera.position.x = MathUtils.lerp(camera.position.x, targetX, smoothing);
        camera.position.y = MathUtils.lerp(camera.position.y, targetY, smoothing);

        float halfW = camera.viewportWidth / 2f;
        float halfH = camera.viewportHeight / 2f;

        camera.position.x = MathUtils.clamp(camera.position.x, halfW, mapWidth - halfW);
        camera.position.y = MathUtils.clamp(camera.position.y, halfH, mapHeight - halfH);

        camera.update();
    }

    @Override
    public void dispose() {
        batch.dispose();
        goblin.dispose();
        tileMapRenderer.dispose();
    }
}
