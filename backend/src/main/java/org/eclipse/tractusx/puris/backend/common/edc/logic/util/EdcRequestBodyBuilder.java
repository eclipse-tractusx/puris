/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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

package org.eclipse.tractusx.puris.backend.common.edc.logic.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.security.DtrSecurityConfiguration;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility Component for building EDC request body json objects.
 */
@Component
@Slf4j
public class EdcRequestBodyBuilder {

    @Autowired
    private DtrSecurityConfiguration dtrSecurityConfig;
    @Autowired
    private VariablesService variablesService;
    @Autowired
    private ObjectMapper MAPPER;
    public static final String EDC_NAMESPACE = "https://w3id.org/edc/v0.0.1/ns/";
    public static final String VOCAB_KEY = "@vocab";
    public static final String ODRL_NAMESPACE = "http://www.w3.org/ns/odrl/2/";
    public static final String ODRL_REMOTE_CONTEXT = "http://www.w3.org/ns/odrl.jsonld";
    public static final String CX_TAXO_NAMESPACE = "https://w3id.org/catenax/taxonomy#";
    public static final String CX_COMMON_NAMESPACE = "https://w3id.org/catenax/ontology/common#";
    public static final String CX_POLICY_NAMESPACE = "https://w3id.org/catenax/policy/";
    public static final String DCT_NAMESPACE = "http://purl.org/dc/terms/";
    public static final String AAS_SEMANTICS_NAMESPACE = "https://admin-shell.io/aas/3/0/HasSemantics/";
    public static final String CONTRACT_POLICY_ID = "Contract_Policy";
    public static final String TX_NAMESPACE = "https://w3id.org/tractusx/v0.0.1/ns/";
    public static final String TX_AUTH_NAMESPACE = "https://w3id.org/tractusx/auth/";
    public static final String DCAT_NAMESPACE = "http://www.w3.org/ns/dcat#";
    public static final String DSPACE_NAMESPACE = "https://w3id.org/dspace/v0.8/";
    public static final String CX_POLICY_CONTEXT = "https://w3id.org/tractusx/policy/v1.0.0";

    /**
     * helper class to encapsulate PolicyConstraint
     **/
    private record PolicyConstraint(String leftOperand, String operator, String rightOperand) {
    }

    /**
     * Creates a request body for requesting a catalog in DSP protocol.
     * You can add filter criteria. However, at the moment there are issues
     * with nested catalog item properties, so it seems advisable to check
     * for the filter criteria programmatically.
     *
     * @param counterPartyDspUrl The protocol url of the other party
     * @param counterPartyBpnl   The bpnl of the other party
     * @param filter             Key-value-pairs, may be empty or null
     * @return The request body
     */
    public ObjectNode buildBasicCatalogRequestBody(String counterPartyDspUrl, String counterPartyBpnl, Map<String, String> filter) {
        var objectNode = getEdcContextObject();
        objectNode.put("protocol", "dataspace-protocol-http");
        objectNode.put("@type", "CatalogRequest");
        objectNode.put("counterPartyAddress", counterPartyDspUrl);
        objectNode.put("counterPartyId", counterPartyBpnl);
        if (filter != null && !filter.isEmpty()) {
            ObjectNode querySpecObject = MAPPER.createObjectNode();
            ArrayNode filterExpressionsArray = MAPPER.createArrayNode();
            querySpecObject.set("filterExpression", filterExpressionsArray);
            for (var entry : filter.entrySet()) {
                ObjectNode filterExpressionObject = MAPPER.createObjectNode();
                filterExpressionObject.put("operandLeft", entry.getKey());
                filterExpressionObject.put("operator", "=");
                filterExpressionObject.put("operandRight", entry.getValue());
                filterExpressionsArray.add(filterExpressionObject);
            }
            objectNode.set("querySpec", querySpecObject);
        }
        log.debug("Built Catalog Request: \n" + objectNode.toPrettyString());
        return objectNode;
    }

