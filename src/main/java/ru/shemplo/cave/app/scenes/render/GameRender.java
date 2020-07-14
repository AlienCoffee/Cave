package ru.shemplo.cave.app.scenes.render;

import java.util.List;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lombok.RequiredArgsConstructor;
import ru.shemplo.cave.app.entity.level.GateType;
import ru.shemplo.cave.app.entity.level.RenderCell;
import ru.shemplo.cave.app.entity.level.RenderGate;
import ru.shemplo.cave.app.entity.level.RenderTumbler;
import ru.shemplo.cave.app.resources.LevelTextures;
import ru.shemplo.cave.utils.IPoint;

@RequiredArgsConstructor
public class GameRender {
    
    private final Canvas canvas;
    private GraphicsContext ctx;
    
    private double ts = 32.0 * 6; // tile size 
    
    public void setScale (double scale) {
        ts = Math.max (4.0, 32 * scale);
    }
    
    public void render (
        List <RenderCell> cells, List <RenderGate> gates, List <RenderTumbler> tumblers, 
        List <IPoint> players, int mx, int my, int countdown, boolean supermode
    ) {
        final double cw = canvas.getWidth (), ch =  canvas.getHeight ();
        final double cx = cw / 2, cy =  ch / 2;
        
        if (ctx == null) {
            ctx = canvas.getGraphicsContext2D ();
        }
        
        ctx.setFill (Color.BLACK);
        ctx.fillRect (0, 0, cw, ch);
        
        for (final var cell : cells) {
            final var cellSkin = LevelTextures.symbol2texture.get (' ');
            ctx.drawImage (cellSkin, cx + (cell.getX () - 0.5) * ts, cy + (cell.getY () - 0.5) * ts, ts, ts);
            final var passageSkin = LevelTextures.passage;
            
            if (cell.hasTopPassage ()) {
                final var px = cx + cell.getX () * ts - ts / 10;
                final var py = cy + (cell.getY () - 0.5) * ts - 2 - ts / 25.0 * 0;
                ctx.drawImage (passageSkin, px, py + 2, ts / 5, ts / 5);
            }
            
            if (cell.hasRightPassage ()) {
                final var px =  cy + cell.getY () * ts - ts / 10;
                final var py = -cx - (cell.getX () + 0.5) * ts + ts / 15 * 0 - 2;
                
                ctx.rotate (90);
                ctx.drawImage (passageSkin, px, py + 2, ts / 5, ts / 5);
                ctx.rotate (-90);
            }
            
            if (cell.hasBottomPassage ()) {
                final var px = cx + cell.getX () * ts + ts / 10;
                final var py = cy + (cell.getY () + 0.5) * ts - ts / 15 * 0 + 2;
                
                ctx.rotate (180);
                ctx.drawImage (passageSkin, -px, -py + 2, ts / 5, ts / 5);
                ctx.rotate (-180);
            }
            
            if (cell.hasLeftPassage ()) {
                final var px =  cy + cell.getY () * ts + ts / 10;
                final var py = -cx - (cell.getX () - 0.5) * ts - ts / 15 * 0 + 2;
                
                ctx.rotate (-90);
                ctx.drawImage (passageSkin, -px, -py + 2, ts / 5, ts / 5);
                ctx.rotate (90);
            }
            
            if (cell.isExit ()) {
                ctx.setFill (Color.WHITESMOKE);
                ctx.drawImage (LevelTextures.ladder, cx + cell.getX () * ts - 20, cy + cell.getY () * ts - 20, 40, 40);
            }
        };
        
        for (final var gate : gates) {
            ctx.setFill (gate.getType () == GateType.GATE 
                    ? (gate.isClosed () ? Color.BROWN : Color.LIMEGREEN) 
                            : (gate.getType () == GateType.SLIT ? Color.ALICEBLUE : Color.BLACK)
                    );
            
            if (gate.getType () == GateType.GATE) {
                final var gateSkin = gate.isClosed () ? LevelTextures.gatesClosedH : LevelTextures.gatesOpenedH;
                if (gate.isVertical ()) {
                    final var px = cx + gate.getX () * ts - ts / 7;
                    final var py = cy + gate.getY () * ts - ts / 24;
                    ctx.drawImage (gateSkin, px, py, ts / 4, ts / 12);
                } else {
                    final var px =  cy + gate.getY () * ts - ts / 7;
                    final var py = -cx - gate.getX () * ts - ts / 24;
                    
                    ctx.rotate (90);
                    ctx.drawImage (gateSkin, px, py, ts / 4, ts / 12);
                    ctx.rotate (-90);
                }
            }
        };
        
        for (final var tumbler : tumblers) {
            final var tumblerSkin = tumbler.isActive () ? LevelTextures.tumblerOn : LevelTextures.tumblerOff;
            ctx.setFill (tumbler.isActive () ? Color.LIMEGREEN : Color.BROWN);
            
            final var fx = cx + (tumbler.getX () - 0.5) * ts + ts / 4 + 4;
            final var fy = cy + (tumbler.getY () - 0.5) * ts + ts / 10;
            
            ctx.fillRect (fx + 1, fy + 1, 13, 8);
            ctx.drawImage (tumblerSkin, fx, fy, 15, 10);
        };
        
        final var playerSkin = LevelTextures.player;
        for (final var player : players) {            
            ctx.drawImage (playerSkin, cx + player.X * ts - ts / 32, cy + player.Y * ts - ts / 16, ts / 16, ts / 8);
        }
        
        ctx.drawImage (playerSkin, cx - ts / 32, cy - ts / 16, ts / 16, ts / 8);
        
        for (final var cell : cells) {
            if (cell.isExit () && cell.getX () == 0 && cell.getY () == 0) { // player stays on exit cell
                ctx.setFill (Color.SANDYBROWN);
                ctx.fillText ("Exit is found! Press <E> to finish the expedition", 20, 100);
            }
        };
        
        ctx.setFill (Color.YELLOW);
        ctx.fillText ("X: " + mx, 20, 40);
        ctx.fillText ("Y: " + my, 20, 55);
        ctx.fillText (String.format ("Rest time: %02d:%02d", countdown / 60, countdown % 60), 20, 70);
        ctx.fillText ("Supermode: " + supermode, 20, 85);
    }
    
}
