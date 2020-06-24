package ru.shemplo.cave.app.server.room;

import static ru.shemplo.cave.app.server.NetworkCommand.*;
import static ru.shemplo.cave.app.server.room.state.ServerState.*;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;
import ru.shemplo.cave.app.entity.Player;
import ru.shemplo.cave.app.server.ClientConnection;
import ru.shemplo.cave.app.server.ConnectionsPool;
import ru.shemplo.cave.app.server.room.state.ServerState;

@Getter
@ToString (of = {"id", "idh"})
public class ServerRoom implements Closeable {
    
    private final int id;
    private final String idh;
    
    private final ConnectionsPool pool;
    
    @Getter (AccessLevel.PRIVATE)
    private final Thread controller;
    
    @Getter (AccessLevel.PRIVATE)
    private final ServerRoomContext context = new ServerRoomContext (this);
    
    public ServerRoom (int id, ConnectionsPool pool) {
        this.id = id; this.pool = pool;
        
        final var random = pool.getRandom ();
        //idh = Utils.digest (String.format ("room-%d", id), CaveServer.SERVER_SALT);
        final var letter = String.valueOf ((char) ('a' + random.nextInt ('z' - 'a')));
        idh = String.format ("%s%d", letter, id);
        
        controller = new Thread (() -> {
            long stateSwitchedTime = System.currentTimeMillis ();
            final var sleepTime = 250;
            
            while (!Thread.currentThread ().isInterrupted ()) {
                final var start = System.currentTimeMillis ();
                
                final var timeSinceSwitch = start - stateSwitchedTime;
                
                synchronized (context) {
                    synchronized (state) {
                        // new state and reason
                        final var sNr = state.update (context, timeSinceSwitch);
                        
                        if (sNr != null && sNr.F != state) {
                            broadcastMessage (SERVER_STATE.getValue (), sNr.F.name (), sNr.S);
                            stateSwitchedTime = System.currentTimeMillis ();
                            state = sNr.F;
                        }
                    }
                }
                
                final var end = System.currentTimeMillis ();
                
                try { 
                    // try to do 4 updates per second
                    final var workingTime = end - start;
                    if (workingTime * 2 > sleepTime) {
                        final var format = "Too long update in room #%d: %dms";
                        System.out.println (String.format (format, id, workingTime)); // SYSOUT
                    }
                    
                    Thread.sleep (Math.max (0, sleepTime - workingTime)); 
                } catch (InterruptedException ie) {
                    Thread.currentThread ().interrupt ();
                    return; 
                }
            }
        }, String.format ("Room-%d-Controller-Thread", id));
        controller.setDaemon (true);
    }
    
    @Getter (AccessLevel.PRIVATE)
    private ServerState state = ServerState.RECRUITING;
    
    { // initialization of state
        synchronized (context) {
            synchronized (state) {
                state.initialize (context);
            }
        }
    }
    
    public List <ClientConnection> getConnections () {
        synchronized (context) {            
            return List.copyOf (context.getRoomConnections ());
        }
    }
    
    public ServerRoom open () {
        System.out.println ("Room #" + id + " is openned"); // SYSOUT
        controller.start ();
        return this;
    }
    
    private volatile long timeSinceEmpty = -1;
    private boolean isEmpty = true;
    
    public void checkConnections () {
        synchronized (context) {
            synchronized (state) {
                final var connections = getConnections ();
                
                int alive = 0;
                for (final var connection : connections) {
                    if (connection.getNonTestedTime () > 250) {                            
                        connection.sendMessage (PING.getValue ());
                    }
                    
                    alive += connection.isAlive () ? 1 : 0;
                }
                
                final var beforeCleaning = connections.size ();
                for (final var connection : connections) {
                    if (!connection.canBeRemoved ()) { continue; }
                    
                    state.onConnectionLost (context, connection);
                }
                
                final var afterCleaning = connections.size ();
                if (beforeCleaning != afterCleaning) {
                    final var logins = getRoomPlayers ().stream ()
                            . map (Player::serialize)
                            . collect (Collectors.joining ("/"));
                    broadcastMessage (LOBBY_PLAYERS.getValue (), logins);
                }
                
                if (alive == 0 && timeSinceEmpty == -1) {
                    System.out.println ("Room #" + id + " is empty"); // SYSOUT
                    timeSinceEmpty = System.currentTimeMillis ();
                } else if (alive > 0 && timeSinceEmpty != -1) {
                    System.out.println ("Room #" + id + " is not empty"); // SYSOUT
                    timeSinceEmpty = -1;
                }
                
                isEmpty = alive == 0;
            }
        }
    }
    
