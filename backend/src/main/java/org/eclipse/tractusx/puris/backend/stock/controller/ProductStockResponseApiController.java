/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.puris.backend.stock.controller;

import org.eclipse.tractusx.puris.backend.common.api.controller.ResponseApiController;
import org.eclipse.tractusx.puris.backend.common.api.logic.service.ResponseApiService;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ProductStockResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("product-stock")
@Slf4j
public class ProductStockResponseApiController extends ResponseApiController {

    public ProductStockResponseApiController(ResponseApiService responseApiService) {
        super(responseApiService);
    }


    @PostMapping("response")
    public ResponseEntity postResponse(@RequestBody ProductStockResponseDto productStockResponseDto) {
        log.info("product-stock/response called");
        
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            var jsonString = objectMapper.writeValueAsString(productStockResponseDto);
            jsonString = objectMapper.readTree(jsonString).toPrettyString();
            log.info("\n" + jsonString);
        } catch (Exception e){

        }
        return super.postResponse(productStockResponseDto);
    }

}
