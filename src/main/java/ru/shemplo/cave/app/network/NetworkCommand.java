package ru.shemplo.cave.app.network;

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
    
    ;
    
    private final String value;
    
}
