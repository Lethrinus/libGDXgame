package io.github.theknightscrusade.core;

import com.badlogic.gdx.math.Vector2;
import java.util.*;

public class GridPathfinder {

    private final boolean[][] passable;
    private final int w, h;

    private static final int[][] DIRS = {
        {1,0}, {-1,0}, {0,1}, {0,-1},
        {1,1}, {-1,1}, {1,-1}, {-1,-1}
    };

    public GridPathfinder(TileMapRenderer map) {
        w = (int) map.getMapWidth();
        h = (int) map.getMapHeight();
        passable = new boolean[w][h];

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                passable[x][y] = !map.isCellBlocked(x, y, null);  // tiny square check
            }
        }
    }

    public List<Vector2> findPath(int sx, int sy, int tx, int ty) {
        // internal node class
        class Node implements Comparable<Node> {
            final int x, y;
            final float g, f;
            final Node parent;
            Node(int x, int y, float g, float f, Node parent) {
                this.x = x; this.y = y; this.g = g; this.f = f; this.parent = parent;
            }
            public int compareTo(Node o) { return Float.compare(this.f, o.f); }
        }

        boolean[][] closed = new boolean[w][h];
        PriorityQueue<Node> open = new PriorityQueue<>(64); // reserve capacity

        open.add(new Node(sx, sy, 0, heur(sx, sy, tx, ty), null));

        while (!open.isEmpty()) {
            Node current = open.poll();

            if (closed[current.x][current.y]) continue;
            closed[current.x][current.y] = true;

            if (current.x == tx && current.y == ty) {
                List<Vector2> path = new ArrayList<>();
                for (Node n = current; n != null; n = n.parent)
                    path.add(new Vector2(n.x + 0.5f, n.y + 0.5f));
                Collections.reverse(path);
                return path;
            }

            for (int[] d : DIRS) {
                int nx = current.x + d[0];
                int ny = current.y + d[1];

                // obstacle and border check
                if (nx < 0 || ny < 0 || nx >= w || ny >= h || !passable[nx][ny] || closed[nx][ny])
                    continue;

                // diagonal corner movement check
                if (d[0] != 0 && d[1] != 0) {
                    if (!passable[current.x + d[0]][current.y] || !passable[current.x][current.y + d[1]])
                        continue;
                }

                float stepCost = (d[0] != 0 && d[1] != 0) ? 1.4142f : 1f;
                float g = current.g + stepCost;
                float f = g + heur(nx, ny, tx, ty);
                open.add(new Node(nx, ny, g, f, current));
            }
        }

        return Collections.emptyList(); // no path
    }

    private float heur(int x, int y, int tx, int ty) {
        return Math.abs(tx - x) + Math.abs(ty - y); // Manhattan distance
    }
}
