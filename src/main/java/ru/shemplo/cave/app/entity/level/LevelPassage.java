package ru.shemplo.cave.app.entity.level;

import java.util.Comparator;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter @Setter
@AllArgsConstructor (access = AccessLevel.PRIVATE)
public class LevelPassage {
    
    private LevelCell from, to;
    
    private boolean gate, closed;
    
    public LevelCell getAnother (LevelCell cell) {
        if (from == cell) { return to; }
        if (to == cell) { return from; }
        
        throw new IllegalArgumentException ();
    }
    
    private static final Comparator <LevelCell> cellsOrderer = Comparator
          . comparing (LevelCell::getY).thenComparing (LevelCell::getX);
    
    public static LevelPassage of (LevelCell cellA, LevelCell cellB, boolean isGate) {
        final var order = cellsOrderer.compare (cellA, cellB);
        if (order > 0) {            
            return new LevelPassage (cellB, cellA, isGate, true);
        } else if (order < 0) {
            return new LevelPassage (cellA, cellB, isGate, true);
        }
        
        throw new IllegalArgumentException ();
    }
    
}
