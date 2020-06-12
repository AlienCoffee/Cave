package ru.shemplo.cave.app.scenes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import ru.shemplo.cave.app.CaveApplication;
import ru.shemplo.cave.app.styles.SizeStyles;

public class MainMenuScene extends AbstractScene {
    
    private final Button joinGameB = new Button ("Join expedition");
    private final Button createGameB = new Button ("Create expedition");
    private final Button exitB = new Button ("Exit");
    
    private final Button testConnectionB = new Button ("Test connection");
    
    public MainMenuScene (CaveApplication app) {
        super (app);
        
        initView ();
    }
    
    protected void initView () {
        final var menuBox = new VBox (32);
        menuBox.setAlignment (Pos.CENTER);
        setCenter (menuBox);
        
        joinGameB.setPrefWidth (SizeStyles.MAIN_MENU_BUTTONS_WIDTH);
        menuBox.getChildren ().add (joinGameB);
        joinGameB.setOnAction (ae -> {
            ApplicationScene.GAME.show (app);
        });
        
        testConnectionB.setPrefWidth (SizeStyles.MAIN_MENU_BUTTONS_WIDTH);
        menuBox.getChildren ().add (testConnectionB);
        testConnectionB.setOnAction (ae -> {
            testConnection ();
        });
        
        createGameB.setPrefWidth (SizeStyles.MAIN_MENU_BUTTONS_WIDTH);
        menuBox.getChildren ().add (createGameB);
        createGameB.setDisable (true);
        
        exitB.setPrefWidth (SizeStyles.MAIN_MENU_BUTTONS_WIDTH);
        menuBox.getChildren ().add (exitB);
        exitB.setOnMouseClicked (me -> {
            app.getStage ().close ();
        });
    }
    
    private final Random r = new Random ();
    
    private void testConnection () {
        //System.setProperty ("javax.net.ssl.trustStorePassword", "");
        //System.setProperty ("javax.net.ssl.trustStore", "client.jks");
        
        try {
            final var factory = SSLSocketFactory.getDefault ();            
            final var socket = (SSLSocket) factory.createSocket ("localhost", 12763);
            socket.setEnabledCipherSuites (new String [] {
                "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256"
            });
            socket.setEnabledProtocols (new String [] {
                "TLSv1.2"
            });
            
            //final var params = new SSLParameters ();
            //params.setEndpointIdentificationAlgorithm ("HTTPS");
            //socket.setSSLParameters (params);
            
            final var out = socket.getOutputStream ();
            
            out.write (("Test connection " + r.nextInt ()).getBytes (StandardCharsets.UTF_8));
            out.flush ();
            
            out.close ();
        } catch (IOException ioe) {
            ioe.printStackTrace ();
        }
    }

    @Override
    public void onVisible () {
        
    }
    
}
