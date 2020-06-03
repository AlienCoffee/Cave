package ru.shemplo.cave.app.scenes;

import javafx.scene.layout.BorderPane;
import ru.shemplo.cave.app.CaveApplication;

public abstract class AbstractScene extends BorderPane {
    
    protected final CaveApplication app;
    
    public AbstractScene (CaveApplication app) {
        this.app = app;
    }
    
    protected abstract void initView ();
    
    public abstract void onVisible ();
    
}
