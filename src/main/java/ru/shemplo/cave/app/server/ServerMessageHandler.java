package ru.shemplo.cave.app.server;

import static ru.shemplo.cave.app.server.NetworkCommand.*;

import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import ru.shemplo.cave.app.entity.LobbyPlayer;

@RequiredArgsConstructor
public class ServerMessageHandler {
    
    private final ConnectionsPool pool;
    
    public void handle (String [] parts, ClientConnection connection) {        
        if (IN_LOBBY.getValue ().equals (parts [1])) {
            connection.setLogin (parts [2]);
            
            connection.sendMessage (EXPEDITION_SIZE.getValue (), String.valueOf (pool.getExpeditionSize ()));
            connection.sendMessage (EXPEDITION_TIME.getValue (), String.valueOf (pool.getExpeditionTime ()));
            
            pool.broadcastMessage (LOBBY_PLAYER.getValue (), parts [2], connection.getIdhh (), "false");
            handle (new String [] {"", GET_LOBBY_PLAYERS.getValue ()}, connection);
        } else if (GET_LOBBY_PLAYERS.getValue ().equals (parts [1])) {
            final var logins = pool.getLobbyPlayers ().stream ()
                . map (LobbyPlayer::serialize)
                . collect (Collectors.joining ("/"));
            connection.sendMessage (LOBBY_PLAYERS.getValue (), logins);
        } else if (LEAVE_LOBBY.getValue ().equals (parts [1])) {
            pool.broadcastMessage (LEAVE_LOBBY.getValue (), connection.getIdhh ());
            connection.setAlive (false);            
        } else if (PLAYER_READY.getValue ().equals (parts [1])) {
            if (pool.getState () != ServerState.WAITIN_FOR_PLAYERS
                    && pool.getState () != ServerState.RECRUITING) {
                return; // wrong state of server for this command
            }
            
            pool.broadcastMessage (PLAYER_READY.getValue (), connection.getIdhh ());
            if (pool.getState () == ServerState.WAITIN_FOR_PLAYERS) {
                pool.getContext ().applyMove (connection, 0, 0);
                pool.deltaCounter (1);
            } else if (pool.getState () == ServerState.RECRUITING) {
                pool.onPlayerReadyStateChanged (connection, true);
            }
        } else if (PLAYER_NOT_READY.getValue ().equals (parts [1])) {
            if (pool.getState () != ServerState.RECRUITING) {
                return; // wrong state of server for this command
            }
            
            pool.broadcastMessage (PLAYER_NOT_READY.getValue (), connection.getIdhh ());
            if (pool.getState () == ServerState.RECRUITING) {
                pool.onPlayerReadyStateChanged (connection, false);
            }
        } else if (PLAYER_MOVE.getValue ().equals (parts [1])) {
            final int dx = Integer.parseInt (parts [2]), dy = Integer.parseInt (parts [3]);
            pool.getContext ().applyMove (connection, dx, dy);
        } else if (PLAYER_ACTION.getValue ().equals (parts [1])) {
            pool.getContext ().applyAction (connection, parts [2]);
        } else if (PLAYER_MODE.getValue ().equals (parts [1])) {
            pool.getContext ().applyUserModeToggle (connection);
        } else if (PLAYER_FOUND_EXIT.getValue ().equals (parts [1])) {
            if (pool.getContext ().exitFound (connection)) {
                pool.onExitFound ();
            }
        } else if (EXPEDITION_SIZE.getValue ().equals (parts [1])) {
            try {
                final var size = Integer.parseInt (parts [2]);
                if (size >= 2) {
                    pool.setExpeditionSize (size);
                }
            } catch (NumberFormatException nfe) {
                // just ignore
            }
            
            pool.getConnections ().forEach (c -> {
                if (!c.isAlive ()) { return; }
                c.sendMessage (EXPEDITION_SIZE.getValue (), String.valueOf (pool.getExpeditionSize ()));
            });
        }  else if (EXPEDITION_TIME.getValue ().equals (parts [1])) {
            try {
                final var time = Integer.parseInt (parts [2]);
                if (time >= 1) {
                    pool.setExpeditionTime (time);
                }
            } catch (NumberFormatException nfe) {
                // just ignore
            }
            
            pool.getConnections ().forEach (c -> {
                if (!c.isAlive ()) { return; }
                c.sendMessage (EXPEDITION_TIME.getValue (), String.valueOf (pool.getExpeditionTime ()));
            });
        } else if (CHAT_MESSAGE.getValue ().equals (parts [1])) {
            pool.broadcastMessage (CHAT_MESSAGE.getValue (), connection.getLogin (), parts [2]);
        }
    }
    
}
