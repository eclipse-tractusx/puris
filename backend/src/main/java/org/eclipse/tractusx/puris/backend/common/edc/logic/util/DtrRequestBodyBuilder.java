package org.eclipse.tractusx.puris.backend.common.edc.logic.util;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class DtrRequestBodyBuilder {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VariablesService variablesService;


    public JsonNode injectMaterialPartnerRelation(MaterialPartnerRelation mpr, String existingNode) throws JsonProcessingException {
        var body = (ObjectNode) objectMapper.readTree(existingNode);
        log.info("Reading for " + mpr + "\n" + body.toPrettyString());
        var assetIdTypes = List.of ("digitalTwinType", "manufacturerPartId", "manufacturerId");
        var assetIdArray = body.get("specificAssetIds");
        Partner partner = mpr.getPartner();
        var keyObject = objectMapper.createObjectNode();
        keyObject.put("type", "GlobalReference");
        keyObject.put("value", partner.getBpnl());
        for (var asset : assetIdArray) {
            String assetName = asset.get("name").asText();
            if (assetIdTypes.contains(assetName)) {
                var externalSubjectIdObject = asset.get("externalSubjectId");
                var keys = (ArrayNode)externalSubjectIdObject.get("keys");
                boolean alreadyThere = false;
                for (var key : keys) {
                    var value = key.get("value").asText();
                    if(partner.getBpnl().equals(value)) {
                        alreadyThere = true;
                    }
                }
                if (!alreadyThere) {
                    keys.add(keyObject);
                }
            }
        }
        log.info("UPDATED BODY " + body.toPrettyString());
        return body;
    }

    public JsonNode createMaterialRegistrationRequestBody(Material material) {
        var body = objectMapper.createObjectNode();
        body.put("id", material.getMaterialNumberCx());
        body.put("globalAssetId", material.getMaterialNumberCx());
        body.put("idShort", material.getName());
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
        var refObject = objectMapper.createObjectNode();
        keysArray.add(refObject);
        refObject.put("type", "GlobalReference");
        refObject.put("value", variablesService.getOwnBpnl());

        var manufacturerPartIdObject = objectMapper.createObjectNode();
        specificAssetIdsArray.add(manufacturerPartIdObject);
        manufacturerPartIdObject.put("name", "manufacturerPartId");
        manufacturerPartIdObject.put("value", material.getOwnMaterialNumber());
        externalSubjectIdObject = objectMapper.createObjectNode();
        manufacturerPartIdObject.set("externalSubjectId", externalSubjectIdObject);
        externalSubjectIdObject.put("type", "ExternalReference");
        keysArray = objectMapper.createArrayNode();
        externalSubjectIdObject.set("keys", keysArray);
        keysArray.add(refObject);

        var manufacturerIdObject = objectMapper.createObjectNode();
        specificAssetIdsArray.add(manufacturerIdObject);
        manufacturerIdObject.put("name", "manufacturerId");
        manufacturerIdObject.put("value", variablesService.getOwnBpnl());
        externalSubjectIdObject = objectMapper.createObjectNode();
        manufacturerIdObject.set("externalSubjectId", externalSubjectIdObject);
        externalSubjectIdObject.put("type", "ExternalReference");
        keysArray = objectMapper.createArrayNode();
        externalSubjectIdObject.put("keys", keysArray);
        keysArray.add(refObject);


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


}
