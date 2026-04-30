package io.browserservice.api.session;

import io.browserservice.api.config.EngineProperties;
import io.browserservice.api.error.SessionCapExceededException;
import io.browserservice.api.error.SessionNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import org.springframework.stereotype.Component;

@Component
public class SessionRegistry {

    private final ConcurrentHashMap<UUID, SessionHandle> sessions = new ConcurrentHashMap<>();
    private final Semaphore capacity;
    private final int maxConcurrent;

    public SessionRegistry(EngineProperties props) {
        this.maxConcurrent = props.session().maxConcurrent();
        this.capacity = new Semaphore(this.maxConcurrent);
    }

    public void acquirePermit() {
        if (!capacity.tryAcquire()) {
            throw new SessionCapExceededException(maxConcurrent);
        }
    }

    public void releasePermit() {
        capacity.release();
    }

    public void register(SessionHandle handle) {
        sessions.put(handle.id(), handle);
    }

    public SessionHandle get(UUID id) {
        SessionHandle h = sessions.get(id);
        if (h == null || h.isClosed()) {
            throw new SessionNotFoundException(id);
        }
        return h;
    }

    public Optional<SessionHandle> find(UUID id) {
        SessionHandle h = sessions.get(id);
        if (h == null || h.isClosed()) {
            return Optional.empty();
        }
        return Optional.of(h);
    }

    public List<SessionHandle> snapshot() {
        return List.copyOf(sessions.values());
    }

    public boolean remove(UUID id) {
        SessionHandle h = sessions.remove(id);
        if (h == null) {
            return false;
        }
        boolean wasOpen = h.closeOnce();
        if (wasOpen) {
            capacity.release();
        }
        return wasOpen;
    }

    public int size() {
        return (int) sessions.values().stream().filter(s -> !s.isClosed()).count();
    }

    public int availablePermits() {
        return capacity.availablePermits();
    }

    public int maxConcurrent() {
        return maxConcurrent;
    }
}
