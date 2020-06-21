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
            
            final var input = String.format ("%s-%s", string, salt).getBytes (StandardCharsets.UTF_8);
            final var bytes = digest.digest (input);
            final var sb = new StringBuilder ();
            for (int i = 0; i < bytes.length; i++) {
                sb.append (String.format ("%02x", bytes [i]));
            }
            
            return sb.toString ();
        } catch (NoSuchAlgorithmException nsae) {
            throw new IllegalStateException ();
        }
    }
    
    public static String flatString (String string) {
        return string.replaceAll ("(\n|\r)", " ");
    }
    
}
