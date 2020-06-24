package ru.shemplo.cave.app.server.room.state;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import ru.shemplo.cave.app.server.room.ServerRoomContext;

@Getter
@RequiredArgsConstructor
@ToString (of = {"transition", "reason"})
public class ServerStateLogicAction {
    
    private final BiPredicate <ServerRoomContext, Long> predicate;
    
    private final ServerState transition;
    
    private final String reason;
    
    private final BiConsumer <ServerRoomContext, Long> onPredicate;
    
    private final BiConsumer <ServerRoomContext, Long> onNonPredicate;
    
    public static final ServerStateLogicAction AT_LEAST_ONE_LOST = new ServerStateLogicAction (
        (ctx, time) -> {
            final var rconnections = ctx.getRequiredConnections ();
            final var required = ctx.getRoomConnections ().stream ()
                . filter (cc -> rconnections.contains (cc.getId ())).count ();
            
            return required != rconnections.size ();
        },
        ServerState.FINISH, "One of expeditors is lost",
        null, null
    );
    
}

