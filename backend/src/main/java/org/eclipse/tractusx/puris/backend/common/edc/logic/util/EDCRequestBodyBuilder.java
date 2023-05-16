package org.eclipse.tractusx.puris.backend.common.edc.logic.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_UseCaseEnum;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.*;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.datatype.DT_ApiBusinessObjectEnum;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.datatype.DT_ApiMethodEnum;
import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.datatype.DT_DataAddressTypeEnum;

/**
 * Static Utility Class for building EDC request body json objects.
 */
public class EDCRequestBodyBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Build an EDC request body for the creation of an asset (using an order).
     *
     * @param orderUrl url where the published order can be received by the controlplane.
     * @param orderId  id of the created asset (currently has to match policy and contract id).
     * @return JsonNode used as requestBody for asset creation.
     */
    public static JsonNode buildAssetRequestBody(String orderUrl, String orderId) {
        var assetRequest = MAPPER.createObjectNode();
        var assetNode = MAPPER.createObjectNode();
        var assetPropNode = MAPPER.createObjectNode();
        assetNode.set("properties", assetPropNode);
        assetPropNode.put("asset:prop:id", orderId);
        assetPropNode.put("asset:prop:description", "EDC Demo Asset");
        assetRequest.set("asset", assetNode);
        var dataAddressNode = MAPPER.createObjectNode();
        var propertiesNode = MAPPER.createObjectNode();
        dataAddressNode.set("properties", propertiesNode);
        propertiesNode.put("type", "HttpData");
        propertiesNode.put("baseUrl", orderUrl);
        assetRequest.set("dataAddress", dataAddressNode);
        return assetRequest;
    }

    /**
     * Build an EDC request body for the creation of a simple USE policy.
     *
     * @param orderId id of the policy to create (currently has to match contract and asset id).
     * @return JsonNode used as requestBody for policy creation.
     */
    public static JsonNode buildPolicyRequestBody(String orderId) {
        var policyNode = MAPPER.createObjectNode();
        var policySubNode = MAPPER.createObjectNode();
        policySubNode.set("prohibitions", MAPPER.createArrayNode());
        policySubNode.set("obligations", MAPPER.createArrayNode());
        var permissionArray = MAPPER.createArrayNode();
        var permissionNode = MAPPER.createObjectNode();
        permissionNode.put("edctype", "dataspaceconnector:permission");
        permissionNode.set("constraints", MAPPER.createArrayNode());
        var actionNode = MAPPER.createObjectNode();
        actionNode.put("type", "USE");
        permissionNode.set("action", actionNode);
        permissionArray.add(permissionNode);
        policySubNode.set("permissions", permissionArray);
        policyNode.put("id", orderId);
        policyNode.set("policy", policySubNode);
        return policyNode;
    }

    /**
     * Build an EDC request body for the creation of a contract.
     *
     * @param orderId id of the contract to create (currently has to match policy and asset id).
     * @return JsonNode used as requestBody for contract creation.
     */
    public static JsonNode buildContractRequestBody(String orderId) {
        var contractNode = MAPPER.createObjectNode();
        contractNode.put("id", orderId);
        contractNode.put("accessPolicyId", orderId);
        contractNode.put("contractPolicyId", orderId);
        var criteriaArray = MAPPER.createArrayNode();
        var criteriaNode = MAPPER.createObjectNode();
        criteriaNode.put("operandLeft", "asset:prop:id");
        criteriaNode.put("operator", "=");
        criteriaNode.put("operandRight", orderId);
        criteriaArray.add(criteriaNode);
        contractNode.set("criteria", criteriaArray);
        return contractNode;
    }

    /**
     * Build an EDC request body used for starting a negotiation.
     *
     * @param connectorAddress ids url of the negotiation counterparty.
     * @param orderId          id of the negotiations target asset.
     * @return JsonNode used as requestBody for an EDC negotiation request.
     */
    public static JsonNode buildNegotiationRequestBody(String connectorAddress, String orderId) {
        var negotiationNode = MAPPER.createObjectNode();
        negotiationNode.put("connectorId", "foo");
        negotiationNode.put("connectorAddress", connectorAddress);
        negotiationNode.put("protocol", "ids-multipart");
        var offerNode = MAPPER.createObjectNode();
        offerNode.put("offerId", orderId + ":foo");
        offerNode.put("assetId", orderId);
        var policyNode = MAPPER.createObjectNode();
        policyNode.put("uid", orderId);
        policyNode.set("prohibiitons", MAPPER.createArrayNode());
        policyNode.set("obligations", MAPPER.createArrayNode());
        var permissionArray = MAPPER.createArrayNode();
        var permissionNode = MAPPER.createObjectNode();
        permissionNode.put("edctype", "dataspaceconnector:permission");
        permissionNode.put("target", orderId);
        permissionNode.set("constraints", MAPPER.createArrayNode());
        var actionNode = MAPPER.createObjectNode();
        actionNode.put("type", "USE");
        permissionNode.set("action", actionNode);
        permissionArray.add(permissionNode);
        policyNode.set("permissions", permissionArray);
        offerNode.set("policy", policyNode);
        negotiationNode.set("offer", offerNode);
        return negotiationNode;
    }

    public static JsonNode buildNegotiationRequestBody(String connectorAddress,
                                                       String contractDefinitionId,
                                                       String assetId) {
        var negotiationNode = MAPPER.createObjectNode();
        negotiationNode.put("connectorId", "foo");
        negotiationNode.put("connectorAddress", connectorAddress);
        negotiationNode.put("protocol", "ids-multipart");
        var offerNode = MAPPER.createObjectNode();
        offerNode.put("offerId", contractDefinitionId);
        offerNode.put("assetId", assetId);
        var policyNode = MAPPER.createObjectNode();
        var permissionsArray = MAPPER.createArrayNode();
        policyNode.put("uid", assetId);
        policyNode.set("prohibiitons", MAPPER.createArrayNode());
        policyNode.set("obligations", MAPPER.createArrayNode());
        var permissionArray = MAPPER.createArrayNode();
        var permissionNode = MAPPER.createObjectNode();
        permissionNode.put("edctype", "dataspaceconnector:permission");
        permissionNode.put("target", assetId);
        permissionNode.set("constraints", MAPPER.createArrayNode());
        var actionNode = MAPPER.createObjectNode();
        actionNode.put("type", "USE");
        permissionNode.set("action", actionNode);
        permissionArray.add(permissionNode);
        policyNode.set("permissions", permissionArray);
        offerNode.set("policy", policyNode);
        negotiationNode.set("offer", offerNode);
        return negotiationNode;
    }

    /**
     * Build an EDC request body used for starting a transfer.
     *
     * @param transferId       id created for the transferprocess.
     * @param connectorAddress ids url of the negotiation counterparty.
     * @param contractId       id of the negotiated contract.
     * @param orderId          id of the transfers target asset.
     * @return JsonNode used as requestBody for an EDC transfer request.
     */
    public static JsonNode buildTransferRequestBody(String transferId,
                                                    String connectorAddress,
                                                    String contractId,
                                                    String orderId) {
        var transferNode = MAPPER.createObjectNode();
        transferNode.put("id", transferId);
        transferNode.put("connectorId", "foo");
        transferNode.put("connectorAddress", connectorAddress);
        transferNode.put("contractId", orderId + ":" + contractId);
        transferNode.put("assetId", orderId);
        transferNode.put("managedResources", "false");
        var destinationNode = MAPPER.createObjectNode();
        destinationNode.put("type", "HttpProxy");
        transferNode.set("dataDestination", destinationNode);
        return transferNode;
    }

    /**
     * Builds a CreateAssetDto for an API
     *
     * @param method     api method to create
     * @param apiBaseUrl api baseUrl to get the data at
     * @return assetDto for creation.
     */
    public static CreateAssetDto buildCreateAssetDtoForApi(DT_ApiMethodEnum method,
                                                           String apiBaseUrl) {
        AssetPropertiesDto apiAssetPropertiesDto = new AssetPropertiesDto();
        if (method == DT_ApiMethodEnum.REQUEST) {
            apiAssetPropertiesDto.setId("product-stock-request-api");
            apiAssetPropertiesDto.setName("Product Stock Request API");
            apiAssetPropertiesDto.setApiMethod(DT_ApiMethodEnum.REQUEST);
        } else if (method == DT_ApiMethodEnum.RESPONSE) {

            apiAssetPropertiesDto.setId("product-stock-response-api");
            apiAssetPropertiesDto.setName("Product Stock Response API");
            apiAssetPropertiesDto.setApiMethod(DT_ApiMethodEnum.RESPONSE);
        }
        apiAssetPropertiesDto.setContentType("appplication/json");
        apiAssetPropertiesDto.setApiBusinessObject(DT_ApiBusinessObjectEnum.productStock);
        apiAssetPropertiesDto.setUseCase(DT_UseCaseEnum.PURIS);

        AssetDto apiAssetDto = new AssetDto();
        apiAssetDto.setPropertiesDto(apiAssetPropertiesDto);

        DataAddressPropertiesDto apiDataAddressPropertiesDto =
                new DataAddressPropertiesDto();
        apiDataAddressPropertiesDto.setBaseUrl(apiBaseUrl);
        apiDataAddressPropertiesDto.setType(DT_DataAddressTypeEnum.HttpData);
        apiDataAddressPropertiesDto.setProxyBody(true);
        apiDataAddressPropertiesDto.setProxyMethod(true);

        DataAddressDto apiDataAddressDto = new DataAddressDto();
        apiDataAddressDto.setDataAddressPropertiesDto(apiDataAddressPropertiesDto);

        CreateAssetDto createApiAssetDto = new CreateAssetDto();
        createApiAssetDto.setAssetDto(apiAssetDto);
        createApiAssetDto.setDataAddressDto(apiDataAddressDto);

        return createApiAssetDto;
    }

}
