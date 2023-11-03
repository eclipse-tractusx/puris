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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.MessageHeader;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_UseCaseEnum;
import org.eclipse.tractusx.puris.backend.common.api.logic.service.VariablesService;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.*;
import org.eclipse.tractusx.puris.backend.stock.logic.adapter.ProductStockSammMapper;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.samm.ProductStockSammDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Service implements the handling of a request for Product Stock
 * <p>
 * That means that one need to lookup
 * {@link org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStock} and return it
 * according to the API specification.
 */
@Service
@Slf4j
public class ProductStockRequestApiServiceImpl implements ProductStockRequestApiService{

    @Autowired
    private ProductStockRequestService productStockRequestService;

    @Autowired
    private MaterialService materialService;

    @Autowired
    private MaterialPartnerRelationService mprService;

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private ProductStockService productStockService;

    @Autowired
    private ProductStockSammMapper productStockSammMapper;

    @Autowired
    private EdcAdapterService edcAdapterService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${edc.dataplane.public.port}")
    String dataPlanePort;

    @Value("${edc.controlplane.host}")
    String dataPlaneHost;

    @Value("${edc.idsUrl}")
    private String ownEdcIdsUrl;

    @Value("${own.bpnl}")
    private String ownBPNL;

    @Value("${edc.applydataplaneworkaround}")
    private boolean applyDataplaneWorkaround;

    @Autowired
    private VariablesService variablesService;

    public static <T> Predicate<T> distinctByKey(
            Function<? super T, ?> keyExtractor) {

        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }


