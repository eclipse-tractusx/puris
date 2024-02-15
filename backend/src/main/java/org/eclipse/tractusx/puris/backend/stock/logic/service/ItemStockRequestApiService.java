/*
 * Copyright (c) 2023, 2024 Volkswagen AG
 * Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.stock.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.datatype.DT_ApiMethodEnum;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ItemStockRequestMessage;
import org.eclipse.tractusx.puris.backend.stock.logic.adapter.ItemStockSammMapper;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ItemStockRequestMessageDto;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ItemStockResponseDto;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
/**
 * This class is a Service that handles requests for MaterialItemStocks or ProductItemStocks.
 */
public class ItemStockRequestApiService {
    @Autowired
    private PartnerService partnerService;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private MaterialPartnerRelationService mprService;
    @Autowired
    private ProductItemStockService productItemStockService;
    @Autowired
    private MaterialItemStockService materialItemStockService;
    @Autowired
    private VariablesService variablesService;
    @Autowired
    private EdcAdapterService edcAdapterService;
    @Autowired
    private ItemStockSammMapper sammMapper;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ItemStockRequestMessageService itemStockRequestMessageService;

    /**
     * This method receives a request message from a customer and
     * assembles and sends a response from your local ProductItemStocks
     * to this customer.
     *
     * @param requestMessageDto the RequestMessage
     */
    public void handleRequestFromCustomer(ItemStockRequestMessageDto requestMessageDto, ItemStockRequestMessage requestMessage) {
        var requestMessageHeader = requestMessageDto.getHeader();
        Partner customerPartner = partnerService.findByBpnl(requestMessageHeader.getSenderBpn());
        if (customerPartner == null || requestMessageDto.getContent().getDirection() != DirectionCharacteristic.OUTBOUND) {
            requestMessage.setState(DT_RequestStateEnum.Error);
            itemStockRequestMessageService.update(requestMessage);
            if(requestMessageDto.getContent().getDirection() != DirectionCharacteristic.OUTBOUND){
                log.error("Wrong direction in request from customer \n" + requestMessageDto);
            }
            if(customerPartner == null) {
                log.error("Unknown Partner in request \n" + requestMessageDto);
            }
            return;
        }
        requestMessage.setState(DT_RequestStateEnum.Working);
        itemStockRequestMessageService.update(requestMessage);
        ItemStockResponseDto responseDto = new ItemStockResponseDto();
        responseDto.getHeader().setRelatedMessageId(requestMessageHeader.getMessageId());

        for (var productRequest : requestMessageDto.getContent().getItemStock()) {
            Material product = null;
            if (productRequest.getMaterialGlobalAssetId() != null) {
                product = materialService.findByMaterialNumberCx(productRequest.getMaterialGlobalAssetId());
            }
            if (product == null && productRequest.getMaterialNumberCustomer() != null) {
                var foundProducts = mprService.findAllByPartnerMaterialNumber(productRequest.getMaterialNumberCustomer());
                foundProducts = foundProducts.stream()
                    .filter(Material::isProductFlag)
                    .filter(m -> mprService.partnerOrdersProduct(m, customerPartner))
                    .toList();
                if (!foundProducts.isEmpty()) {
                    product = foundProducts.get(0);
                }
                if (foundProducts.size() > 1) {
                    log.warn("Ambiguous material definition in request \n" + productRequest);
                    log.warn("Arbitrarily choosing " + product.getOwnMaterialNumber());
                }
            }
            if (product == null && productRequest.getMaterialNumberSupplier() != null) {
                product = materialService.findByOwnMaterialNumber(productRequest.getMaterialNumberCustomer());
            }
            if (product == null) {
                log.error("Could not identify material in " + productRequest);
                continue;
            }
            var productItemStocks = productItemStockService.findByPartnerAndMaterial(customerPartner, product)
                .stream().filter(itemStock -> itemStock.getQuantity() > 0).toList();
            responseDto.getContent().getItemStock().add(sammMapper.productItemStocksToItemStockSamm(productItemStocks));
        }
        sendResponse(customerPartner, responseDto, requestMessage);

    }


