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

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.OwnDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.ReportedDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.dto.DataExchangeApprovalDto;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.service.OwnDataExchangeApprovalService;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.service.ReportedDataExchangeApprovalService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("data-exchange-approval")
@Slf4j
public class DataExchangeApprovalController {
    @Autowired
    private OwnDataExchangeApprovalService ownDataExchangeApprovalService;
    @Autowired
    private ReportedDataExchangeApprovalService reportedDataExchangeApprovalService;
    @Autowired
    private ModelMapper modelMapper;
    
    @GetMapping
    @ResponseBody
    @Operation(summary = "Get all own data exchange approvals", description = "Get all own data exchange approvals.")
    public List<DataExchangeApprovalDto> getAllOwnDataExchangeApprovals() {
        return ownDataExchangeApprovalService.findAll().stream().map(this::convertApprovalToDto).collect(Collectors.toList());
    }

    @GetMapping("reported")
    @ResponseBody
    @Operation(summary = "Get all reported data exchange approvals", description = "Get all reported data exchange approvals.")
    public List<DataExchangeApprovalDto> getAllReportedDataExchangeApprovals() {
        return reportedDataExchangeApprovalService.findAll().stream().map(this::approvalConvertToDto).collect(Collectors.toList());
    }

    private DataExchangeApprovalDto convertApprovalToDto(OwnDataExchangeApproval entity) {
        DataExchangeApprovalDto dto = modelMapper.map(entity, DataExchangeApprovalDto.class);
        dto.setDataExchangeRequestId(entity.getDataExchangeRequest().getRequestId());
        return dto;
    }

    public DataExchangeApprovalDto approvalConvertToDto(ReportedDataExchangeApproval entity) {
        DataExchangeApprovalDto dto = modelMapper.map(entity, DataExchangeApprovalDto.class);
        dto.setDataExchangeRequestId(entity.getDataExchangeRequest().getRequestId());
        return dto;
    }
}
