package ru.shemplo.cave.app.entity.level;

import java.util.Set;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString (of = {"x", "y", "symbol", "exit"})
public class RenderCell {
    
    public static Set <Character> top    = Set.of ('┼', '├', '┴', '┤', '└', '│', '┘', '╧');
    public static Set <Character> right  = Set.of ('┼', '├', '┴', '┬', '└', '┌', '─', '╟');
    public static Set <Character> bottom = Set.of ('┼', '├', '┤', '┬', '│', '┌', '┐', '╤');
    public static Set <Character> left   = Set.of ('┼', '┤', '┴', '┬', '┐', '┘', '─', '╢');
    
    private int x, y;
    
    // client side only
    private Image image;
    
    // server side only
    private char symbol;
    
    private Color effect;
    
    private int subpart;
    
    private boolean exit;
    
    public boolean hasTopPassage () {
        return top.contains (getSymbol ());
    }
    
    public boolean hasRightPassage () {
        return right.contains (getSymbol ());
    }
    
    public boolean hasBottomPassage () {
        return bottom.contains (getSymbol ());
    }
    
    public boolean hasLeftPassage () {
        return left.contains (getSymbol ());
    }
    
}