    /**
     * This method should be called in a separate Thread.
     *
     * It will evaluate the given ProductStockRequest and check, whether this Partner is
     * currently known as a customer for the given products. Then this method will assemble
     * all necessary information from database, generate ProductStockSammDto's and then send
     * them to the Partner via his product-stock-response-api.
     *
     * <p>Please note that this method currently does not support multple BPNS's/BPNA's per Partner.</p>
     *
     * @param productStockRequest a ProductStockRequest you received from a Customer Partner
     */
    public void handleRequest(ProductStockRequest productStockRequest) {

        productStockRequest = productStockRequestService.updateState(productStockRequest,DT_RequestStateEnum.Working);
        String requestingPartnerBpnl = productStockRequest.getHeader().getSender();
        Partner requestingPartner =  partnerService.findByBpnl(requestingPartnerBpnl);

        String partnerIdsUrl = requestingPartner.getEdcUrl();

        if (productStockRequest.getHeader().getSenderEdc() != null && !partnerIdsUrl.equals(productStockRequest.getHeader().getSenderEdc())) {
            log.warn("Partner " + requestingPartner.getName() + " is using unknown idsUrl: " + productStockRequest.getHeader().getSenderEdc());
            log.warn("Request will not be processed");
            productStockRequestService.updateState(productStockRequest, DT_RequestStateEnum.Error);
            return;
        }

        List<ProductStockSammDto> resultProductStocks = new ArrayList<>();


        for (ProductStockRequestForMaterial productStockRequestForMaterial : productStockRequest.getContent().getProductStock()) {

            // Check if product is known
            Material existingMaterial = null;
            if (productStockRequestForMaterial.getMaterialNumberCatenaX() != null) {
                // Identify material via cx number, if possible
                existingMaterial = materialService
                    .findByMaterialNumberCx(productStockRequestForMaterial.getMaterialNumberCatenaX());
            }

            if (existingMaterial == null) {
                // if material could not be found via cx number, try to use materialNumberCustomer
                if (productStockRequestForMaterial.getMaterialNumberCustomer() != null) {
                    var searchResult = mprService.findAllByPartnerMaterialNumber(productStockRequestForMaterial.getMaterialNumberCustomer());
                    if (searchResult.size() == 1) {
                        existingMaterial = searchResult.get(0);
                    } else {
                        if (productStockRequestForMaterial.getMaterialNumberCustomer() != null) {
                            if (productStockRequestForMaterial.getMaterialNumberSupplier() == null
                                || productStockRequestForMaterial.getMaterialNumberSupplier().equals(productStockRequestForMaterial.getMaterialNumberCustomer())) {
                                // possible case: customer did not define his own material number
                                // and is using the supplier's material number (see CX 0085) as his own material number.
                                existingMaterial = materialService.findByOwnMaterialNumber(productStockRequestForMaterial.getMaterialNumberCustomer());
                            } else {
                                // MaterialNumberCustomer is ambiguous or unknown
                                // try to use MaterialNumberSupplier
                                if (productStockRequestForMaterial.getMaterialNumberSupplier() != null) {
                                    existingMaterial = materialService.findByOwnMaterialNumber(productStockRequestForMaterial.getMaterialNumberSupplier());
                                }
                            }
                        }
                    }
                }
            }

            if (existingMaterial == null) {
                log.warn("Material unknown");
                // Material is unknown, error messages in this case currently not supported
                continue;
            } else {
                log.info("Found requested Material: " + existingMaterial.getOwnMaterialNumber());
            }


            boolean ordersProducts = mprService.partnerOrdersProduct(existingMaterial, requestingPartner);
            log.info("Requesting entity orders this Material? " + ordersProducts);
            if (!ordersProducts) {
                log.warn(String.format("Partner %s is not an ordering Partner of Material " +
                                "found for ID Customer %s in request %s",
                        requestingPartnerBpnl,
                        productStockRequestForMaterial.getMaterialNumberCustomer(),
                        productStockRequest.getHeader().getRequestId()));
                continue;
            }

            List<ProductStock> productStocks =
                    productStockService
                            .findAllByMaterialNumberCustomerAndAllocatedToCustomerBpnl(
                                    productStockRequestForMaterial.getMaterialNumberCustomer(),
                                    requestingPartnerBpnl);

            ProductStock productStock = null;
            if (productStocks.size() == 0) {
                log.warn("No Product Stocks of Material " + productStockRequestForMaterial.getMaterialNumberCustomer()
                + " found for " + productStockRequest.getHeader().getSender());
                continue;
            } else productStock = productStocks.get(0);


            if (productStocks.size() > 1) {
                List<ProductStock> distinctProductStocks =
                        productStocks.stream()
                                .filter(distinctByKey(Stock::getLocationId))
                                .collect(Collectors.toList());
                if (distinctProductStocks.size() > 1) {
                    log.warn(String.format("More than one site is not yet supported per " +
                                    "partner. Product Stocks for material ID %s and partner %s in " +
                                    "request %s are accumulated",
                            productStockRequestForMaterial.getMaterialNumberCustomer(),
                            requestingPartnerBpnl, productStockRequest.getHeader().getRequestId()));
                }

                double quantity = productStocks.stream().
                        mapToDouble(Stock::getQuantity).sum();
                productStock.setQuantity(quantity);

            }

            log.info("Assembled productStock:\n" + productStock);

            resultProductStocks.add(productStockSammMapper.toSamm(productStock));
        }


        var data = edcAdapterService.getContractForResponseApi(partnerIdsUrl);
        if(data == null) {
            log.error("Failed to contract response api from " + partnerIdsUrl);
            productStockRequest = productStockRequestService.updateState(productStockRequest, DT_RequestStateEnum.Error);
            log.info("Request status: \n" + productStockRequest.toString());
            return;
        }
        String authKey = data[0];
        String authCode = data[1];
        String endpoint = data[2];
        String contractId = data[3];
        // prepare interface object
        MessageHeader messageHeader = new MessageHeader();
        messageHeader.setRequestId(productStockRequest.getHeader().getRequestId());
        messageHeader.setContractAgreementId(contractId);
        messageHeader.setSender(ownBPNL);
        messageHeader.setSenderEdc(ownEdcIdsUrl);
        // set receiver per partner
        messageHeader.setReceiver(productStockRequest.getHeader().getSender());
        messageHeader.setUseCase(DT_UseCaseEnum.PURIS);
        messageHeader.setCreationDate(new Date());

        ProductStockResponse response = new ProductStockResponse();
        response.setHeader(messageHeader);
        response.getContent().setProductStocks(resultProductStocks);

        if (applyDataplaneWorkaround) {
            log.info("Applying Dataplane Address Workaround");
            endpoint = "http://" + dataPlaneHost + ":" + dataPlanePort + "/api/public";
        }
        try {
            String requestBody = objectMapper.writeValueAsString(response);
            var httpResponse = edcAdapterService.sendDataPullRequest(
                    endpoint, authKey, authCode, requestBody);
            log.info(httpResponse.body().string());
            httpResponse.body().close();
            productStockRequest = productStockRequestService.updateState(productStockRequest, DT_RequestStateEnum.Completed);
        } catch (Exception e) {
            log.error("Failed to send response to " + response.getHeader().getReceiver(), e);
            productStockRequest = productStockRequestService.updateState(productStockRequest, DT_RequestStateEnum.Error);
        } finally {
            log.info("Request status: \n" + productStockRequest.toString());
        }

    }

