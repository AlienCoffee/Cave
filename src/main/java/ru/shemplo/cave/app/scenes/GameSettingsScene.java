package ru.shemplo.cave.app.scenes;



import static ru.shemplo.cave.app.server.NetworkCommand.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.FocusModel;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import ru.shemplo.cave.app.CaveApplication;
import ru.shemplo.cave.app.LobbyListCell;
import ru.shemplo.cave.app.entity.Player;
import ru.shemplo.cave.app.resources.LevelTextures;
import ru.shemplo.cave.app.server.ClientConnection;
import ru.shemplo.cave.app.server.ServerState;
import ru.shemplo.cave.app.styles.SizeStyles;
import ru.shemplo.cave.utils.EmptySelectionModel;

public class GameSettingsScene extends AbstractScene {

    private final ListView <Player> playersLV = new ListView <> ();
    
    private final TextField expeditionTimeTF = new TextField ();
    private final TextField expeditorsTF = new TextField ();
    
    private final Label messageL = new Label ();
    
    private final Button readyB = new Button ("Ready");
    private final Button backB = new Button ("Back");
    
    public GameSettingsScene (CaveApplication app) {
        super (app);
        
        initView ();
        
        Optional.ofNullable (app.getConnection ()).ifPresentOrElse (connection -> {
            connection.setOnReadMessage (this::handleMessage);
            
            connection.sendMessage (IN_LOBBY.getValue (), connection.getLogin ());
        }, () -> {
            Platform.runLater (() -> {
                messageL.setText ("Connection is not established");
            });
        });
    }
    
    private boolean externalUpdate = false;
    private boolean playerReady = false;
    
    private void handleMessage (String [] parts, ClientConnection connection) {
        if (CONNECTION_CLOSED.getValue ().equals (parts [1])) {
            Platform.runLater (() -> {
                expeditionTimeTF.setDisable (true);
                expeditorsTF.setDisable (true);
                messageL.setText (parts [2]);
                readyB.setDisable (true);
                backB.setDisable (false);
            });
        } else if (LOBBY_PLAYERS.getValue ().equals (parts [1])) {
            Platform.runLater (() -> {
                final var players = Arrays.stream (parts [2].split ("/"))
                    . map (str -> {
                        final var playerParts = str.split (",");
                        
                        final var ready = Boolean.parseBoolean (playerParts [2]);
                        return new Player (playerParts [0], playerParts [1], ready);
                    })
                    . collect (Collectors.toList ());
                playersLV.getItems ().setAll (players);
            });
        } else if (LOBBY_PLAYER.getValue ().equals (parts [1])) {
            Platform.runLater (() -> {
                messageL.setText (parts [2] + " joined");
                
                final var ready = Boolean.parseBoolean (parts [4]);
                final var player = new Player (parts [2], parts [3], ready);
                playersLV.getItems ().add (player);
            });
        } else if (LEAVE_LOBBY.getValue ().equals (parts [1])) {
            synchronized (playersLV) {
                Platform.runLater (() -> {                    
                    playersLV.getItems ().removeIf (p -> {
                        final var found = p.getIdhh ().equals (parts [2]);
                        if (found) {
                            System.out.println ("Leaver found: " + p.getLogin ()); // SYSOUT
                            Platform.runLater (() -> {
                                messageL.setText (p.getLogin () + " leaved");
                            });
                        }
                        
                        return found;
                    });
                });
            }
        } else if (START_COUNTDOWN.getValue ().equals (parts [1])) {
            Platform.runLater (() -> {
                expeditionTimeTF.setDisable (true);
                expeditorsTF.setDisable (true);
                readyB.setDisable (true);
                backB.setDisable (true);
            });
        } else if (COUNTDOWN.getValue ().equals (parts [1])) {
            if (Integer.parseInt (parts [2]) > 0) {                
                Platform.runLater (() -> {
                    messageL.setText (parts [2] + " seconds to start");
                });
            }
        } else if (PLAYER_READY.getValue ().equals (parts [1])) {
            if (app.getConnection ().getIdhh ().equals (parts [2])) {
                playerReady = true;
                Platform.runLater (() -> {
                    readyB.setText ("Not ready");
                    readyB.setDisable (false);
                });
            }
            
            Platform.runLater (() -> {
                playersLV.getItems ().forEach (player -> {
                    if (player.getIdhh ().equals (parts [2])) {
                        messageL.setText (player.getLogin () + " is ready");
                        player.setReady (true);
                    }
                });
                
                playersLV.refresh ();
            });
        } else if (PLAYER_NOT_READY.getValue ().equals (parts [1])) {
            if (app.getConnection ().getIdhh ().equals (parts [2])) {
                playerReady = false;
                Platform.runLater (() -> {
                    readyB.setText ("Ready");
                    readyB.setDisable (false);
                });
            }
            
            Platform.runLater (() -> {
                playersLV.getItems ().forEach (player -> {
                    if (player.getIdhh ().equals (parts [2])) {
                        messageL.setText (player.getLogin () + " is not ready");
                        player.setReady (false);
                    }
                });
                
                playersLV.refresh ();
            });
        } else if (EXPEDITION_SIZE.getValue ().equals (parts [1])) {
            Platform.runLater (() -> {
                externalUpdate = true;
                expeditorsTF.setText (parts [2]);
                externalUpdate = false;
            });
        }  else if (EXPEDITION_TIME.getValue ().equals (parts [1])) {
            Platform.runLater (() -> {
                externalUpdate = true;
                expeditionTimeTF.setText (parts [2]);
                externalUpdate = false;
            });
        } else if (SERVER_STATE.getValue ().equals (parts [1])) {
            System.out.println ("Server state: " + parts [2]); // SYSOUT
            final var serverState = ServerState.valueOf (parts [2]);
            
            if (serverState == ServerState.WAITIN_FOR_PLAYERS) {
                ApplicationScene.GAME.show (app);
            }
        }
    }

