package io.browserservice.api.session;

import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.error.SessionBusyException;
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

    @FunctionalInterface
    public interface SessionWork<T> {
        T execute(SessionHandle handle);
    }

    @FunctionalInterface
    public interface VoidSessionWork {
        void execute(SessionHandle handle);
    }
}
