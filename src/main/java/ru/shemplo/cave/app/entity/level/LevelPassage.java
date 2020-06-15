package ru.shemplo.cave.app.entity.level;

import java.util.Comparator;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter @Setter
@EqualsAndHashCode
@AllArgsConstructor (access = AccessLevel.PRIVATE)
public class LevelPassage {
    
    private LevelCell from, to;
    
    private GateType gateType;
    private boolean closed;
    
    // For maze generator purposes only
    private LevelPassage prototype;
    
    public LevelCell getAnother (LevelCell cell) {
        if (from == cell) { return to; }
        if (to == cell) { return from; }
        
        throw new IllegalArgumentException ();
    }
    
    private static final Comparator <LevelCell> cellsOrderer = Comparator
          . comparing (LevelCell::getY).thenComparing (LevelCell::getX);
    
    public static LevelPassage of (LevelCell cellA, LevelCell cellB, GateType gateType) {
        final var order = cellsOrderer.compare (cellA, cellB);
        if (order > 0) {            
            return new LevelPassage (cellB, cellA, gateType, true, null);
        } else if (order < 0) {
            return new LevelPassage (cellA, cellB, gateType, true, null);
        }
        
        throw new IllegalArgumentException ();
    }
    
}
