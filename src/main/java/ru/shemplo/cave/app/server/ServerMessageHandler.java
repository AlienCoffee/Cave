package ru.shemplo.cave.app.server;

import static ru.shemplo.cave.app.server.NetworkCommand.*;

import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import ru.shemplo.cave.app.entity.Player;

@RequiredArgsConstructor
public class ServerMessageHandler {
    
    private final ConnectionsPool pool;
    
    public void handle (String [] parts, ClientConnection connection) {
        final var room = connection.getRoom ();
        
        if (room == null && JOIN_ROOM.getValue ().equals (parts [1])) {
            final var login = parts [2];
            if (login.contains (",") || login.contains ("/")) {
                connection.sendMessage (CONNECTION_REJECTED.getValue (), "Bad name: special characters , and /");
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
            return;
        }
        
        if (IN_LOBBY.getValue ().equals (parts [1])) {
            connection.sendMessage (EXPEDITION_SIZE.getValue (), String.valueOf (room.getExpeditionSize ()));
            connection.sendMessage (EXPEDITION_TIME.getValue (), String.valueOf (room.getExpeditionTime ()));
            connection.sendMessage (SERVER_STATE.getValue (), room.getState ().name ());
            connection.sendMessage (ROOM_ID.getValue (), room.getIdh ());
            
            room.broadcastMessage (LOBBY_PLAYER.getValue (), parts [2], connection.getIdhh (), "false");
            handle (new String [] {"", GET_LOBBY_PLAYERS.getValue ()}, connection);
        } else if (GET_LOBBY_PLAYERS.getValue ().equals (parts [1])) {
            final var logins = room.getRoomPlayers ().stream ()
                . map (Player::serialize)
                . collect (Collectors.joining ("/"));
            connection.sendMessage (LOBBY_PLAYERS.getValue (), logins);
        } else if (LEAVE_LOBBY.getValue ().equals (parts [1])) {
            room.broadcastMessage (LEAVE_LOBBY.getValue (), connection.getIdhh ());
            connection.setAlive (false);            
        } else if (PLAYER_READY.getValue ().equals (parts [1])) {
            if (room.getState () != ServerState.WAITIN_FOR_PLAYERS
                    && room.getState () != ServerState.RECRUITING) {
                return; // wrong state of server for this command
            }
            
            room.broadcastMessage (PLAYER_READY.getValue (), connection.getIdhh ());
            if (room.getState () == ServerState.WAITIN_FOR_PLAYERS) {
                room.getContext ().applyMove (connection, 0, 0);
                room.deltaCounter (1);
            } else if (room.getState () == ServerState.RECRUITING) {
                room.onPlayerReadyStateChanged (connection, true);
            }
        } else if (PLAYER_NOT_READY.getValue ().equals (parts [1])) {
            if (room.getState () != ServerState.RECRUITING) {
                return; // wrong state of server for this command
            }
            
            room.broadcastMessage (PLAYER_NOT_READY.getValue (), connection.getIdhh ());
            if (room.getState () == ServerState.RECRUITING) {
                room.onPlayerReadyStateChanged (connection, false);
            }
        } else if (PLAYER_MOVE.getValue ().equals (parts [1])) {
            final int dx = Integer.parseInt (parts [2]), dy = Integer.parseInt (parts [3]);
            room.getContext ().applyMove (connection, dx, dy);
        } else if (PLAYER_ACTION.getValue ().equals (parts [1])) {
            room.getContext ().applyAction (connection, parts [2]);
        } else if (PLAYER_MODE.getValue ().equals (parts [1])) {
            room.getContext ().applyUserModeToggle (connection);
        } else if (PLAYER_FOUND_EXIT.getValue ().equals (parts [1])) {
            if (room.getContext ().exitFound (connection)) {
                room.onExitFound ();
            }
        } else if (PLAYER_FINISHED_GAME.getValue ().equals (parts [1])) {
            room.deltaCounter (1);
        } else if (EXPEDITION_SIZE.getValue ().equals (parts [1])) {
            try {
                final var size = Integer.parseInt (parts [2]);
                if (size >= 2) {
                    room.setExpeditionSize (size);
                }
            } catch (NumberFormatException nfe) {
                // just ignore
            }
            
            room.broadcastMessage (EXPEDITION_SIZE.getValue (), String.valueOf (room.getExpeditionSize ()));
        }  else if (EXPEDITION_TIME.getValue ().equals (parts [1])) {
            try {
                final var time = Integer.parseInt (parts [2]);
                if (time >= 1) {
                    room.setExpeditionTime (time);
                }
            } catch (NumberFormatException nfe) {
                // just ignore
            }
            
            room.broadcastMessage (EXPEDITION_TIME.getValue (), String.valueOf (room.getExpeditionTime ()));
        } else if (CHAT_MESSAGE.getValue ().equals (parts [1])) {
            room.broadcastMessage (CHAT_MESSAGE.getValue (), connection.getLogin (), parts [2]);
        }
    }
    
}
