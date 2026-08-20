package br.com.devtasker.api.project.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import br.com.devtasker.api.project.domain.Project;
import br.com.devtasker.api.project.domain.ProjectMember;
import br.com.devtasker.api.exception.ProjectNotFoundException;
import br.com.devtasker.api.project.service.ProjectCommandService;
import br.com.devtasker.api.user.domain.UserAccount;
import br.com.devtasker.api.user.repository.UserAccountRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProjectMemberRepositoryIntegrationTest {

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private ProjectCommandService projectCommandService;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void shouldSearchOrderAndFetchOnlyActiveProjects() {
        UserAccount owner = userAccountRepository.saveAndFlush(
                UserAccount.create(
                        "Gabriel",
                        "gabriel.projects@devtasker.test",
                        "encoded-password"
                )
        );

        Project apiProject = persistProject(
                owner,
                "DevTasker API",
                "Backend Java"
        );

        Project webProject = persistProject(
                owner,
                "Portal Web",
                "Frontend Angular"
        );

        entityManager.flush();
        entityManager.clear();

        List<ProjectMember> allProjects =
                projectMemberRepository
                        .findActiveProjectsByUser(
                                owner.getId(),
                                null
                        );

        assertEquals(
                List.of(
                        webProject.getId(),
                        apiProject.getId()
                ),
                allProjects.stream()
                        .map(membership ->
                                membership.getProject().getId()
                        )
                        .toList()
        );

        assertTrue(
                entityManager
                        .getEntityManagerFactory()
                        .getPersistenceUnitUtil()
                        .isLoaded(
                                allProjects.getFirst(),
                                "project"
                        )
        );

        List<ProjectMember> searchResult =
                projectMemberRepository
                        .findActiveProjectsByUser(
                                owner.getId(),
                                "aNgUlAr"
                        );

        assertEquals(1, searchResult.size());
        assertEquals(
                webProject.getId(),
                searchResult.getFirst()
                        .getProject()
                        .getId()
        );

        Project projectToArchive =
                searchResult.getFirst()
                        .getProject();

        projectToArchive.archive();
        projectRepository.saveAndFlush(projectToArchive);
        entityManager.clear();

        List<ProjectMember> afterArchive =
                projectMemberRepository
                        .findActiveProjectsByUser(
                                owner.getId(),
                                null
                        );

        assertEquals(1, afterArchive.size());
        assertEquals(
                apiProject.getId(),
                afterArchive.getFirst()
                        .getProject()
                        .getId()
        );

        assertTrue(
                projectMemberRepository
                        .findActiveMembership(
                                apiProject.getId(),
                                owner.getId()
                        )
                        .isPresent()
        );

        assertFalse(
                projectMemberRepository
                        .findActiveMembership(
                                webProject.getId(),
                                owner.getId()
                        )
                        .isPresent()
        );

        assertFalse(
                projectMemberRepository
                        .existsActiveMembership(
                                webProject.getId(),
                                owner.getId()
                        )
        );
    }

    @Test
    void shouldArchiveWithoutPhysicalDeleteAndHideRepeatedRequest() {
        UserAccount owner = userAccountRepository.saveAndFlush(
                UserAccount.create(
                        "Owner",
                        "owner.archive@devtasker.test",
                        "encoded-password"
                )
        );

        Project project = persistProject(
                owner,
                "Projeto temporário",
                null
        );

        projectCommandService.archive(
                project.getId(),
                owner.getId()
        );

        assertTrue(
                projectRepository.existsById(
                        project.getId()
                )
        );

        assertThrows(
                ProjectNotFoundException.class,
                () -> projectCommandService.archive(
                        project.getId(),
                        owner.getId()
                )
        );
    }

    private Project persistProject(
            UserAccount owner,
            String name,
            String description
    ) {
        Project project = projectRepository.saveAndFlush(
                Project.create(
                        name,
                        description,
                        owner
                )
        );

        projectMemberRepository.saveAndFlush(
                ProjectMember.createOwner(
                        project,
                        owner
                )
        );

        return project;
    }
}
