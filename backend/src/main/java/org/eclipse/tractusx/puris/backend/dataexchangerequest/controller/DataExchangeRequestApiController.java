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
package org.eclipse.tractusx.puris.backend.dataexchangerequest.controller;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.ReportedDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.dto.DataExchangeRequestDto;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.service.ReportedDataExchangeRequestService;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.service.ReportedDemandAndCapacityNotificationService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("data-exchange-request")
@Slf4j
public class DataExchangeRequestApiController {

    @Autowired
    private ReportedDataExchangeRequestService reportedDataExchangeRequestService;
    @Autowired
    private ReportedDemandAndCapacityNotificationService reportedDemandAndCapacityNotificationService;
    @Autowired
    private ModelMapper modelMapper;
    
    @Operation(summary = "This endpoint receives the DataExchangeRequest requests. ")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ok", content = @Content),
        @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
    })
    @PostMapping("request")
    public DataExchangeRequestDto postDataExchangeRequest(@RequestBody DataExchangeRequestDto requestDto)
    {
        ReportedDemandAndCapacityNotification notification =
                reportedDemandAndCapacityNotificationService.findByNotificationId(requestDto.getNotificationId());

        if (notification == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Referenced notification does not exist.");
        }

        ReportedDataExchangeRequest reportedDataExchangeRequest = modelMapper.map(requestDto, ReportedDataExchangeRequest.class);
        reportedDataExchangeRequest.setNotification(notification);

        try {
            ReportedDataExchangeRequest newEntity = reportedDataExchangeRequestService.create(reportedDataExchangeRequest);
            DataExchangeRequestDto responseDto = modelMapper.map(newEntity, DataExchangeRequestDto.class);
            responseDto.setNotificationId(notification.getNotificationId());
            return responseDto;
        } catch (KeyAlreadyExistsException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reported Data Exchange Request already exists.");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reported Data Exchange Request is invalid.");
        }
    }
}
