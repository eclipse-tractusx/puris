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
package org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.logic.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.domain.model.PartnerDataUpdateBatchRun;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.domain.model.PartnerDataUpdateBatchRunEntry;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.domain.repository.PartnerDataUpdateBatchRunEntryRepository;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.domain.repository.PartnerDataUpdateBatchRunRepository;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.logic.service.PartnerDataUpdateBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PartnerDataUpdateBatchServiceImpl implements PartnerDataUpdateBatchService {

    @Autowired
    private final PartnerDataUpdateBatchRunRepository runRepository;
    @Autowired
    private final PartnerDataUpdateBatchRunEntryRepository entryRepository;

    @Value("${puris.batch.partnerdataupdate.cleanup.retention-days:30}")
    private int retentionDays;

    @Override
    public Page<PartnerDataUpdateBatchRun> findAll(Pageable pageable) {
        return runRepository.findAll(pageable);
    }

    @Override
    public PartnerDataUpdateBatchRun findById(UUID id) {
        return runRepository.findById(id).orElse(null);
    }

    @Override
    public Page<PartnerDataUpdateBatchRunEntry> findEntriesByRunId(UUID runId, Pageable pageable) {
        return entryRepository.findAllByBatchRun_Id(runId, pageable);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PartnerDataUpdateBatchRunEntry createEntryRequiresNew(PartnerDataUpdateBatchRunEntry entry) {
        return entryRepository.save(entry);
    }

    @Override
    public void cleanupOldRuns() {
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(retentionDays);
        List<PartnerDataUpdateBatchRun> toDelete = runRepository.findByEndTimeBefore(cutoff);
        if (!toDelete.isEmpty()) {
            runRepository.deleteAll(toDelete);
            log.info("Deleted {} old PartnerDataUpdateBatch runs older than {} days", toDelete.size(), retentionDays);
        }
    }
}
