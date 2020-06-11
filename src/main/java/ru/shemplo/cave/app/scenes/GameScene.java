package ru.shemplo.cave.app.scenes;

import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import ru.shemplo.cave.app.CaveApplication;
import ru.shemplo.cave.app.entity.level.GateType;
import ru.shemplo.cave.app.entity.level.Level;

public class GameScene extends AbstractScene {
    
    private final Canvas canvasC = new Canvas ();
    private final GraphicsContext ctx = canvasC.getGraphicsContext2D ();
    
    private final Button backB = new Button ("Back");
    
    public GameScene (CaveApplication app) {
        super (app);
        
        initView ();
    }
    
    protected void initView () {
        final var menuBox = new HBox (8);
        setTop (menuBox);
        
        menuBox.getChildren ().add (backB);
        backB.setOnAction (ae -> {
            ApplicationScene.MAIN_MENU.show (app);
        });
        
        final var canvasBox = new VBox ();
        canvasBox.setFillWidth (true);
        setCenter (canvasBox);
        
        canvasBox.setBackground (new Background (new BackgroundFill (Color.HOTPINK, null, null)));
        
        final var canvasStack = new StackPane ();
        canvasBox.getChildren ().add (canvasStack);
        
        canvasC.heightProperty ().bind (canvasBox.heightProperty ());
        canvasC.widthProperty ().bind (canvasBox.widthProperty ());
        canvasStack.getChildren ().add (canvasC);
    }

    @Override
    public void onVisible () {
        applyCss (); layout ();
        
        final var level = new Level ();
        startGame (level);
        
        canvasC.requestFocus ();
    }
    
    private Level level;
    
    private int mx = 0, my = 0;
    
    public void startGame (Level level) {
        this.level = level;
     
        mx = level.getIx (); my = level.getIy ();
        app.getStage ().getScene ().setOnKeyPressed (ke -> {
            switch (ke.getCode ()) {
                case W: {
                    if (ke.isShiftDown ()) {
                        my -= 1;
                    } else if (level.canStepOnFrom (0, -1, mx, my)) { my -= 1; }
                } break;
                case A: {
                    if (ke.isShiftDown ()) {
                        mx -= 1;
                    } else if (level.canStepOnFrom (-1, 0, mx, my)) { mx -= 1; }
                } break;
                case S: {
                    if (ke.isShiftDown ()) {
                        my += 1;
                    } else if (level.canStepOnFrom (0, +1, mx, my)) { my += 1; }
                } break;
                case D: {
                    if (ke.isShiftDown ()) {
                        mx += 1;
                    } else if (level.canStepOnFrom (+1, 0, mx, my)) { mx += 1; }
                } break;
                case ENTER: {
                    level.openGates (mx, my);
                } break;
                
                default: break;
            }
        });
        
        renderPulse.setCycleCount (Timeline.INDEFINITE);
        renderPulse.playFromStart ();
    }
    
    private final Timeline renderPulse = new Timeline (
        new KeyFrame (Duration.ZERO, ae -> render ()),
        new KeyFrame (Duration.millis (1000.0 / 10.0))
    );
    
    private final double ts = 32 * 6; // tile size 
    
    // 132 x 326
    
    private static final Image playerSet = new Image (Level.class.getResourceAsStream ("/gfx/player.png"));
    private static final Image player;
    
    static {
        player = new WritableImage (playerSet.getPixelReader (), 316, 44, 132, 326);
    }
    
    private List <Color> subpartColors = List.of (
        Color.RED, Color.BLUE, Color.GREEN, Color.BLUEVIOLET, Color.YELLOW, Color.CYAN,
        Color.BROWN, Color.LIME, Color.BLACK, Color.TOMATO, Color.CADETBLUE
    );
    
    private void render () {
        //final var size = level.getSize ();
        
        final double cw = canvasC.getWidth (), ch =  canvasC.getHeight ();
        final double cx = cw / 2, cy =  ch / 2;
        
        ctx.setFill (Color.BLACK);
        ctx.fillRect (0, 0, cw, ch);
        
        final int px = mx, py = my;
        
        level.getVisibleCells (px, py, 1.0).forEach (cell -> {
            ctx.drawImage (cell.getImage (), cx + (cell.getX () - 0.5) * ts, cy + (cell.getY () - 0.5) * ts, ts + 1, ts + 1);
            
            ctx.setFill (subpartColors.get (cell.getSubpart () % subpartColors.size ()));
            ctx.fillRect (cx + (cell.getX () - 0.5) * ts + 10, cy + (cell.getY () - 0.5) * ts + 10, 10, 10);
        });
        
        level.getVisibleGates (px, py).forEach (gate -> {
            ctx.setFill (gate.getType () == GateType.GATE 
                            ? (gate.isClosed () ? Color.BROWN : Color.LIMEGREEN) 
                            : (gate.getType () == GateType.SILT? Color.ALICEBLUE : Color.BLACK)
                        );
            if (gate.isVertical ()) {                
                ctx.fillRect (cx + gate.getX () * ts - 5, cy + gate.getY () * ts - ts / 4, 10, ts / 2);
            } else {                
                ctx.fillRect (cx + gate.getX () * ts - ts / 4, cy + gate.getY () * ts - 5, ts / 2, 10);
            }
        });
        
        ctx.drawImage (player, cx - 10, cy - 20, 20, 40);
        
        ctx.setFill (Color.YELLOW);
        ctx.fillText ("X: " + mx, 20, 40);
        ctx.fillText ("Y: " + my, 20, 55);
    }
    
}
