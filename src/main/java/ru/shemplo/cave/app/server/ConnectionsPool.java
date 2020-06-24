package ru.shemplo.cave.app.server;

import static ru.shemplo.cave.app.server.NetworkCommand.*;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLSocket;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.shemplo.cave.app.server.room.ServerRoom;
import ru.shemplo.cave.utils.Utils;

@RequiredArgsConstructor
public class ConnectionsPool implements Closeable {
    
    @Getter
    private final CaveServer server;
    
    @Getter
    final Random random = new Random ();
    
    private final ServerMessageHandler messageComputer = new ServerMessageHandler (this);
    private final AtomicInteger connectionIdentifier = new AtomicInteger (0);
    private final AtomicInteger roomIdentifier = new AtomicInteger (0);
    
    private final Map <String, ServerRoom> id2room = new ConcurrentHashMap <> ();
    
    @Getter
    private final List <ClientConnection> pendingConnections = new ArrayList <> ();
    private Thread listener;
    
    public void applyConnection (SSLSocket socket) throws IOException {
        final var id = connectionIdentifier.getAndAdd (CaveServer.SERVER_ID_STEP);
        
        final var connection = new ClientConnection (id, socket);
        connection.setOnReadMessage (messageComputer::handle);
        connection.setOnHandshakeFinished (() -> {
            synchronized (pendingConnections) {
                pendingConnections.add (connection);
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
                
                synchronized (pendingConnections) {
                    for (final var connection : pendingConnections) {                        
                        if (connection.getNonTestedTime () > 250) {                            
                            connection.sendMessage (PING.getValue ());
                        }
                    }
                    
                    pendingConnections.removeIf (ClientConnection::canBeRemoved);
                }
                
                for (final var room : id2room.values ()) {
                    room.checkConnections ();
                    
                    if (room.isEmpty ()) {
                        synchronized (room) {                            
                            final var empty = System.currentTimeMillis () - room.getTimeSinceEmpty ();
                            if (room.isEmpty () && empty > emptyRoomTimeout) {
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
    
    public ServerRoom getRoom (String identifier) {
        if (identifier == null) {
            final var id = roomIdentifier.getAndIncrement ();
            @SuppressWarnings ("resource")
            final var room = new ServerRoom (id, this).open ();
            System.out.println ("New room #" + id + " (" + room.getIdh () + ") created"); // SYSOUT
            id2room.put (room.getIdh (), room);
            return room;
        } else {
            return id2room.get (identifier);
        }
    }
    
    public void onPlayerJoinedTheRoom (ClientConnection connection, ServerRoom room) {
        synchronized (pendingConnections) {
            if (room.addConnection (connection)) {   
                connection.setIdhh (Utils.digest (connection.getIdh (), "salt"));
                connection.sendMessage (CONNECTION_ACCEPTED.getValue ());
                pendingConnections.remove (connection);
            } else {
                connection.sendMessage (CONNECTION_REJECTED.getValue (), 
                        "Room is closed for new connections");
                connection.setAlive (false);
            }
        }
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
