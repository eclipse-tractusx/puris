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
package org.eclipse.tractusx.puris.backend.aggregateddata.controller;

import java.util.Base64;
import java.util.List;

import org.eclipse.tractusx.puris.backend.aggregateddata.domain.model.AggregatedMaterialData;
import org.eclipse.tractusx.puris.backend.aggregateddata.logic.dto.AggregatedMaterialDataDto;
import org.eclipse.tractusx.puris.backend.aggregateddata.logic.service.AggregatedMaterialDataService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("aggregated-data")
@Slf4j
public class AggregatedMaterialDataController {
    @Autowired
    private AggregatedMaterialDataService aggregatedMaterialDataService;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private ObjectMapper mapper;

    @GetMapping()
    @ResponseBody
    @Operation(summary = "Get all aggregated data for the given material", description = "Get all aggregated data reported by partners for the given material number.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ok"),
        @ApiResponse(responseCode = "400", description = "Incorrect material number.", content = @Content),
        @ApiResponse(responseCode = "404", description = "Material does not exist.", content = @Content)
    })
    public ResponseEntity<List<AggregatedMaterialDataDto>> getAggregatedData(@RequestParam @Parameter String ownMaterialNumber) {
        try {
            ownMaterialNumber = new String(Base64.getDecoder().decode(ownMaterialNumber));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect material number.");
        }
        Material material = materialService.findByOwnMaterialNumber(ownMaterialNumber);
        if (material == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Material does not exist.");
        }

        var result = aggregatedMaterialDataService.findAllByMaterial_OwnMaterialNumber(ownMaterialNumber).stream().map(this::convertToDto).toList();
 
        return ResponseEntity.ok(result);

    }

    private AggregatedMaterialDataDto convertToDto(AggregatedMaterialData data) {
        var dto = mapper.convertValue(data, AggregatedMaterialDataDto.class);
        dto.setOwnMaterialNumber(data.getMaterial() == null ? null : data.getMaterial().getOwnMaterialNumber());
        return dto;
    }

}
