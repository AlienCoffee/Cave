package ru.shemplo.cave.app.server.room.state;

import static ru.shemplo.cave.app.server.NetworkCommand.*;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import ru.shemplo.cave.app.server.ClientConnection;
import ru.shemplo.cave.app.server.GameContext;
import ru.shemplo.cave.app.server.room.ServerRoomContext;
import ru.shemplo.snowball.stuctures.Pair;

@RequiredArgsConstructor
public class ServerStateLogic implements BiFunction <ServerRoomContext, Long, Pair <ServerState, String>> {
    
    private final BiConsumer <ServerRoomContext, Long> onEnable;
    
    private final BiConsumer <ServerRoomContext, ClientConnection> onConnectionLost;
    
    private final List <ServerStateLogicAction> transitions;
    
    @Override
    public Pair <ServerState, String> apply (ServerRoomContext context, Long time) {
        for (final var transition : transitions) {
            if (transition.getPredicate ().test (context, time)) {
                Optional.ofNullable (transition.getOnPredicate ()).ifPresent (consumer -> {
                    consumer.accept (context, time);
                });
                Optional.ofNullable (transition.getTransition ().getLogic ().onEnable)
                    .ifPresent (consumer -> consumer.accept (context, time));
                
                return Pair.mp (transition.getTransition (), transition.getReason ());
            } else {
                Optional.ofNullable (transition.getOnNonPredicate ()).ifPresent (consumer -> {
                    consumer.accept (context, time);
                });
            }
        }
        
        return null;
    }
    
    public void onConnectionLost (ServerRoomContext context, ClientConnection connection) {
        Optional.ofNullable (onConnectionLost).ifPresent (consumer -> consumer.accept (context, connection));
    }
    
    public void initialize (ServerRoomContext context) {
        Optional.ofNullable (onEnable).ifPresent (consumer -> consumer.accept (context, 0L));
    }
    
    public static ServerStateLogic makeRecruitingLogic () {
        return new ServerStateLogic ((ctx, time) -> {
            ctx.getRequiredConnections ().clear ();
            ctx.getBufferOfConnections ().clear ();
            ctx.setNewConnectionsAllowed (true);
        }, (ctx, cc) -> {
            ctx.getRoomConnections ().remove (cc);
        }, List.of (
            new ServerStateLogicAction (
                (ctx, time) -> ctx.getBufferOfConnections ().size () >= ctx.getExpeditionSize (), 
                ServerState.PRE_START, "Enough players recruited", 
                (ctx, time) -> { // drop connections that are not ready
                    final var rconnections = ctx.getRequiredConnections ();
                    final var buffer = ctx.getBufferOfConnections ();
                    rconnections.addAll (buffer);
                    buffer.clear ();
                    
                    ctx.getRoomConnections ().stream ()
                    . filter  (cc -> !rconnections.contains (cc.getId ()))
                    . forEach (cc -> {
                        cc.sendMessage (CONNECTION_CLOSED.getValue (), "Connection closed by server");
                        cc.setAlive (false);
                    });
                }, 
                null
            )
        ));
    }
    
    public static ServerStateLogic makePreStartLogic () {
        return new ServerStateLogic ((ctx, time) -> {
            ctx.setNewConnectionsAllowed (false);
            
            final var players = ctx.getRoomConnections ().stream ()
                . filter (cc -> ctx.getRequiredConnections ().contains (cc.getId ()))
                . collect (Collectors.toList ());
            
            final var gameCreator = new Thread (() -> {
                ctx.setGameContext (new GameContext (players));
            });
            gameCreator.setDaemon (true);
            gameCreator.start ();
        }, (ctx, cc) -> {
            ctx.getRoomConnections ().remove (cc);
        }, List.of (
            ServerStateLogicAction.AT_LEAST_ONE_LOST,
            
            new ServerStateLogicAction (
                (ctx, time) -> time >= ServerRoomContext.PRE_START_TIME && ctx.getGameContext () != null, 
                ServerState.WAITING_FOR_PLAYERS, "Pre-start time is over",
                null,
                (ctx, time) -> {
                    final var delta = Math.max (0, ServerRoomContext.PRE_START_TIME - time);
                    final var rest = String.valueOf (Math.round (delta / 1000.0));
                    
                    ctx.getRoom ().broadcastMessage (COUNTDOWN.getValue (), rest);
                }
            )
        ));
    }
    
