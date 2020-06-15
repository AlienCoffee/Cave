package ru.shemplo.cave.app.scenes;

import static ru.shemplo.cave.app.server.NetworkCommand.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import ru.shemplo.cave.app.CaveApplication;
import ru.shemplo.cave.app.entity.level.GateType;
import ru.shemplo.cave.app.entity.level.RenderCell;
import ru.shemplo.cave.app.entity.level.RenderGate;
import ru.shemplo.cave.app.entity.level.RenderTumbler;
import ru.shemplo.cave.app.resources.LevelTextures;
import ru.shemplo.cave.app.server.ClientConnection;
import ru.shemplo.cave.app.server.ServerState;
import ru.shemplo.cave.utils.IPoint;

public class GameScene extends AbstractScene {
    
    private final Canvas canvasC = new Canvas ();
    private final GraphicsContext ctx = canvasC.getGraphicsContext2D ();
    
    private final Button backB = new Button ("Back");
    
    public GameScene (CaveApplication app) {
        super (app);
        
        initView ();
        
        app.getConnection ().setOnReadMessage (this::handleMessage);
        app.getConnection ().sendMessage (PLAYER_READY.getValue ());
    }
    
    private ServerState serverState = ServerState.WAITIN_FOR_PLAYERS;
    private boolean supermode = false;
    private String finishReason = "";
    private int countdown = -1;
    
    private void handleMessage (String [] parts, ClientConnection connection) {
        if (COUNTDOWN.getValue ().equals (parts [1])) {
            countdown = Integer.parseInt (parts [2]);
        } else if (SERVER_STATE.getValue ().equals (parts [1])) {
            System.out.println ("Server state: " + parts [2]); // SYSOUT
            serverState = ServerState.valueOf (parts [2]);
            
            if (serverState == ServerState.FINISH) {
                app.getConnection ().sendMessage (LEAVE_LOBBY.getValue ());
                Platform.runLater (() -> { backB.setDisable (false); });
                finishReason = parts [3];
            } else if (serverState == ServerState.GAME) {
                startGame ();
            }
        } else if (PLAYER_LOCATION.getValue ().equals (parts [1])) {
            mx = Integer.parseInt (parts [2]); my = Integer.parseInt (parts [3]);
            
            final var cells = Arrays.stream (parts [4].split ("@"))
                . filter (str -> !str.isEmpty ())
                . map (str -> str.split (",")).map (cd -> {
                    final var dx = Integer.parseInt (cd [0]);
                    final var dy = Integer.parseInt (cd [1]);
                    
                    final var cs = cd [2].charAt (0);
                    final var image = LevelTextures.symbol2texture.get (cs);
                    final var isExit = Boolean.parseBoolean (cd [3]);
                    
                    return RenderCell.builder ().x (dx).y (dy).image (image)
                         . exit (isExit).build ();
                }).collect (Collectors.toList ());
            
            final var gates = Arrays.stream (parts [5].split ("@"))
                . filter (str -> !str.isEmpty ())
                . map (str -> str.split (",")).map (cd -> {
                    final var dx = Double.parseDouble (cd [0]);
                    final var dy = Double.parseDouble (cd [1]);
                    final var isVertical = Boolean.parseBoolean (cd [2]);
                    final var type = GateType.valueOf (cd [3]);
                    final var isClosed = Boolean.parseBoolean (cd [4]);
                    
                    return RenderGate.builder ().x (dx).y (dy).vertical (isVertical)
                         . type (type).closed (isClosed).build ();
                }).collect (Collectors.toList ());
            
            final var tumblers = Arrays.stream (parts [6].split ("@"))
                . filter (str -> !str.isEmpty ())
                . map (str -> str.split (",")).map (cd -> {
                    final var dx = Integer.parseInt (cd [0]);
                    final var dy = Integer.parseInt (cd [1]);
                    final var isActive = Boolean.parseBoolean (cd [2]);
                    
                    return RenderTumbler.builder ().x (dx).y (dy).active (isActive).build ();
                }).collect (Collectors.toList ());
            
            synchronized (lock) {
                visibleCells.clear ();
                visibleCells.addAll (cells);
                
                visibleGates.clear ();
                visibleGates.addAll (gates);
                
                visibleTumblers.clear ();
                visibleTumblers.addAll (tumblers);
            }
        } else if (PLAYERS_LOCATION.getValue ().equals (parts [1])) {
            mx = Integer.parseInt (parts [2]); my = Integer.parseInt (parts [3]);
            supermode = Boolean.parseBoolean (parts [5]);
            
            final var players = Arrays.stream (parts [4].split ("@"))
                . filter (str -> !str.isEmpty ())
                . map (str -> str.split (",")).map (cd -> {
                    final var dx = Integer.parseInt (cd [0]);
                    final var dy = Integer.parseInt (cd [1]);
                    
                    return IPoint.of (dx, dy);
                }).collect (Collectors.toList ());
            
            synchronized (lock) {
                visiblePlayers.clear ();
                visiblePlayers.addAll (players);
            }
        }
    }
    
