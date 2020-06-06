package ru.shemplo.cave.app.entity.level;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;

import javafx.geometry.Point3D;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import lombok.Getter;
import ru.shemplo.cave.experimental.MazeGenerator;
import ru.shemplo.snowball.stuctures.Pair;

public class Level {
    
    private final int [][] map = new int [100][100];
    private int [][] gates;
    
    @Getter private int ix, iy;
    
    public Level () {
        final var mask = MazeGenerator.generateMaze (map.length, 1);
        gates = MazeGenerator.generateGates (mask, 1000);
        
        for (int i = 1; i < map.length - 1; i++){
            for (int j = 1; j < map [i].length - 1; j++) {
                if (mask [i][j] == 0) { continue; }
                
                if (mask [i][j] == 10 && ix == 0 && iy == 0) {
                    ix = j; iy = i;
                }
                
                final boolean lc = mask [i - 0][j - 1] != 0,
                              cu = mask [i - 1][j - 0] != 0,
                              cb = mask [i + 1][j - 0] != 0,
                              rc = mask [i - 0][j + 1] != 0;
                
                if (lc && cu && cb && rc) {
                    map [i][j] = 6;
                } else if (lc && cu && cb) {
                    map [i][j] = 10;
                } else if (lc && cu && rc) {
                    map [i][j] = 9;
                } else if (lc && cb && rc) {
                    map [i][j] = 5;
                } else if (cu && cb && rc) {
                    map [i][j] = 11;
                } else if (lc && cu) {
                    map [i][j] = 13;
                } else if (lc && cb) {
                    map [i][j] = 15;
                } else if (lc && rc) {
                    map [i][j] = 3;
                } else if (cu && cb) {
                    map [i][j] = 7;
                } else if (cu && rc) {
                    map [i][j] = 14;
                } else if (cb && rc) {
                    map [i][j] = 16;
                } else if (lc) {
                    map [i][j] = 2;
                } else if (cu) {
                    map [i][j] = 4;
                } else if (cb) {
                    map [i][j] = 8;
                } else if (rc) {
                    map [i][j] = 1;
                }
            }
        }
    }
    
    public boolean canStepOn (int x, int y) {
        if (y < 0 || y >= map.length || x < 0 || x >= map [y].length || map [y][x] == 0) {
            return false;
        }
        
        return true;
    }
    
    public boolean canStepOnFrom (int x, int y, int fx, int fy) {
        if (!canStepOn (x, y)) { return false; }
        
        return true;
    }
    
    private final List <Pair <Integer, Integer>> directions = List.of (
        Pair.mp (0, 1), Pair.mp (0, -1), Pair.mp (1, 0), Pair.mp (-1, 0)
    );
    
    private final Queue <Point3D> queue = new LinkedList <> ();
    private final Set <Point3D> visited = new HashSet <> ();
    
    private void runBFS (int fromX, int fromY, double maxDistance, BiConsumer <Point3D, Point3D> cellVisitor) {
        queue.clear (); visited.clear ();
        
        final var fromPoint = new Point3D (fromX, fromY, 0);
        visited.add (fromPoint);
        queue.add (fromPoint);
        
        final var start = System.currentTimeMillis ();
        while (!queue.isEmpty ()) {
            final var point = queue.poll ();
            
            directions.forEach (offset -> {
                if (point.getZ () + 1 > maxDistance) { return; }
                
                final int cx = (int) point.getX ();
                final int cy = (int) point.getY ();
                final int x = cx + offset.F;
                final int y = cy + offset.S;
                
                if (!canStepOn (x, y)) { return; }
                
                final var toPoint = new Point3D (x, y, point.getZ () + 1);
                if (visited.contains (toPoint)) { return; }
                
                visited.add (toPoint);
                queue.add (toPoint);
                
                cellVisitor.accept (point, toPoint);
            });
        }
        
        final var end = System.currentTimeMillis ();
        if (end - start > 0) {            
            System.out.println ("BFS Duration: " + (end - start)); // SYSOUT
        }
    }
    
