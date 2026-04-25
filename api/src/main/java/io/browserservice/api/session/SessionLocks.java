package io.browserservice.api.session;

import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.error.SessionBusyException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class SessionLocks {

    private final long lockAcquireTimeoutMs;

    public SessionLocks(EngineProperties props) {
        this.lockAcquireTimeoutMs = props.session().lockAcquireTimeoutMs();
    }

    public <T> T doWithLock(SessionHandle handle, SessionWork<T> work) {
        boolean locked = false;
        try {
            locked = handle.lock().tryLock(lockAcquireTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SessionBusyException(handle.id());
        }
        if (!locked) {
            throw new SessionBusyException(handle.id());
        }
        try {
            handle.touch();
            return work.execute(handle);
        } finally {
            handle.lock().unlock();
        }
    }

    public void doWithLockVoid(SessionHandle handle, VoidSessionWork work) {
        doWithLock(handle, h -> {
            work.execute(h);
            return null;
        });
    }

    /**
     * Acquires the session lock with a custom short timeout for server-driven background
     * observers (the WS event watchers). Returns {@link Optional#empty()} on contention or
     * interrupt — callers must not block on it.
     *
     * <p>Critically, this variant does <strong>not</strong> call {@link SessionHandle#touch()}.
     * Watcher polling must not refresh idle TTL: only real user operations keep a session alive.
     */
    public <T> Optional<T> tryDoWithLock(SessionHandle handle, long timeoutMs, SessionWork<T> work) {
        boolean locked = false;
        try {
            locked = handle.lock().tryLock(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
        if (!locked) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(work.execute(handle));
        } finally {
            handle.lock().unlock();
        }
    }

    @FunctionalInterface
    public interface SessionWork<T> {
        T execute(SessionHandle handle);
    }

    @FunctionalInterface
    public interface VoidSessionWork {
        void execute(SessionHandle handle);
    }
}

