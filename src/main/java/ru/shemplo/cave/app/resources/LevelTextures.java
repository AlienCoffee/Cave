package ru.shemplo.cave.app.resources;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import ru.shemplo.cave.app.entity.level.Level;

public class LevelTextures {
    
    @SuppressWarnings ("unused")
    private static final Image tunnel     = new Image (Level.class.getResourceAsStream ("/gfx/tunnel.png"));
    
    private static final Image tunnelR    = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-r.png"));
    private static final Image tunnelL    = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-l.png"));
    private static final Image tunnelRL   = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-lr.png"));
    private static final Image tunnelRBL  = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-lrb.png"));
    private static final Image tunnelTRBL = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-lrbt.png"));
    private static final Image tunnelT    = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-t.png"));
    private static final Image tunnelB    = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-b.png"));
    private static final Image tunnelTB   = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-tb.png"));
    private static final Image tunnelTRL  = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-lrt.png"));
    private static final Image tunnelTBL  = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-lbt.png"));
    private static final Image tunnelTRB  = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-rbt.png"));
    private static final Image tunnelTL   = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-lt.png"));
    private static final Image tunnelTR   = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-rt.png"));
    private static final Image tunnelBL   = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-lb.png"));
    private static final Image tunnelRB   = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-rb.png"));
    //private static final Image tunnel0    = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-0.png"));
    private static final Image tunnel0    = new Image (Level.class.getResourceAsStream ("/gfx/tunnel-mask-wire-4.png"));
    
    public static final Image decorationSkull      = new Image (Level.class.getResourceAsStream ("/gfx/skull.png"));
    public static final Image decorationGoldPieces = new Image (Level.class.getResourceAsStream ("/gfx/gold-pieces.png"));
    
    public static final Image tumblerOn  = new Image (Level.class.getResourceAsStream ("/gfx/tumbler-on.png"));
    public static final Image tumblerOff = new Image (Level.class.getResourceAsStream ("/gfx/tumbler-off.png"));
    
    public static final Map <Character, Image> symbol2texture = new HashMap <> ();
    
    static {
        symbol2texture.put ('┼', tunnelTRBL);
        symbol2texture.put ('├', tunnelTRB);
        symbol2texture.put ('┴', tunnelTRL);
        symbol2texture.put ('┤', tunnelTBL);
        symbol2texture.put ('┬', tunnelRBL);
        
        symbol2texture.put ('└', tunnelTR);
        symbol2texture.put ('│', tunnelTB);
        symbol2texture.put ('┘', tunnelTL);
        symbol2texture.put ('┌', tunnelRB);
        symbol2texture.put ('─', tunnelRL);
        symbol2texture.put ('┐', tunnelBL);
        
        symbol2texture.put ('╧', tunnelT);
        symbol2texture.put ('╟', tunnelR);
        symbol2texture.put ('╤', tunnelB);
        symbol2texture.put ('╢', tunnelL);
        
        symbol2texture.put (' ', tunnel0);
    }
    
    // 132 x 326
    
    private static final Image playerSet = new Image (Level.class.getResourceAsStream ("/gfx/player.png"));
    public static final Image player;
    

    public static final Image slitV = new Image (Level.class.getResourceAsStream ("/gfx/slit-tb.png"));
    public static final Image slitH;
    
    public static final Image gatesOpenedV = new Image (Level.class.getResourceAsStream ("/gfx/gates-open-v.png"));
    public static final Image gatesOpenedH;
    
    public static final Image gatesClosedV = new Image (Level.class.getResourceAsStream ("/gfx/gates-closed-v.png"));
    public static final Image gatesClosedH;
    
    static {
        player = new WritableImage (playerSet.getPixelReader (), 316, 44, 132, 326);
        slitH = rotate90 (slitV);
        
        gatesClosedH = rotate90 (gatesClosedV);
        gatesOpenedH = rotate90 (gatesOpenedV);
    }
    
    private static Image rotate90 (Image image) {
        final int width = (int) image.getHeight (), height = (int)image.getWidth ();
        final var writable = new WritableImage (width, height);
        
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                final var color = image.getPixelReader ().getColor (h, w);
                writable.getPixelWriter ().setColor (w, h, color);
            }
        }
        
        return writable;
    }
    
    public static final Image caveBackground2 = new Image (Level.class.getResourceAsStream ("/gfx/cave-background-2.png"));
    public static final Image caveBackground3 = new Image (Level.class.getResourceAsStream ("/gfx/cave-background-3.jpg"));
    public static final Image caveBackground4 = new Image (Level.class.getResourceAsStream ("/gfx/cave-background-4.jpg"));
    public static final Image caveBackground5 = new Image (Level.class.getResourceAsStream ("/gfx/cave-background-5.png"));
    public static final Image caveIcon = new Image (Level.class.getResourceAsStream ("/gfx/cave.png"));
    
    public static final Image crossIcon = new Image (Level.class.getResourceAsStream ("/gfx/criss-cross.png"));
    public static final Image tickIcon = new Image (Level.class.getResourceAsStream ("/gfx/tick.png"));
    
}
