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

import javax.net.ssl.SSLSocket;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ru.shemplo.cave.utils.TimeoutActionExecutor;

@RequiredArgsConstructor
public class ConnectionsPool implements Closeable {
    
    @Getter
    private final CaveServer server;
    
    private final ServerMessageHandler messageComputer = new ServerMessageHandler (this);
    private final TimeoutActionExecutor texecutor = new TimeoutActionExecutor ();
    
    private final AtomicInteger identifier = new AtomicInteger (0);
    
    @Getter
    private final List <ClientConnection> connections = new ArrayList <> ();
    private Thread listener, controller;
    
    @Setter
    private boolean newConnectionsAllowed = true;
    
    public void applyConnection (SSLSocket socket) throws IOException {
        final var id = identifier.getAndIncrement ();
        
        final var connection = new ClientConnection (id, socket);
        connection.setOnReadMessage (messageComputer::handle);
        connection.setOnHandshakeFinished (() -> {
            if (newConnectionsAllowed) {
                connection.sendMessage (EXPEDITION_SIZE.getValue (), String.valueOf (expeditionSize));
                connection.sendMessage (EXPEDITION_TIME.getValue (), String.valueOf (expeditionTime));
                synchronized (connections) { 
                    connections.add (connection);
                }
            } else {
                connection.sendMessage (CONNECTION_REJECTED.getValue (), "New connection rejected by server");
                try   { connection.close (); } 
                catch (IOException ioe) {
                    ioe.printStackTrace ();
                }
            }
        });
        
        connection.startHandshake ();
    }
    
    @Getter
    private ServerState state = ServerState.RECRUITING;
    
    private final AtomicInteger counter = new AtomicInteger ();
    private volatile int alive = 0, players = 0;
    @Getter private GameContext context;
    
    @Setter @Getter
    private int expeditionTime = 10;
    
    @Setter @Getter
    private int expeditionSize = 2;
    
    private final Set <Integer> readyPlayers = new HashSet <> ();
    
    public void open () {
        listener = new Thread (() -> {
            while (!Thread.currentThread ().isInterrupted ()) {
                try { Thread.sleep (250); } catch (InterruptedException ie) {
                    Thread.currentThread ().interrupt ();
                    return; // the work is over
                }
                
                int alive = 0;
                synchronized (connections) {
                    for (final var connection : connections) {
                        if (connection.getNonTestedTime () > 250) {                            
                            connection.sendMessage (PING.getValue ());
                        }
                        
                        alive += connection.isAlive () ? 1 : 0;
                    }
                    
                    connections.removeIf (cc -> cc.canBeRemoved (this));
                }
                
                this.alive = alive;
            }
        }, "Connections-Listener-Thread");
        listener.setDaemon (true);
        listener.start ();
        
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
                            }
                            
                            System.out.println ("Enough players recruited"); // SYSOUT
                            broadcastMessage (SERVER_STATE.getValue (), PRE_SATRT.name ());
                            broadcastMessage (START_COUNTDOWN.getValue ());
                            players = readyPlayers.size ();
                            state = ServerState.PRE_SATRT;
                            
                            readyPlayers.clear ();
                            counter.set (5);
                            
                            final var generatorThread = new Thread (() -> {
                                context = new GameContext (this, connections);
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
                    
                    if (alive < players) {
                        broadcastMessage (SERVER_STATE.getValue (), FINISH.name (), "One of expeditors is lost");
                        counter.set (5);
                        state = FINISH;
                    }
                    
                    try { Thread.sleep (1000); } catch (InterruptedException ie) { 
                        Thread.currentThread ().interrupt ();
                        return; 
                    }
                } else if (state == ServerState.WAITIN_FOR_PLAYERS) {
                    broadcastMessage (COUNTDOWN.getValue (), String.valueOf (players - counter.get ()));
                    if (counter.get () >= players) {
                        broadcastMessage (SERVER_STATE.getValue (), GAME.name ());
                        broadcastMessage (START_COUNTDOWN.getValue ());
                        System.out.println ("All players are ready"); // SYSOUT
                        counter.set (expeditionTime * 60); // game time
                        state = ServerState.GAME;
                    }
                    
                    if (alive < players) {
                        broadcastMessage (SERVER_STATE.getValue (), FINISH.name (), "One of expeditors is lost");
                        counter.set (5);
                        state = FINISH;
                    }
                    
                    try { Thread.sleep (1000); } catch (InterruptedException ie) { 
                        Thread.currentThread ().interrupt ();
                        return; 
                    }
                } else if (state == ServerState.GAME) {
                    broadcastMessage (COUNTDOWN.getValue (), String.valueOf (counter.get ()));
                    if (counter.decrementAndGet () <= 0) {
                        broadcastMessage (SERVER_STATE.getValue (), FINISH.name (), "Expedition time is over");
                        broadcastMessage (START_COUNTDOWN.getValue ());
                        System.out.println ("Game time is over"); // SYSOUT
                        state = ServerState.FINISH;
                        counter.set (5);
                    }
                    
                    if (alive < players) {
                        broadcastMessage (SERVER_STATE.getValue (), FINISH.name (), "One of expeditors is lost");
                        counter.set (5);
                        state = FINISH;
                    }
                    
                    try { Thread.sleep (1000); } catch (InterruptedException ie) { 
                        Thread.currentThread ().interrupt ();
                        return; 
                    }
                } else if (state == ServerState.FINISH) {
                    broadcastMessage (COUNTDOWN.getValue (), String.valueOf (counter.get ()));
                    if (counter.decrementAndGet () <= 0) {
                        synchronized (connections) {
                            for (final var connection : connections) {
                                connection.setAlive (false);
                            }
                        }
                        
                        broadcastMessage (SERVER_STATE.getValue (), RECRUITING.name ());
                        System.out.println ("Server is waiting for players"); // SYSOUT
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
        }, "Server-Controller-Thread");
        controller.setDaemon (true);
        controller.start ();
    }
    
    public void broadcastMessage (String ... values) {
        for (final var connection : connections) {
            if (!connection.isAlive ()) { continue; }
            connection.sendMessage (values);
        }
    }
    
    public void deltaCounter (int d) {
        counter.addAndGet (d);
    }
    
    public void onExitFound () {
        if (state != GAME) { return; /* illegal state for this action */ }
        
        broadcastMessage (SERVER_STATE.getValue (), FINISH.name (), "Exit from the cave is found");
        counter.set (5);
        state = FINISH;
    }
    
    public void onPlayerReadyStateChanged (ClientConnection connection, boolean ready) {
        synchronized (readyPlayers) {
            if (ready) {
                readyPlayers.add (connection.getId ());
            } else {
                readyPlayers.remove (connection.getId ());
            }
        }
    }

    @Override
    public void close () throws IOException {
        listener.interrupt ();
        texecutor.close ();
        
        for (final var connection : connections) {
            connection.close ();
        }
    }
    
}
