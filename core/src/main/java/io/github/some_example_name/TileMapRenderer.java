package io.github.some_example_name;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector3;

/**
 * TileMapRenderer that:
 *  - Renders Ground/Collision/Bush/TreeTop layers
 *  - Applies a circle fade shader if needed
 *  - Provides collision (isCellBlocked), bush check (isCellBush), and tree top check (isCellTreeTop)
 */
public class TileMapRenderer {
    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;
    private OrthographicCamera camera;

    private float mapWidth, mapHeight;
    private final float unitScale = 1f / 32f;

    // Shader for Bush and TreeTop layers
    private com.badlogic.gdx.graphics.glutils.ShaderProgram circleShader;

    public TileMapRenderer(OrthographicCamera camera, String mapPath) {
        this.camera = camera;
        TmxMapLoader loader = new TmxMapLoader();
        map = loader.load(mapPath);

        // Read map dimensions from TMX properties (default 32 if missing)
        if (map.getProperties().containsKey("width")) {
            mapWidth = map.getProperties().get("width", Integer.class);
        } else {
            mapWidth = 32;
        }
        if (map.getProperties().containsKey("height")) {
            mapHeight = map.getProperties().get("height", Integer.class);
        } else {
            mapHeight = 32;
        }

        renderer = new OrthogonalTiledMapRenderer(map, unitScale);
    }

    public float getMapWidth() {
        return mapWidth;
    }

    public float getMapHeight() {
        return mapHeight;
    }

    public void renderBaseLayers(int[] layerIndices) {
        renderer.setView(camera);
        renderer.render(layerIndices);
    }

    public void renderBushWithShader(Player player, float radius) {
        if (map.getLayers().getCount() <= 2) return;
        int[] layers = new int[]{2};

        if (circleShader == null) {
            circleShader = ShaderManager.createCircleShader();
        }
        Batch batch = renderer.getBatch();
        batch.setShader(circleShader);
        circleShader.bind();

        // Convert player world pos to screen pos
        Vector3 playerScreen = new Vector3(player.getX(), player.getY(), 0);
        camera.project(playerScreen);
        circleShader.setUniformf("u_playerScreenPos", playerScreen.x, playerScreen.y);
        circleShader.setUniformf("u_radius", radius);

        renderer.setView(camera);
        renderer.render(layers);
        batch.setShader(null);
    }

    public void renderBushNoShader() {
        if (map.getLayers().getCount() <= 2) return;
        int[] layers = new int[]{2};
        renderer.setView(camera);
        renderer.render(layers);
    }

    /**
     * Renders the TreeTop layer with shader (radius in pixels).
     */
    public void renderTreeTopWithShader(Player player, float radius) {
        if (map.getLayers().getCount() <= 3) return;
        int[] layers = new int[]{3};

        if (circleShader == null) {
            circleShader = ShaderManager.createCircleShader();
        }
        Batch batch = renderer.getBatch();
        batch.setShader(circleShader);
        circleShader.bind();

        // Convert player world pos to screen pos
        Vector3 playerScreen = new Vector3(player.getX(), player.getY(), 0);
        camera.project(playerScreen);
        circleShader.setUniformf("u_playerScreenPos", playerScreen.x, playerScreen.y);
        circleShader.setUniformf("u_radius", radius);

        renderer.setView(camera);
        renderer.render(layers);
        batch.setShader(null);
    }

    /**
     * Renders the TreeTop layer normally (no shader).
     */
    public void renderTreeTopNoShader() {
        if (map.getLayers().getCount() <= 3) return;
        int[] layers = new int[]{3};
        renderer.setView(camera);
        renderer.render(layers);
    }

    /**
     * Checks collision for tileX,tileY in Collision layer (index=1).
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
                        float[] originalVerts = polygon.getTransformedVertices();
                        Polygon tilePoly = new Polygon(originalVerts);
                        tilePoly.setScale(unitScale, unitScale);
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
     * Checks if the tile at (tileX, tileY) is bush tile (layer 2).
     */
    public boolean isCellBush(int tileX, int tileY) {
        if (tileX < 0 || tileY < 0 || tileX >= mapWidth || tileY >= mapHeight) {
            return false;
        }
        if (map.getLayers().getCount() <= 2) return false;
        TiledMapTileLayer bushLayer = (TiledMapTileLayer) map.getLayers().get(2);
        if (bushLayer == null) return false;

        Cell cell = bushLayer.getCell(tileX, tileY);
        return (cell != null && cell.getTile() != null);
    }

    /**
     * Checks if the tile at (tileX, tileY) is tree top tile (layer 3).
     */
    public boolean isCellTreeTop(int tileX, int tileY) {
        if (tileX < 0 || tileY < 0 || tileX >= mapWidth || tileY >= mapHeight) {
            return false;
        }
        if (map.getLayers().getCount() <= 3) return false;
        TiledMapTileLayer treeLayer = (TiledMapTileLayer) map.getLayers().get(3);
        if (treeLayer == null) return false;

        Cell cell = treeLayer.getCell(tileX, tileY);
        return (cell != null && cell.getTile() != null);
    }

    public void dispose() {
        if (circleShader != null) circleShader.dispose();
        renderer.dispose();
        map.dispose();
    }
}
