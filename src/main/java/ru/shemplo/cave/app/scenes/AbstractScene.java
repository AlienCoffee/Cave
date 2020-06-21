package ru.shemplo.cave.app.scenes;

import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import ru.shemplo.cave.app.CaveApplication;

public abstract class AbstractScene extends StackPane {
    
    protected final CaveApplication app;
    
    protected final BorderPane root = new BorderPane ();
    
    protected final Canvas backgroundC = new Canvas ();
    
    public AbstractScene (CaveApplication app) {
        this.app = app;
        
        getChildren ().add (root);
    }
    
    protected abstract void initView ();
    
    public abstract void onVisible ();
    
}
