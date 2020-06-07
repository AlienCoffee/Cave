package ru.shemplo.cave.experimental;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.shemplo.cave.app.entity.level.LevelCell;
import ru.shemplo.cave.utils.IPoint;

@Builder
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class LevelGenerationContext {
    
    private LevelCell [][] mask;
    
    private List <IPoint> seeds;
    
}
