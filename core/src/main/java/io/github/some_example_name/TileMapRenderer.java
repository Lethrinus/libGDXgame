package io.github.some_example_name;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;

/**
 * Responsible for loading and rendering the Tiled map,
 * including the "tree top" layer with the circle shader.
 */
public class TileMapRenderer {
    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;
    private OrthographicCamera camera;

    private float mapWidth, mapHeight;
    private final float unitScale = 1f / 32f;

    // We'll create the circleShader once:
    private ShaderProgram circleShader;

    public TileMapRenderer(OrthographicCamera camera) {
        this.camera = camera;

        // Load your Tiled map (3 layers: 0=Ground,1=Collision,2=TreeTop)
        map = new TmxMapLoader().load("maps/tileset.tmx");

        // Suppose 32×32 in world units
        mapWidth = 32f;
        mapHeight = 32f;

        // Create the orthogonal renderer
        renderer = new OrthogonalTiledMapRenderer(map, unitScale);
    }

    /**
     * Render layers (e.g. {0,1}) normally (no special alpha).
     */
    public void renderBaseLayers(int[] layerIndices) {
        renderer.setView(camera);
        renderer.render(layerIndices);
    }

    /**
     * Renders the top layer (index=2) using a circle fade shader.
     *  - If dist < radius => alpha=0.5
     *  - else alpha=1.0
     */
    public void renderTreeTopWithShader(Player player, float radius) {
        // We assume layer=2 => "TreeTop"
        int[] layers = new int[]{2};

        // Lazy init the circleShader
        if (circleShader == null) {
            circleShader = ShaderManager.createCircleShader();
        }

        Batch batch = renderer.getBatch();
        // Use the circle shader
        batch.setShader(circleShader);

        // Pass uniforms
        circleShader.bind();
        circleShader.setUniformf("u_playerPos", player.getX(), player.getY());
        circleShader.setUniformf("u_radius", radius);

        // Render
        renderer.setView(camera);
        renderer.render(layers);

        // Reset
        batch.setShader(null);
    }

    /**
     * For collisions with layer=1 (Collision).
     * Now we only check the single tile at (x,y).
     * The Player code will call this for each relevant tile in bounding box.
     */
    public boolean isCellBlocked(int tileX, int tileY) {
        // Out of bounds => block
        if(tileX < 0 || tileY < 0 || tileX >= mapWidth || tileY >= mapHeight) {
            return true;
        }

        // layer=1 => collision
        TiledMapTileLayer collisionLayer = (TiledMapTileLayer) map.getLayers().get(1);
        if (collisionLayer == null) return false;

        Cell cell = collisionLayer.getCell(tileX, tileY);
        if(cell == null) return false;

        TiledMapTile tile = cell.getTile();
        if(tile == null) return false;

        return tile.getProperties().containsKey("blocked");
    }

    public float getMapWidth() {
        return mapWidth;
    }

    public float getMapHeight() {
        return mapHeight;
    }

    public void dispose() {
        // Dispose resources
        if (circleShader != null) circleShader.dispose();
        renderer.dispose();
        map.dispose();
    }
}
