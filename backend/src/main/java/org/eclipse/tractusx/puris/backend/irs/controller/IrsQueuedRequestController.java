/*
 * Copyright (c) 2026 Volkswagen AG
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
 * AI-Disclosure: This file was largely AI-generated. The AI-generated portions are made
 * available under CC0-1.0 and not subject to the project's licence.
 * The human contributor has reviewed and verified that the code is correct.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Assisted-by: Claude Code Opus 5.5
 */
package org.eclipse.tractusx.puris.backend.irs.controller;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequest;
import org.eclipse.tractusx.puris.backend.irs.logic.dto.IrsQueuedRequestDto;
import org.eclipse.tractusx.puris.backend.irs.logic.service.IrsRequestQueueService;
import org.eclipse.tractusx.puris.backend.irs.logic.service.IrsRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.media.Content;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exposes the IRS request queue to the frontend and allows retrying failed requests.
 * The class level {@code @PreAuthorize} applies so all endpoints are admin-only.
 */
@RestController
@RequestMapping("irs/requests")
@Slf4j
public class IrsQueuedRequestController {
 
    @Autowired
    private IrsRequestQueueService irsRequestQueueService;
 
    @Autowired
    private IrsRequestService irsRequestService;
 
    @Autowired
    private ModelMapper modelMapper;
 
    @PreAuthorize("hasRole('PURIS_ADMIN')")
    @GetMapping()
    @ResponseBody
    @Operation(summary = "Get all queued IRS requests -- ADMIN ONLY",
        description = "Get all queued IRS requests regardless of their status (PENDING, SUCCEEDED, FAILED, CANCELLED), " +
            "newest first. Only available if the IRS adapter is enabled.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Queued requests were returned."),
        @ApiResponse(responseCode = "401", description = "Not authenticated.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin or the IRS adapter is disabled.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal Server Error.", content = @Content)
    })
    public List<IrsQueuedRequestDto> getAllQueuedRequests() {
        if (!irsRequestService.isEnabled()) {
            log.warn("Rejected request for IRS queued requests because the IRS adapter is disabled.");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "IRS adapter is disabled. Enable it via puris.irsadapter.enabled to access IRS requests.");
        }
        return irsRequestQueueService.findAll().stream().map(this::convertToDto).toList();
    }
 
    @PreAuthorize("hasRole('PURIS_ADMIN')")
    @PostMapping("{uuid}/retry")
    @Operation(summary = "Retry a failed IRS request -- ADMIN ONLY",
        description = "Resets the attempt count of a failed queued IRS request to 0, sets its status to PENDING and makes it " +
            "due immediately. Only available if the IRS adapter is enabled.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Request was reset and will be retried."),
        @ApiResponse(responseCode = "401", description = "Not authenticated.", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin or the IRS adapter is disabled.", content = @Content),
        @ApiResponse(responseCode = "404", description = "Queued request not found.", content = @Content),
        @ApiResponse(responseCode = "409", description = "Request is not failed, or a newer request for the same entity is already pending.", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal Server Error.", content = @Content)
    })
    public IrsQueuedRequestDto retryQueuedRequest(@PathVariable UUID uuid) {
        if (!irsRequestService.isEnabled()) {
            log.warn("Rejected retry of IRS queued request because the IRS adapter is disabled.");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "IRS adapter is disabled. Enable it via puris.irsadapter.enabled to access IRS requests.");
        }
        try {
            return convertToDto(irsRequestQueueService.retry(uuid));
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException | KeyAlreadyExistsException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    private IrsQueuedRequestDto convertToDto(IrsQueuedRequest entity) {
        return modelMapper.map(entity, IrsQueuedRequestDto.class);
    }
}
