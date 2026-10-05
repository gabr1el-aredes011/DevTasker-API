package br.com.devtasker.api.project.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.devtasker.api.project.domain.ProjectLabel;

public interface ProjectLabelRepository extends JpaRepository<ProjectLabel, Long> {

    List<ProjectLabel> findAllByProject_IdAndArchivedAtIsNullOrderByNameAscIdAsc(Long projectId);

    List<ProjectLabel> findAllByIdInAndProject_IdAndArchivedAtIsNull(
            List<Long> ids,
            Long projectId
    );

    Optional<ProjectLabel> findByIdAndProject_IdAndArchivedAtIsNull(
            Long labelId,
            Long projectId
    );

    boolean existsByProject_IdAndNormalizedNameAndArchivedAtIsNull(
            Long projectId,
            String normalizedName
    );

    boolean existsByProject_IdAndNormalizedNameAndArchivedAtIsNullAndIdNot(
            Long projectId,
            String normalizedName,
            Long labelId
    );
}
