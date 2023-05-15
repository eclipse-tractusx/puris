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
package org.eclipse.tractusx.puris.backend.stock.logic.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.api.controller.exception.RequestIdNotFoundException;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.Request;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_UseCaseEnum;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.MessageContentDto;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.RequestDto;
import org.eclipse.tractusx.puris.backend.common.api.logic.service.RequestApiService;
import org.eclipse.tractusx.puris.backend.common.api.logic.service.RequestService;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.datatype.DT_ApiBusinessObjectEnum;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.datatype.DT_ApiMethodEnum;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.datatype.DT_AssetTypeEnum;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.stock.logic.adapter.ProductStockSammMapper;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ProductStockRequestForMaterialDto;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * Service implements the handling of a request for Product Stock
 * <p>
 * That means that one need to lookup
 * {@link org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStock} and return it
 * according to the API speicfication.
 */
@Component
@Slf4j
public class ProductStockRequestApiServiceImpl implements RequestApiService {

    @Autowired
    private RequestService requestService;

    @Autowired
    private ProductStockService productStockService;

    @Autowired
    private ProductStockSammMapper productStockSammMapper;

    @Autowired
    private EdcAdapterService edcAdapterService;

    @Autowired
    private ModelMapper modelMapper;

    private ObjectMapper objectMapper;

    public ProductStockRequestApiServiceImpl(ObjectMapper objectMapper) {
        super();
        this.objectMapper = objectMapper;
    }

    @Override
    public void handleRequest(RequestDto requestDto) {
        //request has been created on post
        Request correspondingRequest = findCorrespondingRequest(requestDto);

        // as soon as we're working on it, we need to set the state to working.
        correspondingRequest = requestService.updateState(correspondingRequest, DT_RequestStateEnum.WORKING);

        // determine Asset for partners Response API
        Map<String, String> filterProperties = new HashMap<>();
        // use shortcut with headers.responseAssetId, if given
        if (requestDto.getHeader().getRespondAssetId() != null) {
            filterProperties.put("asset:prop:id", requestDto.getHeader().getRespondAssetId());
        } else {
            filterProperties.put("asset:prop:usecase", DT_UseCaseEnum.PURIS.name());
            filterProperties.put("asset:prop:type", DT_AssetTypeEnum.API.name());
            filterProperties.put("asset:prop:apibusinessobject", DT_ApiBusinessObjectEnum.productStock.name());
            filterProperties.put("asset:prop:apimethod", DT_ApiMethodEnum.RESPONSE.name());
        }
        String partnerIdsUrl = requestDto.getHeader().getSenderEdc();
        String catalog = null;
        try {
            catalog = edcAdapterService.getCatalog(partnerIdsUrl, Optional.of(filterProperties));
        } catch (IOException e) {
            correspondingRequest = requestService.updateState(correspondingRequest,
                    DT_RequestStateEnum.ERROR);
            log.error(String.format("Catalog for %s for partner %s for request (external =%s ; " +
                            "internal=%s) could not be reached.", partnerIdsUrl,
                    requestDto.getHeader().getSender(), requestDto.getHeader().getRequestId(),
                    requestDto.getUuid()));
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }

        // contains either productStockSamms or messageContentError
        List<MessageContentDto> resultProductStocks = new ArrayList<>();

        for (MessageContentDto messageContentDto : requestDto.getPayload()) {

            if (messageContentDto instanceof ProductStockRequestForMaterialDto) {

                // TODO determine data

                // TODO A) map to samm

                // TODO B) create error

            } else
                throw new IllegalStateException(String.format("Message Content is unknown: %s",
                        messageContentDto));

        }

        // TODO: Init Transfer
        JsonNode catalogNode = objectMapper.valueToTree(catalog);
        JsonNode contractOfferJson = catalogNode.get("contractoffers").get(0);
        // TODO: Does a request need a response-contract-agreement id so that I can determine the
        //  http proxy EndpointDataReference?

        // TODO: async wait for answer of backend

        // TODO: send response

        // Update status - also only MessageContentErrorDtos would be completed
        requestService.updateState(correspondingRequest, DT_RequestStateEnum.COMPLETED);
    }

    private Request findCorrespondingRequest(RequestDto requestDto) {
        UUID requestId = requestDto.getHeader().getRequestId();

        Request requestFound =
                requestService.findRequestByHeaderUuid(requestId);

        if (requestFound == null) {
            throw new RequestIdNotFoundException(requestId);
        } else return null;

    }
}
