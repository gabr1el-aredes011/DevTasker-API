package br.com.devtasker.api.auth.passwordrecovery.service;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class PasswordRecoveryRateLimiter {

    private static final int MAXIMUM_KEYS = 10_000;
    private static final int CLEANUP_INTERVAL = 256;

    private final Map<String, WindowCounter> counters = new HashMap<>();
    private final Clock clock;
    private int operationsSinceCleanup;

    public PasswordRecoveryRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public synchronized boolean allow(
            String scope,
            String fingerprint,
            int maximumRequests,
            Duration window
    ) {
        validatePolicy(maximumRequests, window);

        long now = clock.millis();
        long windowMillis = window.toMillis();
        String key = scope + ":" + fingerprint;
        WindowCounter current = counters.get(key);

        if (
                current == null
                || now - current.startedAtMillis() >= windowMillis
        ) {
            cleanupWhenNeeded(now);

            if (current == null && counters.size() >= MAXIMUM_KEYS) {
                return false;
            }

            counters.put(key, new WindowCounter(now, 1, windowMillis));
            return true;
        }

        if (current.count() >= maximumRequests) {
            return false;
        }

        counters.put(
                key,
                new WindowCounter(
                        current.startedAtMillis(),
                        current.count() + 1,
                        current.windowMillis()
                )
        );
        cleanupWhenNeeded(now);
        return true;
    }

    synchronized int trackedKeyCount() {
        return counters.size();
    }

    private void cleanupWhenNeeded(long now) {
        operationsSinceCleanup++;

        if (
                operationsSinceCleanup < CLEANUP_INTERVAL
                && counters.size() < MAXIMUM_KEYS
        ) {
            return;
        }

        operationsSinceCleanup = 0;
        Iterator<WindowCounter> iterator = counters.values().iterator();

        while (iterator.hasNext()) {
            WindowCounter counter = iterator.next();

            if (
                    now - counter.startedAtMillis()
                    >= counter.windowMillis()
            ) {
                iterator.remove();
            }
        }
    }

    private static void validatePolicy(
            int maximumRequests,
            Duration window
    ) {
        if (maximumRequests <= 0) {
            throw new IllegalArgumentException(
                    "O limite de requisições deve ser positivo."
            );
        }

        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException(
                    "A janela de requisições deve ser positiva."
            );
        }
    }

    private record WindowCounter(
            long startedAtMillis,
            int count,
            long windowMillis
    ) {
    }
}
