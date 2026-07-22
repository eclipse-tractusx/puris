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
package org.eclipse.tractusx.puris.backend.aggregateddata.domain.controller;

import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.tractusx.puris.backend.aggregateddata.domain.model.ChildAggregatedData;
import org.eclipse.tractusx.puris.backend.aggregateddata.domain.model.PartnerAggregatedData;
import org.eclipse.tractusx.puris.backend.aggregateddata.logic.dto.AggregatedDataDto;
import org.eclipse.tractusx.puris.backend.aggregateddata.logic.service.PartnerAggregatedDataService;
import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemQuantityEntity;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.ReportedAnonymizedDelivery;
import org.eclipse.tractusx.puris.backend.delivery.logic.dto.anonymizeddeliverysamm.DeliveryAnonymized;
import org.eclipse.tractusx.puris.backend.delivery.logic.dto.deliverysamm.TransitEvent;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.production.domain.model.ReportedAnonymizedProduction;
import org.eclipse.tractusx.puris.backend.production.logic.dto.anonymizedplannedproductionsamm.AllocatedPlannedProductionOutputAnonymized;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ReportedAnonymizedStock;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.anonymizeditemstocksamm.AllocatedStockAnonymized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("aggregated-data")
@Slf4j
public class AggregatedDataController {

    @Autowired
    private PartnerAggregatedDataService partnerAggregatedDataService;

    @Autowired
    private MaterialService materialService;

    @GetMapping()
    @ResponseBody
    @Operation(summary = "Get all aggregated data for the given material",
        description = "Get all aggregated data reported by partners for the given material number.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ok"),
        @ApiResponse(responseCode = "400", description = "Malformed material number.", content = @Content),
        @ApiResponse(responseCode = "404", description = "Material does not exist.", content = @Content)
    })
    public List<AggregatedDataDto> getAggregatedData(
            @RequestParam @Parameter(description = "encoded in base64") String ownMaterialNumber) {
        try {
            ownMaterialNumber = new String(Base64.getDecoder().decode(ownMaterialNumber));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed material number.");
        }
        Material material = materialService.findByOwnMaterialNumber(ownMaterialNumber);
        if (material == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Material does not exist.");
        }

        return partnerAggregatedDataService.findAllByMaterial(material).stream().map(this::convertToDto).toList();
    }

    private AggregatedDataDto convertToDto(PartnerAggregatedData entity) {
        AggregatedDataDto dto = new AggregatedDataDto();
        dto.setUuid(entity.getUuid());
        dto.setDeliveries(entity.getDeliveries().stream().map(this::convertToDeliveryAnonymized)
                .collect(Collectors.toSet()));
        dto.setStocks(entity.getStocks().stream()
                .map(this::convertToAllocatedStockAnonymized)
                .collect(Collectors.toSet()));
        dto.setProductions(entity.getProductions().stream()
                .map(this::convertToAllocatedPlannedProductionOutputAnonymized)
                .collect(Collectors.toSet()));
        dto.setChildDataIds(entity.getChildData().stream().map(ChildAggregatedData::getUuid).toList());
        return dto;
    }

    private DeliveryAnonymized convertToDeliveryAnonymized(ReportedAnonymizedDelivery entity) {
        Set<TransitEvent> transitEvents = new HashSet<>();
        if (entity.getDepartureType() != null && entity.getDateOfDeparture() != null) {
            transitEvents.add(new TransitEvent(entity.getDateOfDeparture(), entity.getDepartureType()));
        }
        if (entity.getArrivalType() != null && entity.getDateOfArrival() != null) {
            transitEvents.add(new TransitEvent(entity.getDateOfArrival(), entity.getArrivalType()));
        }
        return new DeliveryAnonymized(
            new ItemQuantityEntity(entity.getQuantity(), entity.getMeasurementUnit()),
            entity.getLastUpdatedOnDateTime(),
            transitEvents,
            entity.getOriginBpnsAnonymized(),
            entity.getDestinationBpnsAnonymized());
    }

    private AllocatedStockAnonymized convertToAllocatedStockAnonymized(ReportedAnonymizedStock entity) {
        return new AllocatedStockAnonymized(
            new ItemQuantityEntity(entity.getQuantity(), entity.getMeasurementUnit()),
            entity.getStockLocationBpnsAnonymized(),
            entity.isBlocked(),
            entity.getLastUpdatedOnDateTime());
    }

    private AllocatedPlannedProductionOutputAnonymized convertToAllocatedPlannedProductionOutputAnonymized(ReportedAnonymizedProduction entity) {
        return new AllocatedPlannedProductionOutputAnonymized(
            new ItemQuantityEntity(entity.getQuantity(), entity.getMeasurementUnit()),
            entity.getProductionSiteBpnsAnonymized(),
            entity.getEstimatedTimeOfCompletion(),
            entity.getLastUpdatedOnDateTime());
    }
}
