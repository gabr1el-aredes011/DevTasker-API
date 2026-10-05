package br.com.devtasker.api.task.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.devtasker.api.task.domain.TaskComment;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {

    List<TaskComment> findAllByTask_IdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(Long taskId);

    Optional<TaskComment> findByIdAndTask_IdAndDeletedAtIsNull(Long id, Long taskId);
}
