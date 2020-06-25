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
    
    private double ts = 32 * 6; // tile size 
    
    public void setScale (double scale) {
        ts = Math.max (4.0, 32 * scale);
    }
    
    public void render (
        List <RenderCell> cells, List <RenderGate> gates, List <RenderTumbler> tumblers, 
        List <IPoint> players, int mx, int my, int countdown, boolean supermode, 
        Object lock
    ) {
        final double cw = canvas.getWidth (), ch =  canvas.getHeight ();
        final double cx = cw / 2, cy =  ch / 2;
        
        if (ctx == null) {
            ctx = canvas.getGraphicsContext2D ();
        }
        
        ctx.setFill (Color.BLACK);
        ctx.fillRect (0, 0, cw, ch);
        
        synchronized (lock) {                
            cells.forEach (cell -> {
                ctx.drawImage (cell.getImage (), cx + (cell.getX () - 0.5) * ts, cy + (cell.getY () - 0.5) * ts, ts, ts);
                
                if (cell.isExit ()) {
                    ctx.setFill (Color.WHITESMOKE);
                    ctx.fillRect (cx + cell.getX () * ts - 20, cy + cell.getY () * ts - 20, 40, 40);
                    
                    if (cell.getX () == 0 && cell.getY () == 0) { // player stays on this cell
                        ctx.setFill (Color.SANDYBROWN);
                        ctx.fillText ("Exit is found! Press <E> to finish the expedition", 20, 100);
                    }
                }
            });
            
            gates.forEach (gate -> {
                ctx.setFill (gate.getType () == GateType.GATE 
                        ? (gate.isClosed () ? Color.BROWN : Color.LIMEGREEN) 
                                : (gate.getType () == GateType.SLIT ? Color.ALICEBLUE : Color.BLACK)
                        );
                if (gate.isVertical ()) {
                    if (gate.getType () == GateType.GATE) {   
                        final var gatesSkin = gate.isClosed () ? LevelTextures.gatesClosedV : LevelTextures.gatesOpenedV;
                        ctx.drawImage (gatesSkin, cx + gate.getX () * ts - 15, cy + gate.getY () * ts - ts / 4, 30, ts / 2);
                    } else {
                        final var slitSkin = LevelTextures.gatesClosedV;
                        ctx.drawImage (slitSkin, cx + gate.getX () * ts - 15, cy + gate.getY () * ts - ts / 4, 30, ts / 2);
                    }
                } else {
                    if (gate.getType () == GateType.GATE) {
                        final var gatesSkin = gate.isClosed () ? LevelTextures.gatesClosedH : LevelTextures.gatesOpenedH;
                        ctx.drawImage (gatesSkin, cx + gate.getX () * ts - ts / 4 - 4, cy + gate.getY () * ts - 15, ts / 2, 30);
                    } else {
                        final var slitSkin = LevelTextures.slitH;
                        ctx.drawImage (slitSkin, cx + gate.getX () * ts - ts / 4, cy + gate.getY () * ts - 15, ts / 2, 30);
                    }
                }
            });
            
            tumblers.forEach (tumbler -> {
                final var tumblerSkin = tumbler.isActive () ? LevelTextures.tumblerOn : LevelTextures.tumblerOff;
                ctx.setFill (tumbler.isActive () ? Color.LIMEGREEN : Color.BROWN);
                
                final var fx = cx + (tumbler.getX () - 0.5) * ts + 35;
                final var fy = cy + (tumbler.getY () - 0.5) * ts + 7;
                
                ctx.fillRect (fx + 1, fy + 1, 13, 13);
                ctx.drawImage (tumblerSkin, fx, fy, 15, 15);
            });
            
            final var playerSkin = LevelTextures.player;
            players.forEach (player -> {
                ctx.drawImage (playerSkin, cx + player.X * ts - 10, cy + player.Y * ts - 20, 20, 40);
            });
            
            ctx.drawImage (playerSkin, cx - 10, cy - 20, 20, 40);
            
            cells.forEach (cell -> {
                if (cell.isExit () && cell.getX () == 0 && cell.getY () == 0) { // player stays on exit cell
                    ctx.setFill (Color.SANDYBROWN);
                    ctx.fillText ("Exit is found! Press <E> to finish the expedition", 20, 100);
                }
            });
        }
        
        ctx.setFill (Color.YELLOW);
        ctx.fillText ("X: " + mx, 20, 40);
        ctx.fillText ("Y: " + my, 20, 55);
        ctx.fillText (String.format ("Rest time: %02d:%02d", countdown / 60, countdown % 60), 20, 70);
        ctx.fillText ("Supermode: " + supermode, 20, 85);
    }
    
}
