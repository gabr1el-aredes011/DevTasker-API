package br.com.devtasker.api.project.service;

import java.util.List;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.exception.ProjectLabelException;
import br.com.devtasker.api.project.domain.ProjectLabel;
import br.com.devtasker.api.project.dto.CreateProjectLabelRequest;
import br.com.devtasker.api.project.dto.ProjectLabelResponse;
import br.com.devtasker.api.project.dto.UpdateProjectLabelRequest;
import br.com.devtasker.api.project.repository.ProjectLabelRepository;
import br.com.devtasker.api.task.repository.TaskRepository;

@Service
public class ProjectLabelService {

    private final ProjectLabelRepository projectLabelRepository;
    private final ProjectAccessService projectAccessService;
    private final TaskRepository taskRepository;

    public ProjectLabelService(
            ProjectLabelRepository projectLabelRepository,
            ProjectAccessService projectAccessService,
            TaskRepository taskRepository
    ) {
        this.projectLabelRepository = projectLabelRepository;
        this.projectAccessService = projectAccessService;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectLabelResponse> findAll(Long projectId, Long userId) {
        projectAccessService.requireMembership(projectId, userId);

        return projectLabelRepository
                .findAllByProject_IdAndArchivedAtIsNullOrderByNameAscIdAsc(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProjectLabelResponse create(
            Long projectId,
            Long userId,
            CreateProjectLabelRequest request
    ) {
        var membership = projectAccessService.requireManagementAccess(projectId, userId);
        ensureNameAvailable(projectId, request.name(), null);

        ProjectLabel label = ProjectLabel.create(
                membership.getProject(),
                request.name(),
                request.color()
        );

        return toResponse(save(label));
    }

    @Transactional
    public ProjectLabelResponse update(
            Long projectId,
            Long labelId,
            Long userId,
            UpdateProjectLabelRequest request
    ) {
        projectAccessService.requireManagementAccess(projectId, userId);
        ProjectLabel label = findActive(projectId, labelId);
        ensureNameAvailable(projectId, request.name(), labelId);

        label.update(request.name(), request.color());
        return toResponse(save(label));
    }

    @Transactional
    public void archive(Long projectId, Long labelId, Long userId) {
        projectAccessService.requireManagementAccess(projectId, userId);
        ProjectLabel label = findActive(projectId, labelId);
        label.archive();
        projectLabelRepository.save(label);
    }

    private ProjectLabel findActive(Long projectId, Long labelId) {
        return projectLabelRepository
                .findByIdAndProject_IdAndArchivedAtIsNull(labelId, projectId)
                .orElseThrow(() -> new ProjectLabelException(
                        HttpStatus.NOT_FOUND,
                        "PROJECT_LABEL_NOT_FOUND",
                        "A label solicitada não foi encontrada neste projeto."
                ));
    }

    private void ensureNameAvailable(Long projectId, String name, Long ignoredLabelId) {
        String normalizedName = name.trim().toLowerCase(Locale.ROOT);
        boolean exists = ignoredLabelId == null
                ? projectLabelRepository
                        .existsByProject_IdAndNormalizedNameAndArchivedAtIsNull(projectId, normalizedName)
                : projectLabelRepository
                        .existsByProject_IdAndNormalizedNameAndArchivedAtIsNullAndIdNot(
                                projectId,
                                normalizedName,
                                ignoredLabelId
                        );

        if (exists) {
            throw duplicateName();
        }
    }

    private ProjectLabel save(ProjectLabel label) {
        try {
            return projectLabelRepository.saveAndFlush(label);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateName();
        }
    }

    private ProjectLabelException duplicateName() {
        return new ProjectLabelException(
                HttpStatus.CONFLICT,
                "PROJECT_LABEL_NAME_ALREADY_IN_USE",
                "Já existe uma label ativa com este nome no projeto."
        );
    }

    private ProjectLabelResponse toResponse(ProjectLabel label) {
        return new ProjectLabelResponse(
                label.getId(),
                label.getName(),
                label.getColor(),
                taskRepository.countByLabels_IdAndArchivedAtIsNull(label.getId()),
                label.getCreatedAt(),
                label.getUpdatedAt()
        );
    }
}
