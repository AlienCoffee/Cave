package ru.shemplo.cave.app.entity.level;

import javafx.scene.image.Image;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RenderGate {
    
    private double x, y;
    
    private Image image;
    
    private boolean vertical;
    
    private GateType type;
    
    private boolean closed;
    
}
