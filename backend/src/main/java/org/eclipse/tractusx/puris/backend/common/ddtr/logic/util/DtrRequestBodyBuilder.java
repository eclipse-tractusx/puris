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
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
     * @param materialPartnerRelation   The MaterialPartnerRelation that defines the Material and the corresponding supplier partner.
     * @return                          The Request Body
     */

    public JsonNode createMaterialRegistrationRequestBody(MaterialPartnerRelation materialPartnerRelation) {
        Material material = materialPartnerRelation.getMaterial();
        if(!material.isMaterialFlag() || !materialPartnerRelation.isPartnerSuppliesMaterial()) {
            return null;
        }
        var body = objectMapper.createObjectNode();
        body.put("id", materialPartnerRelation.getPartnerCXNumber());
        body.put("globalAssetId", materialPartnerRelation.getPartnerCXNumber());
        var specificAssetIdsArray = objectMapper.createArrayNode();
        body.set("specificAssetIds", specificAssetIdsArray);

        var partnerRefNode = List.of(getReferenceObject(materialPartnerRelation.getPartner().getBpnl()));

        var digitalTwinObject = getDigitalTwinObject(partnerRefNode);
        specificAssetIdsArray.add(digitalTwinObject);

        var manufacturerIdObject = getManufacturerIdObject(materialPartnerRelation.getPartner().getBpnl(), partnerRefNode);
        specificAssetIdsArray.add(manufacturerIdObject);

        var manufacturerPartIdObject = getManufacturerPartIdObject(materialPartnerRelation.getPartnerMaterialNumber(), partnerRefNode);
        specificAssetIdsArray.add(manufacturerPartIdObject);

        var customerPartIdObject = getCustomerPartIdObject(material.getOwnMaterialNumber(), materialPartnerRelation.getPartner().getBpnl());
        specificAssetIdsArray.add(customerPartIdObject);

        var submodelDescriptorsArray = objectMapper.createArrayNode();
        body.set("submodelDescriptors", submodelDescriptorsArray);

        var itemStockRequestSubmodelObject = getItemStockSubmodelObject();
        submodelDescriptorsArray.add(itemStockRequestSubmodelObject);
        log.info("Created body for material " + material.getOwnMaterialNumber() + "\n" + body.toPrettyString());
        return body;
    }

    /**
     * Creates a RequestBody for registering or updating a <b>Product-Type</b> Material
     *
     * @param material          The Material
     * @param productTwinId     The ProductTwinId
     * @param mprs              The list of all MaterialProductRelations that exist with customers of the given Material
     * @return                  The Request Body
     */
    public JsonNode createProductRegistrationRequestBody(Material material, String productTwinId, List<MaterialPartnerRelation> mprs) {
        var body = objectMapper.createObjectNode();
        body.put("id", productTwinId);
        body.put("globalAssetId", material.getMaterialNumberCx());
        var specificAssetIdsArray = objectMapper.createArrayNode();
        body.set("specificAssetIds", specificAssetIdsArray);
        mprs = mprs.stream().filter(MaterialPartnerRelation::isPartnerBuysMaterial).filter(mpr -> mpr.getMaterial().equals(material)).toList();
        var partnerRefObjects = mprs.stream().map(mpr -> getReferenceObject(mpr.getPartner().getBpnl())).toList();
        if (material.isProductFlag()) {
            var digitalTwinObject = getDigitalTwinObject(partnerRefObjects);
            specificAssetIdsArray.add(digitalTwinObject);
            var manufacturerPartIdObject = getManufacturerPartIdObject(material.getOwnMaterialNumber(), partnerRefObjects);
            specificAssetIdsArray.add(manufacturerPartIdObject);
            var manufacturerIdObject = getManufacturerIdObject(variablesService.getOwnBpnl(), partnerRefObjects);
            specificAssetIdsArray.add(manufacturerIdObject);
            for (var mpr : mprs) {
                var customerPartIdObject = getCustomerPartIdObject(mpr.getPartnerMaterialNumber(), mpr.getPartner().getBpnl());
                specificAssetIdsArray.add(customerPartIdObject);
            }
        } else {
            log.error("Could not create request body: Missing product flag in material " + material.getOwnMaterialNumber());
            return null;
        }

        var submodelDescriptorsArray = objectMapper.createArrayNode();
        body.set("submodelDescriptors", submodelDescriptorsArray);

        var itemStockRequestSubmodelObject = getItemStockSubmodelObject();
        submodelDescriptorsArray.add(itemStockRequestSubmodelObject);

        log.info("Created body for product " + material.getOwnMaterialNumber() + "\n" + body.toPrettyString());
        return body;
    }

    private ObjectNode getManufacturerPartIdObject(String manufacturerPartId, List<ObjectNode> refObjects) {
        var manufacturerPartIdObject = objectMapper.createObjectNode();
        manufacturerPartIdObject.put("name", MANUFACTURER_PART_ID);
        manufacturerPartIdObject.put("value", manufacturerPartId);
        ObjectNode externalSubjectIdObject = objectMapper.createObjectNode();
        manufacturerPartIdObject.set("externalSubjectId", externalSubjectIdObject);
        externalSubjectIdObject.put("type", "ExternalReference");
        ArrayNode keysArray = objectMapper.createArrayNode();
        externalSubjectIdObject.set("keys", keysArray);
        for (var refObject : refObjects) {
            keysArray.add(refObject);
        }
        keysArray.add(getOwnReferenceObject());
        return manufacturerPartIdObject;
    }

    private ObjectNode getManufacturerIdObject(String manufacturerId, List<ObjectNode> refObjects) {
        var manufacturerIdObject = objectMapper.createObjectNode();
        manufacturerIdObject.put("name", MANUFACTURER_ID);
        manufacturerIdObject.put("value", manufacturerId);
        ObjectNode externalSubjectIdObject = objectMapper.createObjectNode();
        manufacturerIdObject.set("externalSubjectId", externalSubjectIdObject);
        externalSubjectIdObject.put("type", "ExternalReference");
        ArrayNode keysArray = objectMapper.createArrayNode();
        externalSubjectIdObject.set("keys", keysArray);
        for (var refObject : refObjects) {
            keysArray.add(refObject);
        }
        keysArray.add(getOwnReferenceObject());
        return manufacturerIdObject;
    }

    private ObjectNode getCustomerPartIdObject(String customerPartId, String customerBpnl) {
        var customerPartIdObject = objectMapper.createObjectNode();
        customerPartIdObject.put("name", CUSTOMER_PART_ID);
        customerPartIdObject.put("value", customerPartId);
        ObjectNode externalSubjectIdObject = objectMapper.createObjectNode();
        customerPartIdObject.set("externalSubjectId", externalSubjectIdObject);
        externalSubjectIdObject.put("type", "ExternalReference");
        ArrayNode keysArray = objectMapper.createArrayNode();
        externalSubjectIdObject.set("keys", keysArray);
        keysArray.add(getOwnReferenceObject());
        keysArray.add(getReferenceObject(customerBpnl));
        return customerPartIdObject;
    }

    private ObjectNode getReferenceObject(String bpnl) {
        var refObject = objectMapper.createObjectNode();
        refObject.put("type", "GlobalReference");
        refObject.put("value", bpnl);
        return refObject;
    }

    private ObjectNode getOwnReferenceObject() {
        return getReferenceObject(variablesService.getOwnBpnl());
    }


    private JsonNode getDigitalTwinObject(List<ObjectNode> refNodes) {
        var digitalTwinObject = objectMapper.createObjectNode();
        digitalTwinObject.put("name", DIGITAL_TWIN_TYPE);
        digitalTwinObject.put("value", "PartType");
        var externalSubjectIdObject = objectMapper.createObjectNode();
        digitalTwinObject.set("externalSubjectId", externalSubjectIdObject);
        externalSubjectIdObject.put("type", "ExternalReference");
        var keysArray = objectMapper.createArrayNode();
        externalSubjectIdObject.set("keys", keysArray);
        keysArray.add(getOwnReferenceObject());
        for (var refNode : refNodes) {
            keysArray.add(refNode);
        }
        return digitalTwinObject;
    }

    private JsonNode getItemStockSubmodelObject() {
        var itemStockRequestSubmodelObject = objectMapper.createObjectNode();

        itemStockRequestSubmodelObject.put("id", UUID.randomUUID().toString());
        var semanticIdObject = objectMapper.createObjectNode();
        itemStockRequestSubmodelObject.set("semanticId", semanticIdObject);
        semanticIdObject.put("type", "ExternalReference");
        var keysArray = objectMapper.createArrayNode();
        semanticIdObject.set("keys", keysArray);
        var keyObject = objectMapper.createObjectNode();
        keysArray.add(keyObject);
        keyObject.put("type", "GlobalReference");
        keyObject.put("value", "urn:samm:io.catenax.item_stock:2.0.0#ItemStock");

        var endpointsArray = objectMapper.createArrayNode();
        itemStockRequestSubmodelObject.set("endpoints", endpointsArray);
        var submodel3EndpointObject = objectMapper.createObjectNode();
        endpointsArray.add(submodel3EndpointObject);
        submodel3EndpointObject.put("interface", "SUBMODEL-3.0");
        var protocolInformationObject = objectMapper.createObjectNode();
        submodel3EndpointObject.set("protocolInformation", protocolInformationObject);
        protocolInformationObject.put("href", variablesService.getEdcProtocolUrl());
        protocolInformationObject.put("endpointProtocol", "HTTP");
        var endpointProtocolVersionArray = objectMapper.createArrayNode();
        protocolInformationObject.set("endpointProtocolVersion", endpointProtocolVersionArray);
        endpointProtocolVersionArray.add("1.1");
        protocolInformationObject.put("subprotocol", "DSP");
        protocolInformationObject.put("subprotocolBodyEncoding", "plain");
        protocolInformationObject.put("subprotocolBody", "id=FOO;dspEndpoint=" + variablesService.getEdcProtocolUrl());
        var securityAttributesArray = objectMapper.createArrayNode();
        protocolInformationObject.set("securityAttributes", securityAttributesArray);
        var securityObject = objectMapper.createObjectNode();
        securityAttributesArray.add(securityObject);
        securityObject.put("type", "NONE");
        securityObject.put("key", "NONE");
        securityObject.put("value", "NONE");
        return itemStockRequestSubmodelObject;
    }


}