    /**
     * This method receives a request message from a supplier and
     * assembles and sends a response from your local MaterialItemStocks
     * to this customer.
     *
     * @param requestMessageDto the RequestMessage
     */
    public void handleRequestFromSupplier(ItemStockRequestMessageDto requestMessageDto, ItemStockRequestMessage requestMessage) {
        var requestMessageHeader = requestMessageDto.getHeader();
        Partner supplierPartner = partnerService.findByBpnl(requestMessageHeader.getSenderBpn());
        if (supplierPartner == null || requestMessageDto.getContent().getDirection() != DirectionCharacteristic.INBOUND) {
            requestMessage.setState(DT_RequestStateEnum.Error);
            itemStockRequestMessageService.update(requestMessage);
            if(requestMessageDto.getContent().getDirection() != DirectionCharacteristic.INBOUND){
                log.error("Wrong direction in request from customer \n" + requestMessageDto);
            }
            if(supplierPartner == null) {
                log.error("Unknown Partner in request \n" + requestMessageDto);
            }
            return;
        }

        requestMessage.setState(DT_RequestStateEnum.Working);
        itemStockRequestMessageService.update(requestMessage);
        ItemStockResponseDto responseDto = new ItemStockResponseDto();
        responseDto.getHeader().setRelatedMessageId(requestMessageHeader.getMessageId());
        for (var materialRequest : requestMessageDto.getContent().getItemStock()) {
            Material material = null;
            if (materialRequest.getMaterialGlobalAssetId() != null) {
                material = materialService.findByMaterialNumberCx(materialRequest.getMaterialGlobalAssetId());
            }
            if (material == null && materialRequest.getMaterialNumberCustomer() != null) {
                material = materialService.findByOwnMaterialNumber(materialRequest.getMaterialNumberCustomer());
            }
            if (material == null && materialRequest.getMaterialNumberSupplier() != null) {
                var foundMaterials = mprService.findAllByPartnerMaterialNumber(materialRequest.getMaterialNumberSupplier());
                foundMaterials = foundMaterials.stream()
                    .filter(Material::isMaterialFlag)
                    .filter(m -> mprService.partnerSuppliesMaterial(m, supplierPartner))
                    .toList();
                if (!foundMaterials.isEmpty()) {
                    material = foundMaterials.get(0);
                }
                if (foundMaterials.size() > 1) {
                    log.warn("Ambiguous material definition in request \n" + materialRequest);
                    log.warn("Arbitrarily choosing " + material.getOwnMaterialNumber());
                }
            }
            if (material == null) {
                log.error("Could not identify material in " + materialRequest);
                continue;
            }
            var materialItemStocks = materialItemStockService.findByPartnerAndMaterial(supplierPartner, material)
                .stream().filter(itemStock -> itemStock.getQuantity() > 0).toList();
            responseDto.getContent().getItemStock().add(sammMapper.materialItemStocksToItemStockSamm(materialItemStocks));
        }
        sendResponse(supplierPartner, responseDto, requestMessage);

    }

    /**
     * Convenience method to bundle the common parts in the workflow of
     * the handleRequestFromCustomer and handleRequestFromSupplier methods.
     * The responseDto object's content is expected to be filled in advance.
     *
     * @param partner     the given Partner
     * @param responseDto the responseDto
     */
    private void sendResponse(Partner partner, ItemStockResponseDto responseDto, ItemStockRequestMessage requestMessage) {
        var data = edcAdapterService.getContractForItemStockApi(partner, DT_ApiMethodEnum.RESPONSE);
        if (data == null) {
            requestMessage.setState(DT_RequestStateEnum.Error);
            itemStockRequestMessageService.update(requestMessage);
            log.error("Failed to contract response api from " + partner.getEdcUrl());
            return;
        }
        String authKey = data[0];
        String authCode = data[1];
        String endpoint = data[2];
        var responseDtoHeader = responseDto.getHeader();
        responseDtoHeader.setMessageId(UUID.randomUUID());
        responseDtoHeader.setReceiverBpn(partner.getBpnl());
        responseDtoHeader.setSenderBpn(variablesService.getOwnBpnl());
        responseDtoHeader.setVersion(ItemStockRequestMessage.VERSION);
        responseDtoHeader.setContext(ItemStockRequestMessage.CONTEXT);
        responseDtoHeader.setSentDateTime(new Date());
        try {
            String requestBody = objectMapper.writeValueAsString(responseDto);
            var httpResponse = edcAdapterService.postProxyPullRequest(
                endpoint, authKey, authCode, requestBody);
            httpResponse.body().close();
            log.info("Sent response \n" + responseDto);
            requestMessage.setState(DT_RequestStateEnum.Completed);
        } catch (Exception e) {
            requestMessage.setState(DT_RequestStateEnum.Error);
            log.error("Failed to send response \n" + responseDto, e);
        } finally {
            log.info("Updating state");
            itemStockRequestMessageService.update(requestMessage);
        }
    }


