package org.eclipse.tractusx.puris.backend.common.edc.logic.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.api.logic.service.VariablesService;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.datatype.DT_ApiMethodEnum;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Utility Component for building EDC request body json objects.
 */
@Component
@Slf4j
public class EdcRequestBodyBuilder {


    @Autowired
    private VariablesService variablesService;

    @Autowired
    private ObjectMapper MAPPER;

    private final String publicPolicyId = "policy1";
    private final String publicContractDefinitionId = "contractdef1";
    private final String EDC_NAMESPACE = "https://w3id.org/edc/v0.0.1/ns/";
    private final String VOCAB_KEY = "@vocab";
    private final String ODRL_NAMESPACE = "http://www.w3.org/ns/odrl/2/";
    private final String CX_TAXO_NAMESPACE = "https://w3id.org/catenax/taxonomy#";
    private final String CX_COMMON_NAMESPACE = "https://w3id.org/catenax/ontology/common#";
    private final String CX_VERSION_KEY = "https://w3id.org/catenax/ontology/common#version";
    private final String CX_VERSION_NUMBER = "2.0.0";
    private final String DCT_NAMESPACE = "https://purl.org/dc/terms/";
    private final String PURL_TYPE_KEY = "https://purl.org/dc/terms/type";


    /**
     * Creates a request body for requesting a catalog in DSP protocol.
     * You can add filter criteria. However, at the moment there are issues
     * with nested catalog item properties, so it seems advisable to check
     * for the filter criteria programmatically.
     *
     * @param counterPartyDspUrl The protocol url of the other party
     * @param filter             Key-value-pairs, may be empty or null
     * @return The request body
     */
    public ObjectNode buildBasicCatalogRequestBody(String counterPartyDspUrl, Map<String, String> filter) {
        var objectNode = getEdcContextObject();
        objectNode.put("protocol", "dataspace-protocol-http");
        objectNode.put("@type", "CatalogRequest");
        objectNode.put("counterPartyAddress", counterPartyDspUrl);
        if (filter != null && !filter.isEmpty()) {
            var querySpecNode = MAPPER.createObjectNode();
            objectNode.set("querySpec", querySpecNode);
            for (var entry : filter.entrySet()) {
                querySpecNode.put(entry.getKey(), entry.getValue());
            }
        }
        return objectNode;
    }

    /**
     * Build a request body for a request to register an api method as an asset in DSP protocol.
     *
     * @param apiMethod The API method you want to register
     * @return The request body
     */
    public JsonNode buildCreateAssetBody(DT_ApiMethodEnum apiMethod) {
        var body = MAPPER.createObjectNode();
        var context = MAPPER.createObjectNode();
        context.put(VOCAB_KEY, EDC_NAMESPACE);
        context.put("cx-taxo", CX_TAXO_NAMESPACE);
        context.put("cx-common", CX_COMMON_NAMESPACE);
        context.put("dct", DCT_NAMESPACE);
        body.set("@context", context);

        String apiId = variablesService.getApiAssetId(apiMethod);
        body.put("@id", apiId);
        var properties = MAPPER.createObjectNode();
        properties.put("asset:prop:type", "api");
        properties.put("asset:prop:apibusinessobject", "product-stock");
        properties.put("asset:prop:version", variablesService.getPurisApiVersion());
        properties.put("asset:prop:apipurpose", apiMethod.PURPOSE);
        body.set("properties", properties);
        var edcProperties = MAPPER.createObjectNode();
        edcProperties.put(CX_VERSION_KEY, CX_VERSION_NUMBER);
        var typeNode = MAPPER.createObjectNode();
        String taxonomy = DT_ApiMethodEnum.REQUEST == apiMethod ? "ProductStockRequestApi" : "ProductStockResponseApi";
        typeNode.put("@id", CX_TAXO_NAMESPACE + taxonomy);
        edcProperties.set(PURL_TYPE_KEY, typeNode);

        var dataAddress = MAPPER.createObjectNode();
        String url = apiMethod == DT_ApiMethodEnum.REQUEST ? variablesService.getRequestServerEndpoint() : variablesService.getResponseServerEndpoint();
        dataAddress.put("baseUrl", url);
        dataAddress.put("type", "HttpData");
        dataAddress.put("proxyPath", "true");
        dataAddress.put("proxyBody", "true");
        dataAddress.put("proxyMethod", "true");
        dataAddress.put("authKey", "x-api-key");
        dataAddress.put("authCode", variablesService.getApiKey());
        body.set("dataAddress", dataAddress);
        return body;
    }


