package ru.shemplo.cave.app.server;

import static ru.shemplo.cave.app.server.NetworkCommand.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import lombok.Getter;
import ru.shemplo.cave.app.entity.level.Level;

public class GameContext {
    
    private final Level level;
    
    private final Map <Integer, Boolean> id2supermode = new HashMap <> ();
    private final Map <Integer, Integer> id2px = new HashMap <> ();
    private final Map <Integer, Integer> id2py = new HashMap <> ();
    
    private final List <ClientConnection> players;
    
    @Getter
    private boolean exitFound = false;
    
    public GameContext (List <ClientConnection> connections) {
        players = connections;
        
        final var ps = connections.size ();
        
        level = new Level (ps * 15, ps * 15, ps);
        final var levelContext = level.getContext ();
        
        int i = 0;
        for (final var connection : connections) {
            final var spawns = levelContext.getSpawns ().get (i++);
            id2supermode.put (connection.getId (), false);
            id2px.put (connection.getId (), spawns.getX ());
            id2py.put (connection.getId (), spawns.getY ());
        }
    }
    
    public void applyMove (ClientConnection connection, int dx, int dy) {
        applyMove (connection, dx, dy, false);
    }
    
    private void applyMove (ClientConnection connection, int dx, int dy, boolean internal) {
        final var id = connection.getId ();
        
        if (Math.abs (dx) > 1 || Math.abs (dy) > 1) { return; }
        
        int px = id2px.getOrDefault (id, -1), py = id2py.getOrDefault (id, -1);
        final var supermode = id2supermode.get (id);
        
        if (dx != 0 || dy != 0) {
            if (!supermode && !level.canStepOnFrom (dx, dy, px, py)) { 
                return; // impossible to step on cell without super mode
            }
            
            px = id2px.compute (id, (__, v) -> v + dx);
            py = id2py.compute (id, (__, v) -> v + dy);
        }
        
        final double illumination = 1.0;
        final int x = px, y = py;
        
        final var visibleCells = level.getVisibleCells (px, py, illumination);
        final var cellsJoiner = new StringJoiner ("@");
        cellsJoiner.add (""); cellsJoiner.add ("");
        
        visibleCells.forEach (cell -> {
            final var symbol = String.valueOf (cell.getSymbol ());
            final String cx = String.valueOf (cell.getX () - x), 
                         cy = String.valueOf (cell.getY () - y);
            final var exit = String.valueOf (cell.isExit ());
            cellsJoiner.add (String.join (",", cx, cy, symbol, exit));
        });
        
        final var gatesJoiner = new StringJoiner ("@");
        gatesJoiner.add (""); gatesJoiner.add ("");
        level.getVisibleGates (px, py, illumination).forEach (gate -> {
            final var vertical = String.valueOf (gate.isVertical ());
            final var closed = String.valueOf (gate.isClosed ());
            final var type = String.valueOf (gate.getType ());
            final String cx = String.valueOf (gate.getX ()), 
                         cy = String.valueOf (gate.getY ());
            gatesJoiner.add (String.join (",", cx, cy, vertical, type, closed));
        });
        
        final var tumblersJoiner = new StringJoiner ("@");
        tumblersJoiner.add (""); tumblersJoiner.add ("");
        visibleCells.forEach (cell -> {
            if (cell.getTumbler () == null) { return; }
            final var active = String.valueOf (cell.getTumbler ().isActive ());
            final String cx = String.valueOf (cell.getX () - x), 
                         cy = String.valueOf (cell.getY () - y);
            tumblersJoiner.add (String.join (",", cx, cy, active));
        });
        
        final var playersJoiner = new StringJoiner ("@");
        playersJoiner.add (""); playersJoiner.add ("");
        for (final var p : players) {
            if (p.getId () == id) { continue; }
            
            final int prx = id2px.getOrDefault (p.getId (), -100000) - x, 
                      pry = id2py.getOrDefault (p.getId (), -100000) - y;
            
            for (final var cell : visibleCells) {
                if (cell.getX () - x == prx && cell.getY () - y == pry) {
                    playersJoiner.add (String.join (",", String.valueOf (prx), String.valueOf (pry)));
                    break;
                }
            }
        }
        
        if (!internal) {            
            connection.getRoom ().getConnections ().forEach (c -> {            
                applyMove (c, 0, 0, true);
            });
        }
        
        final String sx = String.valueOf (x), sy = String.valueOf (y); 
        connection.sendMessage (PLAYER_LOCATION.getValue (), sx, sy, cellsJoiner.toString (), 
                gatesJoiner.toString (), tumblersJoiner.toString ());
        
        connection.sendMessage (PLAYERS_LOCATION.getValue (), sx, sy, playersJoiner.toString (), 
                String.valueOf (supermode));
    }
    
    public void applyAction (ClientConnection connection, String action) {
        final var id = connection.getId ();
        
        final int px = id2px.getOrDefault (id, -1), py = id2py.getOrDefault (id, -1);
        if ("tumbler".equals (action)) {
            level.toggleTumbler (px, py);
        }
        
        connection.getRoom ().getConnections ().forEach (c -> {
            applyMove (c, 0, 0, true);
        });
    }
    
    public void applyUserModeToggle (ClientConnection connection) {
        id2supermode.compute (connection.getId (), (__, v) -> !v);
        applyMove (connection, 0, 0, true);
    }
    
    public boolean exitFound (ClientConnection connection) {
        final var id = connection.getId ();
        
        final int px = id2px.get (id), py = id2py.get (id);
        return exitFound = level.getContext ().getMap () [py][px].isExit ();
    }
    
}
