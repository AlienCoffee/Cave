package ru.shemplo.cave.experimental;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ru.shemplo.cave.app.entity.level.GateType;
import ru.shemplo.cave.app.entity.level.LevelCell;
import ru.shemplo.cave.app.entity.level.LevelPassage;
import ru.shemplo.cave.app.entity.level.LevelTumbler;
import ru.shemplo.cave.utils.IPoint;
import ru.shemplo.cave.utils.Utils;
import ru.shemplo.snowball.stuctures.Pair;
import ru.shemplo.snowball.stuctures.Trio;

public class MazeGenerator {
    
    private static final Random r = new Random (1L);
    
    private static final int parts = 2;
    private static final int size = parts * 20;
    
    public static void main (String ... args) {
        final var context = generateMaze (size, size, parts);
        final var maze = context.getMask ();
        
        System.out.println ("Seed points: " + context.getSeeds ()); // SYSOUT
        
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
                if (maze [i][j].getPart () == 1 && maze [i][j].getSubpart () > 0) {                    
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
        context = generateSubparts (context, 15);
        
        context = generateCyclesWithinSubparts (context, 23);
        context = generateGatesBetweenSubparts (context);
        context = generateSlitsBetweenParts (context);
        context = generateTumblers (context);
        
        return context;
    }
    
    private static LevelGenerationContext generateMask (int width, int height, int parts) {
        final var part2cells = new ArrayList <List <LevelCell>> ();
        final var maze = initializeNetwork (width, height);
        final var seeds = generateSeeds (maze, parts);
        
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
                        //if (cell.hasRelative (s)) { continue; }
                        
                        final var rcell = maze [ry][rx];
                        if (rcell.getPart () > 0) { continue; }
                        searchFlag = true;
                        updated += 1;
                        
                        if (r.nextBoolean ()) {
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
    
    private static LevelCell [][] initializeNetwork (int width, int height) {
        final var maze = new LevelCell [height][width];
        
        for (int h = 0; h < maze.length; h++) {
            for (int w = 0; w < maze [h].length; w++) {
                maze [h][w] = new LevelCell (w, h);
                
                if (w > 0) {
                    maze [h][w - 1].setRight (maze [h][w]);
                    maze [h][w].setLeft (maze [h][w - 1]);
                }
                
                if (h > 0) {
                    maze [h - 1][w].setBottom (maze [h][w]);
                    maze [h][w].setTop (maze [h - 1][w]);
                }
            }
        }
        
        return maze;
    }
    
    private static List <IPoint> generateSeeds (LevelCell [][] maze, int parts) {
        final var seeds = new ArrayList <IPoint> ();
        
        seedsGenerator:
        while (seeds.size () < parts) {
            final int y = r.nextInt (maze.length);
            final int x = r.nextInt (maze [y].length);
            
            for (var seed : seeds) {
                if (Math.abs (seed.F - x) <= 2 || Math.abs (seed.S - y) <= 2
                        || x < 1 || y < 1 || x >= size - 1 || y >= size - 1) {
                    continue seedsGenerator;
                }
            }
            
            seeds.add (IPoint.of (x, y));
            maze [y][x].setPart (seeds.size ());
        }

        return seeds;
    }
    
    private static final List <
        Trio <Function <LevelCell, Pair <LevelCell, LevelPassage>>, 
        BiConsumer <LevelCell, LevelPassage>, 
        BiConsumer <LevelCell, LevelPassage>>
    > connections = new ArrayList <> (List.of (
        Trio.mt (c -> Pair.mp (c.getTop (),    c.getTopPass ()),    LevelCell::setTopPass,    LevelCell::setBottomPass),
        Trio.mt (c -> Pair.mp (c.getRight (),  c.getRightPass ()),  LevelCell::setRightPass,  LevelCell::setLeftPass),
        Trio.mt (c -> Pair.mp (c.getBottom (), c.getBottomPass ()), LevelCell::setBottomPass, LevelCell::setTopPass),
        Trio.mt (c -> Pair.mp (c.getLeft (),   c.getLeftPass ()),   LevelCell::setLeftPass,   LevelCell::setRightPass)
    ));
    
    private static LevelGenerationContext generatePassagesTree (LevelGenerationContext context) {
        final BiPredicate <LevelCell, LevelCell> predicate = (a, b) -> a.getPart () == b.getPart ();
        final var mask = context.getMask ();
        
        for (final var seed: context.getSeeds ()) {
            final var seedCell = mask [seed.Y][seed.X];
            final var seedCellPoint = seedCell.getPoint (0);
            
            final var visited = new HashSet <IPoint> ();
            final var stack = new Stack <IPoint> ();
            
            visited.add (seedCellPoint);
            stack.add (seedCellPoint);
            
            while (!stack.isEmpty ()) {
                final var cellPoint = stack.pop ();
                final var cell = mask [cellPoint.Y][cellPoint.X];
                
                Collections.shuffle (connections, r);
                for (final var connection : connections) {
                    final var neiNpass = connection.F.apply (cell);
                    
                    if (addPassage (cell, neiNpass.F, connection.S, connection.T, predicate, visited)) {
                        stack.add (neiNpass.F.getPoint (0));
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
        BiPredicate <LevelCell, LevelCell> predicate,
        Set <IPoint> visited
    ) {
        return Optional.ofNullable (neighbour).map (nei -> {
            final var point = nei.getPoint (0);
            if (visited != null) {                
                if (visited.contains (point)) { 
                    return false; // already connected
                }
                
                visited.add (point);
            }
            
            if (predicate.test (cell, neighbour)) {                
                final var passage = LevelPassage.of (cell, nei, null);
                cellPassage.accept (cell, passage);
                neiPassage.accept (nei, passage);
                
                return true;
            } else {                
                return false;
            }
        }).orElse (false);
    }
    
    private static LevelGenerationContext generateSubparts (LevelGenerationContext context, int subparts) {
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
                        if (passage.F == null) { continue; }
                        
                        final var nei = passage.F.getAnother (cell);
                        if (nei.getSubpart () == 0 && ssize > 0) {
                            nei.setSubpart (cell.getSubpart ());
                            queue.add (nei.getPoint (0));
                            subpartCells.add (nei);
                            ssize--;
                        }
                    }
                }
                
            }
            
            part2subpart2cells.set (p, relaxSubparts (part2subpart2cells.get (p), subparts, -1));
        }
        
        context.setPart2subpart2cells (part2subpart2cells);
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
            for (final var cell : cells) {
                for (final var passage : cell.getPassageNeighbours ()) {                        
                    if (passage.F == null) { continue; }
                    
                    final var nei = passage.F.getAnother (cell);
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
    
    private static LevelGenerationContext generateCyclesWithinSubparts (LevelGenerationContext context, int reduceFactor) {
        final BiPredicate <LevelCell, LevelCell> predicate = (a, b) -> 
            a.getPart () == b.getPart () && a.getSubpart () == b.getSubpart ();
            
        for (final var subpart : context.getPart2subpart2cells ()) {
            for (final var cells : subpart) {
                final int extra = cells.size () / Math.max (reduceFactor, 1);
                var rest = extra;
                
                final var mutableCells = new ArrayList <> (cells);
                
                extraLoop:
                while (rest > 0) {
                    Collections.shuffle (mutableCells, r);
                    for (final var cell : mutableCells) {
                        Collections.shuffle (connections, r);
                        for (final var connection : connections) {
                            final var neiNpass = connection.F.apply (cell);
                            if (neiNpass.S != null) { continue; }
                            
                            if (addPassage (cell, neiNpass.F, connection.S, connection.T, predicate, null)) {
                                rest--; // extra passage is added
                                continue extraLoop;
                            }
                        }
                    }
                }
            }
        }
        
        return context;
    }
    
    private static LevelGenerationContext generateGatesBetweenSubparts (LevelGenerationContext context) {
        final var mask = context.getMask ();
        
        final var part2subpart2gates = new HashMap <Integer, Map <Integer, List <LevelPassage>>> ();
        
        for (int h = 0; h < mask.length; h++) {
            for (int w = 0; w < mask [h].length; w++) {
                final var cell = mask [h][w];
                
                Optional.ofNullable (cell.getRightPass ()).ifPresent (passage -> {
                    setGateTypeToPassage (cell, passage, part2subpart2gates);
                });
                
                Optional.ofNullable (cell.getBottomPass ()).ifPresent (passage -> {
                    setGateTypeToPassage (cell, passage, part2subpart2gates);
                });
            }
        }
        
        context.setPart2subpart2gates (part2subpart2gates);
        return context;
    }
    
    private static void setGateTypeToPassage (
        LevelCell cell, LevelPassage passage, Map <Integer, Map <Integer, List <LevelPassage>>> p2sp2gs
    ) {
        final var nei = passage.getAnother (cell);
        final int cp = cell.getPart (), csp = cell.getSubpart (), nsp = nei.getSubpart ();
        
        if (cp == nei.getPart () && csp != nsp) {
            p2sp2gs.putIfAbsent (cp, new HashMap <> ());
            
            p2sp2gs.get (cp).putIfAbsent (csp, new ArrayList <> ());
            p2sp2gs.get (cp).putIfAbsent (nsp, new ArrayList <> ());
            
            p2sp2gs.get (cp).get (nsp).add (passage);
            p2sp2gs.get (cp).get (nsp).add (passage);
            
            passage.setGateType (GateType.GATE);
        }
    }
    
    private static LevelGenerationContext generateSlitsBetweenParts (LevelGenerationContext context) {
        final var mask = context.getMask ();
        
        final var fronts = new HashMap <IPoint, List <LevelCell>> ();
        
        for (int h = 0; h < mask.length; h++) {
            for (int w = 0; w < mask [h].length; w++) {
                final var cell = mask [h][w];
                final var cp = cell.getPart ();                
                
                for (final var neiNoffset : cell.getMapNeighbours ()) {
                    if (neiNoffset.F == null) { continue; }
                    final var np = neiNoffset.F.getPart ();
                    if (cp == np) { continue; }
                    
                    // This is check that only top and left cells will be on fronts
                    if (neiNoffset.S.X != 1 && neiNoffset.S.Y != 1) { continue; }
                    
                    final var connection = IPoint.of (Math.min (cp, np), Math.max (cp, np));
                    fronts.putIfAbsent (connection, new ArrayList <LevelCell> ());
                    fronts.get (connection).add (Utils.min (cell, neiNoffset.F));
                }
            }
        }
        
        fronts.forEach ((__, cells) -> {
            Collections.shuffle (cells, r);
            var slits = 1 + r.nextInt (3);
            for (int i = 0; i < slits; i++) {
                final var cell = cells.get (i);
                final var nei = List.of (cell.getRight (), cell.getLeft ()).stream ()
                    . skip (r.nextBoolean () ? 1 : 0).findFirst ().orElse (null);
                if (nei == null) {
                    slits += 1;
                    continue;
                }
                
                if (cell.getRight () == nei) {
                    addPassage (cell, nei, LevelCell::setRightPass, LevelCell::setLeftPass, (a, b) -> true, null);
                    cell.getRightPass ().setGateType (GateType.SILT);
                } else {
                    addPassage (cell, nei, LevelCell::setBottomPass, LevelCell::setTopPass, (a, b) -> true, null);
                    cell.getBottomPass ().setGateType (GateType.SILT);
                }
            }
        });
        
        return context;
    }
    
    private static LevelGenerationContext generateTumblers (LevelGenerationContext context) {
        for (int p = 0; p < context.getSeeds ().size (); p++) {
            final var seed = context.getSeeds ().get (p);
            
            final var subpar2cells = context.getPart2subpart2cells ().get (p);
            for (int sp = 0; sp < subpar2cells.size (); sp++) {
                final var cells = subpar2cells.get (sp);
                Collections.shuffle (cells);
                
                if (cells.isEmpty ()) { continue; }
                final var cell = cells.get (0);
                
                for (int i = 0; i < context.getSeeds ().size (); i++) {
                    if (context.getSeeds ().get (i) == seed) { continue; }
                    
                    final var subpart2gates = context.getPart2subpart2gates ().getOrDefault (i, Map.of ());
                    if (subpart2gates.isEmpty ()) { continue; }
                    final var osp = r.nextInt (subpart2gates.size ());
                    
                    final var gates = subpart2gates.getOrDefault (osp, List.of ());
                    if (gates.isEmpty ()) { i--; continue; }
                    
                    final var gate = gates.get (r.nextInt (gates.size ()));
                    
                    System.out.println (cell.getPoint (0) + " -> " + gate.getFrom ()); // SYSOUT
                    final var tumbler = new LevelTumbler (cell, false);
                    tumbler.getClose ().add (gate);
                    tumbler.getOpen ().add (gate);
                    cell.setTumbler (tumbler);
                }
            }
        }
        
        return context;
    }
    
}
