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
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.dto.DataExchangeRequestDto;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.service.OwnDataExchangeRequestService;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.service.ReportedDemandAndCapacityNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

@RestController
@RequestMapping("data-exchange-request")
public class DataExchangeRequestController {

    @Autowired
    private OwnDataExchangeRequestService ownDataExchangeRequestService;
    @Autowired
    private ReportedDemandAndCapacityNotificationService reportedDemandAndCapacityNotificationService;

    @PostMapping()
    @ResponseBody
    @Operation(summary = "Creates a new own data exchange request", description = "Creates a new own data exchange request. \n")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Own Data Exchange Request was created."),
            @ApiResponse(responseCode = "400", description = "Malformed or invalid request body.", content = @Content),
            @ApiResponse(responseCode = "409", description = "Own Data Exchange Request already exists.", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.", content = @Content)
    })
    @ResponseStatus(HttpStatus.CREATED)
    public DataExchangeRequestDto createDataExchangeRequest(@RequestBody DataExchangeRequestDto requestDto) {
        ReportedDemandAndCapacityNotification notification =
                reportedDemandAndCapacityNotificationService.findByNotificationId(requestDto.getNotificationId());

        if (notification == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Referenced notification does not exist.");
        }

        OwnDataExchangeRequest ownDataExchangeRequest = convertToEntity(requestDto);
        ownDataExchangeRequest.setNotification(notification);

        try {
            return convertToDto(ownDataExchangeRequestService.create(ownDataExchangeRequest));
        } catch (KeyAlreadyExistsException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Own Data Exchange Request already exists.");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Own Data Exchange Request is invalid.");
        }
    }

    private OwnDataExchangeRequest convertToEntity(DataExchangeRequestDto dto) {
        OwnDataExchangeRequest entity = new OwnDataExchangeRequest();
        entity.setUuid(dto.getUuid());
        entity.setCriticality(dto.getCriticality());
        entity.setDesiredStartDateTime(dto.getDesiredStartDateTime());
        entity.setDesiredEndDateTime(dto.getDesiredEndDateTime());
        entity.setRequestedTypes(dto.getRequestedTypes());
        entity.setText(dto.getText());
        entity.setTimestamp(dto.getTimestamp());
        return entity;
    }

    private DataExchangeRequestDto convertToDto(OwnDataExchangeRequest entity) {
        DataExchangeRequestDto dto = new DataExchangeRequestDto();
        dto.setUuid(entity.getUuid());
        dto.setNotificationId(entity.getNotification().getNotificationId());
        dto.setCriticality(entity.getCriticality());
        dto.setDesiredStartDateTime(entity.getDesiredStartDateTime());
        dto.setDesiredEndDateTime(entity.getDesiredEndDateTime());
        dto.setRequestedTypes(entity.getRequestedTypes());
        dto.setText(entity.getText());
        dto.setTimestamp(entity.getTimestamp());
        return dto;
    }
}
