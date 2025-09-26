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

package org.eclipse.tractusx.puris.backend.common.edc.logic.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.tractusx.puris.backend.common.edc.logic.util.EdcRequestBodyBuilder;
import org.eclipse.tractusx.puris.backend.common.edc.logic.util.JsonLdUtils;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.PolicyProfileVersionEnumeration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class EdcAdapterServiceTest {

    @Mock
    private VariablesService variablesService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private EdcRequestBodyBuilder edcRequestBodyBuilder;

    @Mock
    private EdcContractMappingService edcContractMappingService;

    @InjectMocks
    private EdcAdapterService edcAdapterService;

    private final JsonLdUtils jsonLdUtils = new JsonLdUtils();

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // make variablesService mockable
        Field field = EdcAdapterService.class.getDeclaredField("variablesService");
        field.setAccessible(true);
        field.set(edcAdapterService, variablesService);
        Field field2 = EdcAdapterService.class.getDeclaredField("edcRequestBodyBuilder");
        field2.setAccessible(true);
        field2.set(edcAdapterService, edcRequestBodyBuilder);
    }

    /**
     * Tests two constraints as expected
     *
     * @throws JsonProcessingException if json is invalid
     */
    @Test
    public void correctConstraints_testContractPolicyConstraints_succeed() throws JsonProcessingException {
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
        validJsonNode = jsonLdUtils.expand(validJsonNode);
        System.out.println(validJsonNode.toPrettyString());


        // when
        when(variablesService.getPurisFrameworkAgreementWithVersion()).thenReturn("DataExchangeGovernance:1.0");
        when(variablesService.getPurisPurposeWithVersion()).thenReturn("cx.puris.base:1");
        when(variablesService.getEdcProfileVersion()).thenReturn(PolicyProfileVersionEnumeration.POLICY_PROFILE_2509);

        // then
        boolean result = edcAdapterService.testContractPolicyConstraints(validJsonNode, PolicyProfileVersionEnumeration.POLICY_PROFILE_2509);

        assertTrue(result);
    }

    /**
     * Tests two constraints with wrong left operand to fail (compacted json)
     *
     * @throws JsonProcessingException if json is invalid
     */
    @Test
    public void wrongConstraints_testContractPolicyConstraints_fails() throws JsonProcessingException {
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
            "              \"@id\" : \"odrl:eq\"\n" +
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
        invalidJsonNode = jsonLdUtils.expand(invalidJsonNode);

        // when
        when(variablesService.getPurisFrameworkAgreementWithVersion()).thenReturn("Puris:1.0");
        when(variablesService.getPurisPurposeWithVersion()).thenReturn("cx.puris.base:1");
        when(variablesService.getEdcProfileVersion()).thenReturn(PolicyProfileVersionEnumeration.POLICY_PROFILE_2509);

        // then
        boolean result = edcAdapterService.testContractPolicyConstraints(invalidJsonNode, PolicyProfileVersionEnumeration.POLICY_PROFILE_2509);

        assertFalse(result);
    }

    /**
     * Tests two constraints with wrong left operand to fail (just one constraint / no and)
     *
     * @throws JsonProcessingException if json is invalid
     */
    @Test
    public void oneConstraint_testContractPolicyConstraints_fails() throws JsonProcessingException {
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
            "              \"@id\" : \"odrl:eq\"\n" +
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
        invalidJsonNode = jsonLdUtils.expand(invalidJsonNode);

        // when
        when(variablesService.getPurisFrameworkAgreementWithVersion()).thenReturn("Puris:1.0");
        when(variablesService.getPurisPurposeWithVersion()).thenReturn("cx.puris.base:1");
        when(variablesService.getEdcProfileVersion()).thenReturn(PolicyProfileVersionEnumeration.POLICY_PROFILE_2509);

        // then
        boolean result = edcAdapterService.testContractPolicyConstraints(invalidJsonNode, PolicyProfileVersionEnumeration.POLICY_PROFILE_2509);

        assertFalse(result);
    }

    /**
     * Tests policy with unexpected (non-empty) obligation or prohibition, which must be rejected
     *
     * @throws JsonProcessingException if json is invalid
     */
    @ParameterizedTest
    @ValueSource(strings = {unexpectedObligation, unexpectedProhibition})
    public void unexpectedRule_testContractPolicyConstraints_fails(String input) throws JsonProcessingException {
        // given
        JsonNode invalidJsonNode = objectMapper.readTree(input);
        invalidJsonNode = jsonLdUtils.expand(invalidJsonNode);
        System.out.println(invalidJsonNode.toPrettyString());

        // when
        when(variablesService.getPurisFrameworkAgreementWithVersion()).thenReturn("DataExchangeGovernance:1.0");
        when(variablesService.getPurisPurposeWithVersion()).thenReturn("cx.puris.base:1");
        when(variablesService.getEdcProfileVersion()).thenReturn(PolicyProfileVersionEnumeration.POLICY_PROFILE_2509);

        // then
        boolean result = edcAdapterService.testContractPolicyConstraints(invalidJsonNode, PolicyProfileVersionEnumeration.POLICY_PROFILE_2509);
        assertFalse(result);
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
        "              \"@id\" : \"odrl:eq\"\n" +
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
        "              \"@id\" : \"odrl:eq\"\n" +
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

}
