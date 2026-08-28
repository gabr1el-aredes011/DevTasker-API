package br.com.devtasker.api.dashboard.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.board.domain.Board;
import br.com.devtasker.api.board.domain.BoardColumn;
import br.com.devtasker.api.board.domain.BoardColumnCategory;
import br.com.devtasker.api.board.repository.BoardColumnRepository;
import br.com.devtasker.api.board.repository.BoardRepository;
import br.com.devtasker.api.board.service.BoardQueryService;
import br.com.devtasker.api.exception.BoardNotFoundException;
import br.com.devtasker.api.exception.ProjectNotFoundException;
import br.com.devtasker.api.project.domain.Project;
import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.project.repository.ProjectMemberRepository;
import br.com.devtasker.api.project.repository.ProjectRepository;
import br.com.devtasker.api.project.service.ProjectQueryService;
import br.com.devtasker.api.task.domain.Task;
import br.com.devtasker.api.task.domain.TaskPriority;
import br.com.devtasker.api.task.repository.TaskRepository;
import br.com.devtasker.api.task.service.TaskService;
import br.com.devtasker.api.user.domain.UserAccount;
import br.com.devtasker.api.user.repository.UserAccountRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DashboardQueryRepositoryIntegrationTest {

    @Autowired
    private DashboardQueryRepository dashboardQueryRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardColumnRepository boardColumnRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectQueryService projectQueryService;

    @Autowired
    private BoardQueryService boardQueryService;

    @Autowired
    private TaskService taskService;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void shouldExcludeArchivedProjectFromEveryDashboardQuery() {
        UserAccount owner = userAccountRepository.saveAndFlush(
                UserAccount.create(
                        "Gabriel",
                        "gabriel.dashboard@devtasker.test",
                        "encoded-password"
                )
        );

        Project project = projectRepository.saveAndFlush(
                Project.create(
                        "DevTasker",
                        "Projeto ativo",
                        owner
                )
        );

        projectMemberRepository.saveAndFlush(
                ProjectMember.createOwner(
                        project,
                        owner
                )
        );

        Board board = boardRepository.saveAndFlush(
                Board.createInitial(project)
        );

        BoardColumn column = boardColumnRepository.saveAndFlush(
                BoardColumn.create(
                        board,
                        "Backlog",
                        BoardColumnCategory.BACKLOG,
                        0
                )
        );

        Task task = taskRepository.saveAndFlush(
                Task.create(
                        column,
                        owner,
                        "Revisar segurança",
                        null,
                        TaskPriority.URGENT,
                        LocalDate.now().plusDays(1),
                        0
                )
        );

        assertVisibleDashboard(owner.getId());

        project.archive();
        projectRepository.saveAndFlush(project);
        entityManager.clear();

        assertHiddenDashboard(owner.getId());
        assertArchivedResourcesAreInaccessible(
                owner.getId(),
                project.getId(),
                board.getId(),
                task.getId()
        );
    }

    private void assertVisibleDashboard(
            Long userId
    ) {
        assertEquals(
                1L,
                dashboardQueryRepository
                        .countProjectsByUser(userId)
        );

        assertEquals(
                1L,
                dashboardQueryRepository
                        .countBoardsByUser(userId)
        );

        assertEquals(
                1L,
                dashboardQueryRepository
                        .findTaskMetrics(
                                userId,
                                LocalDate.now()
                        )
                        .total()
        );

        assertEquals(
                1,
                dashboardQueryRepository
                        .findRecentProjects(userId)
                        .size()
        );

        assertEquals(
                1L,
                dashboardQueryRepository
                        .findWorkflow(userId)
                        .backlog()
        );

        assertEquals(
                1,
                dashboardQueryRepository
                        .findAttentionTasks(
                                userId,
                                LocalDate.now()
                        )
                        .size()
        );
    }

    private void assertHiddenDashboard(
            Long userId
    ) {
        assertEquals(
                0L,
                dashboardQueryRepository
                        .countProjectsByUser(userId)
        );

        assertEquals(
                0L,
                dashboardQueryRepository
                        .countBoardsByUser(userId)
        );

        assertEquals(
                0L,
                dashboardQueryRepository
                        .findTaskMetrics(
                                userId,
                                LocalDate.now()
                        )
                        .total()
        );

        assertTrue(
                dashboardQueryRepository
                        .findRecentProjects(userId)
                        .isEmpty()
        );

        assertEquals(
                0L,
                dashboardQueryRepository
                        .findWorkflow(userId)
                        .backlog()
        );

        assertTrue(
                dashboardQueryRepository
                        .findAttentionTasks(
                                userId,
                                LocalDate.now()
                        )
                        .isEmpty()
        );
    }

    private void assertArchivedResourcesAreInaccessible(
            Long userId,
            Long projectId,
            Long boardId,
            Long taskId
    ) {
        assertThrows(
                ProjectNotFoundException.class,
                () -> projectQueryService.findById(
                        projectId,
                        userId
                )
        );

        assertThrows(
                ProjectNotFoundException.class,
                () -> boardQueryService.findBoardsByProject(
                        projectId,
                        userId
                )
        );

        assertThrows(
                BoardNotFoundException.class,
                () -> boardQueryService.findBoardById(
                        boardId,
                        userId
                )
        );

        assertThrows(
                BoardNotFoundException.class,
                () -> boardQueryService.findKanbanByBoardId(
                        boardId,
                        userId
                )
        );

        assertThrows(
                ProjectNotFoundException.class,
                () -> taskService.findById(
                        taskId,
                        userId
                )
        );
    }
}
