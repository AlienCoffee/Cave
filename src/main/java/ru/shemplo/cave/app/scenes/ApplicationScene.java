package ru.shemplo.cave.app.scenes;

import static ru.shemplo.cave.app.ApplicationConstants.*;

import java.util.List;
import java.util.function.Function;

import javafx.application.Platform;
import ru.shemplo.cave.app.CaveApplication;

public enum ApplicationScene {
    
    MAIN_MENU     (MainMenuScene::new, List.of ("/common.css"), true),
    GAME_JOIN     (GameJoinScene::new, List.of ("/common.css"), false),
    GAME_SETTINGS (GameSettingsScene::new, List.of ("/common.css"), false),
    GAME          (GameScene::new, List.of ("/common.css"), false),
    
    ;
    
    private final Function <CaveApplication, AbstractScene> sceneProducer;
    private final List <String> styles;
    private final boolean keepLoaded;
    
    private AbstractScene scene;
    
    private ApplicationScene (
        Function <CaveApplication, AbstractScene> sceneProducer, 
        List <String> styles, boolean keepLoaded
    ) {
        this.styles = preparePaths (styles, CSSs_PATH);
        this.sceneProducer = sceneProducer;
        this.keepLoaded = keepLoaded;
    }
    
    public void show (CaveApplication app) {
        if (this.scene == null || !keepLoaded) {
            this.scene = sceneProducer.apply (app);
            //this.scene.onCreated ();
        }
        
        final var stageScene = app.getStage ().getScene ();
        Platform.runLater (() -> {
            stageScene.setRoot (this.scene);
            
            stageScene.getStylesheets ().clear ();
            stageScene.getStylesheets ().setAll (styles);
            
            this.scene.onVisible ();
        });
    }
    
}
