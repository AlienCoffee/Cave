package ru.shemplo.cave.experimental;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
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
    
    private static final int parts = 4;
    private static final int size = parts * 15;
    
    public static void main (String ... args) {
        final var context = generateMaze (size, size, parts);
        final var maze = context.getMask ();
        
        System.out.println ("Seed points: " + context.getPartsSeeds ()); // SYSOUT
        System.out.println ("Exit: " + context.getExit ()); // SYSOUT
        
        System.out.println ("Map parts:"); // SYSOUT
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (maze [i][j].getPart () == -1) {
                    System.out.print (" "); // SYSOUT
                } else if (maze [i][j].getPart () == 0) {
                    System.out.print ("█"); // SYSOUT
                } else if (maze [i][j].getPart () == 1) {
                    System.out.print ("▒"); // SYSOUT
                } else if (maze [i][j].getPart () == 2) {
                    System.out.print ("▓"); // SYSOUT
                } else if (maze [i][j].getPart () == 3) {
                    System.out.print ("░"); // SYSOUT
                } else if (maze [i][j].getPart () == 4) {
                    System.out.print ("|"); // SYSOUT
                } else {
                    System.out.print (maze [i][j].getPart ()); // SYSOUT
                }
            }
            System.out.println (); // SYSOUT
        }
        
        /*
        System.out.println ("Map part:"); // SYSOUT
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (maze [i][j].getPart () == 0 && maze [i][j].getSubpart () == 0) {                    
                    System.out.print (maze [i][j].getSymbol ()); // SYSOUT
                } else {
                    System.out.print (" "); // SYSOUT
                }
            }
            System.out.println (); // SYSOUT
        }
        */
        
        System.out.println ("Subparts mask:"); // SYSOUT
        for (int p = 0; p < context.getParts (); p++) {            
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    if (maze [i][j].getPart () == p) {
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
            System.out.println (); // SYSOUT
        }
    }
    
    public static LevelGenerationContext generateMaze (int width, int height, int parts) {
        LevelGenerationContext context = new LevelGenerationContext (width, height, parts);
        context = generateMap (context);
        context = generatePartSeeds (context);
        context = generateParts (context);
        context = generateSubparts (context);
        context = generateGraph (context);
        context = generatePathWithinPart (context);
        
        /*
        var context = generateMask (width, height, parts);
        context = generatePassagesTree (context);
        context = generateSubparts (context, 5);
        
        context = generateCyclesWithinSubparts (context, 23);
        context = generateGatesBetweenSubparts (context);
        context = generateSlitsBetweenParts (context);
        context = generateExit (context);
        
        context = generateTumblers (context);
        */
        
        //return context;
        return context;
    }
    
    private static LevelGenerationContext generateMap (LevelGenerationContext context) {
        final var map = new LevelCell [context.getHeight ()][context.getWidth ()];
        
        for (int h = 0; h < map.length; h++) {
            for (int w = 0; w < map [h].length; w++) {
                map [h][w] = new LevelCell (w, h);
                map [h][w].setPart (-1);
                
                if (w > 0) {
                    map [h][w - 1].setRight (map [h][w]);
                    map [h][w].setLeft (map [h][w - 1]);
                }
                
                if (h > 0) {
                    map [h - 1][w].setBottom (map [h][w]);
                    map [h][w].setTop (map [h - 1][w]);
                }
            }
        }
        
        context.setMap (map);
        return context;
    }
    
    private static LevelGenerationContext generatePartSeeds (LevelGenerationContext context) {
        final var map = context.getMap ();
        
        final var minDistance = context.getWidth () * context.getHeight () / 210.0;
        System.out.println ("Min distance: " + minDistance); // SYSOUT
        final var seeds = new ArrayList <IPoint> ();
        
        pointsLoop:
        while (seeds.size () < context.getParts ()) {
            final var y = r.nextInt (map.length);
            final var x = r.nextInt (map [y].length);
            
            for (final var seed : seeds) {
                if (seed.distance (x, y) < minDistance) {
                    continue pointsLoop;
                }
            }
            
            seeds.add (IPoint.of (x, y));
        }
        
        System.out.println ("Part seeds: " + seeds); // SYSOUT
        context.setPartSeeds (seeds);
        return context;
    }
    
    private static LevelGenerationContext generateParts (LevelGenerationContext context) {
        final var seeds = context.getPartSeeds ();
        final var map = context.getMap ();
        
        final var minCriteria = context.getWidth () * context.getHeight () / (context.getParts () + 1);
        System.out.println ("Min criteria: " + minCriteria); // SYSOUT
        int attempts = 100;
        
        final var part2cells = new ArrayList <List <LevelCell>> ();
        
        generatorLoop:
        while (attempts-- > 0) {
            for (int h = 0; h < map.length; h++) {
                for (int w = 0; w < map [h].length; w++) {
                    map [h][w].setPart (-1);
                }
            }
            
            final var fronts = new ArrayList <Stack <LevelCell>> ();
            for (int i = 0; i < context.getParts (); i++) {
                if (part2cells.size () > i) {
                    part2cells.get (i).clear ();
                } else {                    
                    part2cells.add (new ArrayList <> ());
                }
                fronts.add (new Stack <> ());
                
                final var seed = seeds.get (i);
                final var cell = map [seed.Y][seed.X];
                
                fronts.get (i).add (cell);
                cell.setPart (i);
            }
            
            final var permutation = new ArrayList <> (List.of (0, 1, 2, 3));
            Collections.shuffle (permutation, r); // initial permutation
            
            boolean updated = true;
            while (updated) {
                updated = false;
                
                for (int i = 0; i < fronts.size (); i++) {
                    final var front = fronts.get (i);
                    if (front.isEmpty ()) { continue; }
                    updated = true;
                    
                    final var cell = front.pop ();
                    
                    final var neis = cell.getMapNeighbors ();
                    Collections.shuffle (permutation, r);
                    
                    for (final var index : permutation) {
                        final var nei = neis.get (index).F;
                        if (nei == null) { continue; }
                        
                        if (nei.getPart () == -1) {
                            part2cells.get (cell.getPart ()).add (nei);
                            nei.setPart (cell.getPart ());
                            front.add (nei);
                        }
                    }
                }
            }
            
            for (int i = 0; i < part2cells.size (); i++) {
                if (part2cells.get (i).size () < minCriteria) {
                    continue generatorLoop;
                }
            }
            
            break;
        }
        
        System.out.println ("Attempts left: " + attempts); // SYSOUT
        for (int i = 0; i < part2cells.size (); i++) {
            System.out.println ("Part #" + i + " has " + part2cells.get (i).size () + " cells"); // SYSOUT
        }
        
        context.setPart2cells (part2cells);
        return context;
    }
    
    private static LevelGenerationContext generateSubparts (LevelGenerationContext context) {
        //final var map = context.getMap ();
        final var subparts = 5;
        
        final var queues = new ArrayList <Queue <LevelCell>> ();
        final var part2subpart2cells = new ArrayList <List <List <LevelCell>>> ();
        
        for (int p = 0; p < context.getParts (); p++) {
            final var points = distributePoints (context, p, subparts);
            part2subpart2cells.add (new ArrayList <> ());
            
            for (int i = 0; i < points.size (); i++) {
                part2subpart2cells.get (p).add (new ArrayList <> ());
                queues.add (new LinkedList <> ());
                
                part2subpart2cells.get (p).get (i).add (points.get (i));
                queues.get (i).add (points.get (i));
                points.get (i).setSubpart (i);
            }
            
            boolean updated = true;
            while (updated) {
                updated = false;
                
                for (int i = 0; i < subparts; i++) {
                    if (queues.get (i).isEmpty ()) { continue; }
                    final var cell = queues.get (i).poll ();
                    updated = true;
                    
                    for (final var nei : cell.getMapNeighbors ()) {
                        if (nei.F == null || nei.F.getPart () != cell.getPart ()) {
                            continue;
                        }
                        
                        if (nei.F.getSubpart () == -1) {
                            part2subpart2cells.get (p).get (cell.getSubpart ()).add (nei.F);
                            nei.F.setSubpart (cell.getSubpart ());
                            queues.get (i).add (nei.F);
                        }
                    }
                }
            }
        }
        
        context.setPart2subpart2cells (part2subpart2cells);
        return context;
    }
    
    private static List <LevelCell> distributePoints (LevelGenerationContext context, int part, int pointsNumber) {
        final var cells = context.getPart2cells ().get (part);
        final var points = new HashSet <IPoint> ();
        final var map = context.getMap ();
        
        final var distances = new double [pointsNumber];
        fillPoints (cells, points, pointsNumber);
        var distanceBound = 50.0;
        
        while (distanceBound > 0) {
            final var pointsList = List.copyOf (points);
            Arrays.fill (distances, 1_000_000.0);
            
            for (int i = 0; i < pointsNumber; i++) {
                final var point  = pointsList.get (i);
                for (int j = 0; j < pointsNumber; j++) {
                    if (i == j) { continue; }
                    
                    distances [i] = Math.min (distances [i], point.distance (pointsList.get (j)));
                }
            }
            
            int needToDistribute = -1;
            for (int i = 0; i < pointsNumber; i++) {
                if (distances [i] < distanceBound) {
                    needToDistribute = i;
                    break;
                }
            }
            
            if (needToDistribute == -1) {
                break; // distances between dots is in required bounds
            } else {
                points.remove (pointsList.get (needToDistribute));
                fillPoints (cells, points, pointsNumber);
                distanceBound -= 0.01;                
            }
        }
        
        System.out.println ("Set up distance: " + distanceBound); // SYSOUT
        return points.stream ().map (p -> map [p.Y][p.X]).collect (Collectors.toList ());
    }
    
    private static void fillPoints (List <LevelCell> cells, Set <IPoint> points, int need) {
        while (points.size () < need) {
            final var cell = cells.get (r.nextInt (cells.size ()));
            final var point = cell.getPoint (0);
            if (!points.contains (point)) {
                points.add (point);
            }
        }
    }
    
    private static LevelGenerationContext generateGraph (LevelGenerationContext context) {
        final var graph = new LevelGenerationGraph (context);
        context.setGraph (graph);
        
        final var subpart2neigbors = new HashMap <Integer, Set <Integer>> ();
        final var toAdd = new ArrayList <Integer> ();
        
        for (int p = 0; p < context.getParts (); p++) {
            for (int i = 0; i < context.getPart2subpart2cells ().get (p).size (); i++) {
                final var node = new LevelCell (0, i);
                node.setPart (p); node.setSubpart (i);
                
                graph.getPart2node ().put (node.getPartPoint (), node);
                graph.getNodes ().add (node);
            }
        }
        
        for (int p = 0; p < context.getParts (); p++) {
            final var subpart2cells = context.getPart2subpart2cells ().get (p);
            subpart2neigbors.clear ();
            
            for (int sp = 0; sp < subpart2cells.size (); sp++) {
                subpart2neigbors.put (sp, new HashSet <> ());
            }
            
            for (int sp = 0; sp < subpart2cells.size (); sp++) {
                toAdd.clear ();
                
                for (final var cell : subpart2cells.get (sp)) {
                    final var csp = cell.getSubpart ();
                    
                    for (final var nei : cell.getMapNeighbors ()) {
                        if (nei.F == null || nei.F.getPart () != cell.getPart ()) { 
                            continue; 
                        }
                        
                        final var nsp = nei.F.getSubpart ();
                        
                        if (nsp != csp) {
                            if (subpart2neigbors.get (csp).add (nsp)) {
                                toAdd.add (nsp);
                            }
                            
                            subpart2neigbors.get (nsp).add (csp);
                        }
                    }
                }
                
                for (final var add : toAdd) {
                    final var a = graph.getPart2node ().get (IPoint.of (p, sp));
                    final var b = graph.getPart2node ().get (IPoint.of (p, add));
                    
                    final var passage = LevelPassage.of (a, b, null);
                    a.getNeighbors ().add (passage);
                    b.getNeighbors ().add (passage);
                }
            }
        }
        
        return context;
    }
    
    private static LevelGenerationContext generatePathWithinPart (LevelGenerationContext context) {
        final var graph = context.getGraph ();
        
        final var part2order = new ArrayList <List <LevelCell>> ();
        for (int p = 0; p < context.getParts (); p++) {
            final var subpart2cells = context.getPart2subpart2cells ().get (p);
            
            final var nodes = new ArrayList <LevelCell> ();
            for (int sp = 0; sp < subpart2cells.size (); sp++) {
                nodes.add (graph.getPart2node ().get (IPoint.of (p, sp)));
            }
            
            final var leafs = nodes.stream ().filter (cell -> cell.getNeighbors ().size () < 2).count ();
            if (leafs > 2) {
                throw new IllegalStateException ("Path is impossible");
            }
            
            //nodes.sort (Comparator.comparing (cell -> cell.getNeighbors ().size ()));
            final var path = new ArrayList <LevelPassage> ();
            pathLoop: do {
                Collections.shuffle (nodes, r);
                path.clear ();
                
                for (int n = 0; n < nodes.size () - 1; n++) {
                    final var next = nodes.get (n + 1);
                    final var current = nodes.get (n);
                    
                    boolean found = false;
                    for (final var passage : current.getNeighbors ()) {
                        final var nei = passage.getAnother (current);
                        if (next.getPart () == nei.getPart () && next.getSubpart () == nei.getSubpart ()) {
                            found = true;
                            break;
                        }
                    }
                    
                    if (!found) {
                        continue pathLoop;
                    }
                }
                
                break; // path is generated
            } while (true);
            
            System.out.println ("Paths are generated"); // SYSOUT
            part2order.add (nodes);
        }
        
        context.setPart2subpartsOrder (part2order);
        return context;
    }
    
    private static LevelGenerationContext generateTreeWithinSubparts (LevelGenerationContext context) {
        final var stack = new Stack <LevelCell> ();
        
        for (int p = 0; p < context.getParts (); p++) {
            final var subpart2cells = context.getPart2subpart2cells ().get (p);
            
            final var nodes = new ArrayList <LevelCell> ();
            for (int sp = 0; sp < subpart2cells.size (); sp++) {
                final var cells = subpart2cells.get (sp);
                if (cells.isEmpty ()) { continue; }
                
                final var ininital = cells.get (0);
                stack.add (ininital);
                stack.clear (); 
            }
        }
        
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
             //. part2cells (part2cells)
             . partSeeds (seeds)
             . map (maze)
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
        
        for (final var seed: context.getPartsSeeds ()) {
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
        for (int p = 0; p < context.getPartsSeeds ().size (); p++) {
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
                    
                    for (final var passage : cell.getPassageNeighbors ()) {                        
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
                for (final var passage : cell.getPassageNeighbors ()) {                        
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
        context = generateGatesOnTreePassages (context);
        
        
        
        return context;
    }
    
    private static LevelGenerationContext generateGatesOnTreePassages (LevelGenerationContext context) {
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
            
            p2sp2gs.get (cp).get (csp).add (passage);
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
                
                for (final var neiNoffset : cell.getMapNeighbors ()) {
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
                    cell.getRightPass ().setGateType (GateType.SLIT);
                } else {
                    addPassage (cell, nei, LevelCell::setBottomPass, LevelCell::setTopPass, (a, b) -> true, null);
                    cell.getBottomPass ().setGateType (GateType.SLIT);
                }
            }
        });
        
        return context;
    }
    
    private static LevelGenerationContext generateExit (LevelGenerationContext context) {
        final var part = r.nextInt (context.getPartsSeeds ().size ());
        final var subpart2cells = context.getPart2subpart2cells ().get (part);
        
        final var exit = subpart2cells.stream ().sorted (Comparator.comparing (List::size))
            . findFirst ().map (cells -> cells.get (0)).orElse (null);
        context.setExit (exit);
        exit.setExit (true);
        
        return context;
    }
    
    private static LevelGenerationContext generateTumblers (LevelGenerationContext context) {
        final var mask = context.getMask ();
        
        final var graph = buildMazeGraph (context);
        graph.runFloydWarshall ();
        context.setGraph (graph);
        
        /*
        for (int i = 0; i < graph.getNodes ().size (); i++) {
            System.out.println (Arrays.toString (graph.getDistances () [i])); // SYSOUT
        }
        */
        
        final var destination = graph.getPart2node ().get (context.getExit ().getPartPoint ());
        System.out.println (destination); // SYSOUT
        int primaryWalksSize = 0;
        
        final var part2walks = new HashMap <Integer, List <LevelPassage>> ();
        
        for (final var seed : context.getPartsSeeds ()) {
            System.out.println ("Seed: " + seed); // SYSOUT
            final var node = graph.getPart2node ().get (mask [seed.Y][seed.X].getPartPoint ());
            final var walksNisPrimary = generateRandomWalks (graph, node, destination, 11);
            if (walksNisPrimary.S) {
                // All other walks will be cut off after this number of steps
                primaryWalksSize = walksNisPrimary.F.size ();
            }
            
            part2walks.put (node.getPart (), walksNisPrimary.F);
        }
        
        final var order = IntStream.range (1, context.getPartsSeeds ().size () + 1)
            . filter (i -> i != destination.getPart ()).mapToObj (i -> i)
            . collect (Collectors.toList ());
        Collections.shuffle (order, r);
        // Door to exit will be opened the last
        order.add (destination.getPart ());
        
        final var inSeed = context.getPartsSeeds ().get (destination.getPart () - 1);
        var inPart = mask [inSeed.Y][inSeed.X].getPartPoint ();
        
        final LevelCell [] locations = new LevelCell [order.size ()];
        for (int i = 0; i < order.size (); i++) {
            final var seed = context.getPartsSeeds ().get (i);
            locations [i] = graph.getPart2node ().get (mask [seed.Y][seed.X].getPartPoint ());
        }
        
        for (int round = 0; round < primaryWalksSize; round++) {
            for (int currentPart : order) {
                final var walk = part2walks.get (currentPart).get (round);
                final var walkTo = walk.getAnother (locations [currentPart - 1]);
                
                final var possibleGates = locations [currentPart - 1].getNeighbors ().stream ().filter (passage -> {
                    final var nei = passage.getAnother (locations [currentPart - 1]);
                    return nei.getPart () == walkTo.getPart () && nei.getSubpart () == walkTo.getSubpart ();
                }).collect (Collectors.toList ());
                //System.out.println ("Possible gates: " + possibleGates.size ()); // SYSOUT
                Collections.shuffle (possibleGates, r);
                
                locations [currentPart - 1] = possibleGates.get (0).getAnother (locations [currentPart - 1]);
                
                final var close = possibleGates.subList (1, possibleGates.size ()).stream ()
                    . map (LevelPassage::getPrototype).collect (Collectors.toList ());
                final var open = possibleGates.get (0).getPrototype ();
                
                final var cells = context.getPart2subpart2cells ().get (inPart.F - 1).get (inPart.S - 1);
                inPart = locations [currentPart - 1].getPartPoint ();
                //System.out.println ("Possible cells: " + cells.size ()); // SYSOUT
                int attempts = 0;
                do {
                    final var cell = cells.get (r.nextInt (cells.size ()));
                    if (cell.getTumbler () == null) {
                        System.out.println (cell.getPoint (0) + " will open " + open.getFrom ().getPoint (0)); // SYSOUT
                        final var tumbler = new LevelTumbler (cell, false);
                        tumbler.getClose ().addAll (close);
                        tumbler.getOpen ().add (open);
                        cell.setTumbler (tumbler);
                        
                        break;
                    }
                } while (attempts++ < 1000);
            }
        }
        
        return context;
    }
    
    private static Pair <List <LevelPassage>, Boolean> generateRandomWalks (
        LevelGenerationGraph graph, LevelCell node, 
        LevelCell destination, int stepsLimit
    ) {
        final var startDistance = graph.getDistances () [node.getY ()][destination.getY ()];
        final boolean canReach = startDistance <= stepsLimit;
        final var walks = new ArrayList <LevelPassage> ();
        final var dists = graph.getDistances ();
        
        var currentNode = node;
        while (stepsLimit-- > 0) {
            //System.out.println ("Steps: " + steps); // SYSOUT
            final var neis = currentNode.getNeighbors ();
            Collections.shuffle (neis, r);
            
            for (final var passage : neis) {   
                final var nei = passage.getAnother (currentNode);
                //System.out.println (nei.getY () + " | " + dists [nei.getY ()][destination.getY ()]); // SYSOUT
                if ((canReach && stepsLimit >= dists [nei.getY ()][destination.getY ()]) || !canReach) {
                    System.out.println (currentNode.getPartPoint () + " -> " + nei.getPartPoint ()); // SYSOUT
                    walks.add (passage);
                    currentNode = nei;
                    break;
                }
            }            
        }
        
        return Pair.mp (walks, canReach);
    }
    
    private static LevelGenerationGraph buildMazeGraph (LevelGenerationContext context) {
        final var graph = new LevelGenerationGraph (context);
        
        final var counter = new AtomicInteger ();
        for (final var seed : context.getPartsSeeds ()) {            
            final var passages = new HashSet <LevelPassage> ();
            final var queue = new LinkedList <IPoint> ();
            final var visited = new HashSet <IPoint> ();
            
            final var startPoint = context.getMask () [seed.Y][seed.X].getPartPoint ();
            queue.add (startPoint); visited.add (startPoint);
            
            while (!queue.isEmpty ()) {
                final var point = queue.poll ();
                final var cnode = graph.getPart2node ().computeIfAbsent (point, k -> {
                    final var nd = new LevelCell (0, counter.getAndIncrement ());
                    graph.getNodes ().add (nd);
                    nd.setSubpart (k.S);
                    nd.setPart (k.F);
                    
                    return nd;
                });
                
                final var gates = context.getPart2subpart2gates ().getOrDefault (point.F, Map.of ())
                        . getOrDefault (point.S, List.of ());
                for (final var gate : gates) {
                    final IPoint ap = gate.getFrom ().getPartPoint (), 
                            bp = gate.getTo ().getPartPoint ();
                    final var next = ap.X == point.X && ap.Y == point.Y ? bp 
                            : (bp.X == point.X && bp.Y == point.Y ? ap : null);
                    if (next == null) { continue; }
                    
                    final var node = graph.getPart2node ().computeIfAbsent (next, k -> {
                        final var nd = new LevelCell (0, counter.getAndIncrement ());
                        graph.getNodes ().add (nd);
                        nd.setSubpart (k.S);
                        nd.setPart (k.F); 
                        
                        return nd;
                    });
                    
                    final var nodePoint = node.getPartPoint ();
                    
                    final var edge = LevelPassage.of (cnode, node, null);
                    edge.setPrototype (gate);
                    
                    if (!passages.contains (edge)) {
                        cnode.getNeighbors ().add (edge);                    
                        node.getNeighbors ().add (edge);
                        passages.add (edge);
                    }
                    
                    if (!visited.contains (nodePoint)) {
                        visited.add (nodePoint);
                        queue.add (nodePoint);
                    }
                }
            }
        }
        
        return graph;
    }
    
    @SuppressWarnings ("unused")
    private static int getRandomIntExcept (int bound, int excpet, Random r) {
        int value = r.nextInt (bound);
        while (value == excpet) {
            value = r.nextInt (bound);
        }
        
        return value;
    }
    
}