    /**
     * create a policy for given constraints
     *
     * @param policyId      to use as for identification (must be unique)
     * @param constraints   list of constraints that are assembled via odrl:and (note: also if just one is given, and will be put)
     * @param policyProfile profile to use for odrl:policy, may be null (should only be used for contract policies)
     * @return body to use for policy request
     */
    private JsonNode buildPolicy(String policyId, List<PolicyConstraint> constraints, String policyProfile) {
        ObjectNode body = getPolicyContextObject();
        body.put("@type", "PolicyDefinitionRequestDto");
        body.put("@id", policyId);

        var policy = MAPPER.createObjectNode();
        body.set("edc:policy", policy);
        policy.put("@type", "Set");

        if (policyProfile != null && !policyProfile.isEmpty()) {
            policy.put("profile", policyProfile);
        }

        var permissionsArray = MAPPER.createArrayNode();
        policy.set("permission", permissionsArray);

        var permissionsObject = MAPPER.createObjectNode();
        permissionsArray.add(permissionsObject);
        permissionsObject.put("action", "use");

        var constraintObject = MAPPER.createObjectNode();
        permissionsObject.set("constraint", constraintObject);
        constraintObject.put("@type", "LogicalConstraint");

        var andArray = MAPPER.createArrayNode();
        constraintObject.set("and", andArray);

        for (PolicyConstraint policyConstraint : constraints) {
            ObjectNode constraint = MAPPER.createObjectNode();
            constraint.put("@type", "LogicalConstraint");
            constraint.put("leftOperand", policyConstraint.leftOperand);

            constraint.put("operator", policyConstraint.operator);

            constraint.put("rightOperand", policyConstraint.rightOperand);
            andArray.add(constraint);
        }

        return body;
    }

    /**
     * Creates a request body that demands all of the following conditions as access policy:
     * 1. The BPNL of the requesting connector is equal to the BPNL of the partner
     * 2. There's a valid CX membership credential present
     *
     * @param partner the partner to create the policy for
     * @return the request body as a {@link JsonNode}
     */
    public JsonNode buildBpnAndMembershipRestrictedPolicy(Partner partner) {

        List<PolicyConstraint> constraints = new ArrayList<>();

        constraints.add(new PolicyConstraint(
            "BusinessPartnerNumber",
            "eq",
            partner.getBpnl()
        ));

        constraints.add(new PolicyConstraint(
            "Membership",
            "eq",
            "active"
        ));

        JsonNode body = buildPolicy(
            getBpnPolicyId(partner),
            constraints,
            null
        );
        log.debug("Built bpn and membership access policy:\n{}", body.toPrettyString());

        return body;
    }

    /**
     * Creates a request body in order to register a contract policy that
     * allows only participants of the framework agreement.
     *
     * @return the request body
     */
    public JsonNode buildFrameworkPolicy() {

        List<PolicyConstraint> constraints = new ArrayList<>();

        constraints.add(new PolicyConstraint(
            CX_POLICY_NAMESPACE + "FrameworkAgreement",
            "eq",
            variablesService.getPurisFrameworkAgreementWithVersion()
        ));

        constraints.add(new PolicyConstraint(
            CX_POLICY_NAMESPACE + "UsagePurpose",
            "eq",
            variablesService.getPurisPurposeWithVersion()
        ));

        JsonNode body = buildPolicy(
            CONTRACT_POLICY_ID,
            constraints,
            "cx-policy:profile2405"
        );
        log.debug("Built framework agreement contract policy:\n{}", body.toPrettyString());

        return body;
    }

    public JsonNode buildSubmodelContractDefinitionWithBpnRestrictedPolicy(String assetId, Partner partner) {
        var body = getEdcContextObject();
        body.put("@id", partner.getBpnl() + "_contractdefinition_for_" + assetId);
        body.put("accessPolicyId", getBpnPolicyId(partner));
        body.put("contractPolicyId", CONTRACT_POLICY_ID);
        var assetsSelector = MAPPER.createObjectNode();
        body.set("assetsSelector", assetsSelector);
        assetsSelector.put("@type", "CriterionDto");
        assetsSelector.put("operandLeft", EDC_NAMESPACE + "id");
        assetsSelector.put("operator", "=");
        assetsSelector.put("operandRight", assetId);
        return body;
    }

