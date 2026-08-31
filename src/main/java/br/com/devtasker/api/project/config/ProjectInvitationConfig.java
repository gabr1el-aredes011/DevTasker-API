package br.com.devtasker.api.project.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ProjectInvitationProperties.class)
public class ProjectInvitationConfig {
}
