package ru.shemplo.cave.app.entity.level;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter @Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class LevelTumbler {
    
    private final LevelCell cell;
    
    /*
     * If tumbler is activated then all passages in `open` will be opened and all passages in `close` will be closed
     */
    private final List <LevelPassage> 
        open  = new ArrayList <> (), 
        close = new ArrayList <> ();
    
    private boolean isActive;
    
}
