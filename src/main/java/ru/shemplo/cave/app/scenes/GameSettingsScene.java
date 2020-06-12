package ru.shemplo.cave.app.scenes;



import static ru.shemplo.cave.app.network.NetworkCommand.*;

import java.io.IOException;
import java.util.Optional;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import ru.shemplo.cave.app.CaveApplication;
import ru.shemplo.cave.app.network.ClientConnection;
import ru.shemplo.cave.app.styles.SizeStyles;

public class GameSettingsScene extends AbstractScene {

    private final Label messageL = new Label ();
    
    private final Button backB = new Button ("Back");
    
    public GameSettingsScene (CaveApplication app) {
        super (app);
        
        initView ();
        
        Optional.ofNullable (app.getConnection ()).ifPresentOrElse (connection -> {
            connection.setOnReadMessage (this::handleMessage);
            
            connection.sendMessage (PLAYER.getValue (), connection.getLogin ());
            connection.sendMessage (GET_LOBBY_PLAYERS.getValue ());
        }, () -> {
            Platform.runLater (() -> {
                messageL.setText ("Connection is not established");
            });
        });
    }
    
    private void handleMessage (String [] parts, ClientConnection connection) {
        if (CONNECTION_REJECTED.getValue ().equals (parts [1])) {
            Platform.runLater (() -> {
                messageL.setText (parts [2]);
            });
        } else if (LOBBY_PLAYERS.getValue ().equals (parts [1])) {
            Platform.runLater (() -> {
                messageL.setText (parts [2] + " in lobby");
            });
        } else if (PLAYER.getValue ().equals (parts [1])) {
            Platform.runLater (() -> {
                messageL.setText (parts [2] + " joined");
            });
        } else if (LEAVE_LOBBY.getValue ().equals (parts [1])) {
            Platform.runLater (() -> {
                messageL.setText (parts [2] + " leaved");
            });
        }
    }

    @Override
    protected void initView () {
        final var menuBox = new VBox (32);
        menuBox.setAlignment (Pos.CENTER);
        setCenter (menuBox);
        
        menuBox.getChildren ().add (messageL);
        
        backB.setPrefWidth (SizeStyles.MAIN_MENU_BUTTONS_WIDTH);
        menuBox.getChildren ().add (backB);
        backB.setOnAction (ae -> {
            Optional.ofNullable (app.getConnection ()).ifPresent (connection -> {
                connection.sendMessage (LEAVE_LOBBY.getValue ());
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
