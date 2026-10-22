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

AI-Disclosure: This file was largely AI-generated. The AI-generated portions are made
available under CC0-1.0 and not subject to the project's licence.
The human contributor has reviewed and verified that the code is correct.

SPDX-License-Identifier: Apache-2.0
Assisted-by: Claude Code Opus 5.5
*/
package org.eclipse.tractusx.puris.backend.irs.controller;

import java.util.List;
import java.util.NoSuchElementException;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.irs.IrsAdapterConfiguration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsJob;
import org.eclipse.tractusx.puris.backend.irs.logic.dto.IrsJobCreationDto;
import org.eclipse.tractusx.puris.backend.irs.logic.dto.IrsJobDto;
import org.eclipse.tractusx.puris.backend.irs.logic.service.IrsJobService;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("irs/jobs")
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("hasRole('PURIS_ADMIN')")
public class IrsJobController {

    private final IrsJobService irsJobService;

    private final IrsAdapterConfiguration irsAdapterConfiguration;

    private final ModelMapper modelMapper;

    @GetMapping
    @ResponseBody
    @Operation(summary = "Get all IRS jobs -- ADMIN ONLY", description = "Get all IRS jobs.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns all IRS jobs."),
            @ApiResponse(responseCode = "401", description = "Missing API key.", content = @Content),
            @ApiResponse(responseCode = "403", description = "IRS adapter is disabled, or the provided API key is invalid.", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.", content = @Content)
    })
    public List<IrsJobDto> getAllJobs() {
        assertIrsAdapterEnabled();
        return irsJobService.findAll().stream()
            .map(this::convertToDto)
            .toList();
    }

    @PostMapping
    @Operation(summary = "Create an IRS job for a root grant -- ADMIN ONLY", description = "Creates a new IRS job for the given root grant " +
        "and enqueues the job creation request to the IRS. The job's material is resolved from the grant's globalAssetId and its " +
        "sourceDisruptionId is taken from the grant. The material must be a product with at least one child material affected " +
        "by a currently-active notification.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "IRS job created and job creation request enqueued."),
            @ApiResponse(responseCode = "400", description = "Malformed request body, deleted root grant, unknown material or material not eligible for an IRS job.", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing API key.", content = @Content),
            @ApiResponse(responseCode = "403", description = "IRS adapter is disabled, or the provided API key is invalid.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Root grant not found.", content = @Content),
            @ApiResponse(responseCode = "409", description = "A job for the same material and source disruption is already queued.", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.", content = @Content)
    })
    public ResponseEntity<?> createJob(@RequestBody IrsJobCreationDto irsJobCreationDto) {
        assertIrsAdapterEnabled();
        if (irsJobCreationDto.getRootGrantUuid() == null) {
            log.warn("Rejected IRS job request without root grant.");
            return ResponseEntity.badRequest().body("IRS job is invalid: a root grant is required");
        }

        try {
            IrsJob created = irsJobService.createAndSendForRootGrant(irsJobCreationDto.getRootGrantUuid());
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(created));
        } catch (NoSuchElementException e) {
            log.warn("Root grant {} not found", irsJobCreationDto.getRootGrantUuid());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Root grant not found");
        } catch (KeyAlreadyExistsException e) {
            log.warn("IRS job for root grant {} is already queued", irsJobCreationDto.getRootGrantUuid());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Could not create IRS job for root grant {}", irsJobCreationDto.getRootGrantUuid(), e);
            return ResponseEntity.badRequest().body("IRS job is invalid: " + e.getMessage());
        }
    }

    private IrsJobDto convertToDto(IrsJob job) {
        IrsJobDto dto = modelMapper.map(job, IrsJobDto.class);
        if (job.getMaterial() != null) {
            dto.setOwnMaterialNumber(job.getMaterial().getOwnMaterialNumber());
        }
        return dto;
    }

    private void assertIrsAdapterEnabled() {
        if (!irsAdapterConfiguration.isIrsAdapterEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "IRS adapter is disabled.");
        }
    }

}
