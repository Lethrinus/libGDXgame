package io.github.ballsofsteel.core;

import com.badlogic.gdx.math.Vector2;
import io.github.ballsofsteel.core.TileMapRenderer;

import java.util.*;

public class GridPathfinder {
    private final boolean[][] passable;
    private final int w,h;
    private static final int[][] DIRS = {
        {1,0},{-1,0},{0,1},{0,-1},
        {1,1},{1,-1},{-1,1},{-1,-1}
    };
    public GridPathfinder(TileMapRenderer map) {
        w = (int)map.getMapWidth(); h = (int)map.getMapHeight();
        passable = new boolean[w][h];
        for(int x=0;x<w;x++){
            for(int y=0;y<h;y++){
                passable[x][y] = !map.isCellBlocked(x,y,
                    /*tiny square*/ null);
            }
        }
    }

    public List<Vector2> findPath(int sx,int sy,int tx,int ty){
        class Node implements Comparable<Node>{
            int x,y; float g,f; Node p;
            Node(int x,int y,float g,float f,Node p){
                this.x=x;this.y=y;this.g=g;this.f=f;this.p=p;
            }
            public int compareTo(Node o){return Float.compare(f,o.f);}
        }
        PriorityQueue<Node> open = new PriorityQueue<>();
        boolean[][] closed = new boolean[w][h];
        open.add(new Node(sx,sy,0,heur(sx,sy,tx,ty),null));

        while(!open.isEmpty()){
            Node n = open.poll();
            if(n.x==tx && n.y==ty){
                List<Vector2> path = new ArrayList<>();
                for(Node cur=n;cur!=null;cur=cur.p)
                    path.add(new Vector2(cur.x+0.5f,cur.y+0.5f));
                Collections.reverse(path);
                return path;
            }
            if(closed[n.x][n.y]) continue;
            closed[n.x][n.y]=true;
            for(int[] d: DIRS){
                int nx=n.x+d[0], ny=n.y+d[1];
                if(nx<0||ny<0||nx>=w||ny>=h||!passable[nx][ny]||closed[nx][ny]) continue;
                float cost = n.g + ((d[0]!=0&&d[1]!=0)?1.4142f:1f);
                open.add(new Node(nx,ny,cost, cost+heur(nx,ny,tx,ty), n));
            }
        }
        return Collections.emptyList();
    }

    private float heur(int x,int y,int tx,int ty){
        return Math.abs(tx-x)+Math.abs(ty-y);
    }
}
