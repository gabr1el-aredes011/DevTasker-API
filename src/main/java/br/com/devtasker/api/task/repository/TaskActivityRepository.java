package br.com.devtasker.api.task.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.devtasker.api.task.domain.TaskActivity;

public interface TaskActivityRepository extends JpaRepository<TaskActivity, Long> {

    List<TaskActivity> findTop100ByTask_IdOrderByCreatedAtDescIdDesc(Long taskId);
}
