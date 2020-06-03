package ru.shemplo.cave.app;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ApplicationConstants {
    
    public static final String TITLE = "MyAdminer v0.0.1";
    
    public static final String LAYOUTs_PATH = "/layout";
    public static final String LAYOUT_COMPONENTs_PATH = LAYOUTs_PATH + "/component";
    public static final String GFXs_PATH = "/gfx";
    public static final String CSSs_PATH = "/css";
    
    public static final String CONNECTION_URL_TEMPLATE = "jdbc:mysql://%s/%s?useUnicode=true&serverTimezone=UTC";
    
    public static List <String> preparePaths (List <String> paths, String parent) {
        return Optional.ofNullable (paths).orElse (List.of ()).stream ()
             . map (parent::concat).collect (Collectors.toList ());
    }
    
}
