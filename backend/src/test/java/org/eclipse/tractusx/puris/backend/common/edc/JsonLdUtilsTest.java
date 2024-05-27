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

package org.eclipse.tractusx.puris.backend.common.edc;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.edc.logic.util.JsonLdUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class JsonLdUtilsTest {


    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @ParameterizedTest
    @ValueSource(strings = {test1, test2})
    public void testUtilClass(String input) throws Exception {
        // GIVEN
        var catalogJson = objectMapper.readTree(input);
        log.info("Input:\n" + catalogJson.toPrettyString());

        //WHEN
        var expanded = JsonLdUtils.expand(catalogJson);
        log.info("Expanded first time:\n"+ expanded.toPrettyString());
        var compacted = JsonLdUtils.compact(expanded);
        log.info("Compacted:\n" + compacted.toPrettyString());
        var expandedAgain = JsonLdUtils.expand(compacted);
        log.info("Expanded again:\n" + expandedAgain.toPrettyString());

        // THEN
        assertEquals(expandedAgain, expanded);
    }


    final static String test1 = "{\n" +
        "  \"@id\" : \"473e5307-c0c5-491f-96b4-68b9e25d4699\",\n" +
        "  \"@type\" : \"dcat:Catalog\",\n" +
        "  \"dspace:participantId\" : \"BPNL1234567890ZZ\",\n" +
        "  \"dcat:dataset\" : {\n" +
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
        "            \"odrl:rightOperand\" : \"Puris:1.0\"\n" +
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
        "    },\n" +
        "    \"dcat:distribution\" : [ {\n" +
        "      \"@type\" : \"dcat:Distribution\",\n" +
        "      \"dct:format\" : {\n" +
        "        \"@id\" : \"HttpData-PULL\"\n" +
        "      },\n" +
        "      \"dcat:accessService\" : {\n" +
        "        \"@id\" : \"e5fe796d-a985-42fb-874f-a3d8be5d9609\",\n" +
        "        \"@type\" : \"dcat:DataService\",\n" +
        "        \"dcat:endpointDescription\" : \"dspace:connector\",\n" +
        "        \"dcat:endpointUrl\" : \"http://supplier-control-plane:9184/api/v1/dsp\",\n" +
        "        \"dct:terms\" : \"dspace:connector\",\n" +
        "        \"dct:endpointUrl\" : \"http://supplier-control-plane:9184/api/v1/dsp\"\n" +
        "      }\n" +
        "    }, {\n" +
        "      \"@type\" : \"dcat:Distribution\",\n" +
        "      \"dct:format\" : {\n" +
        "        \"@id\" : \"HttpData-PUSH\"\n" +
        "      },\n" +
        "      \"dcat:accessService\" : {\n" +
        "        \"@id\" : \"e5fe796d-a985-42fb-874f-a3d8be5d9609\",\n" +
        "        \"@type\" : \"dcat:DataService\",\n" +
        "        \"dcat:endpointDescription\" : \"dspace:connector\",\n" +
        "        \"dcat:endpointUrl\" : \"http://supplier-control-plane:9184/api/v1/dsp\",\n" +
        "        \"dct:terms\" : \"dspace:connector\",\n" +
        "        \"dct:endpointUrl\" : \"http://supplier-control-plane:9184/api/v1/dsp\"\n" +
        "      }\n" +
        "    } ],\n" +
        "    \"https://admin-shell.io/aas/3/0/HasSemantics/semanticId\" : {\n" +
        "      \"@id\" : \"urn:samm:io.catenax.part_type_information:1.0.0#PartTypeInformation\"\n" +
        "    },\n" +
        "    \"https://w3id.org/catenax/ontology/common#version\" : \"3.0\",\n" +
        "    \"id\" : \"PartTypeInformationSubmodelApi@BPNL1234567890ZZ\",\n" +
        "    \"https://purl.org/dc/terms/type\" : {\n" +
        "      \"@id\" : \"https://w3id.org/catenax/taxonomy#Submodel\"\n" +
        "    }\n" +
        "  },\n" +
        "  \"dcat:service\" : {\n" +
        "    \"@id\" : \"e5fe796d-a985-42fb-874f-a3d8be5d9609\",\n" +
        "    \"@type\" : \"dcat:DataService\",\n" +
        "    \"dcat:endpointDescription\" : \"dspace:connector\",\n" +
        "    \"dcat:endpointUrl\" : \"http://supplier-control-plane:9184/api/v1/dsp\",\n" +
        "    \"dct:terms\" : \"dspace:connector\",\n" +
        "    \"dct:endpointUrl\" : \"http://supplier-control-plane:9184/api/v1/dsp\"\n" +
        "  },\n" +
        "  \"participantId\" : \"BPNL1234567890ZZ\",\n" +
        "  \"@context\" : {\n" +
        "    \"@vocab\" : \"https://w3id.org/edc/v0.0.1/ns/\",\n" +
        "    \"edc\" : \"https://w3id.org/edc/v0.0.1/ns/\",\n" +
        "    \"tx\" : \"https://w3id.org/tractusx/v0.0.1/ns/\",\n" +
        "    \"tx-auth\" : \"https://w3id.org/tractusx/auth/\",\n" +
        "    \"cx-policy\" : \"https://w3id.org/catenax/policy/\",\n" +
        "    \"dcat\" : \"http://www.w3.org/ns/dcat#\",\n" +
        "    \"dct\" : \"http://purl.org/dc/terms/\",\n" +
        "    \"odrl\" : \"http://www.w3.org/ns/odrl/2/\",\n" +
        "    \"dspace\" : \"https://w3id.org/dspace/v0.8/\"\n" +
        "  }\n" +
        "}";

    final static String test2 = "\n" +
        "{\n" +
        "\n" +
        "  \"@id\" : \"f26d8d04-5ee5-4478-8de7-2e347b1f6685\",\n" +
        "\n" +
        "  \"@type\" : \"dcat:Catalog\",\n" +
        "\n" +
        "  \"dspace:participantId\" : \"BPNL00000007RXRX\",\n" +
        "\n" +
        "  \"dcat:dataset\" : {\n" +
        "\n" +
        "    \"@id\" : \"PartTypeInformationSubmodelApi@BPNL00000007RXRX\",\n" +
        "\n" +
        "    \"@type\" : \"dcat:Dataset\",\n" +
        "\n" +
        "    \"odrl:hasPolicy\" : {\n" +
        "\n" +
        "      \"@id\" : \"QlBOTDAwMDAwMDA3UlRVUF9jb250cmFjdGRlZmluaXRpb25fZm9yX1BhcnRUeXBlSW5mb3JtYXRpb25TdWJtb2RlbEFwaUBCUE5MMDAwMDAwMDdSWFJY:UGFydFR5cGVJbmZvcm1hdGlvblN1Ym1vZGVsQXBpQEJQTkwwMDAwMDAwN1JYUlg=:NzE3MGJmZDMtYTg5NS00YmU2LWI5Y2EtMDVhYTUwY2VjMDk2\",\n" +
        "\n" +
        "      \"@type\" : \"odrl:Offer\",\n" +
        "\n" +
        "      \"odrl:permission\" : {\n" +
        "\n" +
        "        \"odrl:action\" : {\n" +
        "\n" +
        "          \"odrl:type\" : \"http://www.w3.org/ns/odrl/2/use\"\n" +
        "\n" +
        "        },\n" +
        "\n" +
        "        \"odrl:constraint\" : {\n" +
        "\n" +
        "          \"odrl:and\" : [ {\n" +
        "\n" +
        "            \"odrl:leftOperand\" : \"https://w3id.org/catenax/policy/FrameworkAgreement\",\n" +
        "\n" +
        "            \"odrl:operator\" : {\n" +
        "\n" +
        "              \"@id\" : \"odrl:eq\"\n" +
        "\n" +
        "            },\n" +
        "\n" +
        "            \"odrl:rightOperand\" : \"Puris:1.0\"\n" +
        "\n" +
        "          }, {\n" +
        "\n" +
        "            \"odrl:leftOperand\" : \"https://w3id.org/catenax/policy/UsagePurpose\",\n" +
        "\n" +
        "            \"odrl:operator\" : {\n" +
        "\n" +
        "              \"@id\" : \"odrl:eq\"\n" +
        "\n" +
        "            },\n" +
        "\n" +
        "            \"odrl:rightOperand\" : \"cx.puris.base:1\"\n" +
        "\n" +
        "          } ]\n" +
        "\n" +
        "        }\n" +
        "\n" +
        "      },\n" +
        "\n" +
        "      \"odrl:prohibition\" : [ ],\n" +
        "\n" +
        "      \"odrl:obligation\" : [ ]\n" +
        "\n" +
        "    },\n" +
        "\n" +
        "    \"dcat:distribution\" : [ {\n" +
        "\n" +
        "      \"@type\" : \"dcat:Distribution\",\n" +
        "\n" +
        "      \"dct:format\" : {\n" +
        "\n" +
        "        \"@id\" : \"AzureStorage-PUSH\"\n" +
        "\n" +
        "      },\n" +
        "\n" +
        "      \"dcat:accessService\" : {\n" +
        "\n" +
        "        \"@id\" : \"80ab93d7-847a-42d2-81d0-1f74b89b81b8\",\n" +
        "\n" +
        "        \"@type\" : \"dcat:DataService\",\n" +
        "\n" +
        "        \"dcat:endpointDescription\" : \"dspace:connector\",\n" +
        "\n" +
        "        \"dcat:endpointUrl\" : \"https://isst-edc-supplier.int.demo.catena-x.net/api/v1/dsp\",\n" +
        "\n" +
        "        \"dct:terms\" : \"dspace:connector\",\n" +
        "\n" +
        "        \"dct:endpointUrl\" : \"https://isst-edc-supplier.int.demo.catena-x.net/api/v1/dsp\"\n" +
        "\n" +
        "      }\n" +
        "\n" +
        "    }, {\n" +
        "\n" +
        "      \"@type\" : \"dcat:Distribution\",\n" +
        "\n" +
        "      \"dct:format\" : {\n" +
        "\n" +
        "        \"@id\" : \"HttpData-PULL\"\n" +
        "\n" +
        "      },\n" +
        "\n" +
        "      \"dcat:accessService\" : {\n" +
        "\n" +
        "        \"@id\" : \"80ab93d7-847a-42d2-81d0-1f74b89b81b8\",\n" +
        "\n" +
        "        \"@type\" : \"dcat:DataService\",\n" +
        "\n" +
        "        \"dcat:endpointDescription\" : \"dspace:connector\",\n" +
        "\n" +
        "        \"dcat:endpointUrl\" : \"https://isst-edc-supplier.int.demo.catena-x.net/api/v1/dsp\",\n" +
        "\n" +
        "        \"dct:terms\" : \"dspace:connector\",\n" +
        "\n" +
        "        \"dct:endpointUrl\" : \"https://isst-edc-supplier.int.demo.catena-x.net/api/v1/dsp\"\n" +
        "\n" +
        "      }\n" +
        "\n" +
        "    }, {\n" +
        "\n" +
        "      \"@type\" : \"dcat:Distribution\",\n" +
        "\n" +
        "      \"dct:format\" : {\n" +
        "\n" +
        "        \"@id\" : \"HttpData-PUSH\"\n" +
        "\n" +
        "      },\n" +
        "\n" +
        "      \"dcat:accessService\" : {\n" +
        "\n" +
        "        \"@id\" : \"80ab93d7-847a-42d2-81d0-1f74b89b81b8\",\n" +
        "\n" +
        "        \"@type\" : \"dcat:DataService\",\n" +
        "\n" +
        "        \"dcat:endpointDescription\" : \"dspace:connector\",\n" +
        "\n" +
        "        \"dcat:endpointUrl\" : \"https://isst-edc-supplier.int.demo.catena-x.net/api/v1/dsp\",\n" +
        "\n" +
        "        \"dct:terms\" : \"dspace:connector\",\n" +
        "\n" +
        "        \"dct:endpointUrl\" : \"https://isst-edc-supplier.int.demo.catena-x.net/api/v1/dsp\"\n" +
        "\n" +
        "      }\n" +
        "\n" +
        "    }, {\n" +
        "\n" +
        "      \"@type\" : \"dcat:Distribution\",\n" +
        "\n" +
        "      \"dct:format\" : {\n" +
        "\n" +
        "        \"@id\" : \"AmazonS3-PUSH\"\n" +
        "\n" +
        "      },\n" +
        "\n" +
        "      \"dcat:accessService\" : {\n" +
        "\n" +
        "        \"@id\" : \"80ab93d7-847a-42d2-81d0-1f74b89b81b8\",\n" +
        "\n" +
        "        \"@type\" : \"dcat:DataService\",\n" +
        "\n" +
        "        \"dcat:endpointDescription\" : \"dspace:connector\",\n" +
        "\n" +
        "        \"dcat:endpointUrl\" : \"https://isst-edc-supplier.int.demo.catena-x.net/api/v1/dsp\",\n" +
        "\n" +
        "        \"dct:terms\" : \"dspace:connector\",\n" +
        "\n" +
        "        \"dct:endpointUrl\" : \"https://isst-edc-supplier.int.demo.catena-x.net/api/v1/dsp\"\n" +
        "\n" +
        "      }\n" +
        "\n" +
        "    } ],\n" +
        "\n" +
        "    \"https://admin-shell.io/aas/3/0/HasSemantics/semanticId\" : {\n" +
        "\n" +
        "      \"@id\" : \"urn:samm:io.catenax.part_type_information:1.0.0#PartTypeInformation\"\n" +
        "\n" +
        "    },\n" +
        "\n" +
        "    \"https://w3id.org/catenax/ontology/common#version\" : \"3.0\",\n" +
        "\n" +
        "    \"id\" : \"PartTypeInformationSubmodelApi@BPNL00000007RXRX\",\n" +
        "\n" +
        "    \"https://purl.org/dc/terms/type\" : {\n" +
        "\n" +
        "      \"@id\" : \"https://w3id.org/catenax/taxonomy#Submodel\"\n" +
        "\n" +
        "    }\n" +
        "\n" +
        "  },\n" +
        "\n" +
        "  \"dcat:service\" : {\n" +
        "\n" +
        "    \"@id\" : \"80ab93d7-847a-42d2-81d0-1f74b89b81b8\",\n" +
        "\n" +
        "    \"@type\" : \"dcat:DataService\",\n" +
        "\n" +
        "    \"dcat:endpointDescription\" : \"dspace:connector\",\n" +
        "\n" +
        "    \"dcat:endpointUrl\" : \"https://isst-edc-supplier.int.demo.catena-x.net/api/v1/dsp\",\n" +
        "\n" +
        "    \"dct:terms\" : \"dspace:connector\",\n" +
        "\n" +
        "    \"dct:endpointUrl\" : \"https://isst-edc-supplier.int.demo.catena-x.net/api/v1/dsp\"\n" +
        "\n" +
        "  },\n" +
        "\n" +
        "  \"participantId\" : \"BPNL00000007RXRX\",\n" +
        "\n" +
        "  \"@context\" : {\n" +
        "\n" +
        "    \"@vocab\" : \"https://w3id.org/edc/v0.0.1/ns/\",\n" +
        "\n" +
        "    \"edc\" : \"https://w3id.org/edc/v0.0.1/ns/\",\n" +
        "\n" +
        "    \"tx\" : \"https://w3id.org/tractusx/v0.0.1/ns/\",\n" +
        "\n" +
        "    \"tx-auth\" : \"https://w3id.org/tractusx/auth/\",\n" +
        "\n" +
        "    \"cx-policy\" : \"https://w3id.org/catenax/policy/\",\n" +
        "\n" +
        "    \"dcat\" : \"http://www.w3.org/ns/dcat#\",\n" +
        "\n" +
        "    \"dct\" : \"http://purl.org/dc/terms/\",\n" +
        "\n" +
        "    \"odrl\" : \"http://www.w3.org/ns/odrl/2/\",\n" +
        "\n" +
        "    \"dspace\" : \"https://w3id.org/dspace/v0.8/\"\n" +
        "\n" +
        "  }\n" +
        "\n" +
        "}";

}
