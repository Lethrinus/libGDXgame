package io.github.some_example_name;


import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Player player;
    private Goblin goblin;
    private TileMapRenderer tileMapRenderer;
    private OrthographicCamera camera;
    private Texture bushOverlayTexture;

    private float overlayAlpha = 0f;
    private final float targetOverlayAlpha = 0.3f;
    private final float overlayTransitionSpeed = 1f;
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


        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0, 0, 0, 1));
        pixmap.fill();
        bushOverlayTexture = new Texture(pixmap);
        pixmap.dispose();


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

        if (player.isInBush()) {
            tileMapRenderer.renderBushWithShader(player, BUSH_FADE_RADIUS);
        } else {
            // Eğer bush layer arka planı gereksiz yere kaplıyorsa, renderlamayı devre dışı bırakabilirsiniz.
            // Alternatif olarak bush layer dokularının alfa değerlerini ayarlayabilirsiniz.
            tileMapRenderer.renderBaseLayers(new int[]{2});
        }
        tileMapRenderer.renderTreeTopWithShader(player, TREE_FADE_RADIUS);

        // Overlay alfa geçişi: oyuncu çalıdaysa artıyor, değilse azalıyor
        if (player.isInBush()) {
            overlayAlpha = Math.min(overlayAlpha + overlayTransitionSpeed * delta, targetOverlayAlpha);
        } else {
            overlayAlpha = Math.max(overlayAlpha - overlayTransitionSpeed * delta, 0f);
        }


        // Overlay çizimi: overlayAlpha > 0 ise tüm ekranı kaplar
        if (overlayAlpha > 0f) {
            batch.begin();
            Color previousColor = batch.getColor();
            batch.setColor(0, 0, 0, overlayAlpha);
            batch.draw(bushOverlayTexture,
                camera.position.x - camera.viewportWidth / 2,
                camera.position.y - camera.viewportHeight / 2,
                camera.viewportWidth, camera.viewportHeight);
            batch.setColor(previousColor);
            batch.end();
        }

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
        bushOverlayTexture.dispose();
        tileMapRenderer.dispose();
    }
}
