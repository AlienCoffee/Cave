package ru.shemplo.cave.utils;


public class Utils {
    
    public static <T extends Comparable <T>> T min (T a, T b) {
        if (a.compareTo (b) > 0) {
            return b;
        } else {
            return a;
        }
    }
    
    public static <T extends Comparable <T>> T max (T a, T b) {
        if (a.compareTo (b) < 0) {
            return b;
        } else {
            return a;
        }
    }
    
}
