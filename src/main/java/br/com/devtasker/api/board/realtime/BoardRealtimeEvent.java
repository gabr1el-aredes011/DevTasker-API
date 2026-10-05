package br.com.devtasker.api.board.realtime;

import java.time.Instant;
import java.util.UUID;

public record BoardRealtimeEvent(
        UUID eventId,
        Long boardId,
        Long taskId,
        Long actorUserId,
        BoardRealtimeEventType type,
        Instant occurredAt
) {
    public static BoardRealtimeEvent taskChanged(
            Long boardId,
            Long taskId,
            Long actorUserId,
            BoardRealtimeEventType type
    ) {
        return new BoardRealtimeEvent(
                UUID.randomUUID(),
                boardId,
                taskId,
                actorUserId,
                type,
                Instant.now()
        );
    }
}
