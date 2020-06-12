package ru.shemplo.cave.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import lombok.Getter;
import ru.shemplo.cave.app.scenes.ApplicationScene;

public class CaveApplication extends Application {
    
    @Getter private Stage stage;
    
    @Override
    public void start (Stage stage) throws Exception {
        this.stage = stage;
        
        stage.setFullScreenExitKeyCombination (KeyCombination.NO_MATCH);
        stage.setFullScreenExitHint ("");
        stage.setTitle ("The Cave");
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