    @Override
    protected void initView () {
        getChildren ().add (0, backgroundC);
        
        final var leftBox = new VBox ();
        BorderPane.setMargin (leftBox, new Insets (0, 0, 0, 64));
        leftBox.setAlignment (Pos.CENTER_RIGHT);
        root.setLeft (leftBox);
        
        playersLV.setSelectionModel (new EmptySelectionModel <> ());
        playersLV.setCellFactory (__ -> new LobbyListCell ());
        playersLV.setBackground (Background.EMPTY);
        playersLV.setFocusTraversable (false);
        playersLV.setFocusModel (new FocusModel <> () {
            @Override
            protected Player getModelItem (int index) {
                return null;
            }
            
            @Override
            protected int getItemCount () {
                return 0;
            }
        });
        leftBox.getChildren ().add (playersLV);
        
        final var menuBox = new VBox (32);
        menuBox.setBackground (new Background (new BackgroundFill (Color.color (0.75, 0.75, 0.75, 0.25), null, null)));
        BorderPane.setMargin (menuBox, new Insets (0, 64, 0, 0));
        menuBox.setPadding (new Insets (0, 16, 0, 16));
        menuBox.setAlignment (Pos.CENTER);
        root.setRight (menuBox);
        
        final var expeditorsBox = new HBox (8);
        menuBox.getChildren ().add (expeditorsBox);
        expeditorsBox.setAlignment (Pos.CENTER);
        
        final var expeditorsL = new Label ("Expeditors:");
        expeditorsBox.getChildren ().add (expeditorsL);
        expeditorsL.setTextFill (Color.WHITESMOKE);
        
        expeditorsBox.getChildren ().add (expeditorsTF);
        expeditorsTF.setPrefWidth (132);
        expeditorsTF.textProperty ().addListener ((__, ___, cur) -> {
            if (cur.isBlank () || externalUpdate) { return; }
            final var value = cur.trim ();
            
            try   { Integer.parseInt (value); } 
            catch (NumberFormatException nfe) { return; }
            
            Optional.ofNullable (app.getConnection ()).ifPresent (connection -> {
                connection.sendMessage (EXPEDITION_SIZE.getValue (), value);
            });
        });
        
        final var expeditionTimeBox = new HBox (8);
        menuBox.getChildren ().add (expeditionTimeBox);
        expeditionTimeBox.setAlignment (Pos.CENTER);
        
        final var timeL = new Label ("Expedition time (minutes):");
        expeditionTimeBox.getChildren ().add (timeL);
        timeL.setTextFill (Color.WHITESMOKE);
        
        expeditionTimeBox.getChildren ().add (expeditionTimeTF);
        expeditionTimeTF.setPrefWidth (50);
        expeditionTimeTF.textProperty ().addListener ((__, ___, cur) -> {
            if (cur.isBlank () || externalUpdate) { return; }
            final var value = cur.trim ();
            
            try   { Integer.parseInt (value); } 
            catch (NumberFormatException nfe) { return; }
            
            Optional.ofNullable (app.getConnection ()).ifPresent (connection -> {
                connection.sendMessage (EXPEDITION_TIME.getValue (), value);
            });
        });
        
        readyB.setPrefWidth (SizeStyles.MAIN_MENU_BUTTONS_WIDTH);
        menuBox.getChildren ().add (readyB);
        readyB.setOnAction (ae -> {
            readyB.setDisable (true);
            Optional.ofNullable (app.getConnection ()).ifPresent (connection -> {
                connection.sendMessage (playerReady ? PLAYER_NOT_READY.getValue () : PLAYER_READY.getValue ());
            });
        });
        
        messageL.setTextFill (Color.WHITESMOKE);
        menuBox.getChildren ().add (messageL);
        messageL.setWrapText (true);
        
        backB.setPrefWidth (SizeStyles.MAIN_MENU_BUTTONS_WIDTH);
        menuBox.getChildren ().add (backB);
        backB.setOnAction (ae -> {
            Optional.ofNullable (app.getConnection ()).ifPresent (connection -> {
                try   { connection.close (); } 
                catch (IOException ioe) {}
            });
            
            ApplicationScene.MAIN_MENU.show (app);
        });
    }

    @Override
    public void onVisible () {
        playersLV.setMaxHeight (getHeight () / 2);
        
        final var context = backgroundC.getGraphicsContext2D ();
        backgroundC.setHeight (getHeight ());
        backgroundC.setWidth (getWidth ());
        
        final double w = backgroundC.getWidth (), h = backgroundC.getHeight ();
        context.drawImage (LevelTextures.caveBackground4, 0, 0, w, h);
    }
    
}
