package ru.shemplo.cave.app.server.room.state;

import lombok.Getter;
import ru.shemplo.cave.app.server.ClientConnection;
import ru.shemplo.cave.app.server.room.ServerRoomContext;
import ru.shemplo.snowball.stuctures.Pair;


public enum ServerState {
    
    RECRUITING, 
    PRE_START,
    WAITING_FOR_PLAYERS,
    GAME,
    FINISH
    
    ;
    
    @Getter
    private ServerStateLogic logic;
    
    public Pair <ServerState, String> update (ServerRoomContext context, Long time) {
        return logic.apply (context, time);
    }
    
    public void onConnectionLost (ServerRoomContext context, ClientConnection connection) {
        logic.onConnectionLost (context, connection);
    }
    
    public void initialize (ServerRoomContext context) {
        logic.initialize (context);
    }
    
    public static void initialize () {
        RECRUITING.logic = ServerStateLogic.makeRecruitingLogic ();
        PRE_START.logic = ServerStateLogic.makePreStartLogic ();
        WAITING_FOR_PLAYERS.logic = ServerStateLogic.makeWaitingForPlayersLogic ();
        GAME.logic = ServerStateLogic.makeGameLogic ();
        FINISH.logic = ServerStateLogic.makeFinishLogic ();
    }
    
}