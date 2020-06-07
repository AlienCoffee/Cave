package ru.shemplo.cave.app.entity.level;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;

import javafx.scene.paint.Color;
import lombok.Getter;
import ru.shemplo.cave.app.resources.LevelTextures;
import ru.shemplo.cave.experimental.MazeGenerator;
import ru.shemplo.cave.utils.IPoint;

public class Level {
    
    private final LevelCell [][] map;
    
    @Getter private int ix, iy;
    
    public Level () {
        map = MazeGenerator.generateMaze (100, 100, 3).getMask ();
    }
    
    public boolean canStepOn (int x, int y) {
        if (y < 0 || y >= map.length || x < 0 || x >= map [y].length) {
            return false;
        }
        
        return true;
    }
    
    public boolean canStepOnFrom (int dx, int dy, int fx, int fy) {
        final var cell = map [fy][fx];
        return cell.getPassageNeighbour (dx, dy) != null;
    }
    
    private final Queue <IPoint> queue = new LinkedList <> ();
    private final Set <IPoint> visited = new HashSet <> ();
    
    private void runBFS (int fromX, int fromY, double maxDistance, BiConsumer <LevelCell, LevelCell> cellVisitor) {
        queue.clear (); visited.clear ();
        
        final var fromPoint = IPoint.of (fromX, fromY);
        queue.add (fromPoint); visited.add (fromPoint);
        cellVisitor.accept (null, map [fromY][fromX]);
        
        //final var start = System.currentTimeMillis ();
        while (!queue.isEmpty ()) {
            final var point = queue.poll ();
            final var cell = map [point.Y][point.X];
            
            cell.getPassageNeighbours ().forEach (passage -> {
                if (passage == null) { return; }
                
                final var nei = passage.getAnother (cell);
                final var neiPoint = nei.getPoint (point.D + 1);
                if (fromPoint.distance (neiPoint) > maxDistance || neiPoint.D > maxDistance || visited.contains (neiPoint)) {
                    return; // too far or already visited
                }
                
                visited.add (neiPoint);
                queue.add (neiPoint);
                
                cellVisitor.accept (cell, nei);
            });
        }
        //final var end = System.currentTimeMillis ();
        //System.out.println ("BFS Duration: " + (end - start)); // SYSOUT
    }
    
    public List <RenderCell> getVisibleCells (int x, int y, double illumination) {
        final var list = new ArrayList <RenderCell> ();
        
        runBFS (x, y, 1.0, (__, point) -> {
            final int px = (int) point.getX ();
            final int py = (int) point.getY ();
            list.add (getCellSafe (px, py, px - x, py - y));            
        });
        
        return list;
    }
    
    public List <RenderGate> getVisibleGates (int x, int y) {
        final var gs = new ArrayList <RenderGate> ();
        
        /*
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
        */
        
        return gs;
    }
    
    private RenderCell getCellSafe (int x, int y, int rx, int ry) {
        if (y < 0 || y >= map.length || x < 0 || x >= map [y].length) {
            return RenderCell.builder ().x (rx).y (ry).image (LevelTextures.symbol2texture.get (' '))
                 . effect (Color.BLACK).build ();
        }
        
        //final var distance = Math.sqrt (rx * rx + ry * ry);
        return RenderCell.builder ().x (rx).y (ry)
             . image (LevelTextures.symbol2texture.get (map [y][x].getSymbol ()))
             . effect (Color.gray (0.025, 1.0))
             . build ();
    }
    
}
