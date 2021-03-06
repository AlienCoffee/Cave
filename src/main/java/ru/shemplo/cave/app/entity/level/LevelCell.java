package ru.shemplo.cave.app.entity.level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ru.shemplo.cave.utils.IPoint;
import ru.shemplo.snowball.stuctures.Pair;

@Getter @Setter
@AllArgsConstructor
@RequiredArgsConstructor
@ToString (of = {"x", "y", "part", "subpart", "isExit"})
public class LevelCell implements Comparable <LevelCell> {
    
    private final int x, y;
    
    private LevelCell top, right, bottom, left;
    
    // For maze generator purposes only
    private final List <LevelPassage> neighbors = new ArrayList <> ();
    
    private LevelPassage topPass, rightPass, bottomPass, leftPass;
    
    private LevelTumbler tumbler;
    
    private int part = -1, subpart = -1;
    
    private boolean isExit, isSpawn;
    
    public List <Pair <LevelCell, IPoint>> getMapNeighbors () {
        return Arrays.asList  (
            Pair.mp (top,    IPoint.of (0, -1)),
            Pair.mp (right,  IPoint.of (1, 0)),
            Pair.mp (bottom, IPoint.of (0, 1)),
            Pair.mp (left,   IPoint.of (-1, 0))
        );
    }
    
    public List <Pair <LevelPassage, IPoint>> getPassageNeighbors () {
        return Arrays.asList (
            Pair.mp (topPass,    IPoint.of (0, -1)),
            Pair.mp (rightPass,  IPoint.of (1, 0)),
            Pair.mp (bottomPass, IPoint.of (0, 1)),
            Pair.mp (leftPass,   IPoint.of (-1, 0))
        );
    }
    
    public LevelPassage getPassageNeighbor (int dx, int dy) {
        if (dx > 0 && dy == 0) {
            return rightPass;
        } else if (dx < 0 && dy == 0) {
            return leftPass;
        } else if (dx == 0 && dy > 0) {
            return bottomPass;
        } else if (dx == 0 && dy < 0) {
            return topPass;
        }
        
        throw new IllegalArgumentException (dx + ", " + dy);
    }
    
    public void setPassageNeighbor (int dx, int dy, LevelPassage passage) {
        if (dx > 0 && dy == 0) {
            rightPass = passage;
        } else if (dx < 0 && dy == 0) {
            leftPass = passage;
        } else if (dx == 0 && dy > 0) {
            bottomPass = passage;
        } else if (dx == 0 && dy < 0) {
            topPass = passage;
        }
    }
    
    public IPoint getPoint (double distance) {
        return IPoint.of (x, y, distance);
    }
    
    public IPoint getPartPoint () {
        return IPoint.of (part, subpart);
    }
    
    public void setRelative (IPoint relation, boolean inversed, LevelCell relative) {
        if (inversed) { relation = IPoint.of (-relation.X, -relation.Y); }
        
        if (relation.X > 0 && relation.Y== 0) {
            setRight (relative);
        } else if (relation.X < 0 && relation.Y == 0) {
            setLeft (relative);
        } else if (relation.X == 0 && relation.Y > 0) {
            setBottom (relative);
        } else if (relation.X == 0 && relation.Y < 0) {
            setTop (relative);
        }
    }
    
    public boolean hasRelative (IPoint relation) {
        if (relation.X > 0 && relation.Y == 0) {
            return getRight () != null;
        } else if (relation.X < 0 && relation.Y == 0) {
            return getLeft () != null;
        } else if (relation.X == 0 && relation.Y > 0) {
            return getBottom () != null;
        } else if (relation.X == 0 && relation.Y < 0) {
            return getTop () != null;
        }
        
        throw new IllegalArgumentException ();
    }
    
    public char getSymbol () {
        boolean t = getTopPass () != null,    r = getRightPass () != null,
                b = getBottomPass () != null, l = getLeftPass () != null;
        if (t && r && b && l) {
            return '┼';
        } else if (t && r && b) {
            return '├';
        } else if (t && r && l) {
            return '┴';
        } else if (t && b && l) {
            return '┤';
        } else if (r && b && l) {
            return '┬';
        } else if (t && r) {
            return '└';
        } else if (t && b) {
            return '│';
        } else if (t && l) {
            return '┘';
        } else if (r && b) {
            return '┌';
        } else if (r && l) {
            return '─';
        } else if (b && l) {
            return '┐';
        } else if (t) {
            return '╧';
        } else if (r) {
            return '╟';
        } else if (b) {
            return '╤';
        } else if (l) {
            return '╢';
        }
        
        return ' ';
    }

    @Override
    public int compareTo (LevelCell cell) {
        return getY () == cell.getY () ? (getX () - cell.getX ()) : (getY () - cell.getY ());
    }
    
}
