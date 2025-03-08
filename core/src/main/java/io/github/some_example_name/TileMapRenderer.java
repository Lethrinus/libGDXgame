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

    // Shader used for both TreeTop and Bush layers
    private ShaderProgram circleShader;

    /**
     * Default constructor loads the map from "maps/tileset.tmx".
     */
    public TileMapRenderer(OrthographicCamera camera) {
        this(camera, "maps/tileset.tmx");
    }

    /**
     * Constructor to load a specified map.
     * Expected layer order: 0 = Ground, 1 = Collision, 2 = Bush, 3 = TreeTop.
     */
    public TileMapRenderer(OrthographicCamera camera, String mapPath) {
        this.camera = camera;
        map = new TmxMapLoader().load(mapPath);
        // Assuming the map size is 32x32 in world units
        mapWidth = 32f;
        mapHeight = 32f;
        renderer = new OrthogonalTiledMapRenderer(map, unitScale);
    }

    /**
     * Renders the specified base layers (e.g., Ground and Collision).
     */
    public void renderBaseLayers(int[] layerIndices) {
        renderer.setView(camera);
        renderer.render(layerIndices);
    }

    /**
     * Renders the TreeTop layer (index = 3) using a circle fade shader.
     * The shader reduces the alpha value inside a specified radius around the player.
     */
    public void renderTreeTopWithShader(Player player, float radius) {
        // Check if the expected layer exists
        if (map.getLayers().getCount() <= 3) return;
        int[] layers = new int[]{3}; // Expected TreeTop layer index
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
     * Renders the Bush layer (index = 2) using a circle fade shader.
     * This makes bushes appear faded when the player is behind them.
     */
    public void renderBushWithShader(Player player, float radius) {
        if (map.getLayers().getCount() <= 2) return;
        int[] layers = new int[]{2}; // Expected Bush layer index
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
     * Checks collision for a given cell (on Collision layer index 1) using the provided polygon.
     */
    public boolean isCellBlocked(int tileX, int tileY, Polygon playerPoly) {
        if (tileX < 0 || tileY < 0 || tileX >= mapWidth || tileY >= mapHeight) {
            return true;
        }
        TiledMapTileLayer collisionLayer = (TiledMapTileLayer) map.getLayers().get(1);
        if (collisionLayer == null) return false;
        Cell cell = collisionLayer.getCell(tileX, tileY);
        if (cell == null) return false;
        TiledMapTile tile = cell.getTile();
        if (tile == null) return false;
        MapObjects objects = tile.getObjects();
        if (objects != null && objects.getCount() > 0) {
            for (MapObject object : objects) {
                if (object.getProperties().containsKey("blocked")) {
                    if (object instanceof PolygonMapObject) {
                        Polygon polygon = ((PolygonMapObject) object).getPolygon();
                        Polygon tilePoly = new Polygon(polygon.getVertices());
                        tilePoly.setPosition(tileX, tileY);
                        if (Intersector.overlapConvexPolygons(tilePoly, playerPoly)) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if the given cell (on Bush layer index 2) contains a bush.
     */
    public boolean isCellBush(int tileX, int tileY) {
        if (tileX < 0 || tileY < 0 || tileX >= mapWidth || tileY >= mapHeight) {
            return false;
        }
        TiledMapTileLayer bushLayer = (TiledMapTileLayer) map.getLayers().get(2);
        if (bushLayer == null) return false;
        Cell cell = bushLayer.getCell(tileX, tileY);
        return cell != null && cell.getTile() != null;
    }

    public float getMapWidth() {
        return mapWidth;
    }

    public float getMapHeight() {
        return mapHeight;
    }

    /**
     * Disposes of map resources.
     */
    public void dispose() {
        if (circleShader != null) circleShader.dispose();
        renderer.dispose();
        map.dispose();
    }
}
