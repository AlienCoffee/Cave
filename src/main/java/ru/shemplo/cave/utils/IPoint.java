package ru.shemplo.cave.utils;

import ru.shemplo.snowball.stuctures.Pair;

public class IPoint extends Pair <Integer, Integer> {
    
    private static final long serialVersionUID = 7505452512927984001L;

    public final int X, Y;
    
    public final double D;
    
    public IPoint (int x, int y) {
        this (x, y, 0);
    }
    
    public IPoint (int x, int y, double distance) {
        super (x, y); X = x; Y = y; D = distance;
    }
    
    public double distance (IPoint point) {
        return distance (this, point);
    }
    
    public double distance (int x, int y) {
        return distance (this, IPoint.of (x, y));
    }
    
    public static IPoint of (int x, int y) {
        return of (x, y, 0);
    }
    
    public static IPoint of (int x, int y, double distance) {
        return new IPoint (x, y, distance);
    }
    
    public static double distance (IPoint a, IPoint b) {
        return Math.sqrt ((a.X - b.X) * (a.X - b.X) + (a.Y - b.Y) * (a.Y - b.Y));
    }
    
}
