package ru.shemplo.cave.app.server;

import static ru.shemplo.cave.app.server.NetworkCommand.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import ru.shemplo.cave.app.entity.level.Level;

public class GameContext {
    
    @SuppressWarnings ("unused")
    private final ConnectionsPool pool;
    
    private final Level level;
    
    private final Map <Integer, Integer> id2px = new HashMap <> ();
    private final Map <Integer, Integer> id2py = new HashMap <> ();
    
    private final List <ClientConnection> players;
    
    public GameContext (ConnectionsPool pool, List <ClientConnection> connections) {
        players = connections;
        this.pool = pool;
        
        final var ps = connections.size ();
        
        level = new Level (ps * 20, ps * 20, ps);
        final var levelContext = level.getContext ();
        
        int i = 0;
        for (final var connection : connections) {
            final var seed = levelContext.getSeeds ().get (i++);
            id2px.put (connection.getId (), seed.X);
            id2py.put (connection.getId (), seed.Y);
        }
    }
    
    public void applyMove (ClientConnection connection, int dx, int dy) {
        applyMove (connection, dx, dy, false);
    }
    
    private void applyMove (ClientConnection connection, int dx, int dy, boolean internal) {
        final var id = connection.getId ();
        
        if (Math.abs (dx) > 1 || Math.abs (dy) > 1) { return; }
        
        int px = id2px.getOrDefault (id, -1), py = id2py.getOrDefault (id, -1);
        if (dx != 0 || dy != 0) {
            if (!level.canStepOnFrom (dx, dy, px, py)) { return; }
            
            px = id2px.compute (id, (__, v) -> v + dx);
            py = id2py.compute (id, (__, v) -> v + dy);
        }
        
        final int x = px, y = py;
        
        final var visibleCells = level.getVisibleCells (px, py, 2.0);
        final var cellsJoiner = new StringJoiner ("@");
        cellsJoiner.add (""); cellsJoiner.add ("");
        
        visibleCells.forEach (cell -> {
            final var symbol = String.valueOf (cell.getSymbol ());
            final String cx = String.valueOf (cell.getX ()), 
                         cy = String.valueOf (cell.getY ());
            cellsJoiner.add (String.join (",", cx, cy, symbol));
        });
        
        final var gatesJoiner = new StringJoiner ("@");
        gatesJoiner.add (""); gatesJoiner.add ("");
        level.getVisibleGates (px, py).forEach (gate -> {
            final var vertical = String.valueOf (gate.isVertical ());
            final var closed = String.valueOf (gate.isClosed ());
            final var type = String.valueOf (gate.getType ());
            final String cx = String.valueOf (gate.getX ()), 
                         cy = String.valueOf (gate.getY ());
            gatesJoiner.add (String.join (",", cx, cy, vertical, type, closed));
        });
        
        final var playersJoiner = new StringJoiner ("@");
        playersJoiner.add (""); playersJoiner.add ("");
        for (final var p : players) {
            if (p.getId () == id) { continue; }
            
            final int prx = id2px.getOrDefault (p.getId (), -100000) - x, 
                      pry = id2py.getOrDefault (p.getId (), -100000) - y;
            
            for (final var cell : visibleCells) {
                if (cell.getX () == prx && cell.getY () == pry) {
                    playersJoiner.add (String.join (",", String.valueOf (prx), String.valueOf (pry)));
                    if (!internal) { applyMove (p, 0, 0, true); }
                    break;
                }
            }
        }
        
        final String sx = String.valueOf (x), sy = String.valueOf (y); 
        connection.sendMessage (PLAYER_LOCATION.getValue (), sx, sy, cellsJoiner.toString (), gatesJoiner.toString ());
        connection.sendMessage (PLAYERS_LOCATION.getValue (), sx, sy, playersJoiner.toString ());
    }
    
}
