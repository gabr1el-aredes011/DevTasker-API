package br.com.devtasker.api.user.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.devtasker.api.user.domain.UserRole;
import br.com.devtasker.api.user.dto.CurrentUserResponse;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public CurrentUserResponse currentUser(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Number userId = jwt.getClaim("user_id");
        List<String> roles = jwt.getClaimAsStringList("roles");

        return new CurrentUserResponse(
                userId.longValue(),
                jwt.getClaimAsString("name"),
                jwt.getSubject(),
                UserRole.valueOf(roles.get(0))
        );
    }
}
