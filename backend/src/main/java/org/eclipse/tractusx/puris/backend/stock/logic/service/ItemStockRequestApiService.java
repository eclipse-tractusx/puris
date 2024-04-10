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
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.datatype.DT_ApiMethodEnum;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ItemStockRequestMessage;
import org.eclipse.tractusx.puris.backend.stock.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.stock.logic.adapter.ItemStockSammMapper;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ItemStockRequestMessageDto;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ItemStockResponseDto;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.ItemStockSamm;
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
    private ReportedProductItemStockService reportedProductItemStockService;
    @Autowired
    private ReportedMaterialItemStockService reportedMaterialItemStockService;
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

    public ItemStockSamm handleItemStockSubmodelRequest(String bpnl, String materialNumber, DirectionCharacteristic direction) {
        Partner partner = partnerService.findByBpnl(bpnl);
        if (partner == null) {
            log.error("Unknown Partner BPNL " + bpnl);
            return null;
        }
        switch (direction) {
            case OUTBOUND -> {
                // Partner is customer, requesting our ProductItemStocks for him
                // materialNumber is own CX id:
                Material material = materialService.findByMaterialNumberCx(materialNumber);
                if (material != null) {
                    var currentStocks = productItemStockService.findByPartnerAndMaterial(partner, material);
                    return sammMapper.productItemStocksToItemStockSamm(currentStocks);
                }
                return null;
            }
            case INBOUND -> {
                // Partner is supplier, requesting our MaterialItemStocks from him
                // materialNumber is partner's CX id:
                Material material = mprService.findByPartnerAndPartnerCXNumber(partner, materialNumber).getMaterial();
                if (material != null) {
                    var currentStocks = materialItemStockService.findByPartnerAndMaterial(partner, material);
                    return sammMapper.materialItemStocksToItemStockSamm(currentStocks);
                }
                // Could not identify partner cx number. I.e. we do not have that partner's
                // CX id in one of our MaterialPartnerRelation entities. Try to fix this by
                // looking for MPR's, where that partner is a supplier and where we don't have
                // a partnerCXId yet. Of course this can only work if there was previously an MPR
                // created, but for some unforeseen reason, the initial PartTypeRetrieval didn't succeed.
                log.warn("Could not find " + materialNumber + " from partner " + partner.getBpnl());
                mprService.findAllMaterialsThatPartnerSupplies(partner).stream()
                    .map(mat -> mprService.find(mat, partner))
                    .filter(mpr -> mpr.isPartnerSuppliesMaterial() && mpr.getPartnerCXNumber() == null)
                    .forEach(mpr -> mprService.triggerPartTypeRetrievalTask(mpr));
                return null;
            }
            default -> {
                return null;
            }
        }

    }

    public void doItemStockSubmodelReportedMaterialItemStockRequest(Partner partner, Material material) {
        try {
            var mpr = mprService.find(material, partner);
            var data = edcAdapterService.doItemStockSubmodelRequest(mpr, DirectionCharacteristic.OUTBOUND);
            var samm = objectMapper.treeToValue(data, ItemStockSamm.class);
            var stocks = sammMapper.itemStockSammToReportedMaterialItemStock(samm, partner);
            for (var stock : stocks) {
                var stockPartner = stock.getPartner();
                var stockMaterial = stock.getMaterial();
                if (!partner.equals(stockPartner) || !material.equals(stockMaterial)) {
                    log.warn("Received inconsistent data from " + partner.getBpnl() + "\n" + stocks);
                    return;
                }
            }
            var oldStocks = reportedMaterialItemStockService.findByPartnerAndMaterial(partner, material);
            for (var oldStock : oldStocks) {
                reportedMaterialItemStockService.delete(oldStock.getUuid());
            }
            for (var newStock : stocks) {
                reportedMaterialItemStockService.create(newStock);
            }
            log.info("Updated ReportedMaterialItemStocks for " + material.getOwnMaterialNumber() + " and partner " + partner.getBpnl());
        } catch (Exception e) {
            log.error("Error in ReportedMaterialItemStockRequest for " + material.getOwnMaterialNumber() + " and partner " + partner.getBpnl(), e);
        }
    }

    public void doItemStockSubmodelReportedProductItemStockRequest(Partner partner, Material material) {
        try {
            var mpr = mprService.find(material, partner);
            var data = edcAdapterService.doItemStockSubmodelRequest(mpr, DirectionCharacteristic.INBOUND);
            var samm = objectMapper.treeToValue(data, ItemStockSamm.class);
            var stocks = sammMapper.itemStockSammToReportedProductItemStock(samm, partner);
            for (var stock : stocks) {
                var stockPartner = stock.getPartner();
                var stockMaterial = stock.getMaterial();
                if (!partner.equals(stockPartner) || !material.equals(stockMaterial)) {
                    log.warn("Received inconsistent data from " + partner.getBpnl() + "\n" + stocks);
                    return;
                }
            }
            // delete older data:
            var oldStocks = reportedProductItemStockService.findByPartnerAndMaterial(partner, material);
            for (var oldStock : oldStocks) {
                reportedProductItemStockService.delete(oldStock.getUuid());
            }
            for (var newStock : stocks) {
                reportedProductItemStockService.create(newStock);
            }
            log.info("Updated ReportedProductItemStocks for " + material.getOwnMaterialNumber() + " and partner " + partner.getBpnl());
        } catch (Exception e) {
            log.error("Error in ReportedProductItemStockRequest for " + material.getOwnMaterialNumber() + " and partner " + partner.getBpnl(), e);
        }
    }


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
            if (requestMessageDto.getContent().getDirection() != DirectionCharacteristic.OUTBOUND) {
                log.error("Wrong direction in request from customer \n" + requestMessageDto);
            }
            if (customerPartner == null) {
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
            if (requestMessageDto.getContent().getDirection() != DirectionCharacteristic.INBOUND) {
                log.error("Wrong direction in request from customer \n" + requestMessageDto);
            }
            if (supplierPartner == null) {
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
