package ru.shemplo.cave.app.server;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NetworkCommand {
    
    IDENTIFIER ("identifier"), PING ("ping"),
    CONNECTION_REJECTED ("connection rejected"),
    CONNECTION_CLOSED ("connection closed"),
    CONNECTION_ACCEPTED ("connection accepted"),
    
    JOIN_ROOM ("join room"),
    LOBBY_PLAYER ("lobby player"),
    IN_LOBBY ("lobby players"),
    GET_LOBBY_PLAYERS ("get lobby players"),
    LOBBY_PLAYERS ("lobby players"),
    EXPEDITION_SIZE ("expedition size"),
    EXPEDITION_TIME ("expedition time"),
    ROOM_ID ("room id"),
    LEAVE_LOBBY ("leave lobby"),
    CHAT_MESSAGE ("chat message"),
    
    START_COUNTDOWN ("start countdown"),
    COUNTDOWN ("countdown"),
    
    PLAYER_READY ("player ready"),
    PLAYER_NOT_READY ("player not ready"),
    
    SERVER_STATE ("server state"),
    
    PLAYER_MOVE ("player move"),
    PLAYER_ACTION ("player action"),
    PLAYER_MODE ("player mode"),
    PLAYER_LOCATION ("player location"),
    PLAYERS_LOCATION ("players location"),
    PLAYER_FOUND_EXIT ("player found exit"),
    PLAYER_FINISHED_GAME ("player finished game"),
    
    ;
    
    private final String value;
    
}
