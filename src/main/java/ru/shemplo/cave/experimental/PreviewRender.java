package ru.shemplo.cave.experimental;

import java.util.List;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import ru.shemplo.cave.app.entity.level.RenderCell;
import ru.shemplo.cave.app.entity.level.RenderTumbler;
import ru.shemplo.cave.app.resources.LevelTextures;
import ru.shemplo.cave.app.scenes.render.GameRender;

public class PreviewRender extends Application {

    public static class RunPreviewRender {
        
        public static void main (String ... args) {
            Application.launch (PreviewRender.class, args);
        }
        
    }
    
    private final Canvas canvasC = new Canvas ();
    private final GameRender render = new GameRender (canvasC);
    
    @Override
    public void start (Stage stage) throws Exception {
        final var root = new BorderPane ();
        root.setCenter (canvasC);
        
        final var scene = new Scene (root);
        stage.setScene (scene);
        
        stage.setTitle ("Render preview");
        stage.show ();
        
        root.applyCss ();
        root.layout ();
        
        canvasC.heightProperty ().bind (root.heightProperty ());
        canvasC.widthProperty ().bind (root.widthProperty ());
        
        stage.widthProperty ().addListener ((__, ___, ____) -> {
            doPreviewRender ();
        });
        
        stage.heightProperty ().addListener ((__, ___, ____) -> {
            doPreviewRender ();
        });
        
        doPreviewRender ();
    }
    
    private void doPreviewRender () {
        final var lock = new Object ();
        
        final var cells = List.of (
            RenderCell.builder ().x (0).y (0).image (LevelTextures.symbol2texture.get (' ')).build (),
            RenderCell.builder ().x (1).y (0).image (LevelTextures.symbol2texture.get (' ')).build (),
            RenderCell.builder ().x (1).y (-1).image (LevelTextures.symbol2texture.get ('┬')).build (),
            RenderCell.builder ().x (2).y (0).image (LevelTextures.symbol2texture.get (' ')).exit (true).build (),
            RenderCell.builder ().x (-1).y (0).image (LevelTextures.symbol2texture.get (' ')).build ()
        );
        
        final var tumblers = List.of (
            RenderTumbler.builder ().x (1).y (0).build (),
            RenderTumbler.builder ().x (2).y (0).active (true).build ()
        );
        
        render.render (cells, List.of (), tumblers, List.of (), 0, 0, 0, false, lock);
    }
    
}
