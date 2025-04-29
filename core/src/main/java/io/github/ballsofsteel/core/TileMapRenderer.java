package io.github.ballsofsteel.core;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector3;
import io.github.ballsofsteel.entity.Player;

/**
 * Harita çizimi + çarpışma kontrolü.
 *  • Katman adlarıyla çalışır (“Ground”, “Collision”, “TreeTop”, “Bush”).
 *  • unitScale = 1 / tileWidth  -> harita pikseline otomatik uyar.
 *  • Çarpışma SADECE “Collision” katmanındaki hücrelerde yapılır.
 */
public class TileMapRenderer {

    /* -------- ayarlanabilir katman adları -------- */
    private static final String LAYER_GROUND    = "Ground";
    private static final String LAYER_COLLISION = "Collision";
    private static final String LAYER_BUSH      = "Bush";
    private static final String LAYER_BUILDINGS = "Building";
    private static final String LAYER_TREETOP   = "TreeTop";
    private static final String LAYER_PATHBLOCKER = "PathBlocker";
    private static final String LAYER_WATER = "Water";

    private final TiledMap                   map;
    private final OrthogonalTiledMapRenderer renderer;
    private final OrthographicCamera         camera;

    private final float unitScale;
    private final float mapWidthTiles;
    private final float mapHeightTiles;

    private final int groundLayerIndex;
    private final int buildingLayerIndex;
    private final int waterLayerIndex;
    private final int treeTopLayerIndex;
    private final int pathBlockerIndex;
    private final int collisionLayerIndex;


    /* opsiyonel shader */
    private com.badlogic.gdx.graphics.glutils.ShaderProgram circleShader;

    public TileMapRenderer(OrthographicCamera cam, String mapPath) {

        camera = cam;
        map    = new TmxMapLoader().load(mapPath);

        /* tile boyutuna göre otomatik unitScale */
        int tileW = map.getProperties().get("tilewidth" , Integer.class);
        int tileH = map.getProperties().get("tileheight", Integer.class);
        unitScale = 1f / tileW;                       // kare başına 1 dünya birimi

        mapWidthTiles  = map.getProperties().get("width" , Integer.class);
        mapHeightTiles = map.getProperties().get("height", Integer.class);

        renderer = new OrthogonalTiledMapRenderer(map, unitScale);

        groundLayerIndex = layerIndex(LAYER_GROUND);
        buildingLayerIndex = layerIndex(LAYER_BUILDINGS);
        waterLayerIndex = layerIndex(LAYER_WATER);
        treeTopLayerIndex = layerIndex(LAYER_TREETOP);
        collisionLayerIndex = layerIndex(LAYER_COLLISION);
        pathBlockerIndex = layerIndex(LAYER_PATHBLOCKER);
    }

    /* ------------------------------------------------------------------ */
    /*  Getter’lar                                                         */
    /* ------------------------------------------------------------------ */
    public float getMapWidth () { return mapWidthTiles;  }  // dünya birimi
    public float getMapHeight() { return mapHeightTiles; }

    /* ------------------------------------------------------------------ */
    /*  Çizim (katman adlarıyla)                                          */
    /* ------------------------------------------------------------------ */
    public void renderBase() {                      // zemin + binalar
        int[] idx = { waterLayerIndex,pathBlockerIndex, groundLayerIndex, collisionLayerIndex, buildingLayerIndex, treeTopLayerIndex};
        renderer.setView(camera);
        renderer.render(idx);
    }

    public void renderBush(Player p, float radius, boolean useShader){
        Integer id = layerIndexSafe(LAYER_BUSH); if (id==null) return;

        renderer.setView(camera);
        if (!useShader) { renderer.render(new int[]{id}); return; }

        if (circleShader == null)
            circleShader = ShaderManager.createCircleShader();

        Batch b = renderer.getBatch();
        b.setShader(circleShader); circleShader.bind();

        Vector3 scr = new Vector3(p.getX(), p.getY(), 0);
        camera.project(scr);
        circleShader.setUniformf("u_playerScreenPos", scr.x, scr.y);
        circleShader.setUniformf("u_radius", radius);

        renderer.render(new int[]{id});
        b.setShader(null);
    }

