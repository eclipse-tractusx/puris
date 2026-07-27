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
package org.eclipse.tractusx.puris.backend.aggregateddata.logic.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.delivery.logic.dto.anonymizeddeliverysamm.DeliveryAnonymized;
import org.eclipse.tractusx.puris.backend.production.logic.dto.anonymizedplannedproductionsamm.AllocatedPlannedProductionOutputAnonymized;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.anonymizeditemstocksamm.AllocatedStockAnonymized;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class AggregatedMaterialDataNodeDto implements Serializable {
 
    private UUID uuid;
 
    private String externalMaterialNumber;
 
    private String externalMaterialName;
 
    private double quantity;
 
    private ItemUnitEnumeration measurementUnit;
 
    @Valid
    private Set<AllocatedPlannedProductionOutputAnonymized> productions = new HashSet<>();
 
    @Valid
    private Set<DeliveryAnonymized> deliveries = new HashSet<>();
 
    @Valid
    private Set<AllocatedStockAnonymized> stocks = new HashSet<>();
 
    @Valid
    private List<AggregatedMaterialDataNodeDto> childMaterialData = new ArrayList<>();
}

