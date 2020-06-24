package ru.shemplo.cave.app.server;

import static ru.shemplo.cave.app.server.NetworkCommand.*;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ServerMessageHandler {
    
    private final ConnectionsPool pool;
    
    public void handle (String [] parts, ClientConnection connection) {
        final var room = connection.getRoom ();
        
        if (room == null && JOIN_ROOM.getValue ().equals (parts [1])) {
            final var login = parts [2];
            if (login.contains (",") || login.contains ("/")) {
                connection.sendMessage (CONNECTION_REJECTED.getValue (), "Bad name: special characters , or /");
                connection.setAlive (false);
                return;
            }
            
            if (login.length () > 24) {
                connection.sendMessage (CONNECTION_REJECTED.getValue (), "Bad name: too long (24 limit)");
                connection.setAlive (false);
                return;
            }
            
            if (login.equals ("$")) {
                connection.sendMessage (CONNECTION_REJECTED.getValue (), "Bad name: reserved by server");
                connection.setAlive (false);
                return;
            }
            
            connection.setLogin (login);
            
            final var r = pool.getRoom (parts.length > 3 ? parts [3] : null);
            if (r == null) {
                connection.sendMessage (CONNECTION_REJECTED.getValue (), "Wrong room identifier");
                connection.setAlive (false);
            } else {
                pool.onPlayerJoinedTheRoom (connection, r);
            }
        }
        
        if (room == null) {
            // all next messages can be handled only in the room
            return;
        }
        
        room.handleRoomMessage (connection, parts);
    }
    
}
