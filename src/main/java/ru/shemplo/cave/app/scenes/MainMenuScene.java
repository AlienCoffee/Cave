package ru.shemplo.cave.app.scenes;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import ru.shemplo.cave.app.CaveApplication;
import ru.shemplo.cave.app.resources.LevelTextures;
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
        getChildren ().add (0, backgroundC);
        
        final var menuBox = new VBox (32);
        menuBox.setAlignment (Pos.CENTER);
        root.setCenter (menuBox);
        
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
        final var context = backgroundC.getGraphicsContext2D ();
        backgroundC.setHeight (getHeight ());
        backgroundC.setWidth (getWidth ());
        
        final double w = backgroundC.getWidth (), h = backgroundC.getHeight ();
        context.drawImage (LevelTextures.caveBackground2, 0, 0, w, h);
    }
    
}
