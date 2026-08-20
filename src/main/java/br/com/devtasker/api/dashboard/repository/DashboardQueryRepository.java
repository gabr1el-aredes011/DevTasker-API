package br.com.devtasker.api.dashboard.repository;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import br.com.devtasker.api.board.domain.BoardColumnCategory;
import br.com.devtasker.api.dashboard.dto.DashboardRecentProjectResponse;
import br.com.devtasker.api.dashboard.dto.DashboardTaskMetricsResponse;
import br.com.devtasker.api.dashboard.dto.DashboardWorkflowResponse;
import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.task.domain.Task;
import br.com.devtasker.api.task.domain.TaskPriority;
import br.com.devtasker.api.dashboard.dto.DashboardAttentionTaskResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class DashboardQueryRepository {

    private static final int RECENT_PROJECT_LIMIT = 5;
    private static final int ATTENTION_TASK_LIMIT = 5;
    
    @PersistenceContext
    private EntityManager entityManager;

    public long countProjectsByUser(
            Long userId
    ) {
        return entityManager
                .createQuery(
                        """
                        SELECT COUNT(membership)
                        FROM ProjectMember membership
                        WHERE membership.user.id = :userId
                          AND membership.project.archivedAt IS NULL
                        """,
                        Long.class
                )
                .setParameter("userId", userId)
                .getSingleResult();
    }

    public long countBoardsByUser(
            Long userId
    ) {
        return entityManager
                .createQuery(
                        """
                        SELECT COUNT(board)
                        FROM Board board
                        WHERE board.project.archivedAt IS NULL
                          AND EXISTS (
                            SELECT membership.id
                            FROM ProjectMember membership
                            WHERE membership.project = board.project
                              AND membership.user.id = :userId
                        )
                        """,
                        Long.class
                )
                .setParameter("userId", userId)
                .getSingleResult();
    }

    public DashboardTaskMetricsResponse findTaskMetrics(
            Long userId,
            LocalDate today
    ) {
        Object[] result = entityManager
                .createQuery(
                        """
                        SELECT
                            COUNT(task),
                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN boardColumn.category <> :done
                                        THEN 1
                                        ELSE 0
                                    END
                                ),
                                0
                            ),
                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN boardColumn.category = :doing
                                        THEN 1
                                        ELSE 0
                                    END
                                ),
                                0
                            ),
                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN boardColumn.category = :done
                                        THEN 1
                                        ELSE 0
                                    END
                                ),
                                0
                            ),
                            COALESCE(
                                SUM(
                                    CASE
                                        WHEN task.dueDate < :today
                                         AND boardColumn.category <> :done
                                        THEN 1
                                        ELSE 0
                                    END
                                ),
                                0
                            )
                        FROM Task task
                        JOIN task.column boardColumn
                        WHERE task.archivedAt IS NULL
                          AND boardColumn.board.project.archivedAt IS NULL
                          AND EXISTS (
                              SELECT membership.id
                              FROM ProjectMember membership
                              WHERE membership.project =
                                    boardColumn.board.project
                                AND membership.user.id = :userId
                          )
                        """,
                        Object[].class
                )
                .setParameter("userId", userId)
                .setParameter(
                        "done",
                        BoardColumnCategory.DONE
                )
                .setParameter(
                        "doing",
                        BoardColumnCategory.DOING
                )
                .setParameter("today", today)
                .getSingleResult();

        return new DashboardTaskMetricsResponse(
                toLong(result[0]),
                toLong(result[1]),
                toLong(result[2]),
                toLong(result[3]),
                toLong(result[4])
        );
    }

    public List<DashboardRecentProjectResponse>
    findRecentProjects(
            Long userId
    ) {
        return entityManager
                .createQuery(
                        """
                        SELECT membership
                        FROM ProjectMember membership
                        JOIN FETCH membership.project project
                        WHERE membership.user.id = :userId
                          AND project.archivedAt IS NULL
                        ORDER BY project.createdAt DESC
                        """,
                        ProjectMember.class
                )
                .setParameter("userId", userId)
                .setMaxResults(RECENT_PROJECT_LIMIT)
                .getResultList()
                .stream()
                .map(membership -> {
                    var project =
                            membership.getProject();

                    return new DashboardRecentProjectResponse(
                            project.getId(),
                            project.getName(),
                            project.getDescription(),
                            membership.getRole(),
                            project.getCreatedAt()
                    );
                })
                .toList();
    }

    public DashboardWorkflowResponse findWorkflow(
            Long userId
    ) {
        List<Object[]> rows = entityManager
                .createQuery(
                        """
                        SELECT
                            boardColumn.category,
                            COUNT(task)
                        FROM Task task
                        JOIN task.column boardColumn
                        WHERE task.archivedAt IS NULL
                          AND boardColumn.board.project.archivedAt IS NULL
                          AND EXISTS (
                              SELECT membership.id
                              FROM ProjectMember membership
                              WHERE membership.project =
                                    boardColumn.board.project
                                AND membership.user.id = :userId
                          )
                        GROUP BY boardColumn.category
                        """,
                        Object[].class
                )
                .setParameter("userId", userId)
                .getResultList();

        Map<BoardColumnCategory, Long> counts =
                new EnumMap<>(
                        BoardColumnCategory.class
                );

        for (Object[] row : rows) {
            BoardColumnCategory category =
                    (BoardColumnCategory) row[0];

            Long count =
                    toLong(row[1]);

            counts.put(category, count);
        }

        return new DashboardWorkflowResponse(
                counts.getOrDefault(
                        BoardColumnCategory.BACKLOG,
                        0L
                ),
                counts.getOrDefault(
                        BoardColumnCategory.TODO,
                        0L
                ),
                counts.getOrDefault(
                        BoardColumnCategory.DOING,
                        0L
                ),
                counts.getOrDefault(
                        BoardColumnCategory.REVIEW,
                        0L
                ),
                counts.getOrDefault(
                        BoardColumnCategory.DONE,
                        0L
                )
        );
    }

    public List<DashboardAttentionTaskResponse>
    findAttentionTasks(
            Long userId,
            LocalDate today
    ) {
        List<Task> tasks = entityManager
                .createQuery(
                        """
                        SELECT task
                        FROM Task task
                        JOIN FETCH task.column boardColumn
                        JOIN FETCH boardColumn.board board
                        JOIN FETCH board.project project

                        WHERE task.archivedAt IS NULL

                          AND project.archivedAt IS NULL

                          AND boardColumn.category <> :done

                          AND EXISTS (
                              SELECT membership.id
                              FROM ProjectMember membership
                              WHERE membership.project = project
                                AND membership.user.id = :userId
                          )

                          AND (
                              (
                                  task.dueDate IS NOT NULL
                                  AND task.dueDate < :today
                              )

                              OR task.priority = :urgent

                              OR task.priority = :high
                          )

                        ORDER BY

                          CASE
                              WHEN task.dueDate IS NOT NULL
                               AND task.dueDate < :today
                              THEN 0
                              ELSE 1
                          END,

                          CASE
                              WHEN task.priority = :urgent
                              THEN 0

                              WHEN task.priority = :high
                              THEN 1

                              ELSE 2
                          END,

                          CASE
                              WHEN task.dueDate IS NULL
                              THEN 1
                              ELSE 0
                          END,

                          task.dueDate ASC,
                          task.updatedAt DESC
                        """,
                        Task.class
                )

                .setParameter(
                        "userId",
                        userId
                )

                .setParameter(
                        "today",
                        today
                )

                .setParameter(
                        "done",
                        BoardColumnCategory.DONE
                )

                .setParameter(
                        "urgent",
                        TaskPriority.URGENT
                )

                .setParameter(
                        "high",
                        TaskPriority.HIGH
                )

                .setMaxResults(
                        ATTENTION_TASK_LIMIT
                )

                .getResultList();

        return tasks
                .stream()
                .map(task -> {

                    var column =
                            task.getColumn();

                    var board =
                            column.getBoard();

                    var project =
                            board.getProject();

                    boolean overdue =
                            task.getDueDate() != null
                            && task
                                .getDueDate()
                                .isBefore(today);

                    return new DashboardAttentionTaskResponse(
                            task.getId(),
                            task.getTitle(),
                            task.getPriority(),
                            task.getDueDate(),

                            column.getName(),

                            board.getId(),
                            board.getName(),

                            project.getId(),
                            project.getName(),

                            overdue
                    );
                })
                .toList();
    }
    
    private long toLong(
            Object value
    ) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        return 0L;
    }
}