    protected void initView () {
        final var menuBox = new HBox (8);
        setTop (menuBox);
        
        menuBox.getChildren ().add (backB);
        backB.setOnAction (ae -> {
            ApplicationScene.MAIN_MENU.show (app);
        });
        backB.setDisable (true);
        
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
        canvasC.requestFocus ();
    }
    
    public void startGame () {
        final var connection = app.getConnection ();
        app.getStage ().getScene ().setOnKeyPressed (ke -> {
            if (serverState != ServerState.GAME) { return; }
            
            switch (ke.getCode ()) {
                case W: {
                    connection.sendMessage (PLAYER_MOVE.getValue (), "0", "-1");
                } break;
                case A: {
                    connection.sendMessage (PLAYER_MOVE.getValue (), "-1", "0");
                } break;
                case S: {
                    connection.sendMessage (PLAYER_MOVE.getValue (), "0", "1");
                } break;
                case D: {
                    connection.sendMessage (PLAYER_MOVE.getValue (), "1", "0");
                } break;
                case ENTER: {
                    connection.sendMessage (PLAYER_ACTION.getValue (), "tumbler");
                } break;
                case M: {
                    connection.sendMessage (PLAYER_MODE.getValue ());
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
    
    @SuppressWarnings ("unused")
    private List <Color> subpartColors = List.of (
        Color.RED, Color.BLUE, Color.GREEN, Color.BLUEVIOLET, Color.YELLOW, Color.CYAN,
        Color.BROWN, Color.LIME, Color.BLACK, Color.TOMATO, Color.CADETBLUE
    );
    
    private List <RenderTumbler> visibleTumblers = new ArrayList <> ();
    private List <RenderCell> visibleCells = new ArrayList <> ();
    private List <RenderGate> visibleGates = new ArrayList <> ();
    private List <IPoint> visiblePlayers = new ArrayList <> ();
    private final Object lock = new Object ();
    private int mx = -1, my = -1;
    
    private void render () {
        final double cw = canvasC.getWidth (), ch =  canvasC.getHeight ();
        final double cx = cw / 2, cy =  ch / 2;
        
        ctx.setFill (Color.BLACK);
        ctx.fillRect (0, 0, cw, ch);
        
        if (serverState == ServerState.WAITIN_FOR_PLAYERS) {
            ctx.setFill (Color.YELLOW);
            ctx.fillText ("Waiting for players", 20, 40);
        } else if (serverState == ServerState.GAME) {
            synchronized (lock) {                
                visibleCells.forEach (cell -> {
                    ctx.drawImage (cell.getImage (), cx + (cell.getX () - 0.5) * ts, cy + (cell.getY () - 0.5) * ts, ts + 1, ts + 1);
                    //ctx.setFill (subpartColors.get (cell.getSubpart () % subpartColors.size ()));
                    //ctx.fillRect (cx + (cell.getX () - 0.5) * ts + 10, cy + (cell.getY () - 0.5) * ts + 10, 5, 5);
                    
                    if (cell.isExit ()) {
                        ctx.setFill (Color.WHITESMOKE);
                        ctx.fillRect (cx + cell.getX () * ts - 20, cy + cell.getY () * ts - 20, 40, 40);
                    }
                });
                
                visibleGates.forEach (gate -> {
                    ctx.setFill (gate.getType () == GateType.GATE 
                            ? (gate.isClosed () ? Color.BROWN : Color.LIMEGREEN) 
                                    : (gate.getType () == GateType.SLIT ? Color.ALICEBLUE : Color.BLACK)
                            );
                    if (gate.isVertical ()) {
                        if (gate.getType () == GateType.GATE) {   
                            final var gatesSkin = gate.isClosed () ? LevelTextures.gatesClosedV : LevelTextures.gatesOpenedV;
                            //ctx.fillRect (cx + gate.getX () * ts - 5, cy + gate.getY () * ts - ts / 4, 10, ts / 2);
                            ctx.drawImage (gatesSkin, cx + gate.getX () * ts - 15, cy + gate.getY () * ts - ts / 4, 30, ts / 2);
                        } else {
                            final var slitSkin = LevelTextures.gatesClosedV;
                            ctx.drawImage (slitSkin, cx + gate.getX () * ts - 15, cy + gate.getY () * ts - ts / 4, 30, ts / 2);
                        }
                    } else {
                        if (gate.getType () == GateType.GATE) {
                            final var gatesSkin = gate.isClosed () ? LevelTextures.gatesClosedH : LevelTextures.gatesOpenedH;
                            //ctx.fillRect (cx + gate.getX () * ts - ts / 4, cy + gate.getY () * ts - 5, ts / 2, 10);
                            ctx.drawImage (gatesSkin, cx + gate.getX () * ts - ts / 4 - 4, cy + gate.getY () * ts - 15, ts / 2, 30);
                        } else {
                            final var slitSkin = LevelTextures.slitH;
                            ctx.drawImage (slitSkin, cx + gate.getX () * ts - ts / 4, cy + gate.getY () * ts - 15, ts / 2, 30);
                        }
                    }
                });
                
                visibleTumblers.forEach (tumbler -> {
                    final var tumblerSkin = tumbler.isActive () ? LevelTextures.tumblerOn : LevelTextures.tumblerOff;
                    ctx.setFill (tumbler.isActive () ? Color.LIMEGREEN : Color.BROWN);
                    
                    final var fx = cx + (tumbler.getX () - 0.5) * ts + 35;
                    final var fy = cy + (tumbler.getY () - 0.5) * ts + 15;
                    
                    ctx.fillRect (fx + 1, fy + 1, 13, 13);
                    ctx.drawImage (tumblerSkin, fx, fy, 15, 15);
                });
                
                final var playerSkin = LevelTextures.player;
                visiblePlayers.forEach (player -> {
                    ctx.drawImage (playerSkin, cx + player.X * ts - 10, cy + player.Y * ts - 20, 20, 40);
                });
                
                ctx.drawImage (playerSkin, cx - 10, cy - 20, 20, 40);
            }
            
            ctx.setFill (Color.YELLOW);
            ctx.fillText ("X: " + mx, 20, 40);
            ctx.fillText ("Y: " + my, 20, 55);
            ctx.fillText (String.format ("Rest time: %02d:%02d", countdown / 60, countdown % 60), 20, 70);
            ctx.fillText ("Supermode: " + supermode, 20, 85);
        } else if (serverState == ServerState.FINISH) {
            ctx.setFill (Color.YELLOW);
            ctx.fillText (finishReason, 20, 40);
        }
    }
    
}
