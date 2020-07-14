package ru.shemplo.cave.experimental;

import java.util.List;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import ru.shemplo.cave.app.entity.level.GateType;
import ru.shemplo.cave.app.entity.level.RenderCell;
import ru.shemplo.cave.app.entity.level.RenderGate;
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
        
        stage.getIcons ().add (LevelTextures.caveIcon);
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
        final var cells = List.of (
            RenderCell.builder ().x (0).y (0).symbol ('├').build (),
            RenderCell.builder ().x (1).y (0).symbol ('┴').exit (true).build (),
            RenderCell.builder ().x (-1).y (0).symbol ('│').build ()
        );
        
        final var gates = List.of (
            RenderGate.builder ().x (0.5).y (0).vertical (false).type (GateType.GATE).closed (true).build (),
            RenderGate.builder ().x (-0.5).y (0).vertical (false).type (GateType.GATE).closed (false).build (),
            RenderGate.builder ().x (0).y (0.5).vertical (true).type (GateType.GATE).closed (true).build (),
            RenderGate.builder ().x (0).y (-0.5).vertical (true).type (GateType.GATE).closed (false).build ()
        );
        
        final var tumblers = List.of (
            RenderTumbler.builder ().x (1).y (0).build (),
            RenderTumbler.builder ().x (2).y (0).active (true).build ()
        );
        
        render.render (cells, gates, tumblers, List.of (), 0, 0, 0, false);
    }
    
}
