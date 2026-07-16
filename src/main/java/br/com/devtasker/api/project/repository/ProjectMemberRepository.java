package br.com.devtasker.api.project.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.devtasker.api.project.domain.ProjectMember;

public interface ProjectMemberRepository
        extends JpaRepository<ProjectMember, Long> {

    List<ProjectMember> findAllByUser_IdOrderByJoinedAtAsc(
            Long userId
    );

    Optional<ProjectMember> findByProject_IdAndUser_Id(
            Long projectId,
            Long userId
    );

    boolean existsByProject_IdAndUser_Id(
            Long projectId,
            Long userId
    );
}