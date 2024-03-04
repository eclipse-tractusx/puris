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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
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
     * Inserts or updates information from the given MaterialPartnerRelation into
     * the existing AAS Shell of the Material, that is referenced by the MaterialPartnerRelation.
     *
     * First, you need to request the current dDTR entry of that Material and hand it over to this
     * method.
     *
     * The returned JsonNode is meant to be sent to the PUT endpoint of your dDTR.
     *
     * @param mpr               The MaterialPartnerRelation
     * @param existingNode      The JSON-Content of the existing Material dDTR entry as String
     * @return                  The updated dDTR entry
     * @throws JsonProcessingException
     */
    public JsonNode injectMaterialPartnerRelation(MaterialPartnerRelation mpr, String existingNode) throws JsonProcessingException {
        var body = (ObjectNode) objectMapper.readTree(existingNode);
        var assetIdTypes = List.of(DIGITAL_TWIN_TYPE, MANUFACTURER_PART_ID, CUSTOMER_PART_ID);
        var assetIdArray = body.get("specificAssetIds");
        Partner partner = mpr.getPartner();
        HashMap<String,Boolean> checkAssetTypes = new HashMap<>();
        checkAssetTypes.put(DIGITAL_TWIN_TYPE, true);
        checkAssetTypes.put(MANUFACTURER_PART_ID, mpr.isPartnerSuppliesMaterial() && mpr.getMaterial().isMaterialFlag());
        checkAssetTypes.put(CUSTOMER_PART_ID, mpr.isPartnerBuysMaterial() && mpr.getMaterial().isProductFlag());
        var partnerKeyObject = objectMapper.createObjectNode();
        partnerKeyObject.put("type", "GlobalReference");
        partnerKeyObject.put("value", partner.getBpnl());
        boolean manufacturerPartIdForPartnerNeeded = mpr.isPartnerBuysMaterial() && mpr.getMaterial().isProductFlag();
        boolean foundManufacturerPartIdForPartner = false;
        // check For DIGITAL_TWIN_TYPE and whether Partner needs to be added.
        // Additionally, do a lookup for existing manufacturerPartId/customerPartId elements for partner
        // Update existing manufacturerPartId / customerPartId entries, if necessary.
        for (JsonNode asset : assetIdArray) {
            String assetName = asset.get("name").asText();
            var externalSubjectIdObject = asset.get("externalSubjectId");
            var keys = (ArrayNode) externalSubjectIdObject.get("keys");
            if (assetIdTypes.contains(assetName)) {
                switch (assetName) {
                    case DIGITAL_TWIN_TYPE -> {
                        boolean alreadyThere = false;
                        for (var key : keys) {
                            var value = key.get("value").asText();
                            if (partner.getBpnl().equals(value)) {
                                alreadyThere = true;
                            }
                        }
                        if (!alreadyThere) {
                            keys.add(partnerKeyObject);
                        }
                    }
                    case MANUFACTURER_PART_ID -> {
                        if (manufacturerPartIdForPartnerNeeded) {
                            for (var key : keys) {
                                var value = key.get("value").asText();
                                if (partner.getBpnl().equals(value)) {
                                    foundManufacturerPartIdForPartner = true;
                                }
                            }
                        }
                    }
                }
            }
            var supplementalIds = asset.get("supplementalSemanticIds");
            if (supplementalIds != null && supplementalIds.isArray()) {
                ArrayNode arrayNode = (ArrayNode) supplementalIds;
                if (arrayNode.isEmpty()) {
                    ((ObjectNode) asset).remove("supplementalSemanticIds");
                }
            }
        }
        var submodelDescriptors = (ArrayNode) body.get("submodelDescriptors");
        for (var submodelDescriptor : submodelDescriptors) {
            var supplementalIds = submodelDescriptor.get("supplementalSemanticId");
            if (supplementalIds != null && supplementalIds.isArray()) {
                ArrayNode arrayNode = (ArrayNode) supplementalIds;
                if (arrayNode.isEmpty()) {
                    ((ObjectNode) submodelDescriptor).remove("supplementalSemanticId");
                }
            }
        }
        if (manufacturerPartIdForPartnerNeeded && !foundManufacturerPartIdForPartner) {
            // Insert manufacturerPartId for Partner
            var manufacturerPartIdObjectBody = objectMapper.createObjectNode();
            manufacturerPartIdObjectBody.put("name", MANUFACTURER_PART_ID);
            manufacturerPartIdObjectBody.put("value", mpr.getMaterial().getOwnMaterialNumber());
            var externalSubjectIdBody = objectMapper.createObjectNode();
            externalSubjectIdBody.put("type", "ExternalReference");
            var manufacturerPartIdKeysArray = objectMapper.createArrayNode();
            manufacturerPartIdObjectBody.set("keys", manufacturerPartIdKeysArray);
            manufacturerPartIdKeysArray.add(getOwnReferenceObject());
            manufacturerPartIdKeysArray.add(partnerKeyObject);
            ((ArrayNode)assetIdArray).add(manufacturerPartIdObjectBody);
            log.info("Added manufacturerPartId for " + partner.getBpnl() + " and " + mpr.getMaterial().getOwnMaterialNumber());
        }
        return body;
    }


    private JsonNode getOwnReferenceObject() {
        var refObject = objectMapper.createObjectNode();
        refObject.put("type", "GlobalReference");
        refObject.put("value", variablesService.getOwnBpnl());
        return refObject;
    }

    /**
     * Creates a Request Body for the initial registration of a given Material at
     * your dDTR.
     * @param material  The Material you want to register
     * @return          The Request Body
     */
    public JsonNode createProductRegistrationRequestBody(Material material, String productTwinId) {
        var body = objectMapper.createObjectNode();
        body.put("id", productTwinId); //Random uuid
        body.put("globalAssetId", material.getMaterialNumberCx());
        body.put("idShort", material.getOwnMaterialNumber());
        var specificAssetIdsArray = objectMapper.createArrayNode();
        body.set("specificAssetIds", specificAssetIdsArray);
        var digitalTwinObject = objectMapper.createObjectNode();
        specificAssetIdsArray.add(digitalTwinObject);

        digitalTwinObject.put("name", "digitalTwinType");
        digitalTwinObject.put("value", "PartType");
        var externalSubjectIdObject = objectMapper.createObjectNode();
        digitalTwinObject.set("externalSubjectId", externalSubjectIdObject);
        externalSubjectIdObject.put("type", "ExternalReference");
        var keysArray = objectMapper.createArrayNode();
        externalSubjectIdObject.set("keys", keysArray);
        var ownRefObject = getOwnReferenceObject();
        keysArray.add(ownRefObject);
        if(material.isProductFlag()) {
            var manufacturerPartIdObject = getManufacturerPartIdObject(material, ownRefObject);
            specificAssetIdsArray.add(manufacturerPartIdObject);
        }

        var submodelDescriptorsArray = objectMapper.createArrayNode();
        body.set("submodelDescriptors", submodelDescriptorsArray);

        var itemStockRequestSubmodelObject = objectMapper.createObjectNode();
        submodelDescriptorsArray.add(itemStockRequestSubmodelObject);
        itemStockRequestSubmodelObject.put("id", UUID.randomUUID().toString());
        var semanticIdObject = objectMapper.createObjectNode();
        itemStockRequestSubmodelObject.set("semanticId", semanticIdObject);
        semanticIdObject.put("type", "ExternalReference");
        keysArray = objectMapper.createArrayNode();
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
        return body;
    }

    @NotNull
    private ObjectNode getManufacturerPartIdObject(Material material, JsonNode refObject) {
        var manufacturerPartIdObject = objectMapper.createObjectNode();
        manufacturerPartIdObject.put("name", "manufacturerPartId");
        manufacturerPartIdObject.put("value", material.getOwnMaterialNumber());
        ObjectNode externalSubjectIdObject = objectMapper.createObjectNode();
        manufacturerPartIdObject.set("externalSubjectId", externalSubjectIdObject);
        externalSubjectIdObject.put("type", "ExternalReference");
        ArrayNode keysArray = objectMapper.createArrayNode();
        externalSubjectIdObject.set("keys", keysArray);
        keysArray.add(refObject);
        return manufacturerPartIdObject;
    }


}
