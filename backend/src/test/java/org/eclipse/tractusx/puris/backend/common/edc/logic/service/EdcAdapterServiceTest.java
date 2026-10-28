/*
 * Copyright (c) 2024 Volkswagen AG
 * Copyright (c) 2025 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
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

package org.eclipse.tractusx.puris.backend.common.edc.logic.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import org.eclipse.tractusx.puris.backend.irs.IrsAdapterConfiguration;
import org.eclipse.tractusx.puris.backend.irs.logic.service.IrsPolicyStoreService;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.ArrayList;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.DspProtocolVersionEnum;
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.JsonLdConstants;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService.DspaceVersionParams;
import org.eclipse.tractusx.puris.backend.common.edc.logic.util.EdcRequestBodyBuilder;
import org.eclipse.tractusx.puris.backend.common.security.DtrSecurityConfiguration;
import org.eclipse.tractusx.puris.backend.common.edc.logic.util.JsonLdUtils;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.PolicyProfileVersionEnumeration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EdcAdapterServiceTest {

    @Mock
    private VariablesService variablesService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private EdcRequestBodyBuilder edcRequestBodyBuilder;

    @Mock
    private EdcContractMappingService edcContractMappingService;

    // use setup to inject mocks on our own
    private EdcAdapterService edcAdapterService;

    @Mock
    private VariablesService variableService;

    @Mock
    private IrsAdapterConfiguration irsAdapterConfiguration;

    @Mock
    private IrsPolicyStoreService irsPolicyStoreService;

    private final JsonLdUtils jsonLdUtils = new JsonLdUtils();

    @BeforeEach
    void setUp() throws Exception {
        edcRequestBodyBuilder = new EdcRequestBodyBuilder(
            new DtrSecurityConfiguration(),
            variablesService,
            irsAdapterConfiguration,
            objectMapper
        );

        edcAdapterService = new EdcAdapterService(
            objectMapper,
            variablesService,
            edcRequestBodyBuilder,
            edcContractMappingService,
            jsonLdUtils
        );
        ReflectionTestUtils.setField(edcAdapterService, "irsPolicyStoreService", irsPolicyStoreService);
        ReflectionTestUtils.setField(edcAdapterService, "irsAdapterConfiguration", irsAdapterConfiguration);

        edcAdapterService = org.mockito.Mockito.spy(edcAdapterService);
    }

    /**
     * Tests two constraints as expected
     *
     * @throws JsonProcessingException if json is invalid
     */
    @Test
    public void correctConstraints_findAcceptablePolicy_succeeds() throws JsonProcessingException {
        // given
        String validJson = "{\n" +
            "    \"@id\" : \"PartTypeInformationSubmodelApi@BPNL00000007RXRX\",\n" +
            "    \"@type\" : \"dcat:Dataset\",\n" +
            "    \"odrl:hasPolicy\" : {\n" +
            "      \"@id\" : \"QlBOTDAwMDAwMDA3UlRVUF9jb250cmFjdGRlZmluaXRpb25fZm9yX1BhcnRUeXBlSW5mb3JtYXRpb25TdWJtb2RlbEFwaUBCUE5MMDAwMDAwMDdSWFJY:UGFydFR5cGVJbmZvcm1hdGlvblN1Ym1vZGVsQXBpQEJQTkwwMDAwMDAwN1JYUlg=:NzE3MGJmZDMtYTg5NS00YmU2LWI5Y2EtMDVhYTUwY2VjMDk2\",\n" +
            "      \"@type\" : \"odrl:Offer\",\n" +
            "      \"odrl:permission\" : {\n" +
            "        \"odrl:action\" : {\n" +
            "          \"@id\" : \"odrl:use\"\n" +
            "        },\n" +
            "        \"odrl:constraint\" : {\n" +
            "          \"odrl:and\" : [ {\n" +
            "            \"odrl:leftOperand\" : { \"@id\": \"cx-policy:FrameworkAgreement\"},\n" +
            "            \"odrl:operator\" : {\n" +
            "              \"@id\" : \"odrl:eq\"\n" +
            "            },\n" +
            "            \"odrl:rightOperand\" : \"DataExchangeGovernance:1.0\"\n" +
            "          }, {\n" +
            "            \"odrl:leftOperand\" : { \"@id\": \"cx-policy:UsagePurpose\"},\n" +
            "            \"odrl:operator\" : {\n" +
            "              \"@id\" : \"odrl:isAnyOf\"\n" +
            "            },\n" +
            "            \"odrl:rightOperand\" : \"cx.puris.base:1\"\n" +
            "          } ]\n" +
            "        }\n" +
            "      },\n" +
            "      \"odrl:prohibition\" : [ ],\n" +
            "      \"odrl:obligation\" : [ ]\n" +
            "    }," +
            "    \"@context\": {\n" +
            "        \"@vocab\": \"https://w3id.org/edc/v0.0.1/ns/\",\n" +
            "        \"edc\": \"https://w3id.org/edc/v0.0.1/ns/\",\n" +
            "        \"tx\": \"https://w3id.org/tractusx/v0.0.1/ns/\",\n" +
            "        \"tx-auth\": \"https://w3id.org/tractusx/auth/\",\n" +
            "        \"cx-policy\": \"https://w3id.org/catenax/2025/9/policy/\",\n" +
            "        \"dcat\": \"http://www.w3.org/ns/dcat#\",\n" +
            "        \"dct\": \"http://purl.org/dc/terms/\",\n" +
            "        \"odrl\": \"http://www.w3.org/ns/odrl/2/\",\n" +
            "        \"dspace\": \"https://w3id.org/dspace/v0.8/\"\n" +
            "    }" +
            "}";

        JsonNode validJsonNode = objectMapper.readTree(validJson);
        validJsonNode = jsonLdUtils.expand(validJsonNode, variableService.getEdcProfileVersion());
        System.out.println(validJsonNode.toPrettyString());

        // per specifciation jsonLd wraps into an array if multiple entries, thus take it.
        if (validJsonNode.isArray()) {
            validJsonNode = validJsonNode.get(0);
        }

        // when
        when(variablesService.getPurisFrameworkAgreementWithVersion()).thenReturn("DataExchangeGovernance:1.0");

        // then
        Optional<JsonNode> result = edcAdapterService.findAcceptablePolicy(
            validJsonNode,
            PolicyProfileVersionEnumeration.POLICY_PROFILE_2509,
            List.of("cx.puris.base:1")
        );

        assertTrue(result.isPresent());
    }

    /**
     * Tests two constraints with wrong left operand to fail (compacted json)
     *
     * @throws JsonProcessingException if json is invalid
     */
    @Test
    public void wrongConstraints_findAcceptablePolicy_returnsEmpty() throws JsonProcessingException {
        // given
        String invalidJson = "{\n" +
            "    \"@id\" : \"PartTypeInformationSubmodelApi@BPNL1234567890ZZ\",\n" +
            "    \"@type\" : \"dcat:Dataset\",\n" +
            "    \"odrl:hasPolicy\" : {\n" +
            "      \"@id\" : \"QlBOTDQ0NDQ0NDQ0NDRYWF9jb250cmFjdGRlZmluaXRpb25fZm9yX1BhcnRUeXBlSW5mb3JtYXRpb25TdWJtb2RlbEFwaUBCUE5MMTIzNDU2Nzg5MFpa:UGFydFR5cGVJbmZvcm1hdGlvblN1Ym1vZGVsQXBpQEJQTkwxMjM0NTY3ODkwWlo=:ZjdkMTJiYzYtNWYzZi00MTU3LWI3N2QtZTc1MjY1NjU1YTY4\",\n" +
            "      \"@type\" : \"odrl:Offer\",\n" +
            "      \"odrl:permission\" : {\n" +
            "        \"odrl:action\" : {\n" +
            "          \"@id\" : \"odrl:use\"\n" +
            "        },\n" +
            "        \"odrl:constraint\" : {\n" +
            "          \"odrl:and\" : [ {\n" +
            "            \"odrl:leftOperand\" : {\n" +
            "              \"@id\" : \"cx-policy:FrameworkAgreement\"\n" +
            "            },\n" +
            "            \"odrl:operator\" : {\n" +
            "              \"@id\" : \"odrl:eq\"\n" +
            "            },\n" +
            "            \"odrl:rightOperand\" : \"DataExchangeGovernance:0.1\"\n" +
            "          }, {\n" +
            "            \"odrl:leftOperand\" : {\n" +
            "              \"@id\" : \"cx-policy:UsagePurpose\"\n" +
            "            },\n" +
            "            \"odrl:operator\" : {\n" +
            "              \"@id\" : \"odrl:isAnyOf\"\n" +
            "            },\n" +
            "            \"odrl:rightOperand\" : \"cx.puris.base:1\"\n" +
            "          } ]\n" +
            "        }\n" +
            "      },\n" +
            "      \"odrl:prohibition\" : [ ],\n" +
            "      \"odrl:obligation\" : [ ]\n" +
            "    }," +
            "    \"@context\": {\n" +
            "        \"@vocab\": \"https://w3id.org/edc/v0.0.1/ns/\",\n" +
            "        \"edc\": \"https://w3id.org/edc/v0.0.1/ns/\",\n" +
            "        \"tx\": \"https://w3id.org/tractusx/v0.0.1/ns/\",\n" +
            "        \"tx-auth\": \"https://w3id.org/tractusx/auth/\",\n" +
            "        \"cx-policy\": \"https://w3id.org/catenax/2025/9/policy/\",\n" +
            "        \"dcat\": \"http://www.w3.org/ns/dcat#\",\n" +
            "        \"dct\": \"http://purl.org/dc/terms/\",\n" +
            "        \"odrl\": \"http://www.w3.org/ns/odrl/2/\",\n" +
            "        \"dspace\": \"https://w3id.org/dspace/v0.8/\"\n" +
            "    }" +
            "}";

        JsonNode invalidJsonNode = objectMapper.readTree(invalidJson);
        invalidJsonNode = jsonLdUtils.expand(invalidJsonNode, variableService.getEdcProfileVersion());

        // per specifciation jsonLd wraps into an array if multiple entries, thus take it.
        if (invalidJsonNode.isArray()) {
            invalidJsonNode = invalidJsonNode.get(0);
        }

        // when
        when(variablesService.getPurisFrameworkAgreementWithVersion()).thenReturn("DataExchangeGovernance:1.0");

        // then
        Optional<JsonNode> result = edcAdapterService.findAcceptablePolicy(invalidJsonNode, PolicyProfileVersionEnumeration.POLICY_PROFILE_2509, List.of("cx.puris.base:1")
        );

        assertFalse(result.isPresent());
    }

    /**
     * Tests two constraints with wrong left operand to fail (just one constraint / no and)
     *
     * @throws JsonProcessingException if json is invalid
     */
    @Test
    public void oneConstraint_findAcceptablePolicy_returnsEmpty() throws JsonProcessingException {
        // given
        String invalidJson = "{\n" +
            "    \"@id\" : \"PartTypeInformationSubmodelApi@BPNL1234567890ZZ\",\n" +
            "    \"@type\" : \"dcat:Dataset\",\n" +
            "    \"odrl:hasPolicy\" : {\n" +
            "      \"@id\" : \"QlBOTDQ0NDQ0NDQ0NDRYWF9jb250cmFjdGRlZmluaXRpb25fZm9yX1BhcnRUeXBlSW5mb3JtYXRpb25TdWJtb2RlbEFwaUBCUE5MMTIzNDU2Nzg5MFpa:UGFydFR5cGVJbmZvcm1hdGlvblN1Ym1vZGVsQXBpQEJQTkwxMjM0NTY3ODkwWlo=:ZjdkMTJiYzYtNWYzZi00MTU3LWI3N2QtZTc1MjY1NjU1YTY4\",\n" +
            "      \"@type\" : \"odrl:Offer\",\n" +
            "      \"odrl:permission\" : {\n" +
            "        \"odrl:action\" : {\n" +
            "          \"@id\" : \"odrl:use\"\n" +
            "        },\n" +
            "        \"odrl:constraint\" : {\n" +
            "            \"odrl:leftOperand\" : {\n" +
            "              \"@id\" : \"cx-policy:FrameworkAgreement\"\n" +
            "            },\n" +
            "            \"odrl:operator\" : {\n" +
            "              \"@id\" : \"odrl:isAnyOf\"\n" +
            "            },\n" +
            "            \"odrl:rightOperand\" : \"DataExchangeGovernance:1.0\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      \"odrl:prohibition\" : [ ],\n" +
            "      \"odrl:obligation\" : [ ]\n" +
            "    }," +
            "    \"@context\": {\n" +
            "        \"@vocab\": \"https://w3id.org/edc/v0.0.1/ns/\",\n" +
            "        \"edc\": \"https://w3id.org/edc/v0.0.1/ns/\",\n" +
            "        \"tx\": \"https://w3id.org/tractusx/v0.0.1/ns/\",\n" +
            "        \"tx-auth\": \"https://w3id.org/tractusx/auth/\",\n" +
            "        \"cx-policy\": \"https://w3id.org/catenax/2025/9/policy/\",\n" +
            "        \"dcat\": \"http://www.w3.org/ns/dcat#\",\n" +
            "        \"dct\": \"http://purl.org/dc/terms/\",\n" +
            "        \"odrl\": \"http://www.w3.org/ns/odrl/2/\",\n" +
            "        \"dspace\": \"https://w3id.org/dspace/v0.8/\"\n" +
            "    }" +
            "}";

        JsonNode invalidJsonNode = objectMapper.readTree(invalidJson);
        invalidJsonNode = jsonLdUtils.expand(invalidJsonNode, variableService.getEdcProfileVersion());
        // then
        Optional<JsonNode> result = edcAdapterService.findAcceptablePolicy(invalidJsonNode, PolicyProfileVersionEnumeration.POLICY_PROFILE_2509, List.of("cx.puris.base:1"));

        assertFalse(result.isPresent());
    }

    /**
     * Tests policy with unexpected (non-empty) obligation or prohibition, which must be rejected
     *
     * @throws JsonProcessingException if json is invalid
     */
    @ParameterizedTest
    @ValueSource(strings = {unexpectedObligation, unexpectedProhibition})
    public void unexpectedRule_findAcceptablePolicy_returnsEmpty(String input) throws JsonProcessingException {
        // given
        JsonNode invalidJsonNode = objectMapper.readTree(input);
        invalidJsonNode = jsonLdUtils.expand(invalidJsonNode, variableService.getEdcProfileVersion());
        System.out.println(invalidJsonNode.toPrettyString());

        // then
        Optional<JsonNode> result = edcAdapterService.findAcceptablePolicy(invalidJsonNode, PolicyProfileVersionEnumeration.POLICY_PROFILE_2509, List.of("cx.puris.base:1"));

        assertFalse(result.isPresent());
    }

    /**
     * A dataset may carry more than one offer if several contract definitions target the same asset.
     * The matching offer must be found regardless of its position in the array.
     *
     * @throws JsonProcessingException if json is invalid
     */
    @Test
    public void twoOffersSecondValid_findAcceptablePolicy_returnsTheValidOne() throws JsonProcessingException {
        // given
        JsonNode catalogEntry = objectMapper.readTree(twoOffersSecondValid);
        catalogEntry = jsonLdUtils.expand(catalogEntry, variableService.getEdcProfileVersion());
        if (catalogEntry.isArray()) {
            catalogEntry = catalogEntry.get(0);
        }

        // when
        when(variablesService.getPurisFrameworkAgreementWithVersion()).thenReturn("DataExchangeGovernance:1.0");

        // then
        Optional<JsonNode> result = edcAdapterService.findAcceptablePolicy(catalogEntry, PolicyProfileVersionEnumeration.POLICY_PROFILE_2509, List.of("cx.puris.base:1"));

        assertTrue(result.isPresent());
        assertEquals("urn:offer:valid", result.get().get("@id").asText());
    }

    /**
     * If none of the offered policies can be fulfilled, no offer must be returned.
     *
     * @throws JsonProcessingException if json is invalid
     */
    @Test
    public void twoOffersNoneValid_findAcceptablePolicy_returnsEmpty() throws JsonProcessingException {
        // given
        JsonNode catalogEntry = objectMapper.readTree(twoOffersSecondValid);
        catalogEntry = jsonLdUtils.expand(catalogEntry, variableService.getEdcProfileVersion());
        if (catalogEntry.isArray()) {
            catalogEntry = catalogEntry.get(0);
        }

        // when: demand a framework agreement version that neither offer carries
        when(variablesService.getPurisFrameworkAgreementWithVersion()).thenReturn("DataExchangeGovernance:9.9");

        // then
        Optional<JsonNode> result = edcAdapterService.findAcceptablePolicy(catalogEntry, PolicyProfileVersionEnumeration.POLICY_PROFILE_2509, List.of("cx.puris.base:1"));

        assertFalse(result.isPresent());
    }

    /**
     * The offer selected during validation must be the one that is actually negotiated,
     * not simply the first one of the dataset.
     *
     * @throws JsonProcessingException if json is invalid
     */
    @Test
    public void secondOfferChosen_buildAssetNegotiationBody_negotiatesThatOffer() throws JsonProcessingException {
        // given
        JsonNode catalogEntry = objectMapper.readTree(twoOffersSecondValid);
        catalogEntry = jsonLdUtils.expand(catalogEntry, variableService.getEdcProfileVersion());
        if (catalogEntry.isArray()) {
            catalogEntry = catalogEntry.get(0);
        }
        String assetId = catalogEntry.get("@id").asText();
        JsonNode secondOffer = catalogEntry.get(JsonLdConstants.ODRL_NAMESPACE + "hasPolicy").get(1);

        DspaceVersionParams dspaceVersionParams = new DspaceVersionParams(
            "did:web:partner.example",
            "https://partner.example/api/dsp/2025-1",
            DspProtocolVersionEnum.V_0_8
        );

        // then
        JsonNode body = edcRequestBodyBuilder.buildAssetNegotiationBody(assetId, secondOffer, dspaceVersionParams);

        assertEquals("urn:offer:valid", body.get("policy").get("@id").asText());
        assertEquals(assetId, body.get("policy").get("target").get("@id").asText());
    }

    /**
     * checks whether the dspaceVersionParams is correctly built based on jsonLd
     *
     * @throws IoException if request fails
     */
    @Test
    public void dspaceVersionParamsEndpointGiven_getDspaceVersionParams_returnsEndpointInformation() throws IOException {
        // given
        String expectedCounterPartyAddress = "https://provider.domain.com/api/dsp/2025-1";
        String expectedProtocolString = "dataspace-protocol-http:2025-1";
        String expectedCounterPartyId = "did:web:one-example.com"; 
        DspaceVersionParams expectedDspaceVerstionParams = new DspaceVersionParams(
            expectedCounterPartyId, 
            expectedCounterPartyAddress,
            DspProtocolVersionEnum.fromVersion(expectedProtocolString));

        String validJsonLd = "[{\n" + //
                        "  \"@context\": {\n" + //
                        "    \"edc\": \"https://w3id.org/edc/v0.0.1/ns/\",\n" + //
                        "    \"tx\": \"https://w3id.org/tractusx/v0.0.1/ns/\"\n" + //
                        "  },\n" + //
                        "  \"edc:counterPartyId\": \"" + expectedCounterPartyId + "\",\n" + //
                        "  \"edc:counterPartyAddress\": \"" + expectedCounterPartyAddress + "\",\n" + //
                        "  \"edc:protocol\": \"" + expectedProtocolString + "\"\n" + //
                        "}, {\n" + //
                        "  \"@context\": {\n" + //
                        "    \"edc\": \"https://w3id.org/edc/v0.0.1/ns/\",\n" + //
                        "    \"tx\": \"https://w3id.org/tractusx/v0.0.1/ns/\"\n" + //
                        "  },\n" + //
                        "  \"edc:counterPartyId\": \"http://other-ignored-connector.com/v1/dsp\",\n" + //
                        "  \"edc:counterPartyAddress\": \"BPNL1111111111XX\",\n" + //
                        "  \"edc:protocol\": \"" + DspProtocolVersionEnum.V_0_8.getVersion() + "\"\n" + //
                        "}]";

        String partnerBpnl = "BPNL4444444444XX";
        String partnerDspUrl = "http://customer-control-plane:8184/api/v1/dsp"; 

        // when
        Response mockResponse = mock(Response.class);
        ResponseBody mockBody = mock(ResponseBody.class);
        when(mockResponse.code()).thenReturn(200);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.body()).thenReturn(mockBody);
        when(mockBody.string()).thenReturn(validJsonLd);

        doReturn(mockResponse)
            .when(edcAdapterService)
            .sendPostRequest(any(), any());

        // then
        DspaceVersionParams actualDspaceVersionParams = edcAdapterService.getPartnerDspaceVersionParams(partnerBpnl, partnerDspUrl);

        assertEquals(expectedDspaceVerstionParams, actualDspaceVersionParams);
    }

    /**
     * checks whether the dspaceVersionParams is correctly built as fallback when no endpoint is given
     *
     * @throws IoException if request fails
     */
    @Test
    public void dspaceVersionParamsEndpointNotGiven_getDspaceVersionParams_returnsFallbackInformation() throws IOException {
        
        String partnerBpnl = "BPNL4444444444XX";
        String partnerDspUrl = "http://customer-control-plane:8184/api/v1/dsp"; 

        DspaceVersionParams expectedDspaceVerstionParams = new DspaceVersionParams(
            partnerBpnl,
            partnerDspUrl,
            DspProtocolVersionEnum.V_0_8);

        // when
        Response mockResponse = mock(Response.class);
        when(mockResponse.code()).thenReturn(404);

        doReturn(mockResponse)
            .when(edcAdapterService)
            .sendPostRequest(any(), any());

        // then
        DspaceVersionParams actualDspaceVersionParams = edcAdapterService.getPartnerDspaceVersionParams(partnerBpnl, partnerDspUrl);

        assertEquals(expectedDspaceVerstionParams, actualDspaceVersionParams);
    }

    @Test
    public void dspaceVersionParamsRequestedTwice_getDspaceVersionParams_usesCachedInformation() throws IOException {
        String expectedCounterPartyAddress = "https://provider.domain.com/api/dsp/2025-1";
        String expectedProtocolString = "dataspace-protocol-http:2025-1";
        String expectedCounterPartyId = "did:web:one-example.com";
        DspaceVersionParams expectedDspaceVerstionParams = new DspaceVersionParams(
            expectedCounterPartyId,
            expectedCounterPartyAddress,
            DspProtocolVersionEnum.fromVersion(expectedProtocolString));

        String validJsonLd = "[{\n" +
            "  \"@context\": {\n" +
            "    \"edc\": \"https://w3id.org/edc/v0.0.1/ns/\",\n" +
            "    \"tx\": \"https://w3id.org/tractusx/v0.0.1/ns/\"\n" +
            "  },\n" +
            "  \"edc:counterPartyId\": \"" + expectedCounterPartyId + "\",\n" +
            "  \"edc:counterPartyAddress\": \"" + expectedCounterPartyAddress + "\",\n" +
            "  \"edc:protocol\": \"" + expectedProtocolString + "\"\n" +
            "}]";

        String partnerBpnl = "BPNL4444444444XX";
        String partnerDspUrl = "http://customer-control-plane:8184/api/v1/dsp";

        Response mockResponse = mock(Response.class);
        ResponseBody mockBody = mock(ResponseBody.class);
        when(mockResponse.code()).thenReturn(200);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.body()).thenReturn(mockBody);
        when(mockBody.string()).thenReturn(validJsonLd);

        doReturn(mockResponse)
            .when(edcAdapterService)
            .sendPostRequest(any(), any());

        DspaceVersionParams firstCall = edcAdapterService.getPartnerDspaceVersionParams(partnerBpnl, partnerDspUrl);
        DspaceVersionParams secondCall = edcAdapterService.getPartnerDspaceVersionParams(partnerBpnl, partnerDspUrl);

        assertEquals(expectedDspaceVerstionParams, firstCall);
        assertEquals(expectedDspaceVerstionParams, secondCall);
        verify(edcAdapterService, times(1)).sendPostRequest(any(), any());
    }

    @Test
    public void irsAdapterDisabled_registerAssetsInitially_registersNothingForTheIrs() throws IOException {
        // given
        List<EdcRequest> requests = stubEdcManagementApi(200, Map.of());
        when(irsAdapterConfiguration.isIrsAdapterEnabled()).thenReturn(false);

        // when
        boolean result = edcAdapterService.registerAssetsInitially();

        // then
        assertTrue(result);
        assertThat(registeredIds(requests)).doesNotContain(IRS_RECURSIVE_REGISTRATION_IDS);
        assertThat(requests).noneMatch(EdcAdapterServiceTest::isIrsRecursiveNotificationAsset);
        verify(irsPolicyStoreService, never()).createIrsFrameworkPolicy();
    }

    @Test
    public void irsAssetRegistrationDisabled_registerAssetsInitially_registersNothingForTheIrs() throws IOException {
        // given
        List<EdcRequest> requests = stubEdcManagementApi(200, Map.of());
        when(irsAdapterConfiguration.isIrsAdapterEnabled()).thenReturn(true);
        when(irsAdapterConfiguration.isIrsAdapterRegisterAsset()).thenReturn(false);

        // when
        boolean result = edcAdapterService.registerAssetsInitially();

        // then
        assertTrue(result);
        assertThat(registeredIds(requests)).doesNotContain(IRS_RECURSIVE_REGISTRATION_IDS);
        assertThat(requests).noneMatch(EdcAdapterServiceTest::isIrsRecursiveNotificationAsset);
        verify(irsPolicyStoreService, never()).createIrsFrameworkPolicy();
    }

    @Test
    public void irsAdapterEnabled_registerAssetsInitially_registersAssetPoliciesAndContractDefinition() throws IOException {
        // given
        List<EdcRequest> requests = stubEdcManagementApi(200, Map.of());
        givenIrsAdapterEnabledWithAssetRegistration();

        // when
        boolean result = edcAdapterService.registerAssetsInitially();

        // then
        assertTrue(result);
                   assertThat(requests).filteredOn(EdcAdapterServiceTest::isIrsRecursiveNotificationAsset)
               .extracting(request -> request.body().path("@id").asText())
               .containsExactly(IRS_ASSET_ID);
        assertThat(registeredIds(requests, "v3/policydefinitions")).contains(
            EdcRequestBodyBuilder.RECURSIVE_IRS_NOTIFICATION_ACCESS_POLICY_ID,
            EdcRequestBodyBuilder.RECURSIVE_IRS_NOTIFICATION_CONTRACT_POLICY_ID
        );
        assertThat(registeredIds(requests, "v3/contractdefinitions"))
            .contains(EdcRequestBodyBuilder.RECURSIVE_IRS_NOTIFICATION_CONTRACT_DEFINITION_ID);
        verify(irsPolicyStoreService, times(1)).createIrsFrameworkPolicy();
    }

    private static final String IRS_ASSET_ID = "irsrecursivenotification-api-asset@BPNL1234567890ZZ";

    private static final String[] IRS_RECURSIVE_REGISTRATION_IDS = {
        EdcRequestBodyBuilder.RECURSIVE_IRS_NOTIFICATION_ACCESS_POLICY_ID,
        EdcRequestBodyBuilder.RECURSIVE_IRS_NOTIFICATION_CONTRACT_POLICY_ID,
        EdcRequestBodyBuilder.RECURSIVE_IRS_NOTIFICATION_CONTRACT_DEFINITION_ID
    };

    private record EdcRequest(String path, JsonNode body) {
    }

    /**
     * Stubs the EDC management api for {@link EdcAdapterService#registerAssetsInitially()} and records all POST
     * requests. A request is answered with the status code configured for the {@code @id} of its body, or with
     * the default status code.
     *
     * @param defaultStatusCode the status code to answer with by default
     * @param statusCodeById    status codes to answer with for specific {@code @id}s
     * @return the recorded requests
     */
    private List<EdcRequest> stubEdcManagementApi(int defaultStatusCode, Map<String, Integer> statusCodeById) throws IOException {
        when(variablesService.getEdcProfileVersion()).thenReturn(PolicyProfileVersionEnumeration.POLICY_PROFILE_2405);
        List<EdcRequest> requests = new ArrayList<>();
        doAnswer(invocation -> {
            JsonNode body = invocation.getArgument(0);
            List<String> pathSegments = invocation.getArgument(1);
            requests.add(new EdcRequest(String.join("/", pathSegments), body));
            return response(statusCodeById.getOrDefault(body.path("@id").asText(), defaultStatusCode));
        }).when(edcAdapterService).sendPostRequest(any(), any());
        return requests;
    }

    private void givenIrsAdapterEnabledWithAssetRegistration() {
        when(variablesService.getIrsRecursiveNotificationApiAssetId()).thenReturn(IRS_ASSET_ID);
        when(irsAdapterConfiguration.isIrsAdapterEnabled()).thenReturn(true);
        when(irsAdapterConfiguration.isIrsAdapterRegisterAsset()).thenReturn(true);
        when(irsAdapterConfiguration.getIrsAdapterUrl()).thenReturn("https://irs.example.com");
        when(irsAdapterConfiguration.getIrsAdapterAuthKey()).thenReturn("x-api-key");
        when(irsAdapterConfiguration.getIrsAdapterAuthSecret()).thenReturn("irs-regular-secret");
    }

    private static List<String> registeredIds(List<EdcRequest> requests) {
        return requests.stream()
            .map(request -> request.body().path("@id").asText())
            .toList();
    }

    private static List<String> registeredIds(List<EdcRequest> requests, String path) {
        return requests.stream()
            .filter(request -> request.path().equals(path))
            .map(request -> request.body().path("@id").asText())
            .toList();
    }

    private static boolean isIrsRecursiveNotificationAsset(EdcRequest request) {
        return request.path().equals("v3/assets")
            && EdcRequestBodyBuilder.RECURSIVE_IRS_NOTIFICATION_API_TYPE.equals(request.body().path("properties").path("dct:type").path("@id").asText());
    }

    private static Response response(int code) {
        return new Response.Builder()
            .request(new Request.Builder().url("http://edc.example.com/management").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("HTTP " + code)
            .body(ResponseBody.create("{}", MediaType.parse("application/json")))
            .build();
    }

    private final static String unexpectedProhibition = "{\n" +
        "    \"@id\" : \"PartTypeInformationSubmodelApi@BPNL00000007RXRX\",\n" +
        "    \"@type\" : \"dcat:Dataset\",\n" +
        "    \"odrl:hasPolicy\" : {\n" +
        "      \"@id\" : \"QlBOTDAwMDAwMDA3UlRVUF9jb250cmFjdGRlZmluaXRpb25fZm9yX1BhcnRUeXBlSW5mb3JtYXRpb25TdWJtb2RlbEFwaUBCUE5MMDAwMDAwMDdSWFJY:UGFydFR5cGVJbmZvcm1hdGlvblN1Ym1vZGVsQXBpQEJQTkwwMDAwMDAwN1JYUlg=:NzE3MGJmZDMtYTg5NS00YmU2LWI5Y2EtMDVhYTUwY2VjMDk2\",\n" +
        "      \"@type\" : \"odrl:Offer\",\n" +
        "      \"odrl:permission\" : {\n" +
        "        \"odrl:action\" : {\n" +
        "          \"@id\" : \"odrl:use\"\n" +
        "        },\n" +
        "        \"odrl:constraint\" : {\n" +
        "          \"odrl:and\" : [ {\n" +
        "            \"odrl:leftOperand\" : { \"@id\": \"cx-policy:FrameworkAgreement\"},\n" +
        "            \"odrl:operator\" : {\n" +
        "              \"@id\" : \"odrl:eq\"\n" +
        "            },\n" +
        "            \"odrl:rightOperand\" : \"Puris:1.0\"\n" +
        "          }, {\n" +
        "            \"odrl:leftOperand\" : { \"@id\": \"cx-policy:UsagePurpose\"},\n" +
        "            \"odrl:operator\" : {\n" +
        "              \"@id\" : \"odrl:isAnyOf\"\n" +
        "            },\n" +
        "            \"odrl:rightOperand\" : \"cx.puris.base:1\"\n" +
        "          } ]\n" +
        "        }\n" +
        "      },\n" +
        "      \"odrl:prohibition\" : [ {\"foo\": \"bar\"} ],\n" +
        "      \"odrl:obligation\" : [ ]\n" +
        "    }," +
        "    \"@context\": {\n" +
        "        \"@vocab\": \"https://w3id.org/edc/v0.0.1/ns/\",\n" +
        "        \"edc\": \"https://w3id.org/edc/v0.0.1/ns/\",\n" +
        "        \"tx\": \"https://w3id.org/tractusx/v0.0.1/ns/\",\n" +
        "        \"tx-auth\": \"https://w3id.org/tractusx/auth/\",\n" +
        "        \"cx-policy\": \"https://w3id.org/catenax/2025/9/policy/\",\n" +
        "        \"dcat\": \"http://www.w3.org/ns/dcat#\",\n" +
        "        \"dct\": \"http://purl.org/dc/terms/\",\n" +
        "        \"odrl\": \"http://www.w3.org/ns/odrl/2/\",\n" +
        "        \"dspace\": \"https://w3id.org/dspace/v0.8/\"\n" +
        "    }" +
        "}";

    private final static String unexpectedObligation = "{\n" +
        "    \"@id\" : \"PartTypeInformationSubmodelApi@BPNL00000007RXRX\",\n" +
        "    \"@type\" : \"dcat:Dataset\",\n" +
        "    \"odrl:hasPolicy\" : {\n" +
        "      \"@id\" : \"QlBOTDAwMDAwMDA3UlRVUF9jb250cmFjdGRlZmluaXRpb25fZm9yX1BhcnRUeXBlSW5mb3JtYXRpb25TdWJtb2RlbEFwaUBCUE5MMDAwMDAwMDdSWFJY:UGFydFR5cGVJbmZvcm1hdGlvblN1Ym1vZGVsQXBpQEJQTkwwMDAwMDAwN1JYUlg=:NzE3MGJmZDMtYTg5NS00YmU2LWI5Y2EtMDVhYTUwY2VjMDk2\",\n" +
        "      \"@type\" : \"odrl:Offer\",\n" +
        "      \"odrl:permission\" : {\n" +
        "        \"odrl:action\" : {\n" +
        "          \"@id\" : \"odrl:use\"\n" +
        "        },\n" +
        "        \"odrl:constraint\" : {\n" +
        "          \"odrl:and\" : [ {\n" +
        "            \"odrl:leftOperand\" : { \"@id\": \"cx-policy:FrameworkAgreement\"},\n" +
        "            \"odrl:operator\" : {\n" +
        "              \"@id\" : \"odrl:eq\"\n" +
        "            },\n" +
        "            \"odrl:rightOperand\" : \"Puris:1.0\"\n" +
        "          }, {\n" +
        "            \"odrl:leftOperand\" : { \"@id\": \"cx-policy:UsagePurpose\"},\n" +
        "            \"odrl:operator\" : {\n" +
        "              \"@id\" : \"odrl:isAnyOf\"\n" +
        "            },\n" +
        "            \"odrl:rightOperand\" : \"cx.puris.base:1\"\n" +
        "          } ]\n" +
        "        }\n" +
        "      },\n" +
        "      \"odrl:prohibition\" : [ ],\n" +
        "      \"odrl:obligation\" : [ {\"foo\": \"bar\"} ]\n" +
        "    }," +
        "    \"@context\": {\n" +
        "        \"@vocab\": \"https://w3id.org/edc/v0.0.1/ns/\",\n" +
        "        \"edc\": \"https://w3id.org/edc/v0.0.1/ns/\",\n" +
        "        \"tx\": \"https://w3id.org/tractusx/v0.0.1/ns/\",\n" +
        "        \"tx-auth\": \"https://w3id.org/tractusx/auth/\",\n" +
        "        \"cx-policy\": \"https://w3id.org/catenax/2025/9/policy/\",\n" +
        "        \"dcat\": \"http://www.w3.org/ns/dcat#\",\n" +
        "        \"dct\": \"http://purl.org/dc/terms/\",\n" +
        "        \"odrl\": \"http://www.w3.org/ns/odrl/2/\",\n" +
        "        \"dspace\": \"https://w3id.org/dspace/v0.8/\"\n" +
        "    }" +
        "}";

    /**
     * A dataset carrying two offers, as produced when two contract definitions target the same asset.
     * The first offer demands a framework agreement version we do not hold, the second one matches.
     */
    private final static String twoOffersSecondValid = "{\n" +
        "    \"@id\" : \"NotificationApi@BPNL00000007RXRX\",\n" +
        "    \"@type\" : \"dcat:Dataset\",\n" +
        "    \"odrl:hasPolicy\" : [ {\n" +
        "      \"@id\" : \"urn:offer:invalid\",\n" +
        "      \"@type\" : \"odrl:Offer\",\n" +
        "      \"odrl:permission\" : {\n" +
        "        \"odrl:action\" : {\n" +
        "          \"@id\" : \"odrl:use\"\n" +
        "        },\n" +
        "        \"odrl:constraint\" : {\n" +
        "          \"odrl:and\" : [ {\n" +
        "            \"odrl:leftOperand\" : { \"@id\": \"cx-policy:FrameworkAgreement\"},\n" +
        "            \"odrl:operator\" : {\n" +
        "              \"@id\" : \"odrl:eq\"\n" +
        "            },\n" +
        "            \"odrl:rightOperand\" : \"DataExchangeGovernance:0.1\"\n" +
        "          }, {\n" +
        "            \"odrl:leftOperand\" : { \"@id\": \"cx-policy:UsagePurpose\"},\n" +
        "            \"odrl:operator\" : {\n" +
        "              \"@id\" : \"odrl:isAnyOf\"\n" +
        "            },\n" +
        "            \"odrl:rightOperand\" : \"cx.puris.base:1\"\n" +
        "          } ]\n" +
        "        }\n" +
        "      },\n" +
        "      \"odrl:prohibition\" : [ ],\n" +
        "      \"odrl:obligation\" : [ ]\n" +
        "    }, {\n" +
        "      \"@id\" : \"urn:offer:valid\",\n" +
        "      \"@type\" : \"odrl:Offer\",\n" +
        "      \"odrl:permission\" : {\n" +
        "        \"odrl:action\" : {\n" +
        "          \"@id\" : \"odrl:use\"\n" +
        "        },\n" +
        "        \"odrl:constraint\" : {\n" +
        "          \"odrl:and\" : [ {\n" +
        "            \"odrl:leftOperand\" : { \"@id\": \"cx-policy:FrameworkAgreement\"},\n" +
        "            \"odrl:operator\" : {\n" +
        "              \"@id\" : \"odrl:eq\"\n" +
        "            },\n" +
        "            \"odrl:rightOperand\" : \"DataExchangeGovernance:1.0\"\n" +
        "          }, {\n" +
        "            \"odrl:leftOperand\" : { \"@id\": \"cx-policy:UsagePurpose\"},\n" +
        "            \"odrl:operator\" : {\n" +
        "              \"@id\" : \"odrl:isAnyOf\"\n" +
        "            },\n" +
        "            \"odrl:rightOperand\" : \"cx.puris.base:1\"\n" +
        "          } ]\n" +
        "        }\n" +
        "      },\n" +
        "      \"odrl:prohibition\" : [ ],\n" +
        "      \"odrl:obligation\" : [ ]\n" +
        "    } ]," +
        "    \"@context\": {\n" +
        "        \"@vocab\": \"https://w3id.org/edc/v0.0.1/ns/\",\n" +
        "        \"edc\": \"https://w3id.org/edc/v0.0.1/ns/\",\n" +
        "        \"tx\": \"https://w3id.org/tractusx/v0.0.1/ns/\",\n" +
        "        \"tx-auth\": \"https://w3id.org/tractusx/auth/\",\n" +
        "        \"cx-policy\": \"https://w3id.org/catenax/2025/9/policy/\",\n" +
        "        \"dcat\": \"http://www.w3.org/ns/dcat#\",\n" +
        "        \"dct\": \"http://purl.org/dc/terms/\",\n" +
        "        \"odrl\": \"http://www.w3.org/ns/odrl/2/\",\n" +
        "        \"dspace\": \"https://w3id.org/dspace/v0.8/\"\n" +
        "    }" +
        "}";

}