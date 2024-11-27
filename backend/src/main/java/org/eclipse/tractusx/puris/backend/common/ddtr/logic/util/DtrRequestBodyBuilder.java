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

package org.eclipse.tractusx.puris.backend.common.ddtr.logic.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.AssetType;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * A Service that generates Request Bodies for the DtrAdapterService
 */
@Service
@Slf4j
public class DtrRequestBodyBuilder {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VariablesService variablesService;

    private final static String DIGITAL_TWIN_TYPE = "digitalTwinType";
    private final static String MANUFACTURER_PART_ID = "manufacturerPartId";
    private final static String MANUFACTURER_ID = "manufacturerId";
    private final static String CUSTOMER_PART_ID = "customerPartId";

    /**
     * Creates a RequestBody for registering or updating a <b>Material-Type</b> Material
     *
     * @param materialPartnerRelation The MaterialPartnerRelation that defines the Material and the corresponding supplier partner.
     * @return The Request Body
     */

    public JsonNode createMaterialRegistrationRequestBody(MaterialPartnerRelation materialPartnerRelation) {
        Material material = materialPartnerRelation.getMaterial();
        if (!material.isMaterialFlag() || !materialPartnerRelation.isPartnerSuppliesMaterial()) {
            return null;
        }
        var body = objectMapper.createObjectNode();
        body.put("id", materialPartnerRelation.getPartnerCXNumber());
        body.put("globalAssetId", materialPartnerRelation.getPartnerCXNumber());
        var specificAssetIdsArray = objectMapper.createArrayNode();
        body.set("specificAssetIds", specificAssetIdsArray);

        var partnerRefNode = List.of(createReferenceObject(materialPartnerRelation.getPartner().getBpnl()));

        var digitalTwinObject = createDigitalTwinObject(partnerRefNode);
        specificAssetIdsArray.add(digitalTwinObject);

        var manufacturerIdObject = createManufacturerIdObject(materialPartnerRelation.getPartner().getBpnl(), partnerRefNode);
        specificAssetIdsArray.add(manufacturerIdObject);

        var manufacturerPartIdObject = createManufacturerPartIdObject(materialPartnerRelation.getPartnerMaterialNumber(), partnerRefNode);
        specificAssetIdsArray.add(manufacturerPartIdObject);

        var customerPartIdObject = createCustomerPartIdObject(material.getOwnMaterialNumber(), materialPartnerRelation.getPartner().getBpnl());
        specificAssetIdsArray.add(customerPartIdObject);

        var submodelDescriptorsArray = objectMapper.createArrayNode();
        body.set("submodelDescriptors", submodelDescriptorsArray);

        String href = variablesService.getEdcDataplanePublicUrl();
        href = href.endsWith("/") ? href : href + "/";
        href += materialPartnerRelation.getPartnerCXNumber() + "/";

        submodelDescriptorsArray.add(createSubmodelObject(AssetType.ITEM_STOCK_SUBMODEL.URN_SEMANTIC_ID, href + DirectionCharacteristic.INBOUND + "/", variablesService.getItemStockSubmodelApiAssetId()));
        submodelDescriptorsArray.add(createSubmodelObject(AssetType.DEMAND_SUBMODEL.URN_SEMANTIC_ID, href, variablesService.getDemandSubmodelApiAssetId()));
        submodelDescriptorsArray.add(createSubmodelObject(AssetType.DELIVERY_SUBMODEL.URN_SEMANTIC_ID, href, variablesService.getDeliverySubmodelApiAssetId()));
        submodelDescriptorsArray.add(createSubmodelObject(AssetType.DAYS_OF_SUPPLY.URN_SEMANTIC_ID, href  + DirectionCharacteristic.INBOUND + "/", variablesService.getDaysOfSupplySubmodelApiAssetId()));
        log.debug("Created body for material " + material.getOwnMaterialNumber() + "\n" + body.toPrettyString());
        return body;
    }

