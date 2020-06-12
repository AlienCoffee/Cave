package ru.shemplo.cave.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
    
    public static String digest (String string, String salt) {
        try {
            final var digest = MessageDigest.getInstance ("SHA3-256");
            
            final var bytes = String.format ("%s-%s", string, salt).getBytes (StandardCharsets.UTF_8);
            return new String (digest.digest (bytes), StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException nsae) {
            throw new IllegalStateException ();
        }
    }
    
}
