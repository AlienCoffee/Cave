package ru.shemplo.cave.app.server;

import static ru.shemplo.cave.app.server.NetworkCommand.*;
import static ru.shemplo.cave.app.server.ServerState.*;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.shemplo.cave.app.entity.Player;

@ToString (of = {"id", "idh", "newConnectionsAllowed"})
public class ServerRoom implements Closeable {
    
    @Getter
    private final int id;
    @Getter
    private final String idh;
    
    @SuppressWarnings ("unused")
    private final ConnectionsPool pool;
    
    private final Thread controller;
    
    @Getter
    private boolean newConnectionsAllowed = true;
    
    public ServerRoom (int id, ConnectionsPool pool) {
        this.id = id; this.pool = pool;
        
        final var random = pool.getRandom ();
        //idh = Utils.digest (String.format ("room-%d", id), CaveServer.SERVER_SALT);
        final var letter = String.valueOf ((char) ('a' + random.nextInt ('z' - 'a')));
        idh = String.format ("%s%d", letter, id);
        
        controller = new Thread (() -> {
            while (!Thread.currentThread ().isInterrupted ()) {
                if (state == ServerState.RECRUITING) {
                    newConnectionsAllowed = true;
                    
                    synchronized (readyPlayers) {                        
                        if (readyPlayers.size () >= expeditionSize) {
                            synchronized (connections) {
                                newConnectionsAllowed = false;
                                
                                for (final var connection : connections) {
                                    if (!readyPlayers.contains (connection.getId ())) {
                                        connection.sendMessage (CONNECTION_CLOSED.getValue (), "Connection closed by server");
                                        connection.setAlive (false);
                                    }
                                }
                                
                                players = connections.stream ().filter (cc -> readyPlayers.contains (cc.getId ()))
                                        . collect (Collectors.toList ());
                            }
                            
                            System.out.println ("Enough players recruited"); // SYSOUT
                            broadcastMessage (SERVER_STATE.getValue (), PRE_SATRT.name ());
                            broadcastMessage (START_COUNTDOWN.getValue ());
                            System.out.println ("Ready players: " + readyPlayers); // SYSOUT
                            state = ServerState.PRE_SATRT;
                            
                            readyPlayers.clear ();
                            counter.set (5);
                            
                            final var generatorThread = new Thread (() -> {
                                context = new GameContext (players);
                                System.out.println ("Context is generated"); // SYSOUT
                            });
                            generatorThread.setDaemon (true);
                            generatorThread.start ();
                        }
                    }
                    
                    try { Thread.sleep (500); } catch (InterruptedException ie) { 
                        Thread.currentThread ().interrupt ();
                        return; 
                    }
                } else if (state == ServerState.PRE_SATRT) {
                    newConnectionsAllowed = false;
                    
                    broadcastMessage (COUNTDOWN.getValue (), String.valueOf (counter.get () - 1));
                    if (counter.decrementAndGet () <= 0) {
                        broadcastMessage (SERVER_STATE.getValue (), WAITIN_FOR_PLAYERS.name ());
                        System.out.println ("Pre-start countdown is over"); // SYSOUT
                        state = ServerState.WAITIN_FOR_PLAYERS;
                        counter.set (0);
                    }
                    
                    if (alive < players.size ()) {
                        setFinishState ("One of expeditors is lost");
                    }
                    
                    try { Thread.sleep (1000); } catch (InterruptedException ie) { 
                        Thread.currentThread ().interrupt ();
                        return; 
                    }
                } else if (state == ServerState.WAITIN_FOR_PLAYERS) {
                    broadcastMessage (COUNTDOWN.getValue (), String.valueOf (players.size () - counter.get ()));
                    if (counter.get () >= players.size ()) {
                        broadcastMessage (SERVER_STATE.getValue (), GAME.name ());
                        broadcastMessage (START_COUNTDOWN.getValue ());
                        System.out.println ("All players are ready"); // SYSOUT
                        counter.set (expeditionTime * 60); // game time
                        state = ServerState.GAME;
                    }
                    
                    if (alive < players.size ()) {
                        setFinishState ("One of expeditors is lost");
                    }
                    
                    try { Thread.sleep (1000); } catch (InterruptedException ie) { 
                        Thread.currentThread ().interrupt ();
                        return; 
                    }
                } else if (state == ServerState.GAME) {
                    broadcastMessage (COUNTDOWN.getValue (), String.valueOf (counter.get ()));
                    if (counter.decrementAndGet () <= 0) {
                        setFinishState ("Expedition time is over");
                        System.out.println ("Game time is over"); // SYSOUT
                        
                        broadcastMessage (START_COUNTDOWN.getValue ());
                    }
                    
                    if (alive < players.size ()) {
                        setFinishState ("One of expeditors is lost");
                    }
                    
                    try { Thread.sleep (1000); } catch (InterruptedException ie) { 
                        Thread.currentThread ().interrupt ();
                        return; 
                    }
                } else if (state == ServerState.FINISH) {
                    if (counter.get () >= players.size ()) {
                        broadcastMessage (SERVER_STATE.getValue (), RECRUITING.name ());
                        System.out.println ("Room #" + id + " is waiting for players"); // SYSOUT
                        state = ServerState.RECRUITING;
                        
                        expeditionTime = 10;
                        expeditionSize = 2;
                        counter.set (0);
                    }
                    
                    try { Thread.sleep (1000); } catch (InterruptedException ie) { 
                        Thread.currentThread ().interrupt ();
                        return; 
                    }
                }
            }
        }, String.format ("Room-%d-Controller-Thread", id));
        controller.setDaemon (true);
    }
    
