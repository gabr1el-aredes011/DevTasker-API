package br.com.devtasker.api.board.realtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.devtasker.api.board.domain.Board;
import br.com.devtasker.api.board.repository.BoardRepository;
import br.com.devtasker.api.project.domain.Project;
import br.com.devtasker.api.project.service.ProjectAccessService;

@ExtendWith(MockitoExtension.class)
class BoardRealtimeBrokerTest {

    private static final Long BOARD_ID = 11L;
    private static final Long PROJECT_ID = 7L;
    private static final Long USER_ID = 2L;

    @Mock private BoardRepository boardRepository;
    @Mock private ProjectAccessService projectAccessService;
    @Mock private Board board;
    @Mock private Project project;

    private BoardRealtimeBroker broker;

    @BeforeEach
    void setUp() {
        broker = new BoardRealtimeBroker(boardRepository, projectAccessService);
    }

    @Test
    void shouldAuthorizeMembershipBeforeOpeningBoardStream() {
        when(boardRepository.findByIdAndArchivedAtIsNull(BOARD_ID))
                .thenReturn(Optional.of(board));
        when(board.getProject()).thenReturn(project);
        when(project.getId()).thenReturn(PROJECT_ID);

        broker.subscribe(BOARD_ID, USER_ID);

        verify(projectAccessService).requireMembership(PROJECT_ID, USER_ID);
        assertEquals(1, broker.activeSubscriberCount(BOARD_ID));
    }

    @Test
    void shouldIgnoreEventsWhenTheBoardHasNoConnectedSubscribers() {
        broker.publishAfterCommit(
                BoardRealtimeEvent.taskChanged(
                        BOARD_ID,
                        19L,
                        USER_ID,
                        BoardRealtimeEventType.TASK_MOVED
                )
        );

        assertEquals(0, broker.activeSubscriberCount(BOARD_ID));
    }
}
