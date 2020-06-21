package ru.shemplo.cave.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import ru.shemplo.cave.app.resources.LevelTextures;
import ru.shemplo.cave.app.scenes.ApplicationScene;
import ru.shemplo.cave.app.server.ClientConnection;

public class CaveApplication extends Application {
    
    @Getter private Stage stage;
    
    @Setter @Getter
    private ClientConnection connection;
    
    @Override
    public void start (Stage stage) throws Exception {
        this.stage = stage;
        
        stage.setFullScreenExitKeyCombination (KeyCombination.NO_MATCH);
        stage.getIcons ().add (LevelTextures.caveIcon);
        stage.setFullScreenExitHint ("");
        stage.setTitle ("The Cave");
        stage.setMinWidth (600);
        
        //stage.setFullScreen (true);
        //stage.setResizable (false);
        
        stage.setScene (makeScene (stage));
        stage.show ();
        
        ApplicationScene.MAIN_MENU.show (this);
    }
    
    private Scene makeScene (Stage stage) {
        final var scene = new Scene (new Pane ());
        return scene;
    }
    
}
