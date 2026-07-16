package br.com.devtasker.api.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.devtasker.api.auth.dto.LoginRequest;
import br.com.devtasker.api.auth.dto.LoginResponse;
import br.com.devtasker.api.auth.dto.RegisterRequest;
import br.com.devtasker.api.auth.dto.RegisterResponse;
import br.com.devtasker.api.auth.service.LoginService;
import br.com.devtasker.api.auth.service.RegistrationService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final LoginService loginService;

    public AuthController(
            RegistrationService registrationService,
            LoginService loginService
    ) {
        this.registrationService = registrationService;
        this.loginService = loginService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        RegisterResponse response =
                registrationService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(
                loginService.login(request)
        );
    }
}
