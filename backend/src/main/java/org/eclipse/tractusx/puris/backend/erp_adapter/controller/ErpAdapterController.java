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

package org.eclipse.tractusx.puris.backend.erp_adapter.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.tractusx.puris.backend.erp_adapter.logic.service.ItemStockErpAdapterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("erp_adapter")
@Slf4j
public class ErpAdapterController {

    @Autowired
    private ItemStockErpAdapterService itemStockErpAdapterService;

    @PutMapping
    public ResponseEntity<?> putMethod(
        @RequestParam("request_id") UUID requestId,
        @RequestParam("bpnl") String partnerBpnl,
        @RequestParam("response_type") String responseType,
        @RequestParam("samm_version") String sammVersion,
        @RequestParam(value = "response_timestamp")
        @Parameter(example = "2024-05-28T15:00:00")
        @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") Date responseTimestamp,
        @RequestBody JsonNode requestBody
        ) {
        Dto dto = new Dto(requestId, partnerBpnl, responseType, sammVersion, responseTimestamp, requestBody);
        switch (responseType.toLowerCase()) {
            case "itemstock" -> itemStockErpAdapterService.receiveItemStockUpdate(dto);
            default -> {
                return ResponseEntity.status(501).body("Unsupported response type: " + responseType);
            }
        }

        return ResponseEntity.ok("ok");
    }

    public record Dto(UUID requestId, String partnerBpnl, String responseType, String sammVersion,
                      Date responseTimeStamp, JsonNode responseBody ){}
}
