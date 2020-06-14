package ru.shemplo.cave.app.entity.level;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;

import javafx.scene.paint.Color;
import lombok.Getter;
import ru.shemplo.cave.experimental.LevelGenerationContext;
import ru.shemplo.cave.experimental.MazeGenerator;
import ru.shemplo.cave.utils.IPoint;

public class Level {
    
    @Getter
    private final LevelGenerationContext context;
    
    private final LevelCell [][] map;
    
    public Level (int width, int height, int parts) {
        context = MazeGenerator.generateMaze (width, height, parts);
        map = context.getMask ();
        
        System.out.println ("Maze mask:"); // SYSOUT
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (map [i][j].getPart () == 1) {
                    System.out.print ("█"); // SYSOUT
                } else if (map [i][j].getPart () == 2) {
                    System.out.print ("▒"); // SYSOUT
                } else if (map [i][j].getPart () == 3) {
                    System.out.print ("▓"); // SYSOUT
                } else if (map [i][j].getPart () == 4) {
                    System.out.print ("░"); // SYSOUT
                } else if (map [i][j].getPart () == 5) {
                    System.out.print ("|"); // SYSOUT
                } else {
                    System.out.print (map [i][j].getPart ()); // SYSOUT
                }
            }
            System.out.println (); // SYSOUT
        }
        
        System.out.println ("Maze:"); // SYSOUT
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (map [i][j].getPart () > 0 && map [i][j].getSubpart () > 0) {                    
                    System.out.print (map [i][j].getSymbol ()); // SYSOUT
                } else {
                    System.out.print (" "); // SYSOUT
                }
            }
            System.out.println (); // SYSOUT
        }
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
        synchronized (queue) {            
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
        }
        //final var end = System.currentTimeMillis ();
        //System.out.println ("BFS Duration: " + (end - start)); // SYSOUT
    }
    
    public List <LevelCell> getVisibleCells (int x, int y, double illumination) {
        final var list = new ArrayList <LevelCell> ();
        
        runBFS (x, y, illumination, (__, cell) -> {
            //list.add (getCellSafe (px, py, px - x, py - y));
            list.add (cell);
        });
        
        /*
        System.out.println ("Maze:"); // SYSOUT
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map [i].length; j++) {
                if (i == y && j == x) {
                    System.out.print ("&"); // SYSOUT
                } else {                    
                    if (map [i][j].getPart () > 0 && map [i][j].getSubpart () > 0) {                    
                        System.out.print (map [i][j].getSymbol ()); // SYSOUT
                    } else {
                        System.out.print (" "); // SYSOUT
                    }
                }
            }
            System.out.println (); // SYSOUT
        }
        
        System.out.println (list); // SYSOUT
        */
        
        return list;
    }
    
    public List <RenderGate> getVisibleGates (int x, int y, double illumination) {
        final var gs = new ArrayList <RenderGate> ();
        
        runBFS (x, y, illumination, (__, point) -> {
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
    
    @SuppressWarnings ("unused")
    private RenderCell getCellSafe (int x, int y, int rx, int ry) {
        if (y < 0 || y >= map.length || x < 0 || x >= map [y].length) {
            return RenderCell.builder ().x (rx).y (ry)//.image (LevelTextures.symbol2texture.get (' '))
                 . subpart (map [y][x].getSubpart ()).symbol (' ')
                 . effect (Color.BLACK).build ();
        }
        
        //final var distance = Math.sqrt (rx * rx + ry * ry);
        return RenderCell.builder ().x (rx).y (ry)
             //. image (LevelTextures.symbol2texture.get (map [y][x].getSymbol ()))
             . subpart (map [y][x].getSubpart ())
             . effect (Color.gray (0.025, 1.0))
             . symbol (map [y][x].getSymbol ())
             . build ();
    }
    
    public void toggleTumbler (int x, int y) {
        if (x < 0 || y < 0 || y >= map.length || x >= map [y].length) { return; } 
        Optional.ofNullable (map [y][x].getTumbler ()).ifPresent (tum -> {
            tum.setActive (!tum.isActive ());
            
            if (tum.isActive ()) {                
                tum.getOpen ().forEach (gate -> {
                    gate.setClosed (false);
                });
            } else {                
                tum.getClose ().forEach (gate -> {
                    gate.setClosed (true);
                });
            }
            
        });
    }
    
}