    public void renderTreeTop(Player p, float radius, boolean useShader){
        Integer id = layerIndexSafe(LAYER_TREETOP); if (id==null) return;

        renderer.setView(camera);
        if (!useShader) { renderer.render(new int[]{id}); return; }

        if (circleShader == null)
            circleShader = ShaderManager.createCircleShader();

        Batch b = renderer.getBatch();
        b.setShader(circleShader); circleShader.bind();

        Vector3 scr = new Vector3(p.getX(), p.getY(), 0);
        camera.project(scr);
        circleShader.setUniformf("u_playerScreenPos", scr.x, scr.y);
        circleShader.setUniformf("u_radius", radius);

        renderer.render(new int[]{id});
        b.setShader(null);
    }

    /* ------------------------------------------------------------------ */
    /*  Çarpışma: SADECE “Collision” katmanı                              */
    /* ------------------------------------------------------------------ */
    public boolean isCellBlocked(int tileX, int tileY, Polygon polyWorld) {

        if (tileX < 0 || tileY < 0
            || tileX >= mapWidthTiles || tileY >= mapHeightTiles)
            return true;

        // Önce Water layer'a bak
        TiledMapTileLayer waterLayer = (TiledMapTileLayer) map.getLayers().get(LAYER_WATER);
        if (waterLayer != null) {
            TiledMapTileLayer.Cell waterCell = waterLayer.getCell(tileX, tileY);
            if (waterCell != null && waterCell.getTile() != null) {
                Object blocked = waterLayer.getProperties().get("blocked");
                if (blocked != null && Boolean.parseBoolean(blocked.toString())) {

                    TiledMapTileLayer bridgeLayer = (TiledMapTileLayer) map.getLayers().get(LAYER_BUILDINGS);
                    if (bridgeLayer != null) {
                        TiledMapTileLayer.Cell bridgeCell = bridgeLayer.getCell(tileX, tileY);
                        if (bridgeCell != null && bridgeCell.getTile() != null) {
                            return false;
                        }
                    }
                    // Üstünde köprü yok → blokaj
                    return true;
                }
            }
        }

        // Sonra normal Collision layer'a bak
        TiledMapTileLayer col = (TiledMapTileLayer) map.getLayers().get(LAYER_COLLISION);
        if (col == null) return false;

        TiledMapTileLayer.Cell cell = col.getCell(tileX, tileY);
        if (cell == null || cell.getTile() == null) return false;

        MapObjects objs = cell.getTile().getObjects();
        if (objs == null || objs.getCount() == 0) return false;

        /* Hücrede 'blocked' etiketi olan ilk nesne → çarpışma var */
        for (MapObject o : objs) {
            if (!o.getProperties().containsKey("blocked")) continue;

            if (o instanceof PolygonMapObject) {
                Polygon p = ((PolygonMapObject) o).getPolygon();
                Polygon tilePoly = new Polygon(p.getTransformedVertices());
                tilePoly.setScale(unitScale, unitScale);
                tilePoly.setPosition(tileX, tileY);
                if (Intersector.overlapConvexPolygons(tilePoly, polyWorld))
                    return true;

            } else return true;   // dikdörtgen vb. – ayrıntıya gerek yok
        }
        return false;
    }

    /* ------------------------------------------------------------------ */
    /*  Yardımcılar                                                        */
    /* ------------------------------------------------------------------ */
    private Integer layerIndexSafe(String name){
        MapLayer l = map.getLayers().get(name);
        if (l == null) return null;
        return map.getLayers().getIndex(name);
    }
    private int layerIndex(String name){
        MapLayer l = map.getLayers().get(name);
        if (l == null)
            throw new IllegalArgumentException("Katman bulunamadı: "+name);
        return map.getLayers().getIndex(name);
    }
    /* ------------------------------------------------------------------ */
    /*  Katman hücre sorguları (opsiyonel)                                */
    /* ------------------------------------------------------------------ */
    public boolean isCellBush(int tileX, int tileY) {
        return cellExists(LAYER_BUSH, tileX, tileY);
    }

    public boolean isCellTreeTop(int tileX, int tileY) {
        return cellExists(LAYER_TREETOP, tileX, tileY);
    }

    private boolean cellExists(String layerName, int x, int y) {
        MapLayer l = map.getLayers().get(layerName);
        if (!(l instanceof TiledMapTileLayer)) return false;
        TiledMapTileLayer tl = (TiledMapTileLayer) l;

        if (x < 0 || y < 0 || x >= mapWidthTiles || y >= mapHeightTiles)
            return false;

        TiledMapTileLayer.Cell c = tl.getCell(x, y);
        return c != null && c.getTile() != null;
    }

    public void dispose() {
        if (circleShader != null) circleShader.dispose();
        renderer.dispose();
        map.dispose();
    }
}