    /**
     * Creates a RequestBody for registering or updating a <b>Product-Type</b> Material
     *
     * @param material      The Material
     * @param productTwinId The ProductTwinId
     * @param mprs          The list of all MaterialProductRelations that exist with customers of the given Material
     * @return The Request Body
     */
    public JsonNode createProductRegistrationRequestBody(Material material, String productTwinId, List<MaterialPartnerRelation> mprs) {
        var body = objectMapper.createObjectNode();
        body.put("id", productTwinId);
        body.put("globalAssetId", material.getMaterialNumberCx());
        var specificAssetIdsArray = objectMapper.createArrayNode();
        body.set("specificAssetIds", specificAssetIdsArray);
        mprs = mprs.stream().filter(MaterialPartnerRelation::isPartnerBuysMaterial).filter(mpr -> mpr.getMaterial().equals(material)).toList();
        var partnerRefObjects = mprs.stream().map(mpr -> createReferenceObject(mpr.getPartner().getBpnl())).toList();
        if (material.isProductFlag()) {
            var digitalTwinObject = createDigitalTwinObject(partnerRefObjects);
            specificAssetIdsArray.add(digitalTwinObject);
            var manufacturerPartIdObject = createManufacturerPartIdObject(material.getOwnMaterialNumber(), partnerRefObjects);
            specificAssetIdsArray.add(manufacturerPartIdObject);
            var manufacturerIdObject = createManufacturerIdObject(variablesService.getOwnBpnl(), partnerRefObjects);
            specificAssetIdsArray.add(manufacturerIdObject);
            for (var mpr : mprs) {
                var customerPartIdObject = createCustomerPartIdObject(mpr.getPartnerMaterialNumber(), mpr.getPartner().getBpnl());
                specificAssetIdsArray.add(customerPartIdObject);
            }
        } else {
            log.error("Could not create request body: Missing product flag in material " + material.getOwnMaterialNumber());
            return null;
        }

        var submodelDescriptorsArray = objectMapper.createArrayNode();
        body.set("submodelDescriptors", submodelDescriptorsArray);

        String href = variablesService.getEdcDataplanePublicUrl();
        href = href.endsWith("/") ? href : href + "/";
        href += material.getMaterialNumberCx() + "/";
        
        submodelDescriptorsArray.add(createSubmodelObject(AssetType.ITEM_STOCK_SUBMODEL.URN_SEMANTIC_ID, href + DirectionCharacteristic.OUTBOUND + "/", variablesService.getItemStockSubmodelApiAssetId()));
        submodelDescriptorsArray.add(createSubmodelObject(AssetType.PRODUCTION_SUBMODEL.URN_SEMANTIC_ID, href, variablesService.getProductionSubmodelApiAssetId()));
        submodelDescriptorsArray.add(createSubmodelObject(AssetType.DELIVERY_SUBMODEL.URN_SEMANTIC_ID, href, variablesService.getDeliverySubmodelApiAssetId()));
        submodelDescriptorsArray.add(createSubmodelObject(AssetType.DAYS_OF_SUPPLY.URN_SEMANTIC_ID, href  + DirectionCharacteristic.OUTBOUND + "/", variablesService.getDaysOfSupplySubmodelApiAssetId()));
        submodelDescriptorsArray.add(createPartTypeSubmodelObject(material.getOwnMaterialNumber()));

        log.debug("Created body for product " + material.getOwnMaterialNumber() + "\n" + body.toPrettyString());
        return body;
    }


    private ObjectNode createGenericIdObject(String name, String id, List<ObjectNode> refObjects) {
        var idObject = objectMapper.createObjectNode();
        idObject.put("name", name);
        idObject.put("value", id);
        ObjectNode externalSubjectIdObject = objectMapper.createObjectNode();
        idObject.set("externalSubjectId", externalSubjectIdObject);
        externalSubjectIdObject.put("type", "ExternalReference");
        ArrayNode keysArray = objectMapper.createArrayNode();
        externalSubjectIdObject.set("keys", keysArray);
        for (var refObject : refObjects) {
            keysArray.add(refObject);
        }
        keysArray.add(createOwnReferenceObject());
        return idObject;
    }

