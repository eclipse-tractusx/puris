/*
 * Copyright (c) 2026 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.logic.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.logic.service.PartnerDataUpdateBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler to periodically cleanup old PartnerDataUpdate batch runs.
 * Controlled via properties under `puris.batch.partnerdataupdate.cleanup`.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PartnerDataUpdateCleanupScheduler {

    @Autowired
    private final PartnerDataUpdateBatchService batchService;

    @Value("${puris.batch.partnerdataupdate.cleanup.enabled:true}")
    private boolean enabled;

    /**
     * Default: daily at 4:00 UTC
     */
    @Value("${puris.batch.partnerdataupdate.cleanup.cron:0 0 4 * * *}")
    private String cron;

    @Scheduled(cron = "${puris.batch.partnerdataupdate.cleanup.cron:0 0 4 * * *}")
    public void cleanup() {
        if (!enabled) {
            log.debug("PartnerDataUpdateCleanupScheduler is disabled");
            return;
        }

        log.info("Starting PartnerDataUpdateBatch cleanup (cron={})", cron);
        try {
            batchService.cleanupOldRuns();
        } catch (Exception ex) {
            log.error("Error while cleaning up old PartnerDataUpdateBatch runs", ex);
        }
    }
}