    /**
     * Creates a request body for registering a public policy in DSP protocol that contains no
     * restrictions whatsoever.
     *
     * @return The request body
     */
    public JsonNode buildPublicPolicyBody() {
        var body = MAPPER.createObjectNode();
        var context = MAPPER.createObjectNode();
        context.put("odrl", ODRL_NAMESPACE);
        body.set("@context", context);
        body.put("@type", "PolicyDefinitionRequestDto");
        body.put("@id", publicPolicyId);
        var policy = MAPPER.createObjectNode();
        policy.put("@type", "set");
        var emptyArray = MAPPER.createArrayNode();
        policy.set("odrl:permission", emptyArray);
        policy.set("odrl:prohibition", emptyArray);
        policy.set("odrl:obligation", emptyArray);
        body.set("policy", policy);
        return body;
    }

    /**
     * Creates the request body for registering a public contract definition.
     * Relies on the policy that is created via the buildPublicPolicy() method.
     *
     * @return The request body
     */
    public JsonNode buildContractDefinitionWithPublicPolicyBody() {
        var body = getEdcContextObject();
        body.put("@id", publicContractDefinitionId);
        body.put("accessPolicyId", publicPolicyId);
        body.put("contractPolicyId", publicPolicyId);
        body.set("assetsSelector", MAPPER.createArrayNode());
        return body;
    }

    /**
     * Creates the request body for initiating a negotiation in DSP protocol.
     * Will use the policy terms as specified in the catalog item.
     *
     * @param partner         The Partner to negotiate with
     * @param dcatCatalogItem The catalog entry that describes the target asset.
     * @return The request body
     */
    public ObjectNode buildAssetNegotiationBody(Partner partner, JsonNode dcatCatalogItem) {
        var objectNode = MAPPER.createObjectNode();
        var contextNode = MAPPER.createObjectNode();
        contextNode.put(VOCAB_KEY, EDC_NAMESPACE);
        contextNode.put("odrl", ODRL_NAMESPACE);
        objectNode.set("@context", contextNode);
        objectNode.put("@type", "NegotiationInitiateRequestDto");
        objectNode.put("connectorId", partner.getBpnl());
        objectNode.put("connectorAddress", partner.getEdcUrl());
        objectNode.put("consumerId", variablesService.getOwnBpnl());
        objectNode.put("providerId", partner.getBpnl());
        objectNode.put("protocol", "dataspace-protocol-http");
        String assetId = dcatCatalogItem.get("@id").asText();
        var policyNode = dcatCatalogItem.get("odrl:hasPolicy");
        var offerNode = MAPPER.createObjectNode();
        String offerId = policyNode.get("@id").asText();
        offerNode.put("offerId", offerId);
        offerNode.put("assetId", assetId);
        offerNode.set("policy", policyNode);
        objectNode.set("offer", offerNode);
        return objectNode;
    }

    /**
     * Creates the request body for requesting a proxy pull transfer using the
     * DSP protocol and the Tractus-X-EDC.
     *
     * @param partner    The Partner who controls the target asset
     * @param contractID The contractId
     * @param assetId    The assetId
     * @return The request body
     */
    public JsonNode buildProxyPullRequestBody(Partner partner, String contractID, String assetId) {
        var body = getEdcContextObject();
        body.put("@type", "TransferRequestDto");
        body.put("connectorId", partner.getBpnl());
        body.put("connectorAddress", partner.getEdcUrl());
        body.put("contractId", contractID);
        body.put("assetId", assetId);
        body.put("protocol", "dataspace-protocol-http");
        body.put("managedResources", false);

        var dataDestination = MAPPER.createObjectNode();
        dataDestination.put("type", "HttpProxy");
        body.set("dataDestination", dataDestination);

        var privateProperties = MAPPER.createObjectNode();
        privateProperties.put("receiverHttpEndpoint", variablesService.getEdrEndpoint());
        body.set("privateProperties", privateProperties);
        log.info(body.toPrettyString());
        return body;
    }

    /**
     * A helper method returning a basic request object that can be used to build other
     * specific request bodies.
     *
     * @return A request body stub
     */
    private ObjectNode getEdcContextObject() {
        ObjectNode node = MAPPER.createObjectNode();
        var context = MAPPER.createObjectNode();
        context.put(VOCAB_KEY, EDC_NAMESPACE);
        node.set("@context", context);
        return node;
    }


}
