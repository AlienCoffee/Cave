package ru.shemplo.cave.experimental;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import ru.shemplo.snowball.stuctures.Pair;

public class MazeGenerator {
    
    private static final Random r = new Random ();
    
    private static final int parts = 3;
    private static final int size = parts * 20;
    
    public static void main (String ... args) {
        final var maze = generateMaze (size, parts);
        
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (maze [i][j] == 0) {
                    System.out.print ("█"); // SYSOUT
                } else if (maze [i][j] == 10) {                    
                    System.out.print ("!"); // SYSOUT
                } else {                    
                    System.out.print (" "); // SYSOUT
                }
            }
            System.out.println (); // SYSOUT
        }
    }
    
    public static int [][] generateMaze (int size, int parts) {
        final int [][] maze = new int [size][size];
        
        final List <Pair <Integer, Integer>> seeds = new ArrayList <> ();
        
        seedsGenerator:
        while (seeds.size () < parts) {
            final int x = r.nextInt (size), y = r.nextInt (size);
            for (var seed : seeds) {
                if (Math.abs (seed.F - x) <= 2 || Math.abs (seed.S - y) <= 2
                        || x < 1 || y < 1 || x >= size - 1 || y >= size - 1) {
                    continue seedsGenerator;
                }
            }
            
            seeds.add (Pair.mp (x, y));
            maze [y][x] = 10;
        }
        
        System.out.println (seeds); // SYSOUT
        
        final var canMore = new boolean [parts];
        Arrays.fill (canMore, true);
        
        final var index2points = new HashMap <Integer, List <Pair <Integer, Integer>>> ();
        final var index2pointsSet = new HashMap <Integer, Set <String>> ();
        final var point2index = new HashMap <String, Integer> ();
        for (int i = 0; i < parts; i++) {
            final var seed = seeds.get (i);
            
            index2pointsSet.put (i, new HashSet <> ());
            index2pointsSet.get (i).add (seed.toString ());
            
            index2points.put (i, new ArrayList <> ());
            point2index.put (seed.toString (), i);
            index2points.get (i).add (seed);
        }
        
        final var search = new ArrayList <> (List.of (
            Pair.mp (-1, 0), Pair.mp (0, 1), Pair.mp (1, 0), Pair.mp (0, -1), Pair.mp (1, 0), Pair.mp (-1, 0)
        ));
        
        while (true) {
            int updated = 0;
            for (int i = 0; i < seeds.size (); i++) {
                if (!canMore [i]) { continue; }
                
                final var points = index2points.get (i);
                Collections.shuffle (points, r);
                
                Pair <Integer, Integer> toAdd = null;
                
                newPointFinder:
                for (var point : points) {
                    final int px = point.F, py = point.S;
                    
                    Collections.shuffle (search, r);
                    for (var s : search) {                        
                        final int rx = s.F, ry = s.S;
                        
                        final var suggested = Pair.mp (px + rx, py + ry);
                        if (index2pointsSet.get (i).contains (suggested.toString ())) { continue; }
                        
                        final var index = point2index.getOrDefault (suggested.toString (), -1);
                        if (suggested.F < 1 || suggested.S < 1 || suggested.F >= size - 1 || suggested.S >= size - 1
                                || maze [suggested.S][suggested.F] != 0 || index != -1) { 
                            continue; 
                        }
                        
                        final int drx = rx * 2, dry = ry * 2;
                        if (drx != 0) {                                
                            if (checkPoint (point2index, px + drx, py + ry - 1, i) 
                                    && checkPoint (point2index, px + drx, py + ry + 0, i) 
                                    && checkPoint (point2index, px + drx, py + ry + 1, i) 
                                    && checkPoint (point2index, px + rx, py + ry - 1, i)
                                    && checkPoint (point2index, px + rx, py + ry + 1, i)) {
                                toAdd = suggested;
                                break newPointFinder;
                            }
                        } else if (dry != 0) {
                            if (checkPoint (point2index, px + rx - 1, py + dry, i) 
                                    && checkPoint (point2index, px + rx + 0, py + dry, i)
                                    && checkPoint (point2index, px + rx + 1, py + dry, i) 
                                    && checkPoint (point2index, px + rx - 1, py + ry, i)
                                    && checkPoint (point2index, px + rx + 1, py + ry, i)) {
                                toAdd = suggested;
                                break newPointFinder;
                            }
                        }
                    }
                }
                
                if (toAdd != null) {                    
                    index2pointsSet.get (i).add (toAdd.toString ());
                    point2index.put (toAdd.toString (), i);
                    index2points.get (i).add (toAdd);
                    
                    maze [toAdd.S][toAdd.F] = 1;
                    
                    updated++;
                } else {                    
                    canMore [i] = false;
                }                
            }
            
            if (updated == 0) {
                break;
            }
        }
        
        return maze;
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
    
    private static boolean checkPoint (Map <String, Integer> point2index, int x, int y, int index) {
        final var p = Pair.mp (x, y);
        final var i = point2index.getOrDefault (p.toString (), -1);
        
        return i == -1; //|| i == index;
    }
    
}
