package br.com.devtasker.api.auth.verification.config;

import java.security.SecureRandom;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(
        EmailVerificationProperties.class
)
public class EmailVerificationConfig {

    @Bean
    SecureRandom emailVerificationSecureRandom() {
        return new SecureRandom();
    }
}