package ru.shemplo.cave.app.entity.level;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RenderCell {
    
    private int x, y;
    
    private Image image;
    
    private Color effect;
    
    private int subpart;
    
}
