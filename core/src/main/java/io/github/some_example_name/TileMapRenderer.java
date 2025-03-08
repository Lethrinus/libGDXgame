package io.github.some_example_name;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;

public class TileMapRenderer {
    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;
    private OrthographicCamera camera;

    private float mapWidth, mapHeight;
    private final float unitScale = 1f / 32f;

    // Shader for circle fade on the TreeTop layer
    private ShaderProgram circleShader;

    public TileMapRenderer(OrthographicCamera camera) {
        this.camera = camera;

        // Load your Tiled map (3 layers: 0 = Ground, 1 = Collision, 2 = TreeTop)
        map = new TmxMapLoader().load("maps/tileset.tmx");

        // Assuming the map has 32 x 32 tiles in world units
        mapWidth = 32f;
        mapHeight = 32f;

        // Create the orthogonal renderer
        renderer = new OrthogonalTiledMapRenderer(map, unitScale);
    }

    /**
     * Render specified layers (e.g., layers 0 and 1) normally without special effects.
     */
    public void renderBaseLayers(int[] layerIndices) {
        renderer.setView(camera);
        renderer.render(layerIndices);
    }

    /**
     * Render the top layer (index = 2) using a circle fade shader.
     * The shader sets alpha to 0.5 inside a given radius around the player,
     * and 1.0 outside that radius.
     */
    public void renderTreeTopWithShader(Player player, float radius) {
        int[] layers = new int[]{2};

        // Lazy initialization of the circle shader
        if (circleShader == null) {
            circleShader = ShaderManager.createCircleShader();
        }

        Batch batch = renderer.getBatch();
        batch.setShader(circleShader);

        circleShader.bind();
        circleShader.setUniformf("u_playerPos", player.getX(), player.getY());
        circleShader.setUniformf("u_radius", radius);

        renderer.setView(camera);
        renderer.render(layers);

        batch.setShader(null);
    }

    /**
     * Checks collision on the collision layer (layer index 1) using the collision shape.
     * Instead of blocking the entire tile, it tests the player polygon against the collision shape
     * defined inside the tile (via a property on the collision object).
     *
     * @param tileX The x index of the tile.
     * @param tileY The y index of the tile.
     * @param playerPoly The player's collision area as a polygon.
     * @return True if a collision occurs, false otherwise.
     */
    public boolean isCellBlocked(int tileX, int tileY, Polygon playerPoly) {
        // Out of bounds is considered blocked.
        if (tileX < 0 || tileY < 0 || tileX >= mapWidth || tileY >= mapHeight) {
            return true;
        }

        TiledMapTileLayer collisionLayer = (TiledMapTileLayer) map.getLayers().get(1);
        if (collisionLayer == null) return false;

        Cell cell = collisionLayer.getCell(tileX, tileY);
        if (cell == null) return false;

        TiledMapTile tile = cell.getTile();
        if (tile == null) return false;

        // Do not check the entire tile's properties; only check the collision shapes (objects)
        MapObjects objects = tile.getObjects();
        if (objects != null && objects.getCount() > 0) {
            for (MapObject object : objects) {
                if (object.getProperties().containsKey("blocked")) {
                    if (object instanceof PolygonMapObject) {
                        Polygon polygon = ((PolygonMapObject) object).getPolygon();
                        // The polygon is defined in the tile's local coordinates.
                        // Convert it to world coordinates by setting the tile's position.
                        Polygon tilePoly = new Polygon(polygon.getVertices());
                        tilePoly.setPosition(tileX, tileY);
                        if (Intersector.overlapConvexPolygons(tilePoly, playerPoly)) {
                            return true;
                        }
                    } else {
                        // For other object types, default to blocking.
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public float getMapWidth() {
        return mapWidth;
    }

    public float getMapHeight() {
        return mapHeight;
    }

    public void dispose() {
        if (circleShader != null) circleShader.dispose();
        renderer.dispose();
        map.dispose();
    }
}
