/*
 * Copyright (c) 2025 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.AssetType;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.mockito.Mockito.when;

class DtrRequestBodyBuilderTest {

    @InjectMocks
    DtrRequestBodyBuilder dtrRequestBodyBuilder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    VariablesService variablesService;

    private final static String TEST_CONST_DIGITAL_TWIN_TYPE = "digitalTwinType";
    private final static String TEST_CONST_MANUFACTURER_PART_ID = "manufacturerPartId";
    private final static String TEST_CONST_MANUFACTURER_ID = "manufacturerId";
    private final static String TEST_CONST_CUSTOMER_PART_ID = "customerPartId";

    private final static String ENV_VAR_EDC_DATA_PLANE_PUBLIC_URL = "https://data-plane.com/api/public/";
    private final static String ENV_VAR_EDC_CONTROL_PLANE_PROTOCOL_URL = "https://control-plane:com/api/v1/dsp";
    private final static String ENV_VAR_OWN_BPNL = "BPNL4444444444ZZ";

    private Material MATERIAL;
    private Partner PARTNER;
    private Partner PARTNER2;
    private MaterialPartnerRelation MPR;
    private MaterialPartnerRelation MPR2;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // make variablesService mockable
        Field field = DtrRequestBodyBuilder.class.getDeclaredField("variablesService");
        field.setAccessible(true);
        field.set(dtrRequestBodyBuilder, variablesService);

        // make objectMapper a real object
        Field objectMapperField = DtrRequestBodyBuilder.class.getDeclaredField("objectMapper");
        objectMapperField.setAccessible(true);
        objectMapperField.set(dtrRequestBodyBuilder, objectMapper);

        MATERIAL = Material.builder()
            .ownMaterialNumber("MNR-4711")
            .materialFlag(true)
            .productFlag(true)
            .name("Test Material 1")
            .materialNumberCx("urn:uuid:ccfffbba-cfa0-49c4-bc9c-4e13d7a4ac7a")
            .build();

        PARTNER = new Partner(
            "Test Partner",
            "https://edc-url.com/api/v1/dsp",
            "BPNL1234567890ZZ",
            "BPNS1234567890ZZ",
            "Test Site",
            "BPNA1234567890ZZ",
            "Test Street 5",
            "4711 Test City",
            "Testonia"
        );

        PARTNER2 = new Partner(
            "Test Partner",
            "https://other-edc-url.com/api/v1/dsp",
            "BPNL6666666666ZZ",
            "BPNS6666666666ZZ",
            "Test Site",
            "BPNA6666666666ZZ",
            "Test Street 5",
            "4711 Test City",
            "Testonia"
        );

        MPR = new MaterialPartnerRelation(
            MATERIAL,
            PARTNER,
            "MNR-4711-S",
            true,
            true
        );
        MPR.setPartnerCXNumber("urn:uuid:ccfffbba-cfa0-49c4-bc9c-4e13d7a4ac88");

        MPR2 = new MaterialPartnerRelation(
            MATERIAL,
            PARTNER2,
            "MNR-4711-S2",
            true,
            true
        );
        MPR2.setPartnerCXNumber("urn:uuid:ccfffbba-cfa0-49c4-bc9c-4e13d7a4ac99");
    }

    /**
     * Asserts the correct creation of a ShellDescriptor for a material (shared copy of the twin)
     * <p>
     * Performs the following CX-0002 compliance checks:
     * <li>SubmodelDescriptor via {@linkplain #assertSubmodelDescriptor(JsonNode, AssetType, DirectionCharacteristic, String)} including semanticId</li>
     * <li>SpecificAssetIds via {@linkplain #assertSpecificAssetIds(JsonNode, String, List)}</li>
     * </p>
     * Only checks for the customer side of information shared.
     */
    @Test
    void createMaterialRegistrationRequestBody_givenOneMaterial_ReturnsCompliant() {

        //given Material, Partner, MPR with MPR.partnerBuys=true

        //when
        when(variablesService.getEdcDataplanePublicUrl()).thenReturn(ENV_VAR_EDC_DATA_PLANE_PUBLIC_URL);
        when(variablesService.getEdcProtocolUrl()).thenReturn(ENV_VAR_EDC_CONTROL_PLANE_PROTOCOL_URL);

        JsonNode materialShellDescriptor = dtrRequestBodyBuilder.createMaterialRegistrationRequestBody(MPR);

        Assertions.assertEquals(MPR.getPartnerCXNumber(), materialShellDescriptor.get("globalAssetId").asText());

        JsonNode specificAssetIds = materialShellDescriptor.get("specificAssetIds");

        for (JsonNode specificAssetId : specificAssetIds) {
            switch (specificAssetId.get("name").asText()) {
                case TEST_CONST_DIGITAL_TWIN_TYPE ->
                    assertSpecificAssetIds(specificAssetId, "PartType", List.of(PARTNER.getBpnl()));
                case TEST_CONST_CUSTOMER_PART_ID ->
                    assertSpecificAssetIds(specificAssetId, MATERIAL.getOwnMaterialNumber(), List.of(PARTNER.getBpnl()));
                case TEST_CONST_MANUFACTURER_ID ->
                    assertSpecificAssetIds(specificAssetId, PARTNER.getBpnl(), List.of(PARTNER.getBpnl()));
                case TEST_CONST_MANUFACTURER_PART_ID ->
                    assertSpecificAssetIds(specificAssetId, MPR.getPartnerMaterialNumber(), List.of(PARTNER.getBpnl()));
            }
        }

        JsonNode submodelDescriptors = materialShellDescriptor.get("submodelDescriptors");

        for (JsonNode submodelDescriptor : submodelDescriptors) {

            // assume that we only create one semanticId
            JsonNode semanticId = submodelDescriptor.get("semanticId").get("keys").get(0);

            switch (AssetType.fromUrn(semanticId.get("value").asText())) {
                case AssetType.ITEM_STOCK_SUBMODEL ->
                    assertSubmodelDescriptor(submodelDescriptor, AssetType.ITEM_STOCK_SUBMODEL, DirectionCharacteristic.INBOUND, MPR.getPartnerCXNumber());
                case AssetType.PRODUCTION_SUBMODEL ->
                    assertSubmodelDescriptor(submodelDescriptor, AssetType.PRODUCTION_SUBMODEL, DirectionCharacteristic.INBOUND, MPR.getPartnerCXNumber());
                case AssetType.DAYS_OF_SUPPLY ->
                    assertSubmodelDescriptor(submodelDescriptor, AssetType.DAYS_OF_SUPPLY, DirectionCharacteristic.INBOUND, MPR.getPartnerCXNumber());
                case AssetType.DELIVERY_SUBMODEL ->
                    assertSubmodelDescriptor(submodelDescriptor, AssetType.DELIVERY_SUBMODEL, DirectionCharacteristic.INBOUND, MPR.getPartnerCXNumber());
            }
        }
    }

    /**
     * Asserts that the SubmodelDescriptor matches CX-0002 and the endpoint definitions in this application
     * <p>
     * Performs the following checks regarding CX-0002 ONLY on DSP, submodel-3.0 endpoints (assumes there is only one)
     * <li><code>ProtocolInformation.subprotocolBodyEncoding</code> is set following CX-0002</li>
     * <li><code>ProtocolInformation.href</code> is set to the data plane cut-off with parametrization following CX-0002 conventions(see EDC Asset dataAddress.baseUrl in registration of submodel assets in {@linkplain org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService})</li>
     * <li><code>ProtocolInformation.subprotocolBody</code> is in form of "assetId=\<id\>;dspEndpoint=\<dsp api endpoint of edc\>"</li>
     * </p>
     * <b>ATTENTION:</b> The application currently only handles one SemanticId.
     *
     * @param submodelDescriptor to perform assertions on, must not be null
     * @param submodel           that is represented by this submodel descriptor, must not be null
     * @param direction          that is associated with the submodel type and may be used in the <code>href</code> field, must not be null
     * @param materialNumberCx   that is used for exposure in the <code>href</code> field, must not be null
     */
    private void assertSubmodelDescriptor(JsonNode submodelDescriptor, AssetType submodel, DirectionCharacteristic direction, String materialNumberCx) {

        JsonNode endpoints = submodelDescriptor.get("endpoints");

        // Convert the JsonNode array to a Stream
        Stream<JsonNode> endpointStream = StreamSupport.stream(endpoints.spliterator(), false);

        List<JsonNode> dspEndpoints = endpointStream.filter(
            endpoint -> "SUBMODEL-3.0".equals(endpoint.path("interface").asText()) &&
                "DSP".equals(endpoint.path("protocolInformation").path("subprotocol").asText())
        ).toList();

        Assertions.assertEquals(1, dspEndpoints.size());
        JsonNode protocolInformation = dspEndpoints.getFirst().get("protocolInformation");
        Assertions.assertEquals("plain", protocolInformation.get("subprotocolBodyEncoding").asText());

        String subprotocolBody = protocolInformation.get("subprotocolBody").asText();

        // format: id=<asset_id>;dspEndpoint=<control plan url dsp url>
        String[] bodyParts = subprotocolBody.split(";");

        String idEquation = bodyParts[0];
        String dspEndpointEquation = bodyParts[1];

        Assertions.assertTrue(idEquation.startsWith("id="));

        Assertions.assertTrue(dspEndpointEquation.startsWith("dspEndpoint="));
        Assertions.assertEquals(ENV_VAR_EDC_CONTROL_PLANE_PROTOCOL_URL, dspEndpointEquation.split("=")[1]);

        // href
        // format=<data plane public url>/<your information for the endpoint>/<direction for specific submodel>/submodel
        String href = protocolInformation.path("href").asText();

        boolean needsDirection = submodel.URN_SEMANTIC_ID.equals(AssetType.ITEM_STOCK_SUBMODEL.URN_SEMANTIC_ID)
            || submodel.URN_SEMANTIC_ID.equals(AssetType.DAYS_OF_SUPPLY.URN_SEMANTIC_ID);

        if (needsDirection && direction == DirectionCharacteristic.INBOUND) {
            Assertions.assertEquals(href, ENV_VAR_EDC_DATA_PLANE_PUBLIC_URL + materialNumberCx + "/INBOUND/submodel");
        } else if (needsDirection && direction == DirectionCharacteristic.OUTBOUND) {
            Assertions.assertEquals(href, ENV_VAR_EDC_DATA_PLANE_PUBLIC_URL + materialNumberCx + "/OUTBOUND/submodel");
        } else if (!needsDirection) {
            System.out.println("href: " + href);
            Assertions.assertEquals(href, ENV_VAR_EDC_DATA_PLANE_PUBLIC_URL + materialNumberCx + "/submodel");
        }
    }

    /**
     * asserts that the specificAssetId exactly matches the definition of CX-0002
     * <p>
     * It's assumed, that the specificAssetId Type has already been checked.
     * </p>
     * Performs the following checks:
     * <li><code>ExternalSubjectId</code> is set to CX-0002</li>
     * <li>The expected BPNLs must all be present in the <code>ExternalSubjectId.keys[].value</code></li>
     * <li>The expected BPNLs size must match the size of <code>ExternalSubjectId.keys</code></li>
     *
     * @param specificAssetId             node of the specificAssetId to check, must not be null
     * @param expectedValue               value to be expected in the value field, must not be null
     * @param expectedToBeVisibleForBpnls list of BPNLs that must EXACTLY match the keys, may not be null
     */
    private void assertSpecificAssetIds(JsonNode specificAssetId, String expectedValue, List<String> expectedToBeVisibleForBpnls) {
        Assertions.assertEquals(expectedValue, specificAssetId.get("value").asText());

        JsonNode externalSubjectId = specificAssetId.get("externalSubjectId");
        Assertions.assertEquals("ExternalReference", externalSubjectId.get("type").asText());
        JsonNode keys = externalSubjectId.get("keys");
        Assertions.assertEquals(expectedToBeVisibleForBpnls.size(), keys.size());

        for (String expectedBpnl : expectedToBeVisibleForBpnls) {
            // Convert the JsonNode array to a Stream
            Stream<JsonNode> keysStream = StreamSupport.stream(keys.spliterator(), false);
            Optional<JsonNode> keyOpt = keysStream.filter(
                keyInStream -> expectedBpnl.equals(keyInStream.get("value").asText())
            ).findFirst();
            Assertions.assertTrue(keyOpt.isPresent());
            JsonNode key = keyOpt.get();
            Assertions.assertEquals("GlobalReference", key.get("type").asText());
            Assertions.assertEquals(expectedBpnl, key.get("value").asText());
        }
    }

    /**
     * asserts the correct creation of a ShellDescriptor for a product with two customers (you create the twin)
     * <p>
     * Performs the following CX-0002 compliance checks:
     * <li>SubmodelDescriptor via {@linkplain #assertSubmodelDescriptor(JsonNode, AssetType, DirectionCharacteristic, String)} including semanticId</li>
     * <li>SpecificAssetIds via {@linkplain #assertSpecificAssetIds(JsonNode, String, List)} considering multiple partners to see a specificAssetId or not</li>
     * </p>
     * Only checks for the supplier side of information shared.
     */
    @Test
    void createProductRegistrationRequestBody() {
        // given Material
        // Partner, MPR with MPR.partnerSupplies=true
        // Partner2, MPR2 with MPR.partnerSupplies=true

        // when
        when(variablesService.getEdcDataplanePublicUrl()).thenReturn(ENV_VAR_EDC_DATA_PLANE_PUBLIC_URL);
        when(variablesService.getEdcProtocolUrl()).thenReturn(ENV_VAR_EDC_CONTROL_PLANE_PROTOCOL_URL);
        when(variablesService.getOwnBpnl()).thenReturn(ENV_VAR_OWN_BPNL);

        // operation
        JsonNode materialShellDescriptor = dtrRequestBodyBuilder.createProductRegistrationRequestBody(
            MATERIAL,
            MATERIAL.getMaterialNumberCx(),
            List.of(MPR, MPR2)
        );

        // then
        Assertions.assertEquals(MATERIAL.getMaterialNumberCx(), materialShellDescriptor.get("globalAssetId").asText());

        JsonNode specificAssetIds = materialShellDescriptor.get("specificAssetIds");

        for (JsonNode specificAssetId : specificAssetIds) {
            switch (specificAssetId.get("name").asText()) {
                case TEST_CONST_DIGITAL_TWIN_TYPE:
                    assertSpecificAssetIds(specificAssetId, "PartType", List.of(PARTNER.getBpnl(), PARTNER2.getBpnl()));
                    break;
                case TEST_CONST_CUSTOMER_PART_ID:
                    // customer part ID may only be visible to one partner. Thus, we need to check them separately
                    // and ensure it's only used for one partner
                    if (MPR.getPartnerMaterialNumber().equals(specificAssetId.get("value").asText())) {
                        assertSpecificAssetIds(specificAssetId, MPR.getPartnerMaterialNumber(), List.of(PARTNER.getBpnl()));
                    } else if (MPR2.getPartnerMaterialNumber().equals(specificAssetId.get("value").asText())) {
                        assertSpecificAssetIds(specificAssetId, MPR2.getPartnerMaterialNumber(), List.of(PARTNER2.getBpnl()));
                    }
                    break;
                case TEST_CONST_MANUFACTURER_ID:
                    assertSpecificAssetIds(specificAssetId, ENV_VAR_OWN_BPNL, List.of(PARTNER.getBpnl(), PARTNER2.getBpnl()));
                    break;
                case TEST_CONST_MANUFACTURER_PART_ID:
                    assertSpecificAssetIds(specificAssetId, MATERIAL.getOwnMaterialNumber(), List.of(PARTNER.getBpnl(), PARTNER2.getBpnl()));
                    break;
            }
        }

        JsonNode submodelDescriptors = materialShellDescriptor.get("submodelDescriptors");

        for (JsonNode submodelDescriptor : submodelDescriptors) {
            // assume that we only create one semanticId
            JsonNode semanticId = submodelDescriptor.get("semanticId").get("keys").get(0);

            switch (AssetType.fromUrn(semanticId.get("value").asText())) {
                case AssetType.ITEM_STOCK_SUBMODEL ->
                    assertSubmodelDescriptor(submodelDescriptor, AssetType.ITEM_STOCK_SUBMODEL, DirectionCharacteristic.OUTBOUND, MATERIAL.getMaterialNumberCx());
                case AssetType.DEMAND_SUBMODEL ->
                    assertSubmodelDescriptor(submodelDescriptor, AssetType.DEMAND_SUBMODEL, DirectionCharacteristic.OUTBOUND, MATERIAL.getMaterialNumberCx());
                case AssetType.DELIVERY_SUBMODEL ->
                    assertSubmodelDescriptor(submodelDescriptor, AssetType.DELIVERY_SUBMODEL, DirectionCharacteristic.OUTBOUND, MATERIAL.getMaterialNumberCx());
                case AssetType.DAYS_OF_SUPPLY ->
                    assertSubmodelDescriptor(submodelDescriptor, AssetType.DAYS_OF_SUPPLY, DirectionCharacteristic.OUTBOUND, MATERIAL.getMaterialNumberCx());
                case AssetType.PART_TYPE_INFORMATION_SUBMODEL ->
                    assertSubmodelDescriptor(submodelDescriptor, AssetType.PART_TYPE_INFORMATION_SUBMODEL, DirectionCharacteristic.OUTBOUND, Base64.getEncoder().encodeToString(MATERIAL.getOwnMaterialNumber().getBytes(StandardCharsets.UTF_8)));
            }
        }
    }
}
