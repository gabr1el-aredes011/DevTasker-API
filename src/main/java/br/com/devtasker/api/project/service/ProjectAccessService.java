package br.com.devtasker.api.project.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.exception.ProjectNotFoundException;
import br.com.devtasker.api.exception.ProjectPermissionDeniedException;
import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.project.domain.ProjectMemberRole;
import br.com.devtasker.api.project.repository.ProjectMemberRepository;

@Service
public class ProjectAccessService {

    private final ProjectMemberRepository
            projectMemberRepository;

    public ProjectAccessService(
            ProjectMemberRepository projectMemberRepository
    ) {
        this.projectMemberRepository =
                projectMemberRepository;
    }

    /**
     * Qualquer usuário que participe do projeto
     * pode acessar recursos de leitura.
     *
     * Caso não exista membership, retornamos
     * ProjectNotFoundException para não expor
     * projetos de terceiros.
     */
    @Transactional(readOnly = true)
    public ProjectMember requireMembership(
            Long projectId,
            Long userId
    ) {
        return projectMemberRepository
                .findActiveMembership(
                        projectId,
                        userId
                )
                .orElseThrow(
                        ProjectNotFoundException::new
                );
    }

    /**
     * Permissão operacional.
     *
     * OWNER  -> permitido
     * ADMIN  -> permitido
     * MEMBER -> permitido
     * VIEWER -> bloqueado
     *
     * É utilizada por tarefas e demais
     * operações cotidianas do projeto.
     */
    @Transactional(readOnly = true)
    public ProjectMember requireWriteAccess(
            Long projectId,
            Long userId
    ) {
        ProjectMember membership =
                requireMembership(
                        projectId,
                        userId
                );

        if (
                membership.getRole()
                        == ProjectMemberRole.VIEWER
        ) {
            throw new ProjectPermissionDeniedException();
        }

        return membership;
    }

    /**
     * Permissão administrativa.
     *
     * OWNER -> permitido
     * ADMIN -> permitido
     *
     * MEMBER e VIEWER não podem alterar
     * configurações administrativas do projeto.
     */
    @Transactional(readOnly = true)
    public ProjectMember requireManagementAccess(
            Long projectId,
            Long userId
    ) {
        ProjectMember membership =
                requireMembership(
                        projectId,
                        userId
                );

        ProjectMemberRole role =
                membership.getRole();

        if (
                role != ProjectMemberRole.OWNER
                && role != ProjectMemberRole.ADMIN
        ) {
            throw new ProjectPermissionDeniedException();
        }

        return membership;
    }

    /**
     * Permissão exclusiva do proprietário.
     *
     * Será usada futuramente para operações
     * realmente críticas, como transferência
     * de propriedade e ações destrutivas.
     */
    @Transactional(readOnly = true)
    public ProjectMember requireOwnership(
            Long projectId,
            Long userId
    ) {
        ProjectMember membership =
                requireMembership(
                        projectId,
                        userId
                );

        if (
                membership.getRole()
                        != ProjectMemberRole.OWNER
        ) {
            throw new ProjectPermissionDeniedException();
        }

        return membership;
    }
}
