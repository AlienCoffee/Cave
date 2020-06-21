package ru.shemplo.cave.app.scenes;



import static ru.shemplo.cave.app.server.NetworkCommand.*;

import java.io.IOException;
import java.util.Optional;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import ru.shemplo.cave.app.CaveApplication;
import ru.shemplo.cave.app.server.ClientConnection;
import ru.shemplo.cave.app.server.ServerState;
import ru.shemplo.cave.app.styles.SizeStyles;

public class GameSettingsScene extends AbstractScene {

    private final ListView <String> playersLV = new ListView <> ();
    
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
                messageL.setText (parts [2]);
                readyB.setDisable (true);
                backB.setDisable (false);
            });
        } else if (LOBBY_PLAYERS.getValue ().equals (parts [1])) {
            Platform.runLater (() -> {
                messageL.setText (parts [2] + " in lobby");
                playersLV.getItems ().setAll (parts [2].split (","));
            });
        } else if (LOBBY_PLAYER.getValue ().equals (parts [1])) {
            Platform.runLater (() -> {
                messageL.setText (parts [2] + " joined");
                playersLV.getItems ().add (parts [2]);
            });
        } else if (LEAVE_LOBBY.getValue ().equals (parts [1])) {
            Platform.runLater (() -> {
                messageL.setText (parts [2] + " leaved");
                playersLV.getItems ().remove (parts [2]);
            });
        } else if (START_COUNTDOWN.getValue ().equals (parts [1])) {
            Platform.runLater (() -> {
                expeditionTimeTF.setDisable (true);
                expeditorsTF.setDisable (true);
                backB.setDisable (true);
            });
        } else if (COUNTDOWN.getValue ().equals (parts [1])) {
            if (Integer.parseInt (parts [2]) > 0) {                
                Platform.runLater (() -> {
                    messageL.setText (parts [2] + " seconds to start");
                });
            }
        } else if (PLAYER_READY.getValue ().equals (parts [1])) {
            if (app.getConnection ().getLogin ().equals (parts [2])) {
                playerReady = true;
                Platform.runLater (() -> {
                    readyB.setText ("Not ready");
                    readyB.setDisable (false);
                });
            } else {
                Platform.runLater (() -> {
                    messageL.setText (parts [2] + " is ready");
                });
            }
        } else if (PLAYER_NOT_READY.getValue ().equals (parts [1])) {
            if (app.getConnection ().getLogin ().equals (parts [2])) {                
                playerReady = false;
                Platform.runLater (() -> {
                    readyB.setDisable (false);
                    readyB.setText ("Ready");
                });
            } else {
                Platform.runLater (() -> {
                    messageL.setText (parts [2] + " is not ready");
                });
            }
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
        setLeft (playersLV);
        
        final var menuBox = new VBox (32);
        menuBox.setAlignment (Pos.CENTER);
        setCenter (menuBox);
        
        final var expeditorsBox = new HBox (8);
        menuBox.getChildren ().add (expeditorsBox);
        expeditorsBox.setAlignment (Pos.CENTER);
        
        final var expeditorsL = new Label ("Expeditors:");
        expeditorsBox.getChildren ().add (expeditorsL);
        
        expeditorsBox.getChildren ().add (expeditorsTF);
        expeditorsTF.setPrefWidth (130);
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
        
    }
    
}
