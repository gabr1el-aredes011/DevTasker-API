package br.com.devtasker.api.task.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.devtasker.api.task.domain.TaskAttachment;

public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> {

    List<TaskAttachment> findAllByTask_IdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(Long taskId);

    Optional<TaskAttachment> findByIdAndTask_IdAndDeletedAtIsNull(Long id, Long taskId);

    long countByTask_IdAndDeletedAtIsNull(Long taskId);
}