    public static ServerStateLogic makeWaitingForPlayersLogic () {
        return new ServerStateLogic ((ctx, time) -> {
            ctx.getBufferOfConnections ().clear ();
            ctx.setNewConnectionsAllowed (false);
        }, (ctx, cc) -> {
            ctx.getRoomConnections ().remove (cc);
        }, List.of (
            ServerStateLogicAction.AT_LEAST_ONE_LOST,
            
            new ServerStateLogicAction (
                (ctx, time) -> {
                    final var rconnections = ctx.getRequiredConnections ();
                    final var buffer = ctx.getBufferOfConnections ();
                    
                    int intersection = 0;
                    for (final var id : buffer) {
                        intersection += rconnections.contains (id) ? 1 : 0;
                    }
                    
                    ctx.setRestConnections (rconnections.size () - intersection);
                    return intersection == rconnections.size ();
                }, 
                ServerState.GAME, "All players are ready", 
                null, 
                (ctx, time) -> {
                    final var rest = String.valueOf (ctx.getRestConnections ());
                    ctx.getRoom ().broadcastMessage (COUNTDOWN.getValue (), rest);
                }
            )
        ));
    }
    
    public static ServerStateLogic makeGameLogic () {
        return new ServerStateLogic ((ctx, time) -> {
            ctx.setNewConnectionsAllowed (false);
            
            for (final var conneciton : ctx.getRoomConnections ()) {
                ctx.getGameContext ().applyMove (conneciton, 0, 0);
            }
        }, (ctx, cc) -> {
            ctx.getRoomConnections ().remove (cc);
        }, List.of (
            ServerStateLogicAction.AT_LEAST_ONE_LOST,
            
            new ServerStateLogicAction (
                (ctx, time) -> time >= ctx.getExpeditionTimeInMilliseconds (), 
                ServerState.FINISH, "Expedition time is over",
                null,
                (ctx, time) -> {
                    final var delta = ctx.getExpeditionTimeInMilliseconds () - time;
                    final var rest = String.valueOf (Math.round (delta / 1000.0));
                    
                    ctx.getRoom ().broadcastMessage (COUNTDOWN.getValue (), rest);
                }
            ),
            
            new ServerStateLogicAction (
                (ctx, time) -> ctx.getGameContext ().isExitFound (), 
                ServerState.FINISH, "Exit from the cave is found",
                null,
                (ctx, time) -> {
                    final var delta = ctx.getExpeditionTimeInMilliseconds () - time;
                    final var rest = String.valueOf (Math.round (delta / 1000.0));
                    
                    ctx.getRoom ().broadcastMessage (COUNTDOWN.getValue (), rest);
                }
            )
        ));
    }
    
    public static ServerStateLogic makeFinishLogic () {
        return new ServerStateLogic ((ctx, time) -> {
            ctx.getBufferOfConnections ().clear ();
            ctx.setNewConnectionsAllowed (false);
            
            final var inGame = ctx.getRoomConnections ().stream ().filter (cc -> cc.isAlive ())
                . map (ClientConnection::getId).collect (Collectors.toList ());
            
            ctx.getRequiredConnections ().clear ();
            ctx.getRequiredConnections ().addAll (inGame);
        }, (ctx, cc) -> {
            ctx.getBufferOfConnections ().remove (cc.getId ());
            ctx.getRequiredConnections ().remove (cc.getId ());
            
            ctx.getRoomConnections ().remove (cc);
        }, List.of (
            new ServerStateLogicAction (
                (ctx, time) -> {
                    final var rconnections = ctx.getRequiredConnections ();
                    final var buffer = ctx.getBufferOfConnections ();
                    
                    int intersection = 0;
                    for (final var id : buffer) {
                        intersection += rconnections.contains (id) ? 1 : 0;
                    }
                    
                    return intersection == rconnections.size ();
                }, 
                ServerState.RECRUITING, "All players finished the game", 
                (ctx, time) -> {
                    ctx.getRequiredConnections ().clear ();
                    ctx.getBufferOfConnections ().clear ();
                }, 
                null
            )
        ));
    }
    
}
