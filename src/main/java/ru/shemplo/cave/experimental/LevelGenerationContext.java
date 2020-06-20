package ru.shemplo.cave.experimental;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ru.shemplo.cave.app.entity.level.LevelCell;
import ru.shemplo.cave.app.entity.level.LevelPassage;
import ru.shemplo.cave.utils.IPoint;

@Builder
@Getter @Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class LevelGenerationContext {
    
    private final int width, height, parts, subparts;
    
    private LevelCell [][] map;
    
    @Deprecated
    public LevelCell [][] getMask () {
        return map;
    }
    
    private List <IPoint> partSeeds;
    
    private List <LevelCell> spawns;
    
    @Deprecated
    public List <IPoint> getPartsSeeds () {
        return partSeeds;
    }
    
    private List <List <LevelCell>> part2cells;
    
    private List <List <List <LevelCell>>> part2subpart2cells;
    
    private List <List <LevelCell>> part2subpartsOrder;
    
    private List <List <LevelPassage>> part2gates;
    
    private LevelCell exit;
    
    private LevelGenerationGraph graph;
    
}
