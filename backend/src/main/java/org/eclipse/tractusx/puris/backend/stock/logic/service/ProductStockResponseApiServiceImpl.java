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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStockRequest;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.MessageContentDto;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.MessageContentErrorDto;
import org.eclipse.tractusx.puris.backend.common.api.logic.dto.ResponseDto;
import org.eclipse.tractusx.puris.backend.common.api.logic.service.ResponseApiService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.PartnerProductStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStockResponse;
import org.eclipse.tractusx.puris.backend.stock.logic.adapter.ProductStockSammMapper;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.PartnerProductStockDto;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.samm.ProductStockSammDto;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Service implements the handling of a response for Product Stock
 * <p>
 * That means that one need to save
 * {@link org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStock} according to the
 * API specification.
 */
@Component
@Slf4j
public class ProductStockResponseApiServiceImpl {

    @Autowired
    private ProductStockRequestService productStockRequestService;

    @Autowired
    private PartnerProductStockService partnerProductStockService;

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private ProductStockSammMapper productStockSammMapper;

    @Autowired
    private ModelMapper modelMapper;


    public void consumeResponse(ProductStockResponse response) {

//        ProductStockRequest correspondingProductStockRequest = findCorrespondingRequest(responseDto);
        ProductStockRequest correspondingProductStockRequest = productStockRequestService.findRequestByHeaderUuid(response.getHeader().getRequestId());
        if (correspondingProductStockRequest == null) {
            log.error("Received Response without corresponding request");
            return;
        }
        Partner partner = partnerService.findByBpnl(response.getHeader().getSender());
        for (ProductStockSammDto sammDto : response.getContent().getProductStocks()) {

                PartnerProductStockDto partnerProductStockDto =
                        productStockSammMapper.fromSamm(sammDto, partner);
                // check whether a new PartnerProductStock must be created
                // or whether an update is sufficient.
                List<PartnerProductStock> existingPartnerProductStocks =
                    partnerProductStockService.findAllByOwnMaterialNumberAndPartnerUuid(
                        partnerProductStockDto.getMaterial().getMaterialNumberCustomer(),
                        partnerProductStockDto.getSupplierPartner().getUuid());

                // currently we only accept a one to one mapping of partner - material - stock -site
                // therefore the can only be one PartnerProductStock
                // if > 0 -> IllegalState
                if (existingPartnerProductStocks.size() > 1) {
                    throw new IllegalStateException(String.format("There exist %d " +
                                    "PartnerProductStocks for material uuid %s and supplier partner uuid " +
                                    "%s", existingPartnerProductStocks.size(),
                            partnerProductStockDto.getMaterial().getUuid(),
                            partnerProductStockDto.getSupplierPartner().getUuid()));
                }

                // Create or update
                if (existingPartnerProductStocks.isEmpty()) {
                    PartnerProductStock createdPartnerProductStock =
                            partnerProductStockService.create(modelMapper.map(partnerProductStockDto,
                                    PartnerProductStock.class));
                    log.info(String.format("Created Partner ProductStock from SAMM: %s",
                            createdPartnerProductStock));
                } else {
                    PartnerProductStock updatedPartnerProductStock =
                            partnerProductStockService.update(existingPartnerProductStocks.get(0));
                    log.info(String.format("Updated Partner ProductStock from SAMM: %s",
                            updatedPartnerProductStock));
                }
        }
        // Update status - also only MessageContentErrorDtos would be completed
        productStockRequestService.updateState(correspondingProductStockRequest, DT_RequestStateEnum.COMPLETED);
    }

    private ProductStockRequest findCorrespondingRequest(ResponseDto responseDto) {
        UUID requestId = responseDto.getHeader().getRequestId();
        return productStockRequestService.findRequestByHeaderUuid(requestId);

    }

}
