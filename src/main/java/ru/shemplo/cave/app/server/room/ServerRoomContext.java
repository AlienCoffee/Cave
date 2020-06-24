package ru.shemplo.cave.app.server.room;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ru.shemplo.cave.app.server.ClientConnection;
import ru.shemplo.cave.app.server.GameContext;

// this object is available only for ServerState enumeration
@Getter @Setter
@RequiredArgsConstructor
public class ServerRoomContext {
    
    public static final long PRE_START_TIME = 5_000; // 5 seconds
    
    private final ServerRoom room;
    
    private final List <ClientConnection> roomConnections = new ArrayList <> ();
    
    private final Set <Integer> requiredConnections = new HashSet <> (),
                                bufferOfConnections = new HashSet <> ();
    
    private int expeditionTime = 10;
    
    private int expeditionSize = 2;
    
    private boolean newConnectionsAllowed = true;
    
    private GameContext gameContext;
    
    // this field only for for fast communication in WAILTING FOR PLAYERS state
    private int restConnections;
    
    public long getExpeditionTimeInMilliseconds () {
        return expeditionTime * 60 * 1000;
    }
    
}