    private final List <ClientConnection> connections = new ArrayList <> ();
    private final Set <Integer> readyPlayers = new HashSet <> ();
    
    public List <ClientConnection> getConnections () {
        synchronized (connections) {            
            return List.copyOf (connections);
        }
    }
    
    @Getter
    private ServerState state = ServerState.RECRUITING;
    
    private final AtomicInteger counter = new AtomicInteger ();
    private List <ClientConnection> players;
    @Getter private GameContext context;
    private volatile int alive = 0;
    
    @Setter @Getter
    private int expeditionTime = 10;
    
    @Setter @Getter
    private int expeditionSize = 2;
    
    public ServerRoom open () {
        System.out.println ("Room #" + id + " is openned"); // SYSOUT
        controller.start ();
        return this;
    }
    
    public void checkConnections () {
        synchronized (connections) {
            int alive = 0;
            for (final var connection : connections) {
                if (connection.getNonTestedTime () > 250) {                            
                    connection.sendMessage (PING.getValue ());
                }
                
                alive += connection.isAlive () ? 1 : 0;
            }
            
            final var beforeCleaning = connections.size ();
            connections.removeIf (cc -> {
                final var canRemove = cc.canBeRemoved ();
                if (canRemove) {
                    synchronized (readyPlayers) {
                        readyPlayers.remove (cc.getId ());
                        if (state == FINISH) {
                            counter.decrementAndGet ();
                            players.remove (cc);
                        }
                    }
                }
                
                return canRemove;
            });
            
            final var afterCleaning = connections.size ();
            if (beforeCleaning != afterCleaning) {
                final var logins = getRoomPlayers ().stream ()
                    . map (Player::serialize)
                    . collect (Collectors.joining ("/"));
                broadcastMessage (LOBBY_PLAYERS.getValue (), logins);
            }
            
            this.alive = alive;
            if (alive == 0 && timeSinceEmpty == -1) {
                System.out.println ("Room #" + id + " is empty"); // SYSOUT
                timeSinceEmpty = System.currentTimeMillis ();
            } else if (alive > 0 && timeSinceEmpty != -1) {
                System.out.println ("Room #" + id + " is not empty"); // SYSOUT
                timeSinceEmpty = -1;
            }
        }
    }
    
    public int getRoomPlayersNumber () {
        synchronized (connections) {
            return connections.size ();
        }
    }
    
    @Getter
    private volatile long timeSinceEmpty = -1;
    
    public boolean addConnection (ClientConnection client) {
        if (!newConnectionsAllowed) {
            return false;
        }
        
        synchronized (connections) {
            connections.add (client);
            client.setRoom (this);
        }
        
        return true;
    }
    
    public void deltaCounter (int d) {
        counter.addAndGet (d);
    }
    
    public void broadcastMessage (String ... values) {
        synchronized (connections) {            
            for (final var connection : connections) {
                if (!connection.isAlive ()) { continue; }
                connection.sendMessage (values);
            }
        }
    }
    
    public List <Player> getRoomPlayers () {
        synchronized (connections) {
            synchronized (readyPlayers) {                
                return connections.stream ().filter (ClientConnection::isAlive)
                     . map (cc -> {
                         final var ready = readyPlayers.contains (cc.getId ());
                         return new Player (cc.getLogin (), cc.getIdhh (), ready);
                     })
                     . collect (Collectors.toList ());
            }
        }
    }
    
    public void onPlayerReadyStateChanged (ClientConnection connection, boolean ready) {
        if (state != RECRUITING && state != WAITIN_FOR_PLAYERS) { 
            return; /* illegal state for this action */ 
        }
        
        synchronized (readyPlayers) {
            if (ready) {
                readyPlayers.add (connection.getId ());
            } else {
                readyPlayers.remove (connection.getId ());
            }
        }
    }
    
    public void onExitFound () {
        if (state != GAME) { return; /* illegal state for this action */ }
        setFinishState ("Exit from the cave is found");
    }
    
    private void setFinishState (String reason) {
        synchronized (readyPlayers) {
            readyPlayers.clear ();
            
            broadcastMessage (SERVER_STATE.getValue (), FINISH.name (), reason);
            context = null; state = FINISH;            
            counter.set (0);
        }
    }

    @Override
    public void close () throws IOException {
        for (final var connection : connections) {
            connection.close ();
        }
    }
    
}
