package io.github.some_example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Player player;
    private TileMapRenderer tileMapRenderer;
    private OrthographicCamera camera;

    private float mapWidth, mapHeight;

    // The radius for semi-transparency around the player's center
    private static final float TREE_FADE_RADIUS = 1.2f;

    @Override
    public void create() {
        batch = new SpriteBatch();

        // 16×9 in world coords
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 16, 9);

        tileMapRenderer = new TileMapRenderer(camera);
        mapWidth = tileMapRenderer.getMapWidth();
        mapHeight= tileMapRenderer.getMapHeight();

        // Player
        player = new Player(camera, tileMapRenderer);
        player.setPosition(16, 9); // near center
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        // update
        player.update(delta);
        updateCamera(delta);

        // clear
        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 1) Layers 0..1 => ground + collision
        tileMapRenderer.renderBaseLayers(new int[]{0,1});

        // 2) Draw player
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        player.render(batch);
        batch.end();

        // 3) "TreeTop" layer with circle shader
        tileMapRenderer.renderTreeTopWithShader(player, TREE_FADE_RADIUS);
    }

    private void updateCamera(float delta) {
        float smoothing = 0.15f;

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
        player.dispose();
        tileMapRenderer.dispose();
    }
}
