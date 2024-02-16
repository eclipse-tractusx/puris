/*
 * Copyright (c) 2023, 2024 Volkswagen AG
 * Copyright (c) 2023, 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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

package org.eclipse.tractusx.puris.backend.common.edc.logic.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
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
    private final String EDC_NAMESPACE = "https://w3id.org/edc/v0.0.1/ns/";
    private final String VOCAB_KEY = "@vocab";
    private final String ODRL_NAMESPACE = "http://www.w3.org/ns/odrl/2/";
    private final String CX_TAXO_NAMESPACE = "https://w3id.org/catenax/taxonomy#";
    private final String CX_COMMON_NAMESPACE = "https://w3id.org/catenax/ontology/common#";
    private final String DCT_NAMESPACE = "https://purl.org/dc/terms/";
    private final String FRAMEWORK_POLICY_ID = "Framework_Agreement_Policy";

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

    public JsonNode buildCreateItemStockAssetBody(DT_ApiMethodEnum apiMethod) {
        var body = getAssetRegistrationContext();
        body.put("@id", variablesService.getApiAssetId(apiMethod));
        var propertiesObject = MAPPER.createObjectNode();
        body.set("properties", propertiesObject);
        var dctTypeObject = MAPPER.createObjectNode();
        propertiesObject.set("dct:type", dctTypeObject);
        dctTypeObject.put("@id", "cx-taxo:" + apiMethod.CX_TAXO);
        propertiesObject.put("asset:prop:type", apiMethod.TYPE);
        propertiesObject.put("cx-common:version", "1.0");
        propertiesObject.put("description", apiMethod.DESCRIPTION);

        var dataAddress = MAPPER.createObjectNode();
        String url = switch (apiMethod) {
            case REQUEST -> variablesService.getRequestServerEndpoint();
            case RESPONSE -> variablesService.getResponseServerEndpoint();
            case STATUS_REQUEST -> variablesService.getStatusRequestServerEndpoint();
        };
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
     * Creates a request body in order to register a policy that
     * allows only the BPNL of the given partner.
     *
     * @param partner the partner
     * @return the request body
     */
    public JsonNode buildBpnRestrictedPolicy(Partner partner) {
        var body = MAPPER.createObjectNode();
        var context = MAPPER.createObjectNode();
        context.put("odrl", ODRL_NAMESPACE);
        body.set("@context", context);
        body.put("@type", "PolicyDefinitionRequestDto");
        body.put("@id", getBpnPolicyId(partner));
        var policy = MAPPER.createObjectNode();
        body.set("policy", policy);
        policy.put("@type", "Policy");
        var permissionsArray = MAPPER.createArrayNode();
        policy.set("odrl:permission", permissionsArray);
        var permissionsObject = MAPPER.createObjectNode();
        permissionsArray.add(permissionsObject);
        permissionsObject.put("odrl:action", "USE");
        var constraintObject = MAPPER.createObjectNode();
        permissionsObject.set("odrl:constraint", constraintObject);
        constraintObject.put("@type", "LogicalConstraint");
        constraintObject.put("odrl:leftOperand", "BusinessPartnerNumber");
        var operatorObject = MAPPER.createObjectNode();
        constraintObject.set("odrl:operator", operatorObject);
        operatorObject.put("@id", "odrl:eq");
        constraintObject.put("odrl:rightOperand", partner.getBpnl());
        return body;
    }

    /**
     * Creates a request body in order to register a policy that
     * allows only participants of the framework agreement.
     *
     * @return the request body
     */
    public JsonNode buildFrameworkAgreementPolicy() {
        var body = MAPPER.createObjectNode();
        var context = MAPPER.createObjectNode();
        context.put("odrl", ODRL_NAMESPACE);
        body.set("@context", context);
        body.put("@type", "PolicyDefinitionRequestDto");
        body.put("@id", FRAMEWORK_POLICY_ID);
        var policy = MAPPER.createObjectNode();
        body.set("policy", policy);
        policy.put("@type", "Policy");
        var permissionsArray = MAPPER.createArrayNode();
        policy.set("odrl:permission", permissionsArray);
        var permissionsObject = MAPPER.createObjectNode();
        permissionsArray.add(permissionsObject);
        permissionsObject.put("odrl:action", "USE");
        var constraintObject = MAPPER.createObjectNode();
        permissionsObject.set("odrl:constraint", constraintObject);
        constraintObject.put("@type", "LogicalConstraint");
        constraintObject.put("odrl:leftOperand", variablesService.getPurisFrameworkAgreement());
        var operatorObject = MAPPER.createObjectNode();
        constraintObject.set("odrl:operator", operatorObject);
        operatorObject.put("@id", "odrl:eq");
        constraintObject.put("odrl:rightOperand", "active");
        return body;
    }

    /**
     * Creates a request body in order to register a contract definition for the given partner and the given
     * api method that uses the BPNL-restricted policy created with the buildBpnRestrictedPolicy - method.
     * Depending on your configuration, it will also use the Framework Agreement Policy as the contract policy.
     *
     * @param partner   the partner
     * @param apiMethod the api method
     * @return the request body
     */
    public JsonNode buildContractDefinitionWithBpnRestrictedPolicy(Partner partner, DT_ApiMethodEnum apiMethod) {
        var body = getEdcContextObject();
        body.put("@id", partner.getBpnl() + "_contractdefinition_for_" + apiMethod);
        body.put("accessPolicyId", getBpnPolicyId(partner));
        if(variablesService.isUseFrameworkPolicy()) {
            body.put("contractPolicyId", FRAMEWORK_POLICY_ID);
        } else {
            body.put("contractPolicyId", getBpnPolicyId(partner));
        }
        var assetsSelector = MAPPER.createObjectNode();
        body.set("assetsSelector", assetsSelector);
        assetsSelector.put("@type", "CriterionDto");
        assetsSelector.put("operandLeft", EDC_NAMESPACE + "id");
        assetsSelector.put("operator", "=");
        assetsSelector.put("operandRight", variablesService.getApiAssetId(apiMethod));
        return body;
    }

    public JsonNode buildDtrContractDefinitionForPartner(Partner partner) {
        var body = getEdcContextObject();
        body.put("@id", partner.getBpnl() +"_contractdefinition_for_dtr");
        body.put("accessPolicyId", getBpnPolicyId(partner));
        body.put("contractPolicyId", getBpnPolicyId(partner));
        var assetsSelector = MAPPER.createObjectNode();
        body.set("assetsSelector", assetsSelector);
        assetsSelector.put("@type", "CriterionDto");
        assetsSelector.put("operandLeft", EDC_NAMESPACE + "id");
        assetsSelector.put("operator", "=");
        assetsSelector.put("operandRight", getDtrAssetId());
        return body;
    }

    /**
     * This method helps to ensure that the buildContractDefinitionWithBpnRestrictedPolicy uses the
     * same policy-id as the one that is created with the buildContractDefinitionWithBpnRestrictedPolicy
     * - method.
     *
     * @param partner the partner
     * @return the policy-id
     */
    private String getBpnPolicyId(Partner partner) {
        return partner.getBpnl() + "_policy";
    }

    /**
     * Creates the request body for initiating a negotiation in DSP protocol.
     * Will use the policy terms as specified in the catalog item.
     *
     * @param partner         The Partner to negotiate with
     * @param dcatCatalogItem The catalog entry that describes the target asset.
     * @return The request body
     */
    public JsonNode buildAssetNegotiationBody(Partner partner, JsonNode dcatCatalogItem) {
        var body = MAPPER.createObjectNode();
        var contextNode = MAPPER.createObjectNode();
        contextNode.put(VOCAB_KEY, EDC_NAMESPACE);
        contextNode.put("odrl", ODRL_NAMESPACE);
        body.set("@context", contextNode);
        body.put("@type", "NegotiationInitiateRequestDto");
        body.put("connectorId", partner.getBpnl());
        body.put("connectorAddress", partner.getEdcUrl());
        body.put("consumerId", variablesService.getOwnBpnl());
        body.put("providerId", partner.getBpnl());
        body.put("protocol", "dataspace-protocol-http");
        String assetId = dcatCatalogItem.get("@id").asText();
        var policyNode = dcatCatalogItem.get("odrl:hasPolicy");
        var offerNode = MAPPER.createObjectNode();
        String offerId = policyNode.get("@id").asText();
        offerNode.put("offerId", offerId);
        offerNode.put("assetId", assetId);
        offerNode.set("policy", policyNode);
        body.set("offer", offerNode);
        return body;
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
        return body;
    }

    /**
     * Creates the request body for requesting a full list of all
     * negotiations in the history of your EDC control plane.
     *
     * @return The request body
     */
    public JsonNode buildNegotiationsRequestBody() {
        var body = getEdcContextObject();
        body.put("@type", "QuerySpec");
        body.put("sortOrder", "DESC");
        body.put("sortField", "createdAt");
        return body;
    }

    /**
     * Creates the request body for requesting a full list of all
     * transfers in the history of your EDC control plane.
     *
     * @return The request body
     */
    public JsonNode buildTransfersRequestBody() {
        var body = getEdcContextObject();
        body.put("@type", "QuerySpec");
        body.put("sortOrder", "DESC");
        body.put("sortField", "stateTimestamp");
        return body;
    }

    public JsonNode buildDtrRegistrationBody() {
        var body = getAssetRegistrationContext();
        body.put("@id", getDtrAssetId());

        var propertiesObject = MAPPER.createObjectNode();
        var dctTypeObject = MAPPER.createObjectNode();
        dctTypeObject.put("@id", "cx-taxo:DigitalTwinRegistry");
        propertiesObject.set("dct:type", dctTypeObject);
        propertiesObject.put("cx-common:version", "3.0");
        propertiesObject.put("asset:prop:type", "data.core.digitalTwinRegistry");
        propertiesObject.put("description", "");
        body.set("properties", propertiesObject);

        var dataAddress = MAPPER.createObjectNode();
        String url = variablesService.getDtrUrl();
        dataAddress.put("@type", "DataAddress");
        dataAddress.put("proxyPath", "true");
        dataAddress.put("proxyQueryParams", "true");
        dataAddress.put("proxyMethod", "false");
        dataAddress.put("type", "HttpData");
        dataAddress.put("baseUrl", url);
        body.set("dataAddress", dataAddress);

        return body;
    }

    private String getDtrAssetId() {
        return "DigitalTwinRegistryId@" + variablesService.getOwnBpnl();
    }

    private ObjectNode getAssetRegistrationContext() {
        var body = MAPPER.createObjectNode();
        var context = MAPPER.createObjectNode();
        context.put(VOCAB_KEY, EDC_NAMESPACE);
        context.put("cx-taxo", CX_TAXO_NAMESPACE);
        context.put("cx-common", CX_COMMON_NAMESPACE);
        context.put("dct", DCT_NAMESPACE);
        body.set("@context", context);
        body.put("@type", "Asset");
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
