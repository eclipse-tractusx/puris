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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.dataexchangeapproval.controller.DataExchangeApprovalController;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.OwnDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.ReportedDataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.dto.DataExchangeApprovalDto;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.service.DataExchangeApprovalApiService;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.service.OwnDataExchangeApprovalService;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.service.ReportedDataExchangeApprovalService;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.DataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.OwnDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model.ReportedDataExchangeRequest;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.dto.DataExchangeRequestDto;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.service.DataExchangeRequestApiService;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.service.DataExchangeRequestForwardService;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.service.OwnDataExchangeRequestService;
import org.eclipse.tractusx.puris.backend.dataexchangerequest.logic.service.ReportedDataExchangeRequestService;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.service.ReportedDemandAndCapacityNotificationService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Site;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("data-exchange-request")
@Slf4j
public class DataExchangeRequestController {
    
    @Autowired
    private OwnDataExchangeRequestService ownDataExchangeRequestService;
    @Autowired
    private DataExchangeApprovalApiService dataExchangeApprovalApiService;
    @Autowired
    private ReportedDemandAndCapacityNotificationService reportedDemandAndCapacityNotificationService;
    @Autowired
    private ReportedDataExchangeRequestService reportedDataExchangeRequestService;
    @Autowired
    private DataExchangeRequestApiService dataExchangeRequestApiService;
    @Autowired
    private OwnDataExchangeApprovalService ownDataExchangeApprovalService;
    @Autowired
    private ReportedDataExchangeApprovalService reportedDataExchangeApprovalService;
    @Autowired
    private DataExchangeApprovalController dataExchangeApprovalController;
    @Autowired
    private DataExchangeRequestForwardService dataExchangeForwardService;
    @Autowired
    private PartnerService partnerService;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private Validator validator;
    @Autowired
    private ExecutorService executorService;
 
    @GetMapping
    @ResponseBody
    @Operation(summary = "Get all own data exchange requests", description = "Get all own data exchange requests.")
    public List<DataExchangeRequestDto> getAllOwnDataExchangeRequests() {
        return ownDataExchangeRequestService.findAll().stream().map(this::convertToDto).collect(Collectors.toList());
    }
 
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
        var validate = requestDto == null ? null : validator.validate(requestDto);
        if (requestDto == null || !validate.isEmpty()) {
            log.warn("Rejected own data exchange request, constraint violations: {}", validate);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed data exchange request.");
        }
 
