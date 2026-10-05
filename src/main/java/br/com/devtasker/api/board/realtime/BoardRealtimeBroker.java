package br.com.devtasker.api.board.realtime;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import br.com.devtasker.api.board.domain.Board;
import br.com.devtasker.api.board.repository.BoardRepository;
import br.com.devtasker.api.exception.BoardNotFoundException;
import br.com.devtasker.api.project.service.ProjectAccessService;

@Service
public class BoardRealtimeBroker {

    private static final long CONNECTION_TIMEOUT_MS = 5 * 60 * 1000L;

    private final BoardRepository boardRepository;
    private final ProjectAccessService projectAccessService;
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> subscribersByBoard =
            new ConcurrentHashMap<>();

    public BoardRealtimeBroker(
            BoardRepository boardRepository,
            ProjectAccessService projectAccessService
    ) {
        this.boardRepository = boardRepository;
        this.projectAccessService = projectAccessService;
    }

    @Transactional(readOnly = true)
    public SseEmitter subscribe(Long boardId, Long userId) {
        Board board = boardRepository
                .findByIdAndArchivedAtIsNull(boardId)
                .orElseThrow(BoardNotFoundException::new);

        projectAccessService.requireMembership(board.getProject().getId(), userId);

        SseEmitter emitter = new SseEmitter(CONNECTION_TIMEOUT_MS);
        CopyOnWriteArrayList<SseEmitter> subscribers = subscribersByBoard
                .computeIfAbsent(boardId, ignored -> new CopyOnWriteArrayList<>());

        subscribers.add(emitter);
        Runnable cleanup = () -> removeSubscriber(boardId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());

        try {
            emitter.send(
                    SseEmitter.event()
                            .name("connected")
                            .data(Map.of(
                                    "boardId", boardId,
                                    "connectedAt", Instant.now().toString()
                            ))
            );
        } catch (IOException exception) {
            removeSubscriber(boardId, emitter);
            emitter.completeWithError(exception);
        }

        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAfterCommit(BoardRealtimeEvent event) {
        sendToBoard(
                event.boardId(),
                SseEmitter.event()
                        .id(event.eventId().toString())
                        .name("board-change")
                        .data(event)
        );
    }

    @Scheduled(fixedDelay = 15_000L)
    void sendHeartbeat() {
        for (Long boardId : List.copyOf(subscribersByBoard.keySet())) {
            sendToBoard(
                    boardId,
                    SseEmitter.event().comment("keep-alive")
            );
        }
    }

    int activeSubscriberCount(Long boardId) {
        return subscribersByBoard.getOrDefault(boardId, new CopyOnWriteArrayList<>()).size();
    }

    private void sendToBoard(Long boardId, SseEmitter.SseEventBuilder event) {
        CopyOnWriteArrayList<SseEmitter> subscribers = subscribersByBoard.get(boardId);
        if (subscribers == null) return;

        for (SseEmitter emitter : subscribers) {
            try {
                emitter.send(event);
            } catch (IOException | IllegalStateException exception) {
                removeSubscriber(boardId, emitter);
                emitter.complete();
            }
        }
    }

    private void removeSubscriber(Long boardId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> subscribers = subscribersByBoard.get(boardId);
        if (subscribers == null) return;

        subscribers.remove(emitter);
        if (subscribers.isEmpty()) subscribersByBoard.remove(boardId, subscribers);
    }
}
