/*
 * Copyright (c) 2026 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.logic.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.logic.service.PartnerDataUpdateBatchProcessService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PartnerDataUpdateBatchScheduler {

    private final PartnerDataUpdateBatchProcessService processService;

    @Value("${puris.batch.partnerdataupdate.enabled:true}")
    private boolean enabled;

    @Value("${puris.batch.partnerdataupdate.cron}")
    private String cron;

    @Scheduled(cron = "${puris.batch.partnerdataupdate.cron}")
    public void scheduledRun() {
        if (!enabled) {
            log.debug("PartnerDataUpdateBatchScheduler is disabled by configuration");
            return;
        }
        log.info("Triggering PartnerDataUpdateBatch scheduled run (cron={})", cron);
        processService.executeFullBatch();
    }
}