    public void request(Material material, Partner supplierPartner){
        ProductStockRequest productStockRequest = new ProductStockRequest();

        MaterialPartnerRelation materialPartnerRelation = mprService.find(material, supplierPartner);

        if (materialPartnerRelation == null) {
            log.error("Missing material-partner-relation for " + material.getOwnMaterialNumber()
                + " and " + supplierPartner.getBpnl());
            return;
        }

        ProductStockRequestForMaterial materialToRequest = new ProductStockRequestForMaterial(
            material.getOwnMaterialNumber(),
            material.getMaterialNumberCx(),
            materialPartnerRelation.getPartnerMaterialNumber()
        );
        productStockRequest.getContent().getProductStock().add(materialToRequest);

        String [] data = edcAdapterService.getContractForRequestApi(supplierPartner.getEdcUrl());
        if(data == null) {
            log.error("failed to obtain request api from " + supplierPartner.getEdcUrl());
            return;
        }
        String authKey = data[0];
        String authCode = data[1];
        String endpoint = data[2];
        if (applyDataplaneWorkaround) {
            log.info("Applying Dataplane Address Workaround");
            endpoint = "http://" + dataPlaneHost + ":" + dataPlanePort + "/api/public";
        }

        String cid = data[3];
        MessageHeader messageHeader = new MessageHeader();
        UUID randomUuid = UUID.randomUUID();

        // Avoid randomly choosing a UUID that was already used by this customer.
        while (productStockRequestService.findRequestByHeaderUuid(randomUuid) != null) {
            randomUuid = UUID.randomUUID();
        }
        messageHeader.setRequestId(randomUuid);
        messageHeader.setRespondAssetId(variablesService.getResponseApiAssetId());
        messageHeader.setContractAgreementId(cid);
        messageHeader.setSender(variablesService.getOwnBpnl());
        messageHeader.setSenderEdc(variablesService.getOwnEdcIdsUrl());
        // set receiver per partner
        messageHeader.setReceiver(supplierPartner.getBpnl());
        messageHeader.setUseCase(DT_UseCaseEnum.PURIS);
        messageHeader.setCreationDate(new Date());

        productStockRequest.setHeader(messageHeader);
        productStockRequest.setState(DT_RequestStateEnum.Working);
        productStockRequest = productStockRequestService.createRequest(productStockRequest);
        var test = productStockRequestService.findRequestByHeaderUuid(productStockRequest.getHeader().getRequestId());
        log.debug("Stored in Database " + (test != null) + " " + productStockRequest.getHeader().getRequestId());
        Response response = null;
        try {
            String requestBody = objectMapper.writeValueAsString(productStockRequest);
            response = edcAdapterService.sendDataPullRequest(endpoint, authKey, authCode, requestBody);
            log.debug(response.body().string());
            if(response.code() < 400) {
                productStockRequest = productStockRequestService.updateState(productStockRequest, DT_RequestStateEnum.Requested);
                log.debug("Sent request and received HTTP Status code " + response.code());
                log.debug("Setting request state to " + DT_RequestStateEnum.Requested);
                productStockRequestService.updateState(productStockRequest, DT_RequestStateEnum.Requested);
            } else {
                log.warn("Received HTTP Status Code " + response.code() + " for request " + productStockRequest.getHeader().getRequestId()
                    + " from " + productStockRequest.getHeader().getReceiver());
                productStockRequest = productStockRequestService.updateState(productStockRequest, DT_RequestStateEnum.Error);
            }

        } catch (Exception e) {
            log.error("Failed to send data pull request to " + supplierPartner.getEdcUrl(), e);
            productStockRequestService.updateState(productStockRequest, DT_RequestStateEnum.Error);
        } finally {
            try {
                if(response != null) {
                    response.body().close();
                }
            } catch (Exception e) {
                log.warn("Failed to close response body");
            }
        }
    }
}
