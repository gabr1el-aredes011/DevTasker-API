package br.com.devtasker.api.task.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

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
import br.com.devtasker.api.project.domain.Project;
import br.com.devtasker.api.project.domain.ProjectLabel;
import br.com.devtasker.api.project.domain.ProjectLabelColor;
import br.com.devtasker.api.project.repository.ProjectLabelRepository;
import br.com.devtasker.api.project.repository.ProjectRepository;
import br.com.devtasker.api.task.domain.Task;
import br.com.devtasker.api.task.domain.TaskPriority;
import br.com.devtasker.api.user.domain.UserAccount;
import br.com.devtasker.api.user.repository.UserAccountRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TaskRepositoryIntegrationTest {

    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectLabelRepository projectLabelRepository;
    @Autowired private BoardRepository boardRepository;
    @Autowired private BoardColumnRepository boardColumnRepository;
    @Autowired private TaskRepository taskRepository;

    @PersistenceContext private EntityManager entityManager;

    @Test
    void shouldPersistAndReplaceOrderedTaskLabels() {
        UserAccount owner = userAccountRepository.saveAndFlush(
                UserAccount.create(
                        "Gabriel",
                        "gabriel.labels@devtasker.test",
                        "encoded-password"
                )
        );
        Project project = projectRepository.saveAndFlush(
                Project.create("Labels", null, owner)
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
        ProjectLabel backend = projectLabelRepository.saveAndFlush(
                ProjectLabel.create(project, "Backend", ProjectLabelColor.BLUE)
        );
        ProjectLabel urgent = projectLabelRepository.saveAndFlush(
                ProjectLabel.create(project, "Urgente", ProjectLabelColor.RED)
        );
        Task task = Task.create(
                column,
                owner,
                "Publicar labels",
                null,
                TaskPriority.HIGH,
                null,
                0
        );
        task.replaceLabels(List.of(backend, urgent));
        task.addChecklistItem("Preparar ambiente");
        task.addChecklistItem("Executar validação");
        task = taskRepository.saveAndFlush(task);

        entityManager.clear();

        Task persistedTask = taskRepository.findActiveById(task.getId()).orElseThrow();
        assertEquals(
                List.of("Backend", "Urgente"),
                persistedTask.getLabels().stream().map(ProjectLabel::getName).toList()
        );
        assertEquals(2, persistedTask.getChecklistItems().size());
        assertEquals("Preparar ambiente", persistedTask.getChecklistItems().getFirst().getTitle());
        assertEquals(0, persistedTask.getChecklistItems().getFirst().getPosition());

        ProjectLabel frontend = projectLabelRepository.saveAndFlush(
                ProjectLabel.create(project, "Frontend", ProjectLabelColor.VIOLET)
        );
        ProjectLabel improvement = projectLabelRepository.saveAndFlush(
                ProjectLabel.create(project, "Melhoria", ProjectLabelColor.GREEN)
        );
        persistedTask.replaceLabels(List.of(frontend, improvement));
        taskRepository.saveAndFlush(persistedTask);
        entityManager.clear();

        Task updatedTask = taskRepository.findActiveById(task.getId()).orElseThrow();
        assertEquals(
                List.of("Frontend", "Melhoria"),
                updatedTask.getLabels().stream().map(ProjectLabel::getName).toList()
        );
    }
}
