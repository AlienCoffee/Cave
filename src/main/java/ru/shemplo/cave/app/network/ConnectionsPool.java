package ru.shemplo.cave.app.network;

import static ru.shemplo.cave.app.network.NetworkCommand.*;

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
    private Thread listener;
    
    @Setter
    private boolean newConnectionsAllowed = true;
    
    public void applyConnection (SSLSocket socket) throws IOException {
        final var id = identifier.getAndIncrement ();
        
        final var connection = new ClientConnection (id, socket);
        connection.setOnReadMessage (messageComputer::handle);
        connection.setOnHandshakeFinished (() -> {
            if (newConnectionsAllowed) {                
                synchronized (connections) { connections.add (connection); }
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
    
    public void open () {
        listener = new Thread (() -> {
            while (!Thread.currentThread ().isInterrupted ()) {
                try { 
                    final var size = connections.size ();
                    final var time = Math.max (50, 3000 - size * 50);
                    Thread.sleep (time); 
                } catch (InterruptedException ie) {
                    return; // the work is over
                }
                
                synchronized (connections) {                    
                    for (final var connection : connections) {
                        if (connection.getNonTestedTime () > 5000) {                            
                            connection.sendMessage (PING.getValue ());
                            if (!connection.isAlive ()) {
                                try   { connection.close (); }
                                catch (IOException e) {}
                            }
                        }
                    }
                    
                    connections.removeIf (ClientConnection::canBeRemoved);
                    System.out.println ("Connections: " + connections.size ()); // SYSOUT
                }
            }
            
            Thread.currentThread ().interrupt ();
        }, "Connections-Listener-Thread");
        listener.setDaemon (true);
        listener.start ();
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
