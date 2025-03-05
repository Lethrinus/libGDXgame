package io.github.some_example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Player player;
    private TileMapRenderer tileMapRenderer;
    private OrthographicCamera camera;
    private BitmapFont font;

    // Harita boyutları (world unit olarak): 32×32
    private float mapWidth, mapHeight;

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();

        // Kamerayı oluşturun. Örneğin, 16x9 world unit viewport (görüntü alanı).
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 16, 9);

        // TileMapRenderer ile haritayı yükleyip boyutları alın.
        tileMapRenderer = new TileMapRenderer(camera);
        mapWidth = tileMapRenderer.getMapWidth();   // 32
        mapHeight = tileMapRenderer.getMapHeight(); // 32

        // Oyuncuyu oluşturun ve haritanın ortasına yerleştirin (16,16).
        player = new Player();
        player.setPosition(mapWidth / 2f, mapHeight / 2f);
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        // Oyuncuyu güncelle (animasyon ve giriş dahil).
        player.update(delta);

        // Kamerayı güncelle: oyuncuyu takip et, fakat kamera harita sınırlarına ulaştığında duracak.
        updateCamera(delta);

        // Ekranı temizle.
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // SpriteBatch, kamera projeksiyonunu kullansın.
        batch.setProjectionMatrix(camera.combined);

        // Tilemap'i renderla.
        tileMapRenderer.render();

        // Oyuncu ve FPS sayacını renderla.
        batch.begin();
        player.render(batch);
        font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(),
            camera.position.x - camera.viewportWidth / 2 + 10,
            camera.position.y + camera.viewportHeight / 2 - 10);
        batch.end();
    }

    /**
     * Kamera, oyuncuyu takip eder ancak tilemap sınırına geldiğinde kamera sabit kalır.
     * Oyuncu sınırı aşsa bile kamera yer değiştirmez.
     */
    private void updateCamera(float delta) {
        float smoothing = 0.15f;
        // Önce oyuncunun pozisyonuna doğru yumuşak geçiş (lerp) yap.
        float targetX = player.getX();
        float targetY = player.getY();
        camera.position.x = MathUtils.lerp(camera.position.x, targetX, smoothing);
        camera.position.y = MathUtils.lerp(camera.position.y, targetY, smoothing);

        // Kamera sınırları: viewport yarısı kadar kenardan içeride kalmalı.
        float halfW = camera.viewportWidth / 2f;
        float halfH = camera.viewportHeight / 2f;
        camera.position.x = MathUtils.clamp(camera.position.x, halfW, mapWidth - halfW);
        camera.position.y = MathUtils.clamp(camera.position.y, halfH, mapHeight - halfH);

        camera.update();
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        player.dispose();
        tileMapRenderer.dispose();
    }
}
