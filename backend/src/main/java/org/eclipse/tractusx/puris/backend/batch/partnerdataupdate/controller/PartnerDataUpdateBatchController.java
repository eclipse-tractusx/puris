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
package org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.controller;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.logic.dto.PartnerDataUpdateBatchRunDto;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.logic.dto.PartnerDataUpdateBatchRunEntryDto;
import org.modelmapper.ModelMapper;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.domain.model.PartnerDataUpdateBatchRun;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.domain.model.PartnerDataUpdateBatchRunEntry;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.logic.service.PartnerDataUpdateBatchProcessService;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.logic.service.PartnerDataUpdateBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/batch/partner-data-update")
@Slf4j
public class PartnerDataUpdateBatchController {

    @Autowired
    private PartnerDataUpdateBatchService batchService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private PartnerDataUpdateBatchProcessService processService;

    @GetMapping
    @PreAuthorize("hasRole('ROLE_PURIS_ADMIN')")
    public Page<PartnerDataUpdateBatchRunDto> history(@PageableDefault(sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PartnerDataUpdateBatchRun> runs = batchService.findAll(pageable);
        return runs.map(r -> modelMapper.map(r, PartnerDataUpdateBatchRunDto.class));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_PURIS_ADMIN')")
    public ResponseEntity<PartnerDataUpdateBatchRunDto> status(@PathVariable UUID id) {
        PartnerDataUpdateBatchRun run = batchService.findById(id);
        if (run == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(modelMapper.map(run, PartnerDataUpdateBatchRunDto.class));
    }

    @GetMapping("/{id}/entries")
    @PreAuthorize("hasRole('ROLE_PURIS_ADMIN')")
    public Page<PartnerDataUpdateBatchRunEntryDto> entries(@PathVariable UUID id, Pageable pageable) {
        Page<PartnerDataUpdateBatchRunEntry> entries = batchService.findEntriesByRunId(id, pageable);
        return entries.map(e -> modelMapper.map(e, PartnerDataUpdateBatchRunEntryDto.class));
    }

    @PostMapping("/run")
    @PreAuthorize("hasRole('ROLE_PURIS_ADMIN')")
    public ResponseEntity<String> triggerRun() {
        processService.executeFullBatch();
        return ResponseEntity.accepted().body("Batch started");
    }
}
