/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Contributors to the Eclipse Foundation

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
package org.eclipse.tractusx.puris.backend.demandandcapacitynotification.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.demand.logic.services.DemandRequestApiService;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.OwnDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.dto.DemandAndCapacityNotificationDto;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.service.DemandAndCapacityNotifcationRequestApiService;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.service.OwnDemandAndCapacityNotificationService;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.service.ReportedDemandAndCapacityNotificationService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Site;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.PartnerDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("demand-and-capacity-notification")
public class DemandAndCapacityNotificationController {
    @Autowired
    private OwnDemandAndCapacityNotificationService ownNotificationService;

    @Autowired
    private ReportedDemandAndCapacityNotificationService reportedNotificationService;

    @Autowired
    private DemandAndCapacityNotifcationRequestApiService demandAndCapacityNotifcationRequestApiService;

    @Autowired
    private MaterialService materialService;

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private Validator validator;

    @Autowired
    private ExecutorService executorService;

    @GetMapping()
    @ResponseBody
    @Operation(summary = "Get all own notifications", description = "Get all own notifications. Optionally the partner can be filtered by its bpnl.")
    public List<DemandAndCapacityNotificationDto> getAllNotifications(Optional<@Pattern(regexp = PatternStore.BPNL_STRING) String> partnerBpnl) {
        if (partnerBpnl.isEmpty()) {
            return ownNotificationService.findAll().stream().map(this::convertToDto).collect(Collectors.toList());
        } else {
            return ownNotificationService.findAllByPartnerBpnl(partnerBpnl.get()).stream().map(this::convertToDto).collect(Collectors.toList());
        }
    }

