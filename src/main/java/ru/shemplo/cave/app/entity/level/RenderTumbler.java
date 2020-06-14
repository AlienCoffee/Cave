package ru.shemplo.cave.app.entity.level;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RenderTumbler {
    
    private int x, y;
    
    private boolean active;
    
}
