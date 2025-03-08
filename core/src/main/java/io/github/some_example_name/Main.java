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

    // world sizes
    private float mapWidth, mapHeight;

    private static final float TREE_FADE_RADIUS = 1.2f;
    private static final float BUSH_FADE_RADIUS = 0.6f;
    @Override
    public void create() {
        batch = new SpriteBatch();

        // custom cursor
        Pixmap cursorPixmap = new Pixmap(Gdx.files.internal("cursor.png"));
        int hotspotX = 0, hotspotY = 0;
        Cursor customCursor = Gdx.graphics.newCursor(cursorPixmap, hotspotX, hotspotY);
        Gdx.graphics.setCursor(customCursor);
        cursorPixmap.dispose();

        // 16:9 camera creation
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 16, 9);
        camera.update();

        // default map loading
        tileMapRenderer = new TileMapRenderer(camera);
        mapWidth = tileMapRenderer.getMapWidth();
        mapHeight = tileMapRenderer.getMapHeight();

        // player and goblin create
        player = new Player(camera, tileMapRenderer);
        player.setPosition(16, 9);
        goblin = new Goblin(camera, player, 10, 10, 8, 12, 8, 12);

        // giving player reference to the goblin
        player.setTargetGoblin(goblin);
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        // map change with M key ( wip )
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.M)) {
            // close current map renderer
            tileMapRenderer.dispose();
            // new map loading
            tileMapRenderer = new TileMapRenderer(camera, "maps/another_map.tmx");
            mapWidth = tileMapRenderer.getMapWidth();
            mapHeight = tileMapRenderer.getMapHeight();
            // player renderer reference
            player.setTileMapRenderer(tileMapRenderer);
        }

        // game objects update
        player.update(delta);
        goblin.update(delta);
        updateCamera(delta);

        // screen clear
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Base layers render (Ground, Collision, Bush)
        tileMapRenderer.renderBaseLayers(new int[]{0, 1});

        // sprite rendering (goblin before , after player)
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        goblin.render(batch);
        player.render(batch);
        batch.end();

        // health bar render
        batch.begin();
        goblin.renderHealthBar(batch);
        player.renderHealthBar(batch);
        batch.end();

        // TreeTop layer with shader render
        tileMapRenderer.renderBushWithShader(player, BUSH_FADE_RADIUS);

        // after tree top layer render with shader
        tileMapRenderer.renderTreeTopWithShader(player, TREE_FADE_RADIUS);
    }

    // follow camera with player
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
