package ru.shemplo.cave.app.scenes;

import static ru.shemplo.cave.app.network.NetworkCommand.*;

import java.io.IOException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import ru.shemplo.cave.app.CaveApplication;
import ru.shemplo.cave.app.network.ClientConnection;
import ru.shemplo.cave.app.styles.SizeStyles;

public class GameJoinScene extends AbstractScene {

    private final TextField loginTF = new TextField ();
    private final TextField hostTF = new TextField ();
    private final Label messageL = new Label ();
    
    private final Button connectB = new Button ("Connect");
    private final Button backB = new Button ("Back");
    
    public GameJoinScene (CaveApplication app) {
        super (app);
        
        initView ();
    }

    @Override
    protected void initView () {
        final var menuBox = new VBox (32);
        menuBox.setAlignment (Pos.CENTER);
        setCenter (menuBox);
        
        loginTF.setMaxWidth (SizeStyles.MAIN_MENU_BUTTONS_WIDTH);
        menuBox.getChildren ().add (loginTF);
        loginTF.setPromptText ("Login");
        
        hostTF.setMaxWidth (SizeStyles.MAIN_MENU_BUTTONS_WIDTH);
        menuBox.getChildren ().add (hostTF);
        hostTF.setPromptText ("Host");
        
        connectB.setPrefWidth (SizeStyles.MAIN_MENU_BUTTONS_WIDTH);
        menuBox.getChildren ().add (connectB);
        connectB.setOnAction (ae -> {
            if (loginTF.getText ().isBlank ()) {
                messageL.setText ("Enter login before connection");
            } else {                
                connectToHost (hostTF.getText ().trim ());
            }
        });
        
        menuBox.getChildren ().add (messageL);
        
        backB.setPrefWidth (SizeStyles.MAIN_MENU_BUTTONS_WIDTH);
        menuBox.getChildren ().add (backB);
        backB.setOnAction (ae -> {
            ApplicationScene.MAIN_MENU.show (app);
        });
    }
    
    private void connectToHost (String host) {
        final var connectionThread = new Thread (() -> {            
            try {
                final var factory = SSLSocketFactory.getDefault ();            
                final var socket = (SSLSocket) factory.createSocket (host, 12763);
                socket.setEnabledCipherSuites (new String [] {
                    "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256"
                });
                socket.setEnabledProtocols (new String [] {
                    "TLSv1.2"
                });
                
                final var connection = new ClientConnection (null, socket);
                connection.setOnHandshakeFinished (() -> {
                    connection.setOnReadMessage ((parts, c) -> {
                        if (IDENTIFIER.getValue ().equals (parts [1])) {
                            connection.setLogin (loginTF.getText ().trim ());
                            connection.setIdh (parts [0]);
                            
                            app.setConnection (connection);
                            ApplicationScene.GAME_SETTINGS.show (app);
                        }
                    });                    
                });
                
                connection.startHandshake ();
            } catch (IOException ioe) {
                Platform.runLater (() -> {                    
                    messageL.setText (String.valueOf (ioe));
                });
            }
        });
        
        connectionThread.setDaemon (true);
        connectionThread.start ();
    }

    @Override
    public void onVisible () {
        
    }
    
}