    public JsonNode buildDtrContractDefinitionForPartner(Partner partner) {
        var body = getEdcContextObject();
        body.put("@id", partner.getBpnl() + "_contractdefinition_for_dtr");
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

    public JsonNode buildPartTypeInfoContractDefinitionForPartner(Partner partner) {
        var body = getEdcContextObject();
        body.put("@id", partner.getBpnl() + "_contractdefinition_for_PartTypeInfoAsset");
        body.put("accessPolicyId", getBpnPolicyId(partner));
        body.put("contractPolicyId", CONTRACT_POLICY_ID);
        var assetsSelector = MAPPER.createObjectNode();
        body.set("assetsSelector", assetsSelector);
        assetsSelector.put("@type", "CriterionDto");
        assetsSelector.put("operandLeft", EDC_NAMESPACE + "id");
        assetsSelector.put("operator", "=");
        assetsSelector.put("operandRight", getPartTypeInfoAssetId());
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
     * @param dspUrl          The dspUrl if a specific (not from MAD Partner) needs to be used, not null
     * @return The request body
     */
    public JsonNode buildAssetNegotiationBody(Partner partner, JsonNode dcatCatalogItem, String dspUrl) {
        ObjectNode body = MAPPER.createObjectNode();
        ObjectNode contextNode = MAPPER.createObjectNode();
        contextNode.put(VOCAB_KEY, EDC_NAMESPACE);
        contextNode.put("odrl", ODRL_NAMESPACE);
        body.set("@context", contextNode);
        body.put("@type", "ContractRequest");
        body.put("counterPartyAddress", dspUrl);
        body.put("protocol", "dataspace-protocol-http");

        // extract policy and information from offer
        // framework agreement and co has been checked during catalog request
        String assetId = dcatCatalogItem.get("@id").asText();
        JsonNode policyNode = dcatCatalogItem.get(ODRL_NAMESPACE + "hasPolicy");
        if (policyNode.isArray()) {
            policyNode = policyNode.get(0);
        }

        ObjectNode targetIdObject = MAPPER.createObjectNode();
        targetIdObject.put("@id", assetId);
        ((ObjectNode) policyNode).put("@context", "http://www.w3.org/ns/odrl.jsonld");
        ((ObjectNode) policyNode).set("target", targetIdObject);
        ((ObjectNode) policyNode).put("assigner", partner.getBpnl());

        ObjectNode offerNode = MAPPER.createObjectNode();
        String offerId = policyNode.get("@id").asText();
        offerNode.put("offerId", offerId);
        offerNode.put("assetId", assetId);
        offerNode.set("policy", policyNode);

        body.set("policy", policyNode);

        log.debug("Created asset negotiation body:\n" + body.toPrettyString());
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
    public JsonNode buildProxyPullRequestBody(Partner partner, String contractID, String partnerEdcUrl) {
        var body = getEdcContextObject();
        body.put("connectorId", partner.getBpnl());
        body.put("counterPartyAddress", partnerEdcUrl);
        body.put("contractId", contractID);
        body.put("protocol", "dataspace-protocol-http");
        body.put("managedResources", false);
        body.put("transferType", "HttpData-PULL");

        var dataDestination = MAPPER.createObjectNode();
        dataDestination.put("type", "HttpProxy");
        body.set("dataDestination", dataDestination);

        log.debug("Built Proxy Pull Request:\n{}", body.toPrettyString());
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
        /* 
        to be readded with edc version 0.7.2
        
        body.put("sortOrder", "DESC");
        body.put("sortField", "stateTimestamp"); 
        */
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
        body.set("properties", propertiesObject);

        var dataAddress = MAPPER.createObjectNode();
        String url = variablesService.getDtrUrl();
        dataAddress.put("@type", "DataAddress");
        dataAddress.put("proxyPath", "true");
        dataAddress.put("proxyQueryParams", "true");
        dataAddress.put("proxyMethod", "false");
        dataAddress.put("type", "HttpData");
        dataAddress.put("baseUrl", url);
        // if IDP is configured, grant only read-access via idp
        if (dtrSecurityConfig.isOauth2InterceptorEnabled()) {
            dataAddress.put("oauth2:clientId", dtrSecurityConfig.getEdcClientId());
            dataAddress.put("oauth2:clientSecretKey", dtrSecurityConfig.getEdcClientSecretAlias());
            dataAddress.put("oauth2:tokenUrl", dtrSecurityConfig.getTokenUrl());
        }
        body.set("dataAddress", dataAddress);

        return body;
    }

    public JsonNode buildSubmodelRegistrationBody(String assetId, String endpoint, String semanticId) {
        var body = getAssetRegistrationContext();
        body.put("@id", assetId);
        var propertiesObject = MAPPER.createObjectNode();
        body.set("properties", propertiesObject);
        var dctTypeObject = MAPPER.createObjectNode();
        propertiesObject.set("dct:type", dctTypeObject);
        dctTypeObject.put("@id", "cx-taxo:Submodel");
        propertiesObject.put("cx-common:version", "3.0");
        var semanticIdObject = MAPPER.createObjectNode();
        propertiesObject.set("aas-semantics:semanticId", semanticIdObject);
        semanticIdObject.put("@id", semanticId);
        body.set("privateProperties", MAPPER.createObjectNode());
        body.set("dataAddress", createDataAddressObject(endpoint, "false"));
        return body;
    }

    public JsonNode buildNotificationRegistrationBody(String assetId, String endpoint) {
        var body = getAssetRegistrationContext();
        body.put("@id", assetId);
        var propertiesObject = MAPPER.createObjectNode();
        body.set("properties", propertiesObject);
        var dctTypeObject = MAPPER.createObjectNode();
        propertiesObject.set("dct:type", dctTypeObject);
        dctTypeObject.put("@id", "cx-taxo:DemandAndCapacityNotificationApi");
        propertiesObject.put("cx-common:version", "1.0");
        body.set("dataAddress", createDataAddressObject(endpoint, "true"));
        return body;
    }

    public JsonNode createDataAddressObject(String endpoint, String proxyMethodAndBody) {
        var dataAddress = MAPPER.createObjectNode();
        dataAddress.put("@type", "DataAddress");
        dataAddress.put("proxyPath", "true");
        dataAddress.put("proxyQueryParams", "false");
        dataAddress.put("proxyMethod", proxyMethodAndBody);
        dataAddress.put("proxyBody", proxyMethodAndBody);
        dataAddress.put("type", "HttpData");
        dataAddress.put("baseUrl", endpoint);
        dataAddress.put("authKey", "x-api-key");
        dataAddress.put("authCode", variablesService.getApiKey());
        return dataAddress;
    }

    public JsonNode buildPartTypeInfoSubmodelRegistrationBody() {
        var body = getAssetRegistrationContext();
        body.put("@id", getPartTypeInfoAssetId());
        var propertiesObject = MAPPER.createObjectNode();
        body.set("properties", propertiesObject);
        var dctTypeObject = MAPPER.createObjectNode();
        propertiesObject.set("dct:type", dctTypeObject);
        dctTypeObject.put("@id", CX_TAXO_NAMESPACE + "Submodel");
        propertiesObject.put("cx-common:version", "3.0");
        var semanticIdObject = MAPPER.createObjectNode();
        propertiesObject.set("aas-semantics:semanticId", semanticIdObject);
        semanticIdObject.put("@id", "urn:samm:io.catenax.part_type_information:1.0.0#PartTypeInformation");
        var dataAddress = MAPPER.createObjectNode();
        String url = variablesService.getParttypeInformationServerendpoint();
        if (!url.endsWith("/")) {
            url += "/";
        }
        dataAddress.put("@type", "DataAddress");
        dataAddress.put("proxyPath", "true");
        dataAddress.put("proxyQueryParams", "false");
        dataAddress.put("proxyMethod", "false");
        dataAddress.put("type", "HttpData");
        dataAddress.put("baseUrl", url);
        dataAddress.put("authKey", "x-api-key");
        dataAddress.put("authCode", variablesService.getApiKey());
        body.set("dataAddress", dataAddress);
        return body;
    }

    private String getDtrAssetId() {
        return "DigitalTwinRegistryId@" + variablesService.getOwnBpnl();
    }

    private String getPartTypeInfoAssetId() {
        return variablesService.getPartTypeSubmodelApiAssetId();
    }

    private ObjectNode getAssetRegistrationContext() {
        var body = MAPPER.createObjectNode();
        var context = MAPPER.createObjectNode();
        context.put(VOCAB_KEY, EDC_NAMESPACE);
        context.put("cx-taxo", CX_TAXO_NAMESPACE);
        context.put("cx-common", CX_COMMON_NAMESPACE);
        context.put("dct", DCT_NAMESPACE);
        context.put("aas-semantics", AAS_SEMANTICS_NAMESPACE);
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

    /**
     * A helper method returning a basic request object meant for policy interactions to be used to build other
     * specific request bodies.
     *
     * @return A request body stub
     */
    private ObjectNode getPolicyContextObject() {
        ObjectNode node = MAPPER.createObjectNode();
        ArrayNode contextArray = MAPPER.createArrayNode();
        contextArray.add(ODRL_REMOTE_CONTEXT);

        ObjectNode contextObject = MAPPER.createObjectNode();
        contextObject.put("edc", EDC_NAMESPACE);
        contextObject.put("cx-policy", CX_POLICY_NAMESPACE);
        contextArray.add(contextObject);

        node.set("@context", contextArray);
        return node;
    }


    /**
     * builds a body to terminate the transfer process
     *
     * @param reason why transfer is terminated
     * @return transfer process termination request body
     */
    public JsonNode buildTransferProcessTerminationBody(String reason) {

        ObjectNode body = getEdcContextObject();

        body.put("@type", "TerminateTransfer");
        body.put("reason", reason);

        return body;
    }
}
