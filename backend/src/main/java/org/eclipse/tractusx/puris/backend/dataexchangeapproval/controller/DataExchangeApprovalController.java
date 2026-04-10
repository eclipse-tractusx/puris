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
package org.eclipse.tractusx.puris.backend.dataexchangeapproval.controller;
import java.util.UUID;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.OwnDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.dto.DataExchangeApprovalDto;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.service.OwnDataExchangeApprovalService;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.ReportedDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.service.ReportedDataExchangeRequestService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("data-exchange-request")
@Slf4j
public class DataExchangeApprovalController {

    @Autowired
    private OwnDataExchangeApprovalService ownDataExchangeApprovalService;
    @Autowired
    private ReportedDataExchangeRequestService reportedDataExchangeRequestService;
    @Autowired
    private ModelMapper modelMapper;

    @PostMapping("/{requestId}/approval")
    @ResponseBody
    @Operation(summary = "Creates a new own data exchange approval", description = "Creates a new own data exchange approval. \n")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Own Data Exchange Approval was created."),
            @ApiResponse(responseCode = "400", description = "Malformed or invalid request body.", content = @Content),
            @ApiResponse(responseCode = "409", description = "Own Data Exchange Approval already exists.", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.", content = @Content)
    })
    @ResponseStatus(HttpStatus.CREATED)
    public DataExchangeApprovalDto createDataExchangeApproval(@PathVariable UUID requestId, @RequestBody DataExchangeApprovalDto requestDto) {
        ReportedDataExchangeRequest reportedRequest =
                reportedDataExchangeRequestService.findByRequestId(requestId);

        if (reportedRequest == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Referenced reported data exchange request does not exist.");
        }

        OwnDataExchangeApproval ownDataExchangeApproval = modelMapper.map(requestDto, OwnDataExchangeApproval.class);
        ownDataExchangeApproval.setDataExchangeRequest(reportedRequest);

        try {
            OwnDataExchangeApproval newEntity = ownDataExchangeApprovalService.create(ownDataExchangeApproval);
            DataExchangeApprovalDto responseDto = modelMapper.map(newEntity, DataExchangeApprovalDto.class);
            responseDto.setDataExchangeRequestId(reportedRequest.getUuid());
            return responseDto;
        } catch (KeyAlreadyExistsException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Own Data Exchange Approval already exists." + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Own Data Exchange Approval is invalid." + e.getMessage());
        }
    }
    
}