        OwnDataExchangeRequest ownDataExchangeRequest = requestDto.getNotificationId() != null
                ? buildRequestFromNotification(requestDto)
                : buildRequestWithoutNotification(requestDto);
        try {
            OwnDataExchangeRequest newEntity = ownDataExchangeRequestService.create(ownDataExchangeRequest);
            executorService.submit(() -> dataExchangeRequestApiService.sendDataExchangeRequest(newEntity));
            return convertToDto(newEntity);
        } catch (KeyAlreadyExistsException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Own Data Exchange Request already exists.");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Own Data Exchange Request is invalid.");
        } catch (Exception e) {
            log.error("Error while creating own data exchange request", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while creating the own data exchange request.");
        }
    }
 
    @PostMapping("reported/{id}/approvals")
    @ResponseBody
    @Operation(summary = "Creates a new own data exchange approval", description = "Creates a new own data exchange approval in response to an existing ReportedDataExchangeRequest. \n")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Own Data Exchange Approval was created."),
            @ApiResponse(responseCode = "400", description = "Malformed or invalid request body.", content = @Content),
            @ApiResponse(responseCode = "409", description = "Own Data Exchange Approval already exists.", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.", content = @Content)
    })
    @ResponseStatus(HttpStatus.CREATED)
    public DataExchangeApprovalDto createDataExchangeApproval(@PathVariable UUID id, @RequestParam(name = "forward", defaultValue = "false") boolean forward, @RequestBody DataExchangeApprovalDto requestDto) {
        ReportedDataExchangeRequest reportedRequest = reportedDataExchangeRequestService.findById(id);
 
        if (reportedRequest == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Referenced reported data exchange request does not exist.");
        }
        Partner partner = reportedRequest.getPartner();
 
        List<DataExchangeRequestForwardService.ForwardTarget> targets = List.of();
        if (forward) {
            targets = dataExchangeForwardService.resolveForwardTargets(reportedRequest);
            if (targets.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot forward: no partners to forward this request to could be resolved.");
            }
        }
 
 
        OwnDataExchangeApproval ownDataExchangeApproval = modelMapper.map(requestDto, OwnDataExchangeApproval.class);
        ownDataExchangeApproval.setDataExchangeRequest(reportedRequest);
        ownDataExchangeApproval.setFinalized(!forward);
 
        try {
            OwnDataExchangeApproval newEntity = ownDataExchangeApprovalService.create(ownDataExchangeApproval);
 
            List<OwnDataExchangeRequest> forwardedRequests = dataExchangeForwardService.createForwardedRequests(reportedRequest, targets);
            if (forward && forwardedRequests.isEmpty()) {
                log.warn("No forwarded request could be created for request {}, finalizing the approval", reportedRequest.getRequestId());
                newEntity.setFinalized(true);
                ownDataExchangeApprovalService.update(newEntity);
            }
            for (OwnDataExchangeRequest fwd : forwardedRequests) {
                executorService.submit(() -> dataExchangeRequestApiService.sendDataExchangeRequest(fwd));
            }
 
            executorService.submit(() -> dataExchangeApprovalApiService.sendDataExchangeApproval(newEntity, partner));
            DataExchangeApprovalDto responseDto = modelMapper.map(newEntity, DataExchangeApprovalDto.class);
            responseDto.setDataExchangeRequestId(reportedRequest.getRequestId());
            return responseDto;
        } catch (KeyAlreadyExistsException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Own Data Exchange Approval already exists." + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Own Data Exchange Approval is invalid." + e.getMessage());
        } catch (Exception e) {
            log.error("Error while creating own data exchange approval", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while creating the own data exchange approval.");
        }
    }
 
    @GetMapping("reported")
    @ResponseBody
    @Operation(summary = "Get all reported data exchange requests", description = "Get all reported data exchange requests.")
    public List<DataExchangeRequestDto> getAllReportedDataExchangeRequest() {
        return reportedDataExchangeRequestService.findAll().stream().map(this::convertReportedToDto).collect(Collectors.toList());
    }
 
    @GetMapping("{id}/approvals")
    @ResponseBody
    @Operation(summary = "Get all reported data exchange approvals for a specific request", description = "Get all reported data exchange approvals for a specific request.")
    public DataExchangeApprovalDto getReportedDataExchangeApproval(@PathVariable UUID id) {
        OwnDataExchangeRequest ownRequest = ownDataExchangeRequestService.findById(id);
 
        if (ownRequest == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Referenced own data exchange request does not exist.");
        }
        ReportedDataExchangeApproval approval = reportedDataExchangeApprovalService.findByDataExchangeRequest_Uuid(id);
        if (approval == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No reported approval exists for this request.");
        }
        return dataExchangeApprovalController.approvalConvertToDto(approval);
    }
 
    private OwnDataExchangeRequest buildRequestFromNotification(DataExchangeRequestDto requestDto) {
        ReportedDemandAndCapacityNotification notification = reportedDemandAndCapacityNotificationService.findByNotificationId(requestDto.getNotificationId());
 
        if (notification == null) {
            log.warn("Rejected own data exchange request: notification {} does not exist", requestDto.getNotificationId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Referenced notification does not exist.");
        }
 
        OwnDataExchangeRequest ownDataExchangeRequest = buildRequest(requestDto);
        ownDataExchangeRequest.setNotification(notification);
        ownDataExchangeRequest.setPartner(notification.getPartner());
        ownDataExchangeRequest.setSourceDisruptionId(notification.getSourceDisruptionId());
        ownDataExchangeRequest.setEffect(notification.getEffect());
        ownDataExchangeRequest.setMaterials(copyOf(notification.getMaterials()));
        ownDataExchangeRequest.setAffectedSitesSender(copyOf(notification.getAffectedSitesRecipient()));
        ownDataExchangeRequest.setAffectedSitesRecipient(copyOf(notification.getAffectedSitesSender()));
        return ownDataExchangeRequest;
    }
 
    private OwnDataExchangeRequest buildRequestWithoutNotification(DataExchangeRequestDto requestDto) {
        if (requestDto.getPartnerBpnl() == null || requestDto.getEffect() == null
                || requestDto.getAffectedMaterialNumbers() == null || requestDto.getAffectedMaterialNumbers().isEmpty()) {
            log.warn("Rejected own data exchange request: required properties are missing (partnerBpnl, leadingRootCause, effect, affectedMaterialNumbers)");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Required properties are missing: partnerBpnl, leadingRootCause, effect, affectedMaterialNumbers.");
        }
 
        Partner partner = partnerService.findByBpnl(requestDto.getPartnerBpnl());
        if (partner == null) {
            log.warn("Rejected own data exchange request: partner {} could not be found", requestDto.getPartnerBpnl());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Partner for bpnl %s could not be found.", requestDto.getPartnerBpnl()));
        }
 
        Map<String, Material> materials = new LinkedHashMap<>();
        for (String ownMaterialNumber : requestDto.getAffectedMaterialNumbers()) {
            Material material = materialService.findByOwnMaterialNumber(ownMaterialNumber);
            if (material == null) {
                log.warn("Rejected own data exchange request: material {} could not be found", ownMaterialNumber);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Material for ownMaterialNumber %s could not be found.", ownMaterialNumber));
            }
            materials.putIfAbsent(material.getOwnMaterialNumber(), material);
        }
 
        OwnDataExchangeRequest ownDataExchangeRequest = buildRequest(requestDto);
        ownDataExchangeRequest.setPartner(partner);
        ownDataExchangeRequest.setSourceDisruptionId(UUID.randomUUID());
        ownDataExchangeRequest.setEffect(requestDto.getEffect());
        ownDataExchangeRequest.setMaterials(new ArrayList<>(materials.values()));
        ownDataExchangeRequest.setAffectedSitesSender(resolveSites(partnerService.getOwnPartnerEntity(), requestDto.getAffectedSitesBpnsSender()));
        ownDataExchangeRequest.setAffectedSitesRecipient(resolveSites(partner, requestDto.getAffectedSitesBpnsRecipient()));
        return ownDataExchangeRequest;
    }
 
    private static OwnDataExchangeRequest buildRequest(DataExchangeRequestDto requestDto) {
        return OwnDataExchangeRequest.builder()
                .requestId(requestDto.getRequestId())
                .criticality(requestDto.getCriticality())
                .desiredStartDateTime(requestDto.getDesiredStartDateTime())
                .desiredEndDateTime(requestDto.getDesiredEndDateTime())
                .requestedTypes(requestDto.getRequestedTypes() != null ? new ArrayList<>(requestDto.getRequestedTypes()) : null)
                .text(requestDto.getText())
                .build();
    }
 
    private static List<Site> resolveSites(Partner owner, List<String> bpnsList) {
        List<Site> sites = new ArrayList<>();
        if (bpnsList == null) {
            return sites;
        }
        for (String bpns : new LinkedHashSet<>(bpnsList)) {
            Site site = owner.getSites().stream().filter(s -> s.getBpns().equals(bpns)).findFirst().orElse(null);
            if (site == null) {
                log.warn("Rejected own data exchange request: site {} could not be found", bpns);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Site for bpns %s could not be found.", bpns));
            }
            sites.add(site);
        }
        return sites;
    }
 
    private static <T> List<T> copyOf(List<T> list) {
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }
 
    private DataExchangeRequestDto convertReportedToDto(ReportedDataExchangeRequest entity) {
        DataExchangeRequestDto dto = convertBaseToDto(entity);
        dto.setNotificationId(entity.getNotification() != null ? entity.getNotification().getNotificationId() : null);
        return dto;
    }
 
    private DataExchangeRequestDto convertToDto(OwnDataExchangeRequest entity) {
        DataExchangeRequestDto dto = convertBaseToDto(entity);
        dto.setNotificationId(entity.getNotification() != null ? entity.getNotification().getNotificationId() : null);
        dto.setRelatedDataExchangeRequestId(entity.getRelatedDataExchangeRequest() != null ? entity.getRelatedDataExchangeRequest().getRequestId() : null);
        return dto;
    }
 
    private DataExchangeRequestDto convertBaseToDto(DataExchangeRequest entity) {
        DataExchangeRequestDto dto = modelMapper.map(entity, DataExchangeRequestDto.class);
        dto.setPartnerBpnl(entity.getPartner() != null ? entity.getPartner().getBpnl() : null);
        dto.setAffectedMaterialNumbers(entity.getMaterials() == null ? new ArrayList<>()
            : entity.getMaterials().stream().map(Material::getOwnMaterialNumber).toList());
        dto.setAffectedSitesBpnsSender(entity.getAffectedSitesSender() == null ? new ArrayList<>()
            : entity.getAffectedSitesSender().stream().map(Site::getBpns).toList());
        dto.setAffectedSitesBpnsRecipient(entity.getAffectedSitesRecipient() == null ? new ArrayList<>()
            : entity.getAffectedSitesRecipient().stream().map(Site::getBpns).toList());
        return dto;
    }
}
