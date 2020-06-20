package ru.shemplo.cave.experimental;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ru.shemplo.cave.app.entity.level.GateType;
import ru.shemplo.cave.app.entity.level.LevelCell;
import ru.shemplo.cave.app.entity.level.LevelPassage;
import ru.shemplo.cave.app.entity.level.LevelTumbler;
import ru.shemplo.cave.utils.IPoint;

public class MazeGenerator {
    
    private static final Random r = new Random (1L);
    
    private static final int parts = 2;
    private static final int size = parts * 5;
    
    public static void main (String ... args) {
        final var context = generateMaze (size, size, parts);
        final var maze = context.getMap ();
        
        System.out.println ("Seed points: " + context.getPartSeeds ()); // SYSOUT
        System.out.println ("Spawns: " + context.getSpawns ()); // SYSOUT
        System.out.println ("Exit: " + context.getExit ()); // SYSOUT
        
        System.out.println ("Map parts:"); // SYSOUT
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (maze [i][j].getPart () == -1) {
                    System.out.print (" ");
                } else if (maze [i][j].getPart () == 0) {
                    System.out.print ("█");
                } else if (maze [i][j].getPart () == 1) {
                    System.out.print ("▒");
                } else if (maze [i][j].getPart () == 2) {
                    System.out.print ("▓");
                } else if (maze [i][j].getPart () == 3) {
                    System.out.print ("░");
                } else if (maze [i][j].getPart () == 4) {
                    System.out.print ("|");
                } else {
                    System.out.print (maze [i][j].getPart ());
                }
            }
            System.out.println ();
        }
        
        System.out.println ("Subparts masks:"); // SYSOUT
        for (int p = 0; p < context.getParts (); p++) {
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    if (maze [i][j].getPart () == p) {
                        if (maze [i][j].getSubpart () == 10) {
                            System.out.print ("█");
                        } else if (maze [i][j].getSubpart () == 11) {
                            System.out.print ("▒");
                        } else if (maze [i][j].getSubpart () == 12) {
                            System.out.print ("▓");
                        } else if (maze [i][j].getSubpart () == 13) {
                            System.out.print ("░"); 
                        } else if (maze [i][j].getSubpart () == 14) {
                            System.out.print ("|"); 
                        } else if (maze [i][j].getSubpart () > 14) {
                            System.out.print ("*");            
                        } else {
                            System.out.print (maze [i][j].getSubpart ());
                        }
                    } else {
                        System.out.print (" ");
                    }
                }
                System.out.println ();
            }
            System.out.println ();
        }
        
        System.out.println ("Subpart trees:"); // SYSOUT
        for (int p = 0; p < context.getParts (); p++) {            
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    if (maze [i][j].getPart () == p && maze [i][j].getSubpart () >= 0) {                    
                        System.out.print (maze [i][j].getSymbol ());
                    } else {
                        System.out.print (" ");
                    }
                }
                System.out.println ();
            }
        }
        
        System.out.println ("Maze tree:"); // SYSOUT
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                System.out.print (maze [i][j].getSymbol ());
            }
            System.out.println ();
        }
    }
    
    public static LevelGenerationContext generateMaze (int width, int height, int parts) {
        LevelGenerationContext context = new LevelGenerationContext (width, height, parts, 3);
        context = generateMap (context);
        context = generatePartSeeds (context);
        context = generateParts (context);
        context = generateSubparts (context);
        context = generateGraph (context);
        context = generatePathWithinPart (context);
        context = generateTreeWithinSubparts (context);
        context = generateGatesBetweenSubparts (context);
        context = generateSpawnsAndExit (context);
        context = generateTumblers (context);
        
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
        final var subparts = context.getSubparts ();
        
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
            
            part2order.add (nodes);
        }
        
        System.out.println ("Paths are generated"); // SYSOUT
        context.setPart2subpartsOrder (part2order);
        return context;
    }
    
    private static LevelGenerationContext generateTreeWithinSubparts (LevelGenerationContext context) {
        final var order = new ArrayList <> (List.of (0, 1, 2, 3));
        final var included = new ArrayList <LevelCell> ();
        final var toRemove = new ArrayList <LevelCell> ();
        final var toAdd = new ArrayList <LevelCell> ();
        
        for (int p = 0; p < context.getParts (); p++) {
            final var subpart2cells = context.getPart2subpart2cells ().get (p);
            
            for (int sp = 0; sp < subpart2cells.size (); sp++) {
                final var cells = subpart2cells.get (sp);
                if (cells.isEmpty ()) { continue; }
                included.clear ();
                                
                included.add (cells.get (0));
                boolean updated = true;
                
                while (updated) {
                    Collections.shuffle (included, r);
                    updated = false;
                    
                    toRemove.clear ();
                    toAdd.clear ();
                    
                    for (final var cell : included) {
                        final var neis = cell.getMapNeighbors ();
                        Collections.shuffle (order, r);
                        boolean active = false;
                        
                        neighborLoop:
                        for (final var index : order) {
                            final var nei = neis.get (index);
                            
                            if (nei.F == null || nei.F.getPart () != cell.getPart ()
                                    || nei.F.getSubpart () != cell.getSubpart ()) {
                                continue;
                            }
                            
                            for (final var passage : nei.F.getPassageNeighbors ()) {
                                if (passage.F != null) {
                                    // cell is already connected
                                    continue neighborLoop;
                                }
                            }
                            
                            final var passage = LevelPassage.of (cell, nei.F, null);
                            nei.F.setPassageNeighbor (-nei.S.X, -nei.S.Y, passage);
                            cell.setPassageNeighbor (nei.S.X, nei.S.Y, passage);
                            toAdd.add (nei.F);
                            updated = true;
                            active = true;
                            break;
                        }
                        
                        if (!active) {
                            toRemove.add (cell);
                        }
                    }
                    
                    included.removeAll (toRemove);
                    included.addAll (toAdd);
                }
            }
        }
        
        return context;
    }
    
    private static LevelGenerationContext generateGatesBetweenSubparts (LevelGenerationContext context) {
        final var part2gates = new ArrayList <List <LevelPassage>> ();
        
        for (int p = 0; p < context.getParts (); p++) {
            final var subpart2cells = context.getPart2subpart2cells ().get (p);
            final var pathOrder = context.getPart2subpartsOrder ().get (p);
            part2gates.add (new ArrayList <> ());
            
            connectorLoop:
            for (int i = 0; i < pathOrder.size () - 1; i++) {
                final var next = pathOrder.get (i + 1);
                final var current = pathOrder.get (i);
                
                final var cells = subpart2cells.get (current.getSubpart ());
                Collections.shuffle (cells, r);
                
                for (final var cell : cells) {
                    for (final var nei : cell.getMapNeighbors ()) {
                        if (nei.F == null || nei.F.getPart () != cell.getPart ()) { 
                            continue; 
                        }
                        
                        if (nei.F.getSubpart () == next.getSubpart ()) {
                            final var passage = LevelPassage.of (cell, nei.F, GateType.GATE);
                            nei.F.setPassageNeighbor (-nei.S.X, -nei.S.Y, passage);
                            cell.setPassageNeighbor (nei.S.X, nei.S.Y, passage);
                            part2gates.get (p).add (passage);
                            
                            continue connectorLoop;
                        }
                    }
                }
            }
        }
        
        context.setPart2gates (part2gates);
        return context;
    }
    
    private static LevelGenerationContext generateSpawnsAndExit (LevelGenerationContext context) {
        final var exitPart = r.nextInt (context.getParts ());
        System.out.println ("Exit part: " + exitPart); // SYSOUT
        
        final var spawns = new ArrayList <LevelCell> ();
        
        for (int p = 0; p < context.getParts (); p++) {
            final var subpart2cells = context.getPart2subpart2cells ().get (p);
            final var pathOrder = context.getPart2subpartsOrder ().get (p);
            
            final var cells = subpart2cells.get (pathOrder.get (0).getSubpart ());
            spawns.add (cells.get (r.nextInt (cells.size ())));
            spawns.get (p).setSpawn (true);
            
            if (p == exitPart) {
                final var lastSubpart = pathOrder.get (pathOrder.size () - 1).getSubpart ();
                final var exitCells = subpart2cells.get (lastSubpart);
                
                context.setExit (exitCells.get (r.nextInt (exitCells.size ())));
                context.getExit ().setExit (true);
                
                System.out.println ("Exit: " + context.getExit ()); // SYSOUT
            }
        }
        
        context.setSpawns (spawns);
        return context;
    }
    
    private static LevelGenerationContext generateTumblers (LevelGenerationContext context) {
        final var partsOrder = IntStream.range (0, context.getParts ())
            . boxed ().collect (Collectors.toList ());
        
        final var exitPart = context.getExit ().getPart ();
        var round = context.getPart2gates ().get (exitPart).size ();
        final var length = round;
        
        final var ps = context.getParts ();
        
        final var currentLocations = new int [ps];
        for (final var spawn : context.getSpawns ()) {
            currentLocations [spawn.getPart ()] = spawn.getSubpart ();
        }
        
        final var tumblers = new ArrayList <LevelTumbler> ();
        
        while (round > 0) {
            Collections.shuffle (partsOrder, r);
            partsOrder.remove (partsOrder.indexOf (exitPart));
            partsOrder.add (0, exitPart); // move exit part to the start
            
            for (int i = 0; i < partsOrder.size (); i++) {
                final var next = partsOrder.get ((i + 1) % ps);
                final var current = partsOrder.get (i);
                
                final var gate = context.getPart2gates ().get (next).get (length - round);
                final var gatePart = gate.getFrom ().getPart ();
                
                final var nextLocation = gate.getFrom ().getSubpart () == currentLocations [gatePart]
                    ? gate.getTo ().getSubpart () : gate.getFrom ().getSubpart ();
                currentLocations [gatePart] = nextLocation;
                
                final var subpart = currentLocations [current];
                final var cells = context.getPart2subpart2cells ().get (current).get (subpart);
                final var location = cells.get (r.nextInt (cells.size ()));
                
                if (location.getTumbler () == null) {
                    final var tumbler = new LevelTumbler (location, false);
                    location.setTumbler (tumbler);
                    tumblers.add (tumbler);
                }
                
                final var tumbler = location.getTumbler ();
                tumbler.getOpen ().add (gate);
                
                if (current == exitPart && length - round == 0) {
                    continue; // no enter gates for start location
                } 
                
                final var index = length - round + (current == exitPart ? -1 : 0);
                final var enterGate = context.getPart2gates ().get (current).get (index);
                tumbler.getClose ().add (enterGate);
            }
            
            round -= 1;
        }
        
        System.out.println ("Tumblers:"); // SYSOUT
        tumblers.forEach (tumbler -> {
            System.out.print (tumbler.getCell ().getPartPoint () + " (" + tumbler.getCell ().getPoint (0) + ") ~~ "); // SYSOUT
            tumbler.getOpen ().forEach (open -> {
                System.out.print (open.getFrom ().getPartPoint () + " (" + open.getFrom ().getPoint (0) + ") <-> "
                        + open.getTo ().getPartPoint () + " (" + open.getTo ().getPoint (0) + "); "); // SYSOUT
            });
            tumbler.getClose ().forEach (close -> {
                System.out.print (close.getFrom ().getPartPoint () + " (" + close.getFrom ().getPoint (0) + ") <x> "
                        + close.getTo ().getPartPoint () + " (" + close.getTo ().getPoint (0) + "); "); // SYSOUT
            });
            System.out.println (); // SYSOUT
        });
        
        return context;
    }
    
}