    public List <RenderCell> getVisibleCells (int x, int y, double illumination) {
        final var list = new ArrayList <RenderCell> ();
        
        runBFS (x, y, 2.0, (__, point) -> {
            final int px = (int) point.getX ();
            final int py = (int) point.getY ();
            list.add (getCellSafe (px, py, px - x, py - y));            
        });
        
        return list;
    }
    
    public List <RenderGate> getVisibleGates (int x, int y) {
        final var gs = new ArrayList <RenderGate> ();
        final var maxDistance = 3.0;
        
        runBFS (x, y, maxDistance, (from, point) -> {
            final int px = (int) point.getX ();
            final int py = (int) point.getY ();
            
            if (point.getZ () > maxDistance - 1) {
                final var dx = (int) (px - from.getX ());
                final var dy = (int) (py - from.getY ());
                
                if (dy < 0 && dx == 0 && gates [py][px * 2] > 0) {
                    gs.add (RenderGate.builder ().x (px - x).y (py - y + 0.5).build ());
                } else if (dx < 0 && dy == 0 && gates [py][px * 2 + 1] > 0) {
                    gs.add (RenderGate.builder ().x (px - x + 0.5).y (py - y).vertical (true).build ());
                }
            } else {                
                if (gates [py][px * 2] > 0) {                
                    gs.add (RenderGate.builder ().x (px - x).y (py - y + 0.5).build ());            
                }
                if (gates [py][px * 2 + 1] > 0) {                
                    gs.add (RenderGate.builder ().x (px - x + 0.5).y (py - y).vertical (true).build ());            
                }
            }
            
        });
        
        return gs;
    }
    
    private static final Image tunnel = new Image (Level.class.getResourceAsStream ("/gfx/tunnel.png"));
    private static final Image tunnel_r = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-r.png"));
    private static final Image tunnel_l = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-l.png"));
    private static final Image tunnel_lr = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-lr.png"));
    private static final Image tunnel_lrb = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-lrb.png"));
    private static final Image tunnel_lrbt = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-lrbt.png"));
    private static final Image tunnel_t = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-t.png"));
    private static final Image tunnel_b = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-b.png"));
    private static final Image tunnel_tb = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-tb.png"));
    private static final Image tunnel_lrt = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-lrt.png"));
    private static final Image tunnel_lbt = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-lbt.png"));
    private static final Image tunnel_rbt = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-rbt.png"));
    private static final Image tunnel_lt = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-lt.png"));
    private static final Image tunnel_rt = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-rt.png"));
    private static final Image tunnel_lb = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-lb.png"));
    private static final Image tunnel_rb = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-rb.png"));
    private static final Image tunnel_0 = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-0.png"));

    private final List <Image> tunnels = List.of (
        tunnel_0, // 0
        tunnel_r, 
        tunnel_l, // 2
        tunnel_lr, 
        tunnel_t, // 4
        tunnel_lrb,
        tunnel_lrbt, // 6
        tunnel_tb, 
        tunnel_b, // 8
        tunnel_lrt, 
        tunnel_lbt, // 10
        tunnel_rbt, 
        tunnel, // 12
        tunnel_lt,
        tunnel_rt, // 14
        tunnel_lb,
        tunnel_rb // 16
    );
    
    @SuppressWarnings ("unused")
    private Set <Integer> horizontalOpen = Set.of (3, 5, 6, 9);
    @SuppressWarnings ("unused")
    private Set <Integer> verticalOpen = Set.of (6, 7, 10, 11);
    
    @SuppressWarnings ("unused")
    private int getTypeSafe (int x, int y) {
        if (y < 0 || y >= map.length || x < 0 || x >= map [y].length) {
            return 0;
        }
        
        return map [y][x];
    }
    
    private RenderCell getCellSafe (int x, int y, int rx, int ry) {
        if (y < 0 || y >= map.length || x < 0 || x >= map [y].length) {
            return RenderCell.builder ().x (rx).y (ry).image (tunnel_0).effect (Color.BLACK).build ();
        }
        
        //final var distance = Math.sqrt (rx * rx + ry * ry);
        return RenderCell.builder ().x (rx).y (ry)
             . image (tunnels.get (map [y][x]))
             . effect (Color.gray (0.025, 1.0))
             . build ();
    }
    
}
