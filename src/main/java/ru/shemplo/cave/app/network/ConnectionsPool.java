package ru.shemplo.cave.app.network;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLSocket;

import lombok.Setter;

public class ConnectionsPool implements Closeable {
    
    private final AtomicInteger identifier = new AtomicInteger (0);
    
    private final List <ClientConnection> connections = new ArrayList <> ();
    private Thread listener;
    
    @Setter
    private boolean newConnectionsAllowed = true;
    
    public void applyConnection (SSLSocket socket) throws IOException {
        final var id = identifier.getAndIncrement ();
        
        final var connection = new ClientConnection (id, socket);
        connection.setOnHandshakeFinished (() -> {
            if (newConnectionsAllowed) {                
                synchronized (connections) {                
                    connections.add (connection);
                }
            } else {
                connection.sendMessage ("connection rejected;New connection rejected by server");
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
            while (true) {
                try   { Thread.sleep (50); } 
                catch (InterruptedException e) {
                    return;
                }
                
                synchronized (connections) {                    
                    for (final var connection : connections) {
                        connection.sendMessage ("ping;");
                        connection.readMessage ().ifPresent (message -> {
                            System.out.println (message); // SYSOUT
                        });
                    }
                
                    connections.removeIf (cc -> !cc.isAlive ());
                }
            }
        }, "Connections-Listener-Thread");
        listener.setDaemon (true);
        listener.start ();
    }

    @Override
    public void close () throws IOException {
        listener.interrupt ();
        
        for (final var connection : connections) {
            connection.close ();
        }
    }
    
}
