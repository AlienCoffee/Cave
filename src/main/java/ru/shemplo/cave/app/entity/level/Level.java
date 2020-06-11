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
        final var context = MazeGenerator.generateMaze (80, 80, 4);
        map = context.getMask ();
        
        ix = context.getSeeds ().get (0).X;
        iy = context.getSeeds ().get (0).Y;
    }
    
    public boolean canStepOn (int x, int y) {
        if (y < 0 || y >= map.length || x < 0 || x >= map [y].length) {
            return false;
        }
        
        return true;
    }
    
    public boolean canStepOnFrom (int dx, int dy, int fx, int fy) {
        final var cell = map [fy][fx];
        
        final var passage = cell.getPassageNeighbour (dx, dy);
        return passage != null && (passage.getGateType () == null || !passage.isClosed ());
    }
    
    private final Queue <IPoint> queue = new LinkedList <> ();
    private final Set <IPoint> visited = new HashSet <> ();
    
    private void runBFS (int fromX, int fromY, double maxDistance, BiConsumer <LevelCell, LevelCell> cellVisitor) {
        if (fromX < 0 || fromY < 0 || fromY >= map.length || fromX >= map [fromY].length) { return; } 
        queue.clear (); visited.clear ();
        
        final var fromPoint = IPoint.of (fromX, fromY);
        queue.add (fromPoint); visited.add (fromPoint);
        cellVisitor.accept (null, map [fromY][fromX]);
        
        //final var start = System.currentTimeMillis ();
        while (!queue.isEmpty ()) {
            final var point = queue.poll ();
            final var cell = map [point.Y][point.X];
            
            cell.getPassageNeighbours ().forEach (passage -> {
                if (passage.F == null) { return; }
                
                final var nei = passage.F.getAnother (cell);
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
        
        runBFS (x, y, 10.0, (__, point) -> {
            final int px = (int) point.getX ();
            final int py = (int) point.getY ();
            list.add (getCellSafe (px, py, px - x, py - y));            
        });
        
        return list;
    }
    
    public List <RenderGate> getVisibleGates (int x, int y) {
        final var gs = new ArrayList <RenderGate> ();
        
        runBFS (x, y, 10.0, (__, point) -> {
            final int px = (int) point.getX ();
            final int py = (int) point.getY ();
            final var cell = map [py][px];
            
            if (cell.getTopPass () != null && cell.getTopPass ().getGateType () != null) {   
                final var type = cell.getTopPass ().getGateType ();
                final var closed = cell.getTopPass ().isClosed ();
                gs.add (RenderGate.builder ().x (px - x).y (py - y - 0.5).vertical (false).type (type).closed (closed).build ());            
            }
            if (cell.getRightPass () != null && cell.getRightPass ().getGateType () != null) {   
                final var type = cell.getRightPass ().getGateType ();
                final var closed = cell.getRightPass ().isClosed ();
                gs.add (RenderGate.builder ().x (px - x + 0.5).y (py - y).vertical (true).type (type).closed (closed).build ());            
            }
            if (cell.getBottomPass () != null && cell.getBottomPass ().getGateType () != null) {
                final var type = cell.getBottomPass ().getGateType ();
                final var closed = cell.getBottomPass ().isClosed ();
                gs.add (RenderGate.builder ().x (px - x).y (py - y + 0.5).vertical (false).type (type).closed (closed).build ());            
            }
            if (cell.getLeftPass () != null && cell.getLeftPass ().getGateType () != null) {
                final var type = cell.getLeftPass ().getGateType ();
                final var closed = cell.getLeftPass ().isClosed ();
                gs.add (RenderGate.builder ().x (px - x - 0.5).y (py - y).vertical (true).type (type).closed (closed).build ());            
            }
        });
        
        return gs;
    }
    
    private RenderCell getCellSafe (int x, int y, int rx, int ry) {
        if (y < 0 || y >= map.length || x < 0 || x >= map [y].length) {
            return RenderCell.builder ().x (rx).y (ry).image (LevelTextures.symbol2texture.get (' '))
                 . subpart (map [y][x].getSubpart ())
                 . effect (Color.BLACK).build ();
        }
        
        //final var distance = Math.sqrt (rx * rx + ry * ry);
        return RenderCell.builder ().x (rx).y (ry)
             . image (LevelTextures.symbol2texture.get (map [y][x].getSymbol ()))
             . subpart (map [y][x].getSubpart ())
             . effect (Color.gray (0.025, 1.0))
             . build ();
    }
    
    public void openGates (int x, int y) {
        if (x < 0 || y < 0 || y >= map.length || x >= map [y].length) { return; } 
        final var cell = map [y][x];
        
        cell.getPassageNeighbours ().forEach (neiNoffset -> {
            if (neiNoffset.F == null || neiNoffset.F.getGateType () != GateType.GATE) { 
                return; 
            }
            
            neiNoffset.F.setClosed (!neiNoffset.F.isClosed ());
        });
    }
    
}
