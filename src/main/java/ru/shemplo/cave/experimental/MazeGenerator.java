package ru.shemplo.cave.experimental;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
                } else if (maze [i][j].getPart () == 2) {
                    System.out.print ("▒"); // SYSOUT
                } else if (maze [i][j].getPart () == 3) {
                    System.out.print ("▓"); // SYSOUT
                } else if (maze [i][j].getPart () == 4) {
                    System.out.print ("░"); // SYSOUT
                } else if (maze [i][j].getPart () == 5) {
                    System.out.print ("|"); // SYSOUT
                } else {
                    System.out.print (maze [i][j].getPart ()); // SYSOUT
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
        
        System.out.println ("Subparts mask:"); // SYSOUT
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (maze [i][j].getPart () == 1) {
                    if (maze [i][j].getSubpart () == 10) {
                        System.out.print ("█"); // SYSOUT
                    } else if (maze [i][j].getSubpart () == 11) {
                        System.out.print ("▒"); // SYSOUT
                    } else if (maze [i][j].getSubpart () == 12) {
                        System.out.print ("▓"); // SYSOUT
                    } else if (maze [i][j].getSubpart () == 13) {
                        System.out.print ("░"); // SYSOUT
                    } else if (maze [i][j].getSubpart () == 14) {
                        System.out.print ("|"); // SYSOUT
                    } else if (maze [i][j].getSubpart () > 14) {
                        System.out.print ("*"); // SYSOUT                        
                    } else {
                        System.out.print (maze [i][j].getSubpart ()); // SYSOUT
                    }
                } else {
                    System.out.print (" "); // SYSOUT
                }
            }
            System.out.println (); // SYSOUT
        }
    }
    
    public static LevelGenerationContext generateMaze (int width, int height, int parts) {
        var context = generateMask (width, height, parts);
        context = generatePassagesTree (context);
        context = generateSubparts (context);
        
        return context;
    }
    
    private static LevelGenerationContext generateMask (int width, int height, int parts) {
        final var part2cells = new ArrayList <List <LevelCell>> ();
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
        
        System.out.println ("Seed points: " + seeds); // SYSOUT
        final var fronts = new ArrayList <List <IPoint>> ();
        for (int p = 0; p < parts; p++) {
            final var point = seeds.get (p);
            
            part2cells.add (new ArrayList <> ());
            fronts.add (new LinkedList <>  ());
            fronts.get (p).add (point);
            
            part2cells.get (p).add (maze [point.Y][point.X]);
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
                            
                            part2cells.get (cell.getPart () - 1).add (rcell);
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
        
        return LevelGenerationContext.builder ()
             . part2cells (part2cells)
             . seeds (seeds)
             . mask (maze)
             . build ();
    }
    
    private static LevelGenerationContext generatePassagesTree (LevelGenerationContext context) {
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
    
    private static LevelGenerationContext generateSubparts (LevelGenerationContext context) {
        final var mask = context.getMask ();
        
        final var part2subpart2cells = new ArrayList <List <List <LevelCell>>> ();
        for (int p = 0; p < context.getSeeds ().size (); p++) {
            final var partCells = context.getPart2cells ().get (p);
            final var cells = new ArrayList <> (partCells);
            Collections.shuffle (cells, r);
            
            part2subpart2cells.add (new ArrayList <> ());
            
            int subpart = 1;
            for (final var initCell : cells) {
                if (initCell.getSubpart () != 0) { continue; }
                
                final var subpartCells = new ArrayList <LevelCell> ();
                part2subpart2cells.get (p).add (subpartCells);
                
                int ssize = 100 + r.nextInt (cells.size () / ((parts + 1) * 3));
                initCell.setSubpart (subpart++);
                subpartCells.add (initCell);
                ssize--;
                
                final var queue = new LinkedList <IPoint> ();
                queue.add (initCell.getPoint (0));
                
                while (!queue.isEmpty () && ssize > 0) {
                    final var cellPoint = queue.poll ();
                    final var cell = mask [cellPoint.Y][cellPoint.X];
                    
                    for (final var passage : cell.getPassageNeighbours ()) {                        
                        if (passage == null) { continue; }
                        
                        final var nei = passage.getAnother (cell);
                        if (nei.getSubpart () == 0 && ssize > 0) {
                            nei.setSubpart (cell.getSubpart ());
                            queue.add (nei.getPoint (0));
                            subpartCells.add (nei);
                            ssize--;
                        }
                    }
                }
                
            }
            
            part2subpart2cells.set (p, relaxSubparts (part2subpart2cells.get (p), 15, -1));
        }
        
        return context;
    }
    
    private static final AtomicInteger relaxSpCounter = new AtomicInteger (0);
    
    private static List <List <LevelCell>> relaxSubparts (List <List <LevelCell>> subpart2cells, int limit, int sizeTreshold) {
        if (subpart2cells.size () < limit) { return subpart2cells; }
        if (sizeTreshold == -1) {
            sizeTreshold = subpart2cells.stream ().map (List::size)
                         . sorted (Collections.reverseOrder ())
                         . skip (Math.max (limit - 1, 0))
                         . mapToInt (i -> i).max ().orElse (0);
        }
        
        final var treshold = sizeTreshold;
        final var toRelax = subpart2cells.stream ().map (cells -> Pair.mp (cells, cells.size ()))
            . sorted (Comparator.comparing (Pair <List <LevelCell>, Integer>::getS).reversed ())
            . skip (limit).filter (pair -> pair.S > 0 && pair.S < treshold).map (Pair::getF)
            . collect (Collectors.<List <LevelCell>> toList ());
        
        boolean hasNotFound = false;
        
        relaxLoop:
        for (final var cells : toRelax) {
            //System.out.println (cells.size ()); // SYSOUT
            for (final var cell : cells) {
                for (final var passage : cell.getPassageNeighbours ()) {                        
                    if (passage == null) { continue; }
                    
                    final var nei = passage.getAnother (cell);
                    final var neiSubpart = nei.getSubpart ();
                    
                    final var bigEnough = subpart2cells.get (neiSubpart - 1).size () >= treshold;
                    if (neiSubpart != cell.getSubpart () && bigEnough) {
                        for (final var tmp : cells) {
                            subpart2cells.get (neiSubpart - 1).add (tmp);
                            tmp.setSubpart (neiSubpart);
                        }
                        
                        continue relaxLoop;
                    }
                }
            }
            
            hasNotFound = true;
        }
        
        final var sp2cs = IntStream.range (0, subpart2cells.size ())
            . <List <LevelCell>> mapToObj (i -> {
                final var cells = subpart2cells.get (i);
                if (cells.isEmpty ()) { return cells; }
                
                final var sp = cells.get (0).getSubpart ();
                return sp == i + 1 ? cells : List.of ();
            })
            . collect (Collectors.toList ());
        
        relaxSpCounter.set (0);
        
        return hasNotFound 
             ? relaxSubparts (sp2cs, limit, sizeTreshold) 
             : sp2cs.stream ()
                 . filter (cells -> !cells.isEmpty ())
                 . peek (cells -> {
                     final var sp = relaxSpCounter.incrementAndGet ();
                     for (final var cell : cells) {
                         cell.setSubpart (sp);
                     }
                 })
                 . collect (Collectors.toList ());
    }
    
}
