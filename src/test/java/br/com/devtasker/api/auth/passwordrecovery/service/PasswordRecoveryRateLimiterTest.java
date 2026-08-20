package br.com.devtasker.api.auth.passwordrecovery.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class PasswordRecoveryRateLimiterTest {

    @Test
    void shouldEnforceLimitAtomicallyAcrossConcurrentCalls() {
        PasswordRecoveryRateLimiter limiter =
                new PasswordRecoveryRateLimiter(
                        Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)
                );
        AtomicInteger allowed = new AtomicInteger();

        IntStream.range(0, 100).parallel().forEach(index -> {
            if (
                    limiter.allow(
                            "verify",
                            "fingerprint",
                            5,
                            Duration.ofMinutes(10)
                    )
            ) {
                allowed.incrementAndGet();
            }
        });

        assertEquals(5, allowed.get());
    }

    @Test
    void shouldOpenANewWindowAfterExpiration() {
        MutableClock clock = new MutableClock();
        PasswordRecoveryRateLimiter limiter =
                new PasswordRecoveryRateLimiter(clock);

        assertTrue(
                limiter.allow("request", "client", 1, Duration.ofMinutes(1))
        );
        assertFalse(
                limiter.allow("request", "client", 1, Duration.ofMinutes(1))
        );

        clock.advance(Duration.ofMinutes(1));

        assertTrue(
                limiter.allow("request", "client", 1, Duration.ofMinutes(1))
        );
    }

    private static final class MutableClock extends Clock {

        private Instant instant = Instant.EPOCH;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
