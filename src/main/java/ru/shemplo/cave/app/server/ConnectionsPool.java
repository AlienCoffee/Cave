package ru.shemplo.cave.app.server;

import static ru.shemplo.cave.app.server.NetworkCommand.*;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLSocket;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConnectionsPool implements Closeable {
    
    @Getter
    private final CaveServer server;
    
    private final ServerMessageHandler messageComputer = new ServerMessageHandler ();
    private final AtomicInteger identifier = new AtomicInteger (0);
    
    private final Map <String, ServerRoom> id2room = new ConcurrentHashMap <> ();
    
    @Getter
    private final List <ClientConnection> pendingConnections = new ArrayList <> ();
    private Thread listener;
    
    public void applyConnection (SSLSocket socket) throws IOException {
        final var id = identifier.getAndIncrement ();
        
        final var connection = new ClientConnection (id, socket);
        connection.setOnReadMessage (messageComputer::handle);
        connection.setOnHandshakeFinished (() -> {
            synchronized (pendingConnections) {
                pendingConnections.add (connection);
                /*
                if (newConnectionsAllowed) {
                    connection.setIdhh (Utils.digest (connection.getIdh (), "salt"));
                    connection.sendMessage (CONNECTION_ACCEPTED.getValue ());
                    pendingConnections.add (connection);
                } else {
                    connection.sendMessage (CONNECTION_REJECTED.getValue (), "New connection rejected by server");
                    try   { connection.close (); } 
                    catch (IOException ioe) {
                        ioe.printStackTrace ();
                    }
                }
                */
            }
        });
        
        connection.startHandshake ();
    }
    
    public void open () {
        final var emptyRoomTimeout = 1000 * 60 * 1; // 1 minute
        
        listener = new Thread (() -> {
            while (!Thread.currentThread ().isInterrupted ()) {
                try { Thread.sleep (250); } catch (InterruptedException ie) {
                    Thread.currentThread ().interrupt ();
                    return; // the work is over
                }
                
                for (final var room : id2room.values ()) {
                    room.checkConnections ();
                    
                    if (room.getRoomPlayersNumber () == 0) {
                        synchronized (room) {                            
                            final var empty = System.currentTimeMillis () - room.getTimeSinceEmpty ();
                            if (room.getRoomPlayersNumber () == 0 && empty > emptyRoomTimeout) {
                                System.out.println ("Closing empty room #" + room.getId ()); // SYSOUT
                                
                                try   { room.close (); } 
                                catch (IOException ioe) {}
                                
                                id2room.remove (room.getIdh ());
                            }
                        }
                    }
                }
            }
        }, "Connections-Listener-Thread");
        listener.setDaemon (true);
        listener.start ();
    }
    
    public void broadcastMessage (boolean toRoomsToo, String ... values) {
        synchronized (pendingConnections) {            
            for (final var connection : pendingConnections) {
                if (!connection.isAlive ()) { continue; }
                connection.sendMessage (values);
            }
        }
        
        if (toRoomsToo) {
            for (final var room : id2room.values ()) {
                room.broadcastMessage (values);
            }
        }
    }

    @Override
    public void close () throws IOException {
        listener.interrupt ();
        
        broadcastMessage (true, CONNECTION_CLOSED.getValue (), "Server is stopped");
        for (final var connection : pendingConnections) {
            connection.close ();
        }
        
        for (final var room : id2room.values ()) {
            room.close ();
        }
    }
    
}
