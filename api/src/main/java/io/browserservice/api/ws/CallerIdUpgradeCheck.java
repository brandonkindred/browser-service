package io.browserservice.api.ws;

import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Rejects the WS HTTP upgrade with status 401 when {@code X-Caller-Id} is missing or blank, so the
 * connection is never established for an unidentifiable caller. A malformed-but-present header
 * passes this check and is rejected later in {@link SessionSocket#onOpen} with WS close code 4401
 * via {@code CallerId.parse}.
 */
@ApplicationScoped
public class CallerIdUpgradeCheck implements HttpUpgradeCheck {

  public static final String CALLER_HEADER = "X-Caller-Id";

  @Override
  public Uni<CheckResult> perform(HttpUpgradeContext ctx) {
    String raw = ctx.httpRequest().getHeader(CALLER_HEADER);
    if (raw == null || raw.isBlank()) {
      return CheckResult.rejectUpgrade(401);
    }
    return CheckResult.permitUpgrade();
  }
}