    public void handleRoomMessage (ClientConnection connection, String ... parts) {
        if (IN_LOBBY.getValue ().equals (parts [1])) {
            synchronized (context) {
                final var size = String.valueOf (context.getExpeditionSize ());
                connection.sendMessage (EXPEDITION_SIZE.getValue (), size);  
                
                final var time = String.valueOf (context.getExpeditionTime ());
                connection.sendMessage (EXPEDITION_TIME.getValue (), time);
                
                synchronized (state) {                
                    connection.sendMessage (SERVER_STATE.getValue (), getState ().name ());
                }
                
                connection.sendMessage (ROOM_ID.getValue (), getIdh ());
            }
            
            broadcastMessage (CHAT_MESSAGE.getValue (), "$", connection.getLogin () + " joined the room");
            broadcastMessage (LOBBY_PLAYER.getValue (), parts [2], connection.getIdhh (), "false");
            handleRoomMessage (connection, new String [] {"", GET_LOBBY_PLAYERS.getValue ()});
        } else if (GET_LOBBY_PLAYERS.getValue ().equals (parts [1])) {
            final var logins = getRoomPlayers ().stream ().map (Player::serialize)
                . collect (Collectors.joining ("/"));
            connection.sendMessage (LOBBY_PLAYERS.getValue (), logins);
        } else if (LEAVE_LOBBY.getValue ().equals (parts [1])) {
            broadcastMessage (CHAT_MESSAGE.getValue (), "$", connection.getLogin () + " leaved the room");
            broadcastMessage (LEAVE_LOBBY.getValue (), connection.getIdhh ());
            connection.setAlive (false);            
        } else if (PLAYER_READY.getValue ().equals (parts [1])) {
            synchronized (context) {
                synchronized (state) {                    
                    if (state != ServerState.WAITING_FOR_PLAYERS && state != ServerState.RECRUITING) {
                        return; // wrong state of server for this command
                    }
                    
                    broadcastMessage (PLAYER_READY.getValue (), connection.getIdhh ());
                    context.getBufferOfConnections ().add (connection.getId ());
                    System.out.println ("BoC (pr): " + context.getBufferOfConnections ()); // SYSOUT
                    
                    if (state == WAITING_FOR_PLAYERS) {
                        //getGameContext ().applyMove (connection, 0, 0);
                    }
                }
            }
        } else if (PLAYER_NOT_READY.getValue ().equals (parts [1])) {
            synchronized (context) {
                synchronized (state) {                    
                    if (state != ServerState.RECRUITING) {
                        return; // wrong state of server for this command
                    }
                    
                    broadcastMessage (PLAYER_NOT_READY.getValue (), connection.getIdhh ());
                    context.getBufferOfConnections ().remove (connection.getId ());
                    System.out.println ("BoC (pnr): " + context.getBufferOfConnections ()); // SYSOUT
                }
            }
        } else if (PLAYER_MOVE.getValue ().equals (parts [1])) {
            final int dx = Integer.parseInt (parts [2]), dy = Integer.parseInt (parts [3]);
            if (state == GAME) {
                context.getGameContext ().applyMove (connection, dx, dy);
            }
        } else if (PLAYER_ACTION.getValue ().equals (parts [1])) {
            if (state == GAME) {
                context.getGameContext ().applyAction (connection, parts [2]);
            }
            //room.getGameContext ().applyAction (connection, parts [2]);
        } else if (PLAYER_MODE.getValue ().equals (parts [1])) {
            if (state == GAME) {
                context.getGameContext ().applyUserModeToggle (connection);
            }
            //room.getGameContext ().applyUserModeToggle (connection);
        } else if (PLAYER_FOUND_EXIT.getValue ().equals (parts [1])) {
            if (state == GAME && context.getGameContext ().exitFound (connection)) {
                
            }
        } else if (PLAYER_FINISHED_GAME.getValue ().equals (parts [1])) {
            synchronized (context) {
                synchronized (state) {                    
                    if (state != ServerState.FINISH) {
                        return; // wrong state of server for this command
                    }
                    
                    context.getBufferOfConnections ().add (connection.getId ());
                    System.out.println ("BoC (pfg): " + context.getBufferOfConnections ()); // SYSOUT
                }
            }
        } else if (EXPEDITION_SIZE.getValue ().equals (parts [1])) {
            synchronized (context) {                
                try {
                    final var size = Integer.parseInt (parts [2]);
                    if (size >= 2) {
                        context.setExpeditionSize (size);
                    }
                } catch (NumberFormatException nfe) {
                    // just ignore
                }
                
                broadcastMessage (EXPEDITION_SIZE.getValue (), String.valueOf (context.getExpeditionSize ()));
            }
        }  else if (EXPEDITION_TIME.getValue ().equals (parts [1])) {
            synchronized (context) {                
                try {
                    final var time = Integer.parseInt (parts [2]);
                    if (time >= 1) {
                        context.setExpeditionTime (time);
                    }
                } catch (NumberFormatException nfe) {
                    // just ignore
                }
                
                broadcastMessage (EXPEDITION_TIME.getValue (), String.valueOf (context.getExpeditionTime ()));
            }
        } else if (CHAT_MESSAGE.getValue ().equals (parts [1])) {
            broadcastMessage (CHAT_MESSAGE.getValue (), connection.getLogin (), parts [2]);
        }
    }
    
    public boolean addConnection (ClientConnection client) {
        if (!context.isNewConnectionsAllowed ()) {
            return false;
        }
        
        synchronized (context) {
            context.getRoomConnections ().add (client);
            client.setRoom (this);
        }
        
        return true;
    }
    
    public void broadcastMessage (String ... values) {
        synchronized (context) {            
            for (final var connection : context.getRoomConnections ()) {
                if (!connection.isAlive ()) { continue; }
                connection.sendMessage (values);
            }
        }
    }
    
    public List <Player> getRoomPlayers () {
        synchronized (context) {
            synchronized (state) {                
                final var ready = state == RECRUITING 
                    ? context.getBufferOfConnections () 
                    : (state == FINISH ? Set.of () 
                        : context.getRequiredConnections ());
                final var players = context.getRoomConnections ().stream ()
                    . filter (ClientConnection::isAlive).map (cc -> {
                        final var isReady = ready.contains (cc.getId ());
                        return new Player (cc.getLogin (), cc.getIdhh (), isReady);
                    })
                    . collect (Collectors.toList ());
                return players;
            }
        }
    }

    @Override
    public void close () throws IOException {
        synchronized (context) {            
            for (final var connection : context.getRoomConnections ()) {
                connection.close ();
            }
        }
    }
    
}
