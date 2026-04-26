package io.browserservice.api.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BrowserSessionRepository extends JpaRepository<BrowserSessionEntity, UUID> {

    List<BrowserSessionEntity> findByStatus(BrowserSessionEntity.Status status);
}
