package ru.shemplo.cave.app.server;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NetworkCommand {
    
    IDENTIFIER ("identifier"), PING ("ping"),
    CONNECTION_REJECTED ("connection rejected"),
    
    PLAYER ("player"),
    
    GET_LOBBY_PLAYERS ("get lobby players"),
    LOBBY_PLAYERS ("lobby players"),
    LEAVE_LOBBY ("leave lobby"),
    
    START_COUNTDOWN ("start countdown"),
    COUNTDOWN ("countdown"),
    
    PLAYER_READY ("player ready"),
    
    SERVER_STATE ("server state"),
    
    PLAYER_MOVE ("player move"),
    PLAYER_LOCATION ("player location"),
    PLAYERS_LOCATION ("players location")
    
    ;
    
    private final String value;
    
}
