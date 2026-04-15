package com.zoopzoop.zoopzoop.domain.policy.service;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "policy.sync", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PolicySyncScheduler {

    private final PolicySyncService policySyncService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${policy.sync.on-shutdown.enabled:false}")
    private boolean syncOnShutdownEnabled;

    @Scheduled(cron = "${policy.sync.cron:0 0 0 * * *}", zone = "${policy.sync.zone:Asia/Seoul}")
    public void syncPoliciesDaily() {
        syncPolicies("scheduled");
    }

    @EventListener(ContextClosedEvent.class)
    public void syncPoliciesOnShutdown() {
        if (!syncOnShutdownEnabled) {
            log.info("Policy sync on shutdown skipped because it is disabled.");
            return;
        }

        syncPolicies("shutdown");
    }

    private void syncPolicies(String trigger) {
        if (!running.compareAndSet(false, true)) {
            log.warn("Policy sync skipped because a previous sync is still running. trigger={}", trigger);
            return;
        }

        try {
            log.info("Policy sync started. trigger={}", trigger);
            String result = policySyncService.syncFullData();
            log.info("Policy sync finished. trigger={}, result={}", trigger, result);
        } catch (Exception e) {
            log.error("Policy sync failed. trigger={}", trigger, e);
        } finally {
            running.set(false);
        }
    }
}
