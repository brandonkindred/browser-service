package io.browserservice.api.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "browser_sessions")
public class BrowserSessionEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "browser_type", nullable = false, length = 32)
  private String browserType;

  @Column(name = "environment", nullable = false, length = 32)
  private String environment;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private Status status;

  @Column(name = "is_mobile", nullable = false)
  private boolean mobile;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "last_used_at", nullable = false)
  private Instant lastUsedAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "closed_at")
  private Instant closedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "closed_reason", length = 32)
  private ClosedReason closedReason;

  @Column(name = "idle_ttl_secs", nullable = false)
  private int idleTtlSecs;

  @Column(name = "absolute_ttl_secs", nullable = false)
  private int absoluteTtlSecs;

  protected BrowserSessionEntity() {}

  public BrowserSessionEntity(
      UUID id,
      String browserType,
      String environment,
      Status status,
      boolean mobile,
      Instant createdAt,
      Instant lastUsedAt,
      Instant expiresAt,
      int idleTtlSecs,
      int absoluteTtlSecs) {
    this.id = id;
    this.browserType = browserType;
    this.environment = environment;
    this.status = status;
    this.mobile = mobile;
    this.createdAt = createdAt;
    this.lastUsedAt = lastUsedAt;
    this.expiresAt = expiresAt;
    this.idleTtlSecs = idleTtlSecs;
    this.absoluteTtlSecs = absoluteTtlSecs;
  }

  public UUID getId() {
    return id;
  }

  public String getBrowserType() {
    return browserType;
  }

  public String getEnvironment() {
    return environment;
  }

  public Status getStatus() {
    return status;
  }

  public boolean isMobile() {
    return mobile;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getLastUsedAt() {
    return lastUsedAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getClosedAt() {
    return closedAt;
  }

  public ClosedReason getClosedReason() {
    return closedReason;
  }

  public int getIdleTtlSecs() {
    return idleTtlSecs;
  }

  public int getAbsoluteTtlSecs() {
    return absoluteTtlSecs;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public void setLastUsedAt(Instant lastUsedAt) {
    this.lastUsedAt = lastUsedAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public void setClosedAt(Instant closedAt) {
    this.closedAt = closedAt;
  }

  public void setClosedReason(ClosedReason closedReason) {
    this.closedReason = closedReason;
  }

  public enum Status {
    ACTIVE,
    CLOSED,
    EXPIRED
  }

  public enum ClosedReason {
    CLIENT,
    REAPED_IDLE,
    REAPED_ABSOLUTE,
    ERROR
  }
}