    private ObjectNode createManufacturerPartIdObject(String manufacturerPartId, List<ObjectNode> refObjects) {
        return createGenericIdObject(MANUFACTURER_PART_ID, manufacturerPartId, refObjects);
    }

    private ObjectNode createManufacturerIdObject(String manufacturerId, List<ObjectNode> refObjects) {
        return createGenericIdObject(MANUFACTURER_ID, manufacturerId, refObjects);
    }

    private ObjectNode createCustomerPartIdObject(String customerPartId, String customerBpnl) {
        return createGenericIdObject(CUSTOMER_PART_ID, customerPartId, List.of(createReferenceObject(customerBpnl)));
    }

    private JsonNode createDigitalTwinObject(List<ObjectNode> refNodes) {
        return createGenericIdObject(DIGITAL_TWIN_TYPE, "PartType", refNodes);
    }

    private ObjectNode createReferenceObject(String bpnl) {
        var refObject = objectMapper.createObjectNode();
        refObject.put("type", "GlobalReference");
        refObject.put("value", bpnl);
        return refObject;
    }

    private ObjectNode createOwnReferenceObject() {
        return createReferenceObject(variablesService.getOwnBpnl());
    }

    private JsonNode createSubmodelObject(String semanticId, String href, String assetId) {
        var requestSubmodelObject = objectMapper.createObjectNode();

        requestSubmodelObject.put("id", UUID.randomUUID().toString());
        var semanticIdObject = objectMapper.createObjectNode();
        requestSubmodelObject.set("semanticId", semanticIdObject);
        semanticIdObject.put("type", "ExternalReference");
        var keysArray = objectMapper.createArrayNode();
        semanticIdObject.set("keys", keysArray);
        var keyObject = objectMapper.createObjectNode();
        keysArray.add(keyObject);
        keyObject.put("type", "GlobalReference");
        keyObject.put("value", semanticId);

        var endpointsArray = objectMapper.createArrayNode();
        requestSubmodelObject.set("endpoints", endpointsArray);
        var submodel3EndpointObject = objectMapper.createObjectNode();
        endpointsArray.add(submodel3EndpointObject);
        submodel3EndpointObject.put("interface", "SUBMODEL-3.0");
        var protocolInformationObject = objectMapper.createObjectNode();
        submodel3EndpointObject.set("protocolInformation", protocolInformationObject);
        protocolInformationObject.put("href", href);
        protocolInformationObject.put("endpointProtocol", "HTTP");
        var endpointProtocolVersionArray = objectMapper.createArrayNode();
        protocolInformationObject.set("endpointProtocolVersion", endpointProtocolVersionArray);
        endpointProtocolVersionArray.add("1.1");
        protocolInformationObject.put("subprotocol", "DSP");
        protocolInformationObject.put("subprotocolBodyEncoding", "plain");
        protocolInformationObject.put("subprotocolBody", "id=" + assetId + ";dspEndpoint=" + variablesService.getEdcProtocolUrl());
        var securityAttributesArray = objectMapper.createArrayNode();
        protocolInformationObject.set("securityAttributes", securityAttributesArray);
        var securityObject = objectMapper.createObjectNode();
        securityAttributesArray.add(securityObject);
        securityObject.put("type", "NONE");
        securityObject.put("key", "NONE");
        securityObject.put("value", "NONE");
        return requestSubmodelObject;
    }

    private JsonNode createPartTypeSubmodelObject(String materialId) {
        String href = variablesService.getEdcDataplanePublicUrl();
        href = href.endsWith("/") ? href : href + "/";
        href += Base64.getEncoder().encodeToString(materialId.getBytes(StandardCharsets.UTF_8));
        return createSubmodelObject(AssetType.PART_TYPE_INFORMATION_SUBMODEL.URN_SEMANTIC_ID, href, variablesService.getPartTypeSubmodelApiAssetId());
    }

}
