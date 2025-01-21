/*
 * Copyright (c) 2024 Volkswagen AG
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.puris.backend.supply.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.eclipse.tractusx.puris.backend.supply.domain.model.Supply;
import org.eclipse.tractusx.puris.backend.supply.logic.dto.SupplyDto;
import org.eclipse.tractusx.puris.backend.supply.logic.service.CustomerSupplyService;
import org.eclipse.tractusx.puris.backend.supply.logic.service.SupplierSupplyService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("days-of-supply")
public class SupplyController {
    @Autowired
    private CustomerSupplyService customerSupplyService;
    @Autowired
    private SupplierSupplyService supplierSupplyService;

    @Autowired
    private ModelMapper modelMapper;

    @GetMapping("customer")
    @ResponseBody
    @Operation(summary = "Calculate days of supply for customer for given number of days.",
        description = "Calculate days of supply for customer for given number of days. Filtered by given material number, partner bpnl and site bpns. " +
            "materialNumber is expected to be base64 encoded")
    public List<SupplyDto> calculateCustomerDaysOfSupply(String materialNumber, Optional<String> bpnl, Optional<String> siteBpns, int numberOfDays) {
        materialNumber = new String(Base64.getDecoder().decode(materialNumber.getBytes(StandardCharsets.UTF_8)));
        return customerSupplyService.calculateCustomerDaysOfSupply(materialNumber, bpnl, siteBpns, numberOfDays)
            .stream().map(this::convertToDto).toList();
    }

    @GetMapping("customer/reported")
    @Operation(summary = "Get days of supply for customer.", 
        description = "Get days of supply for customer for given material number and partner bpnl. " +
            "materialNumber is expected to be base64 encoded")
    public List<SupplyDto> getCustomerDaysOfSupply(String materialNumber, String bpnl) {
        materialNumber = new String(Base64.getDecoder().decode(materialNumber.getBytes(StandardCharsets.UTF_8)));
        return customerSupplyService.findByPartnerBpnlAndOwnMaterialNumber(materialNumber, bpnl)
            .stream().map(this::convertToDto).toList();
    }

    @GetMapping("supplier")
    @Operation(summary = "Calculate days of supply for supplier for given number of days.",
        description = "Calculate days of supply for supplier for given number of days. Filtered by given material number, partner bpnl and site bpns. "+
            "materialNumber is expected to be base64 encoded")
    public List<SupplyDto> calculateSupplierDaysOfSupply(String materialNumber, Optional<String> bpnl, Optional<String> siteBpns, int numberOfDays) {
        materialNumber = new String(Base64.getDecoder().decode(materialNumber.getBytes(StandardCharsets.UTF_8)));
        return supplierSupplyService.calculateSupplierDaysOfSupply(materialNumber, bpnl, siteBpns, numberOfDays)
            .stream().map(this::convertToDto).toList();
    }

    @GetMapping("supplier/reported")
    @Operation(summary = "Get days of supply for supplier.", 
        description = "Get days of supply for supplier for given material number and partner bpnl. " +
            "materialNumber is expected to be base64 encoded")
    public List<SupplyDto> getSupplierDaysOfSupply(String materialNumber, String bpnl) {
        materialNumber = new String(Base64.getDecoder().decode(materialNumber.getBytes(StandardCharsets.UTF_8)));
        return supplierSupplyService.findByPartnerBpnlAndOwnMaterialNumber(materialNumber, bpnl)
            .stream().map(this::convertToDto).toList();
    }
    
    private SupplyDto convertToDto(Supply entity) {
        SupplyDto dto = modelMapper.map(entity, SupplyDto.class);
        
        return dto;
    }
}
