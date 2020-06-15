package ru.shemplo.cave.experimental;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.shemplo.cave.app.entity.level.LevelCell;
import ru.shemplo.cave.utils.IPoint;

@Getter
@RequiredArgsConstructor
public class LevelGenerationGraph {
    
    public static final int INFINITY = 1_000_000;
    
    private final LevelGenerationContext context;
    
    private List <LevelCell> nodes = new ArrayList <> ();
    
    private final Map <IPoint, LevelCell> part2node = new HashMap <> ();
    
    private int [][] distances;
    
    public void runFloydWarshall () {
        final int [][] matrix = new int [nodes.size ()][nodes.size ()];
        for (int i = 0; i < nodes.size (); i++) {
            for (int j = 0; j < i; j++) {
                final var toNode = nodes.get (j);
                final var index = i;
                
                final var distance = nodes.get (i).getNeighbours ().stream ().<Integer> map (passage -> {
                    final var nei = passage.getAnother (nodes.get (index));
                    if (nei.getPart () == toNode.getPart () && nei.getSubpart () == toNode.getSubpart ()) {
                        return 1;
                    }
                    
                    return null;
                }).dropWhile (Objects::isNull).findFirst ().orElse (INFINITY);
                matrix [i][j] = matrix [j][i] = distance;
            }
        }
        
        for (int k = 0; k < matrix.length; k++) {
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix.length; j++) {
                    matrix [i][j] = Math.min (matrix [i][j], matrix [i][k] + matrix [k][j]);
                }
            }
        }
        
        distances = matrix;
    }
    
}
