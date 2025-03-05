package io.github.some_example_name;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.graphics.OrthographicCamera;

public class TileMapRenderer {
    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;
    private OrthographicCamera camera;

    // 32 tile x 32 tile, her tile 32x32 piksel, unitScale = 1/32f olduğundan world unit olarak 32x32 olur.
    private float mapWidth, mapHeight;
    private final float unitScale = 1f / 32f;

    public TileMapRenderer(OrthographicCamera camera) {
        this.camera = camera;
        // Tilemap dosyanızı yükleyin (Tiled'de width=32, height=32, tilewidth=32, tileheight=32 olduğundan emin olun)
        map = new TmxMapLoader().load("maps/tileset.tmx");

        // World biriminde harita boyutu: 32x32
        mapWidth = 32f;
        mapHeight = 32f;

        renderer = new OrthogonalTiledMapRenderer(map, unitScale);
    }

    public void render() {
        renderer.setView(camera);
        renderer.render();
    }

    public float getMapWidth() {
        return mapWidth;
    }

    public float getMapHeight() {
        return mapHeight;
    }

    public void dispose() {
        map.dispose();
        renderer.dispose();
    }
}