    /**
     * This method creates and sends a request to the given supplier-partner
     * in order to receive his current ItemStocks for the given materials.
     *
     * @param supplierPartner the supplier Partner
     * @param materials       the materials
     */
    public void doRequestForMaterialItemStocks(Partner supplierPartner, Material... materials) {
        ItemStockRequestMessage itemStockRequestMessage = getItemStockRequestMessage(supplierPartner);
        itemStockRequestMessage.setDirection(DirectionCharacteristic.OUTBOUND);
        for (var material : materials) {
            ItemStockRequestMessage.Request request = new ItemStockRequestMessage.Request();
            request.setMaterialGlobalAssetId(material.getMaterialNumberCx());
            request.setMaterialNumberCustomer(material.getOwnMaterialNumber());
            request.setMaterialNumberSupplier(mprService.find(material, supplierPartner).getPartnerMaterialNumber());
            itemStockRequestMessage.getItemStock().add(request);
        }
        sendRequest(supplierPartner, itemStockRequestMessage);
    }

    /**
     * This method creates and sends a request to the given customer-partner
     * in order to receive his current ItemStocks for the given materials.
     *
     * @param customerPartner the supplier Partner
     * @param materials       the materials
     */
    public void doRequestForProductItemStocks(Partner customerPartner, Material... materials) {
        ItemStockRequestMessage itemStockRequestMessage = getItemStockRequestMessage(customerPartner);
        itemStockRequestMessage.setDirection(DirectionCharacteristic.INBOUND);
        for (var material : materials) {
            ItemStockRequestMessage.Request request = new ItemStockRequestMessage.Request();
            request.setMaterialGlobalAssetId(material.getMaterialNumberCx());
            request.setMaterialNumberSupplier(material.getOwnMaterialNumber());
            request.setMaterialNumberCustomer(mprService.find(material, customerPartner).getPartnerMaterialNumber());
            itemStockRequestMessage.getItemStock().add(request);
        }
        sendRequest(customerPartner, itemStockRequestMessage);

    }

    private ItemStockRequestMessage getItemStockRequestMessage(Partner partner) {
        Partner mySelf = partnerService.getOwnPartnerEntity();
        ItemStockRequestMessage.Key key = new ItemStockRequestMessage.Key(UUID.randomUUID(), mySelf.getBpnl(), partner.getBpnl());
        while (itemStockRequestMessageService.find(key) != null) {
            key.setMessageId(UUID.randomUUID());
        }
        ItemStockRequestMessage itemStockRequestMessage = new ItemStockRequestMessage();
        itemStockRequestMessage.setKey(key);
        return itemStockRequestMessage;
    }

    /**
     * Convenience method to bundle the common parts in the workflow of
     * the doRequestForMaterialItemStocks and doRequestForProductItemStocks methods.
     *
     * @param partner                 the partner
     * @param itemStockRequestMessage the itemStockRequestMessage
     */
    private void sendRequest(Partner partner, ItemStockRequestMessage itemStockRequestMessage) {
        itemStockRequestMessage = itemStockRequestMessageService.create(itemStockRequestMessage);
        log.info("Created Message : \n" + (itemStockRequestMessage != null));
        itemStockRequestMessage.setState(DT_RequestStateEnum.Working);
        String[] data = edcAdapterService.getContractForItemStockApi(partner, DT_ApiMethodEnum.REQUEST);
        if (data == null) {
            log.error("Failed to obtain request api from " + partner.getEdcUrl());
            itemStockRequestMessage.setState(DT_RequestStateEnum.Error);
            itemStockRequestMessageService.update(itemStockRequestMessage);
            return;
        }
        String authKey = data[0];
        String authCode = data[1];
        String endpoint = data[2];

        try {
            itemStockRequestMessage.setSentDateTime(new Date());
            var requestDto = ItemStockRequestMessageDto.convertToDto(itemStockRequestMessage);
            var response = edcAdapterService.postProxyPullRequest(endpoint, authKey, authCode,
                objectMapper.writeValueAsString(requestDto));
            if (response.isSuccessful()) {
                itemStockRequestMessage.setState(DT_RequestStateEnum.Requested);
            } else {
                itemStockRequestMessage.setState(DT_RequestStateEnum.Error);
            }
            itemStockRequestMessageService.update(itemStockRequestMessage);
            response.body().close();
            log.info("Sent request \n" + requestDto);
        } catch (Exception e) {
            itemStockRequestMessage.setState(DT_RequestStateEnum.Error);
            itemStockRequestMessageService.update(itemStockRequestMessage);
            log.error("Failed to send request \n" + itemStockRequestMessage, e);
        }
    }

}
