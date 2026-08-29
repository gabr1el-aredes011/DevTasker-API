package br.com.devtasker.api.board.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import br.com.devtasker.api.board.dto.UpdateBoardRequest;
import br.com.devtasker.api.board.service.BoardCommandService;
import br.com.devtasker.api.board.service.BoardQueryService;

@ExtendWith(MockitoExtension.class)
class BoardControllerTest {

    private static final Long BOARD_ID = 11L;
    private static final Long USER_ID = 7L;

    @Mock
    private BoardQueryService boardQueryService;

    @Mock
    private BoardCommandService boardCommandService;

    @Mock
    private Jwt jwt;

    private BoardController controller;

    @BeforeEach
    void setUp() {
        controller = new BoardController(
                boardQueryService,
                boardCommandService
        );

        when(jwt.getClaim("user_id"))
                .thenReturn(USER_ID);
    }

    @Test
    void shouldRenameBoardForAuthenticatedUser() {
        UpdateBoardRequest request =
                new UpdateBoardRequest("Entregas");

        controller.update(
                BOARD_ID,
                request,
                jwt
        );

        verify(boardCommandService).update(
                BOARD_ID,
                USER_ID,
                request
        );
    }

    @Test
    void shouldArchiveBoardAndReturnNoContent() {
        ResponseEntity<Void> response =
                controller.archive(
                        BOARD_ID,
                        jwt
                );

        assertEquals(
                HttpStatus.NO_CONTENT,
                response.getStatusCode()
        );

        verify(boardCommandService).archive(
                BOARD_ID,
                USER_ID
        );
    }
}
