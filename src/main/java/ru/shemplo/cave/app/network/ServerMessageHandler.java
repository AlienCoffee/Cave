package ru.shemplo.cave.app.network;

import static ru.shemplo.cave.app.network.NetworkCommand.*;

import java.util.Objects;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ServerMessageHandler {
    
    private final ConnectionsPool pool;
    
    public void handle (String [] parts, ClientConnection connection) {
        if (GET_LOBBY_PLAYERS.getValue ().equals (parts [1])) {
            final var logins = pool.getConnections ().stream ()
                . filter (ClientConnection::isAlive)
                . map (ClientConnection::getLogin)
                . filter (Objects::nonNull).collect (Collectors.joining (","));
            connection.sendMessage (LOBBY_PLAYERS.getValue (), logins);
        } else if (PLAYER.getValue ().equals (parts [1])) {
            connection.setLogin (parts [2]);
            
            pool.getConnections ().forEach (c -> {
                if (!c.isAlive ()) { return; }
                c.sendMessage (PLAYER.getValue (), parts [2]);
            });
        } else if (LEAVE_LOBBY.getValue ().equals (parts [1])) {
            final var login = connection.getLogin ();
            connection.setAlive (false);
            
            pool.getConnections ().forEach (c -> {
                if (!c.isAlive ()) { return; }
                c.sendMessage (LEAVE_LOBBY.getValue (), login);
            });
        }
    }
    
}
