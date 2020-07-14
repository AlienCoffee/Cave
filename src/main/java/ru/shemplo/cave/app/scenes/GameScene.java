package ru.shemplo.cave.app.scenes;

import static ru.shemplo.cave.app.server.NetworkCommand.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import ru.shemplo.cave.app.CaveApplication;
import ru.shemplo.cave.app.entity.ChatMessage;
import ru.shemplo.cave.app.entity.level.GateType;
import ru.shemplo.cave.app.entity.level.RenderCell;
import ru.shemplo.cave.app.entity.level.RenderGate;
import ru.shemplo.cave.app.entity.level.RenderTumbler;
import ru.shemplo.cave.app.resources.LevelTextures;
import ru.shemplo.cave.app.scenes.render.GameRender;
import ru.shemplo.cave.app.server.ClientConnection;
import ru.shemplo.cave.app.server.room.state.ServerState;
import ru.shemplo.cave.utils.IPoint;

public class GameScene extends AbstractScene {
    
    private final Canvas canvasC = new Canvas ();
    private final GraphicsContext ctx = canvasC.getGraphicsContext2D ();
    private final GameRender render = new GameRender (canvasC);
    
    private final TextField chatTF = new TextField ();
    
    private final Button backB = new Button ("Back");
    
    public GameScene (CaveApplication app) {
        super (app);
        
        initView ();
        
        app.getConnection ().setOnReadMessage (this::handleMessage);
        startGame ();
    }
    
    private ServerState serverState = ServerState.WAITING_FOR_PLAYERS;
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
                //app.getConnection ().sendMessage (LEAVE_LOBBY.getValue ());
                Platform.runLater (() -> { backB.setDisable (false); });
                finishReason = parts [3];
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
                         . exit (isExit).symbol (cs).build ();
                }).collect (Collectors.toList ());
            
            final var gates = Arrays.stream (parts [5].split ("@"))
                . filter (str -> !str.isEmpty ())
                . map (str -> str.split (",")).map (cd -> {
                    final var dx = Double.parseDouble (cd [0]);
                    final var dy = Double.parseDouble (cd [1]);
                    final var isVertical = !Boolean.parseBoolean (cd [2]);
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
        } else if (CHAT_MESSAGE.getValue ().equals (parts [1])) {
            synchronized (chat) {
                final var time = System.currentTimeMillis ();
                
                chat.add (new ChatMessage (parts [2], parts [3], time));
            }
        }
    }
    
    protected void initView () {
        final var menuBox = new HBox (8);
        root.setTop (menuBox);
        
        menuBox.getChildren ().add (backB);
        backB.setFocusTraversable (false);
        backB.setOnAction (ae -> {
            if (serverState == ServerState.FINISH) {
                app.getConnection ().sendMessage (PLAYER_FINISHED_GAME.getValue ());
                ApplicationScene.GAME_SETTINGS.show (app);
            } else {                
                app.getConnection ().sendMessage (LEAVE_LOBBY.getValue ());
                ApplicationScene.MAIN_MENU.show (app);
            }
        });
        
        final var canvasBox = new VBox ();
        canvasBox.setFillWidth (true);
        root.setCenter (canvasBox);
        
        canvasBox.setBackground (new Background (new BackgroundFill (Color.BLACK, null, null)));
        
        final var canvasStack = new StackPane ();
        canvasBox.getChildren ().add (canvasStack);
        
        canvasC.heightProperty ().bind (canvasBox.heightProperty ());
        canvasC.widthProperty ().bind (canvasBox.widthProperty ());
        canvasStack.getChildren ().add (canvasC);
        
        chatTF.setPromptText ("Type chat message here");
        chatTF.setFocusTraversable (false);
        chatTF.setOnKeyReleased (ke -> {
            final var text = chatTF.getText ();
            if (ke.getCode () == KeyCode.ENTER) {
                if (!text.isBlank ()) {                    
                    app.getConnection ().sendMessage (CHAT_MESSAGE.getValue (), text);
                    chatTF.setText ("");
                }
                
                canvasC.requestFocus ();
            }
        });
        root.setBottom (chatTF);
    }

    @Override
    public void onVisible () {
        applyCss (); layout ();        
        canvasC.requestFocus ();
    }
    
    private boolean playerReady = false;
    
    public void startGame () {
        final var connection = app.getConnection ();
        app.getStage ().getScene ().setOnKeyPressed (ke -> {
            if (serverState == ServerState.WAITING_FOR_PLAYERS && !playerReady
                    && ke.getCode () == KeyCode.ENTER) {
                app.getConnection ().sendMessage (PLAYER_READY.getValue ());
                playerReady = true;
            } else if (serverState == ServerState.GAME) {                
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
                        if (!chatTF.isFocused ()) { // Do action if it's not sending message event                            
                            connection.sendMessage (PLAYER_ACTION.getValue (), "tumbler");
                        }
                    } break;
                    case M: {
                        connection.sendMessage (PLAYER_MODE.getValue ());
                    } break;
                    case E: {
                        connection.sendMessage (PLAYER_FOUND_EXIT.getValue ());
                        System.out.println ("Finish message sent"); // SYSOUT
                    } break;
                    
                    default: break;
                }
            }
            
        });
        
        renderPulse.setCycleCount (Timeline.INDEFINITE);
        renderPulse.playFromStart ();
    }
    
    private final Timeline renderPulse = new Timeline (
        new KeyFrame (Duration.ZERO, ae -> render ()),
        new KeyFrame (Duration.millis (1000.0 / 10.0))
    );
    
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
        
        if (serverState != ServerState.GAME) {
            ctx.setFill (Color.BLACK);
            ctx.fillRect (0, 0, cw, ch);
        }
        
        if (serverState == ServerState.WAITING_FOR_PLAYERS) {
            ctx.setFill (Color.YELLOW);
            ctx.fillText ("Waiting for players: " + (countdown == -1 ? "..." : countdown), 20, 40);
            
            ctx.setFill (Color.WHEAT);
            ctx.fillText ("Control: W, A, S, D", 20, 70);
            ctx.fillText ("Action: Enter", 20, 85);
            
            ctx.setFill (Color.SANDYBROWN);
            ctx.fillText ("Task: found the exit from the cave until expedition time is over", 20, 115);
            ctx.fillText ("To start the expedition type <Enter> key", 20, 130);
        } else if (serverState == ServerState.GAME) {
            synchronized (lock) {
                render.render (
                    visibleCells, visibleGates, visibleTumblers, visiblePlayers, 
                    mx, my, countdown, supermode
                );
            }
        } else if (serverState == ServerState.FINISH) {
            ctx.setFill (Color.YELLOW);
            ctx.fillText (finishReason, 20, 40);
        }
        
        renderChat ();
    }
    
    private final Queue <ChatMessage> chat = new LinkedList <> ();
    
    private void renderChat () {
        final double ch =  canvasC.getHeight ();
        
        synchronized (chat) {            
            final var time = System.currentTimeMillis ();
            int counter = 0;
            
            for (final var message : chat) {
                ctx.setFill (Color.WHITE);
                ctx.fillText (
                    String.format ("< %s > %s", message.getAuthor (), message.getText ()), 
                    20, ch - (chat.size () - counter + 1) * 15
                );
                
                counter += 1;
            }
            
            while (!chat.isEmpty () && time - chat.element ().getCreated () >= 15000) { 
                chat.poll (); // message time expired
            }
        }
    }
    
}
