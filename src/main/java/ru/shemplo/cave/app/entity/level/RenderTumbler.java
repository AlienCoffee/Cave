package ru.shemplo.cave.app.entity.level;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString (of = {"x", "y", "active"})
public class RenderTumbler {
    
    private int x, y;
    
    private boolean active;
    
}
