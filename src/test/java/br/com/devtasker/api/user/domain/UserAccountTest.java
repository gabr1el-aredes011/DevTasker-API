package br.com.devtasker.api.user.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class UserAccountTest {

    @Test
    void shouldChangePasswordAndIncrementCredentialVersion() {
        UserAccount user = UserAccount.create(
                "Gabriel",
                "gabriel@example.com",
                "old-encoded-hash"
        );

        user.changePassword("new-encoded-hash");

        assertAll(
                () -> assertEquals(
                        "new-encoded-hash",
                        user.getPasswordHash()
                ),
                () -> assertEquals(
                        1L,
                        user.getCredentialVersion()
                )
        );
    }

    @Test
    void shouldRejectBlankPasswordHashWithoutChangingCredentials() {
        UserAccount user = UserAccount.create(
                "Gabriel",
                "gabriel@example.com",
                "current-encoded-hash"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> user.changePassword("   ")
        );

        assertAll(
                () -> assertEquals(
                        "current-encoded-hash",
                        user.getPasswordHash()
                ),
                () -> assertEquals(
                        0L,
                        user.getCredentialVersion()
                )
        );
    }
}
