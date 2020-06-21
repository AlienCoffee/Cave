package ru.shemplo.cave.app.scenes;

import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ru.shemplo.cave.app.CaveApplication;
import ru.shemplo.cave.app.resources.LevelTextures;
import ru.shemplo.cave.app.styles.SizeStyles;

public class MainMenuScene extends AbstractScene {
    
    private final Button joinGameB = new Button ("Join expedition");
    private final Button createGameB = new Button ("Create expedition");
    private final Button exitB = new Button ("Exit");
    
    private final Canvas canvasC = new Canvas ();
    
    public MainMenuScene (CaveApplication app) {
        super (app);
        
        initView ();
    }
    
    protected void initView () {
        final var stackP = new StackPane ();
        setCenter (stackP);
        
        stackP.getChildren ().add (canvasC);
        
        final var menuBox = new VBox (32);
        menuBox.setAlignment (Pos.CENTER);
        stackP.getChildren ().add (menuBox);
        
        joinGameB.setPrefWidth (SizeStyles.MAIN_MENU_BUTTONS_WIDTH);
        menuBox.getChildren ().add (joinGameB);
        joinGameB.setOnAction (ae -> {
            ApplicationScene.GAME_JOIN.show (app);
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

    @Override
    public void onVisible () {
        final var context = canvasC.getGraphicsContext2D ();
        canvasC.setHeight (getHeight ());
        canvasC.setWidth (getWidth ());
        
        /*
        for (double i = 0; i < canvasC.getWidth (); i += 64.0) {            
            context.drawImage (LevelTextures.caveBackground, i, 0, 64, canvasC.getHeight ());
        }
        */
        
        final double w = canvasC.getWidth (), h = canvasC.getHeight ();
        context.drawImage (LevelTextures.caveBackground, 0, 0, w, h);        
    }
    
}
