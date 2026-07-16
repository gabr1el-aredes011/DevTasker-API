package br.com.devtasker.api.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.devtasker.api.project.domain.Project;

public interface ProjectRepository
        extends JpaRepository<Project, Long> {
}