package ru.shemplo.cave.experimental;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ru.shemplo.cave.app.entity.level.LevelCell;
import ru.shemplo.cave.app.entity.level.LevelPassage;
import ru.shemplo.cave.utils.IPoint;
import ru.shemplo.snowball.stuctures.Pair;
import ru.shemplo.snowball.stuctures.Trio;

public class MazeGenerator {
    
    private static final Random r = new Random ();
    
    private static final int parts = 4;
    private static final int size = parts * 20;
    
    public static void main (String ... args) {
        final var maze = generateMaze (size, size, parts).getMask ();
        
        System.out.println ("Maze mask:"); // SYSOUT
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (maze [i][j].getPart () == 1) {
                    System.out.print ("█"); // SYSOUT
                }
                if (maze [i][j].getPart () == 2) {
                    System.out.print ("▒"); // SYSOUT
                }
                if (maze [i][j].getPart () == 3) {
                    System.out.print ("▓"); // SYSOUT
                }
                if (maze [i][j].getPart () == 4) {
                    System.out.print ("░"); // SYSOUT
                }
                if (maze [i][j].getPart () == 5) {
                    System.out.print ("|"); // SYSOUT
                }
            }
            System.out.println (); // SYSOUT
        }
        
        System.out.println ("Maze:"); // SYSOUT
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (maze [i][j].getPart () == 1) {                    
                    System.out.print (maze [i][j].getSymbol ()); // SYSOUT
                } else {
                    System.out.print (" "); // SYSOUT
                }
            }
            System.out.println (); // SYSOUT
        }
    }
    
    public static LevelGenerationContext generateMaze (int width, int height, int parts) {
        var context = generateMask (width, height, parts);
        context = generatePassages (context);
        
        return context;
    }
    
    private static LevelGenerationContext generateMask (int width, int height, int parts) {
        final var maze = new LevelCell [height][width];
        final var seeds = new ArrayList <IPoint> ();
        
        for (int h = 0; h < maze.length; h++) {
            for (int w = 0; w < maze [h].length; w++) {
                maze [h][w] = new LevelCell (w, h);
            }
        }
        
        seedsGenerator:
        while (seeds.size () < parts) {
            final int x = r.nextInt (size), y = r.nextInt (size);
            for (var seed : seeds) {
                if (Math.abs (seed.F - x) <= 2 || Math.abs (seed.S - y) <= 2
                        || x < 1 || y < 1 || x >= size - 1 || y >= size - 1) {
                    continue seedsGenerator;
                }
            }
            
            seeds.add (IPoint.of (x, y));
            maze [y][x].setPart (seeds.size ());
        }
        
        System.out.println (seeds); // SYSOUT
        final var fronts = new ArrayList <List <IPoint>> ();
        for (int p = 0; p < parts; p++) {
            fronts.add (new LinkedList <>  ());
            fronts.get (p).add (seeds.get (p));
        }
        
        final var search = new ArrayList <> (List.of (
            IPoint.of (-1, 0), IPoint.of (0, 1), IPoint.of (1, 0), IPoint.of (0, -1), IPoint.of (1, 0), IPoint.of (-1, 0)
        ));
        
        final var partsOrder = IntStream.range (0, parts).mapToObj (i -> i).collect (Collectors.toList ());
        
        int updated = 1;
        while (updated > 0) {
            updated = 0;
            
            Collections.shuffle (partsOrder, r);
            for (final var part : partsOrder) {
                final var front = fronts.get (part);
                
                if (front.isEmpty ()) { continue; }
                Collections.shuffle (front, r);
                
                frontLoop:
                for (int i = 0; i < front.size (); i++) {
                    final var fpoint = front.get (i);
                    
                    final var cell = maze [fpoint.Y][fpoint.X];
                    Collections.shuffle (search, r);
                    boolean searchFlag = false;
                    
                    for (final var s : search) {
                        final int rx = fpoint.X + s.X, ry = fpoint.Y + s.Y;
                        
                        if (rx < 0 || rx >= width || ry < 0 || ry >= height) { continue; }
                        if (cell.hasRelative (s)) { continue; }
                        
                        final var rcell = maze [ry][rx];
                        if (rcell.getPart () > 0) { continue; }
                        searchFlag = true;
                        updated += 1;
                        
                        if (r.nextBoolean ()) {                            
                            cell.setRelative (s, false, rcell);
                            rcell.setRelative (s, true, cell);
                            rcell.setPart (cell.getPart ());
                            
                            front.add (IPoint.of (rx, ry));
                        }
                        
                        break frontLoop;
                    }
                    
                    if (!searchFlag) {
                        front.remove (fpoint);
                    }
                }
            }
        }
        
        return LevelGenerationContext.builder ().seeds (seeds).mask (maze).build ();
    }
    
    private static LevelGenerationContext generatePassages (LevelGenerationContext context) {
        final var mask = context.getMask ();
        
        final var connections = new ArrayList <> (List.<Trio <
            Function <LevelCell, LevelCell>, 
            BiConsumer <LevelCell, LevelPassage>, 
            BiConsumer <LevelCell, LevelPassage>>
        > of (
            Trio.mt (LevelCell::getTop,    LevelCell::setTopPass,    LevelCell::setBottomPass),
            Trio.mt (LevelCell::getRight,  LevelCell::setRightPass,  LevelCell::setLeftPass),
            Trio.mt (LevelCell::getBottom, LevelCell::setBottomPass, LevelCell::setTopPass),
            Trio.mt (LevelCell::getLeft,   LevelCell::setLeftPass,   LevelCell::setRightPass)
        ));
        
        for (final var seed: context.getSeeds ()) {
            final var seedCell = mask [seed.Y][seed.X];
            final var seedCellPoint = seedCell.getPoint (0);
            
            final var queue = new LinkedList <IPoint> ();
            final var visited = new HashSet <IPoint> ();
            
            visited.add (seedCellPoint);
            queue.add (seedCellPoint);
            
            while (!queue.isEmpty ()) {
                final var cellPoint = queue.poll ();
                final var cell = mask [cellPoint.Y][cellPoint.X];
                
                Collections.shuffle (connections, r);
                for (final var connection : connections) {
                    final var nei = connection.F.apply (cell);
                    
                    if (addPassage (cell, nei, connection.S, connection.T, visited)) {
                        queue.add (nei.getPoint (0));
                    }
                }
            }
        }
        
        
        return context;
    }
    
    private static boolean addPassage (
        LevelCell cell, LevelCell neighbour, 
        BiConsumer <LevelCell, LevelPassage> cellPassage, 
        BiConsumer <LevelCell, LevelPassage> neiPassage,
        Set <IPoint> visited
    ) {
        return Optional.ofNullable (neighbour).map (nei -> {
            final var point = nei.getPoint (0);
            if (visited.contains (point)) { 
                return false; // already connected
            }
            
            visited.add (point);
            
            final var passage = LevelPassage.of (cell, nei);
            cellPassage.accept (cell, passage);
            neiPassage.accept (nei, passage);
            
            return true;
        }).orElse (false);
    }
    
    public static int [][] generateGates (int [][] maze, int gates) {
        final var mask = new int [maze.length][maze.length * 2];
        
        final var search = new ArrayList <> (List.of (
            Pair.mp (-1, 0), Pair.mp (0, 1), Pair.mp (1, 0), Pair.mp (0, -1), Pair.mp (1, 0), Pair.mp (-1, 0)
        ));
        
        while (gates > 0) {
            final int x = 1 + r.nextInt (maze.length - 1), y = 1 + r.nextInt (maze.length - 1);
            if (maze [y][x] == 0) { continue; }
            
            Collections.shuffle (search, r);
            for (final var s : search) {
                final var maskY = y + (s.S == -1 ? -1 : 0);
                final var maskX = x * 2 + s.F;
                
                if (maze [y + s.S][x + s.F] > 0 && mask [maskY][maskX] == 0) {
                    mask [maskY][maskX] = 1;
                    gates--;
                    
                    break;
                }
            }
        }
        
        return mask;
    }
    
}
