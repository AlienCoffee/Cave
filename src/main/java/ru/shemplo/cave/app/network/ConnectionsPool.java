package ru.shemplo.cave.app.network;

import static ru.shemplo.cave.app.network.NetworkCommand.*;
import static ru.shemplo.cave.app.network.ServerState.*;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    
    private final AtomicInteger countdown = new AtomicInteger ();
    private volatile int alive = 0, players = 0;
    
    public void open () {
        listener = new Thread (() -> {
            while (!Thread.currentThread ().isInterrupted ()) {
                try { Thread.sleep (1000); } catch (InterruptedException ie) {
                    Thread.currentThread ().interrupt ();
                    return; // the work is over
                }
                
                alive = 0;
                synchronized (connections) {
                    for (final var connection : connections) {
                        if (connection.getNonTestedTime () > 1000) {                            
                            connection.sendMessage (PING.getValue ());
                        }
                        
                        alive += connection.isAlive () ? 1 : 0;
                    }
                    
                    connections.removeIf (cc -> cc.canBeRemoved (this));
                }
            }
        }, "Connections-Listener-Thread");
        listener.setDaemon (true);
        listener.start ();
        
        controller = new Thread (() -> {
            while (!Thread.currentThread ().isInterrupted ()) {
                if (state == ServerState.RECRUITING) {
                    newConnectionsAllowed = true;
                    
                    if (countdown.get () >= 2) {
                        System.out.println ("Enough players recruited"); // SYSOUT
                        broadcastMessage (SERVER_STATE.getValue (), PRE_SATRT.name ());
                        broadcastMessage (START_COUNTDOWN.getValue ());
                        state = ServerState.PRE_SATRT;
                        players = countdown.get ();
                        countdown.set (3);
                    }
                    
                    try { Thread.sleep (500); } catch (InterruptedException ie) { 
                        Thread.currentThread ().interrupt ();
                        return; 
                    }
                } else if (state == ServerState.PRE_SATRT) {
                    newConnectionsAllowed = false;
                    
                    broadcastMessage (COUNTDOWN.getValue (), String.valueOf (countdown.get ()));
                    if (countdown.decrementAndGet () <= 0) {
                        broadcastMessage (SERVER_STATE.getValue (), WAITIN_FOR_PLAYERS.name ());
                        System.out.println ("Pre-start countdown is over"); // SYSOUT
                        state = ServerState.WAITIN_FOR_PLAYERS;
                        countdown.set (0);
                    }
                    
                    if (alive < players) {
                        broadcastMessage (SERVER_STATE.getValue (), RECRUITING.name ());
                        state = RECRUITING;
                        countdown.set (0);
                    }
                    
                    try { Thread.sleep (1000); } catch (InterruptedException ie) { 
                        Thread.currentThread ().interrupt ();
                        return; 
                    }
                } else if (state == ServerState.WAITIN_FOR_PLAYERS) {
                    broadcastMessage (COUNTDOWN.getValue (), String.valueOf (countdown.get ()));
                    if (countdown.get () >= players) {
                        broadcastMessage (SERVER_STATE.getValue (), GAME.name ());
                        broadcastMessage (START_COUNTDOWN.getValue ());
                        System.out.println ("All players are ready"); // SYSOUT
                        countdown.set (1000); // game time
                        state = ServerState.GAME;
                    }
                    
                    if (alive < players) {
                        broadcastMessage (SERVER_STATE.getValue (), RECRUITING.name ());
                        state = RECRUITING;
                        countdown.set (0);
                    }
                    
                    try { Thread.sleep (1000); } catch (InterruptedException ie) { 
                        Thread.currentThread ().interrupt ();
                        return; 
                    }
                } else if (state == ServerState.GAME) {
                    broadcastMessage (COUNTDOWN.getValue (), String.valueOf (countdown.get ()));
                    if (countdown.decrementAndGet () <= 0) {
                        broadcastMessage (SERVER_STATE.getValue (), FINISH.name ());
                        broadcastMessage (START_COUNTDOWN.getValue ());
                        System.out.println ("Game time is over"); // SYSOUT
                        state = ServerState.FINISH;
                        countdown.set (5);
                    }
                    
                    if (alive < players) {
                        broadcastMessage (SERVER_STATE.getValue (), RECRUITING.name ());
                        state = RECRUITING;
                        countdown.set (0);
                    }
                    
                    try { Thread.sleep (1000); } catch (InterruptedException ie) { 
                        Thread.currentThread ().interrupt ();
                        return; 
                    }
                } else if (state == ServerState.FINISH) {
                    broadcastMessage (COUNTDOWN.getValue (), String.valueOf (countdown.get ()));
                    if (countdown.decrementAndGet () <= 0) {
                        broadcastMessage (SERVER_STATE.getValue (), RECRUITING.name ());
                        System.out.println ("Server is waiting for players"); // SYSOUT
                        state = ServerState.RECRUITING;
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
    
    public void deltaCountdown (int d) {
        countdown.addAndGet (d);
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