    @PostMapping()
    @ResponseBody
    @Operation(summary = "Creates a new notification")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Notification was created."),
            @ApiResponse(responseCode = "400", description = "Malformed or invalid request body."),
            @ApiResponse(responseCode = "409", description = "Notification already exists. Use PUT instead."),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    @ResponseStatus(HttpStatus.CREATED)
    public DemandAndCapacityNotificationDto createNotification(@RequestBody DemandAndCapacityNotificationDto notificationDto) {
        if (!validator.validate(notificationDto).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Notification.");
        }

        if (notificationDto.getPartnerBpnl() == null || notificationDto.getPartnerBpnl().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Notification Information misses partner identification.");
        }

        try {
            var entity = ownNotificationService.create(convertToEntity(notificationDto));
            executorService.submit(() -> demandAndCapacityNotifcationRequestApiService.sendDemandAndCapacityNotification(entity));
            return convertToDto(entity);
        } catch (KeyAlreadyExistsException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Notification already exists. Use PUT instead.");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Notification is invalid.");
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping()
    @Operation(summary = "Updates a notification by its UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notification was updated."),
            @ApiResponse(responseCode = "400", description = "Malformed or invalid request body."),
            @ApiResponse(responseCode = "404", description = "Notification does not exist."),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    @ResponseStatus(HttpStatus.OK)
    public DemandAndCapacityNotificationDto updateNotification(@RequestBody DemandAndCapacityNotificationDto dto) {
        OwnDemandAndCapacityNotification updatedNotification = ownNotificationService.update(convertToEntity(dto));
        if (updatedNotification == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification does not exist.");
        }
        executorService.submit(() -> demandAndCapacityNotifcationRequestApiService.sendDemandAndCapacityNotification(updatedNotification));
        return convertToDto(updatedNotification);
    }

    @DeleteMapping("{id}")
    @Operation(summary = "Deletes a notification by its UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Notification was deleted."),
            @ApiResponse(responseCode = "400", description = "Malformed or invalid request body."),
            @ApiResponse(responseCode = "404", description = "Notification does not exist."),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNotification(@PathVariable UUID id) {
        OwnDemandAndCapacityNotification demand = ownNotificationService.findById(id);
        if (demand == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification does not exist.");
        }
        ownNotificationService.delete(id);
    }

    @GetMapping("reported")
    @ResponseBody
    @Operation(summary = "Get all reported notifications", description = "Get all reported notifications. Optionally the partner can be filtered by its bpnl.")
    public List<DemandAndCapacityNotificationDto> getAllReportedNotifications(Optional<@Pattern(regexp = PatternStore.BPNL_STRING) String> partnerBpnl) {
        if (partnerBpnl.isEmpty()) {
            return reportedNotificationService.findAll().stream().map(this::convertToDto).collect(Collectors.toList());
        } else {
            return reportedNotificationService.findAllByPartnerBpnl(partnerBpnl.get()).stream().map(this::convertToDto).collect(Collectors.toList());
        }
    }

    private DemandAndCapacityNotificationDto convertToDto(OwnDemandAndCapacityNotification entity) {
        DemandAndCapacityNotificationDto dto = modelMapper.map(entity, DemandAndCapacityNotificationDto.class);
        if (entity.getMaterials() != null) {
            dto.setAffectedMaterialNumbers(entity.getMaterials().stream().map(Material::getOwnMaterialNumber).toList());
        }
        if (entity.getAffectedSitesRecipient() != null) {
            dto.setAffectedSitesBpnsRecipient(entity.getAffectedSitesRecipient().stream().map(Site::getBpns).toList());
        }
        if (entity.getAffectedSitesSender() != null) {
            dto.setAffectedSitesBpnsSender(entity.getAffectedSitesSender().stream().map(Site::getBpns).toList());
        }
        dto.setPartnerBpnl(entity.getPartner().getBpnl());
        dto.setPartnerBpnl(entity.getPartner().getBpnl());
        dto.setReported(false);
        return dto;
    }

    private DemandAndCapacityNotificationDto convertToDto(ReportedDemandAndCapacityNotification entity) {
        DemandAndCapacityNotificationDto dto = modelMapper.map(entity, DemandAndCapacityNotificationDto.class);
        if (entity.getMaterials() != null) {
            dto.setAffectedMaterialNumbers(entity.getMaterials().stream().map(Material::getOwnMaterialNumber).toList());
        }
        if (entity.getAffectedSitesRecipient() != null) {
            dto.setAffectedSitesBpnsRecipient(entity.getAffectedSitesRecipient().stream().map(Site::getBpns).toList());
        }
        if (entity.getAffectedSitesSender() != null) {
            dto.setAffectedSitesBpnsSender(entity.getAffectedSitesSender().stream().map(Site::getBpns).toList());
        }
        dto.setPartnerBpnl(entity.getPartner().getBpnl());
        return dto;
    }

    private OwnDemandAndCapacityNotification convertToEntity(DemandAndCapacityNotificationDto dto) {
        OwnDemandAndCapacityNotification entity = modelMapper.map(dto, OwnDemandAndCapacityNotification.class);

        Partner existingPartner = partnerService.findByBpnl(dto.getPartnerBpnl());
        if (existingPartner == null) {
            throw new IllegalStateException(String.format(
                    "Partner for bpnl %s could not be found",
                    dto.getPartnerBpnl()));
        }
        entity.setPartner(existingPartner);

        List<Material> materials = new ArrayList<>();
        for (String ownMaterialNumber : dto.getAffectedMaterialNumbers()) {
            Material material = materialService.findByOwnMaterialNumber(ownMaterialNumber);
            if (material == null) {
                throw new IllegalStateException(String.format(
                        "Material for ownMaterialNumber %s could not be found",
                        ownMaterialNumber));
            }
            materials.add(material);
        }
        entity.setMaterials(materials);

        List<Site> affectedSitesRecipient = new ArrayList<>();
        for (String bpns : dto.getAffectedSitesBpnsRecipient()) {
            Site site = existingPartner.getSites().stream().filter(p -> p.getBpns().equals(bpns)).findFirst()
                    .orElse(null);
            if (site == null) {
                throw new IllegalStateException(String.format(
                        "Site for bpns %s could not be found",
                        bpns));
            }
            affectedSitesRecipient.add(site);
        }
        entity.setAffectedSitesRecipient(affectedSitesRecipient);

        Partner ownPartner = partnerService.getOwnPartnerEntity();
        List<Site> affectedSitesSender = new ArrayList<>();
        for (String bpns : dto.getAffectedSitesBpnsSender()) {
            Site site = ownPartner.getSites().stream().filter(p -> p.getBpns().equals(bpns)).findFirst().orElse(null);
            if (site == null) {
                throw new IllegalStateException(String.format(
                        "Site for bpns %s could not be found",
                        bpns));
            }
            affectedSitesSender.add(site);
        }
        entity.setAffectedSitesSender(affectedSitesSender);

        if (dto.getRelatedNotificationId() != null) {
            OwnDemandAndCapacityNotification relatedNotification = ownNotificationService.findById(dto.getRelatedNotificationId());
            if (relatedNotification == null) {
                throw new IllegalStateException(String.format(
                        "Related notification for UUID %s could not be found",
                        dto.getRelatedNotificationId()));
            }
        }

        return entity;
    }
}
