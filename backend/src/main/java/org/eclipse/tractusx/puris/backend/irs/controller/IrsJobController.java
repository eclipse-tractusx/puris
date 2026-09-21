/*
Copyright (c) 2026 Volkswagen AG

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.

SPDX-License-Identifier: Apache-2.0
*/
package org.eclipse.tractusx.puris.backend.irs.controller;

import java.util.List;

import org.eclipse.tractusx.puris.backend.irs.IrsAdapterConfiguration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsJob;
import org.eclipse.tractusx.puris.backend.irs.domain.repository.IrsJobRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("irs/jobs")
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("hasRole('PURIS_ADMIN')")
public class IrsJobController {

    private final IrsJobRepository irsJobRepository;

    private final IrsAdapterConfiguration irsAdapterConfiguration;

    @GetMapping
    @ResponseBody
    @Operation(summary = "Get all IRS jobs -- ADMIN ONLY", description = "Get all IRS jobs.")
    public List<IrsJob> getAllJobs() {
        assertIrsAdapterEnabled();
        return irsJobRepository.findAll();
    }

    private void assertIrsAdapterEnabled() {
        if (!irsAdapterConfiguration.isIrsAdapterEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "IRS adapter is disabled.");
        }
    }

}
