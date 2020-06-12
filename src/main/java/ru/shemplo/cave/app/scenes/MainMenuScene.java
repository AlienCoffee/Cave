package ru.shemplo.cave.app.scenes;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import ru.shemplo.cave.app.CaveApplication;
import ru.shemplo.cave.app.styles.SizeStyles;

public class MainMenuScene extends AbstractScene {
    
    private final Button joinGameB = new Button ("Join expedition");
    private final Button createGameB = new Button ("Create expedition");
    private final Button exitB = new Button ("Exit");
    
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
        
        createGameB.setPrefWidth (SizeStyles.MAIN_MENU_BUTTONS_WIDTH);
        menuBox.getChildren ().add (createGameB);
        createGameB.setDisable (true);
        
        exitB.setPrefWidth (SizeStyles.MAIN_MENU_BUTTONS_WIDTH);
        menuBox.getChildren ().add (exitB);
        exitB.setOnMouseClicked (me -> {
            app.getStage ().close ();
        });
    }
    
    /*
    private void testConnection () {
        System.setProperty ("javax.net.ssl.trustStore", "client.jks");
        //System.setProperty ("javax.net.ssl.trustStorePassword", "");
        
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
            final var in = socket.getInputStream ();
            
            System.out.println (new String (in.readNBytes (32), StandardCharsets.UTF_8)); // SYSOUT
        } catch (IOException ioe) {
            ioe.printStackTrace ();
        }
    }
    */

    @Override
    public void onVisible () {
        
    }
    
}
