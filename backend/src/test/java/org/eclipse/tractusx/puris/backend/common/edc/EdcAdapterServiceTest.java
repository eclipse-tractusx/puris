package org.eclipse.tractusx.puris.backend.common.edc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcContractMappingService;
import org.eclipse.tractusx.puris.backend.common.edc.logic.util.EdcRequestBodyBuilder;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EdcAdapterServiceTest {

    @Mock
    private VariablesService variablesService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private EdcRequestBodyBuilder edcRequestBodyBuilder;
    @Mock
    private EdcContractMappingService edcContractMappingService;
    private final Pattern urlPattern = PatternStore.URL_PATTERN;
    private static TitaniumJsonLd titaniumJsonLd = new TitaniumJsonLd(new ConsoleMonitor());

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private void allContexts() {
        titaniumJsonLd = new TitaniumJsonLd(new ConsoleMonitor());
        titaniumJsonLd.registerContext(EdcRequestBodyBuilder.ODRL_REMOTE_CONTEXT);
        titaniumJsonLd.registerContext("https://w3id.org/tractusx/policy/v1.0.0");
        titaniumJsonLd.registerContext("https://w3id.org/tractusx/edc/v0.0.1");

        titaniumJsonLd.registerNamespace(JsonLdKeywords.VOCAB, EdcRequestBodyBuilder.EDC_NAMESPACE);

        titaniumJsonLd.registerNamespace("aas-semantics", EdcRequestBodyBuilder.AAS_SEMANTICS_NAMESPACE);

        titaniumJsonLd.registerNamespace("cx-common", EdcRequestBodyBuilder.CX_COMMON_NAMESPACE);

        titaniumJsonLd.registerNamespace("cx-policy", EdcRequestBodyBuilder.CX_POLICY_NAMESPACE);

//        titaniumJsonLd.registerNamespace("odrl", EdcRequestBodyBuilder.ODRL_NAMESPACE);

        titaniumJsonLd.registerNamespace("cx-taxo", EdcRequestBodyBuilder.CX_TAXO_NAMESPACE);

//        titaniumJsonLd.registerNamespace("tx-auth", EdcRequestBodyBuilder.TX_AUTH_NAMESPACE);

        titaniumJsonLd.registerNamespace("tx", EdcRequestBodyBuilder.TX_NAMESPACE);

        titaniumJsonLd.registerNamespace("dcat", EdcRequestBodyBuilder.DCAT_NAMESPACE);

        titaniumJsonLd.registerNamespace("dspace", EdcRequestBodyBuilder.DSPACE_NAMESPACE);

        String PREFIX = "json-ld" + File.separator;
        Map<String, String> FILES = Map.of(
            "https://w3id.org/tractusx/edc/v0.0.1", PREFIX + "tx-v1.jsonld",
            "https://w3id.org/tractusx/policy/v1.0.0", PREFIX + "cx-policy-v1.jsonld",
//            EdcRequestBodyBuilder.TX_AUTH_NAMESPACE, PREFIX + "tx-auth-v1.jsonld",
            "https://w3id.org/edc/v0.0.1", PREFIX + "edc-v1.jsonld",
            EdcRequestBodyBuilder.ODRL_REMOTE_CONTEXT, PREFIX + "odrl.jsonld"

//            EdcRequestBodyBuilder.DSPACE_NAMESPACE, PREFIX + "dspace.jsonld"
        );

        for (var e : FILES.entrySet()) {
            var file = mapToFile(e);
            titaniumJsonLd.registerCachedDocument(e.getKey(), file.getValue().toURI());
        }
    }

    private void smallContexts() {
            titaniumJsonLd = new TitaniumJsonLd(new ConsoleMonitor());

            titaniumJsonLd.registerContext(EdcRequestBodyBuilder.ODRL_REMOTE_CONTEXT);
            titaniumJsonLd.registerContext("https://w3id.org/tractusx/policy/v1.0.0");
            titaniumJsonLd.registerNamespace(JsonLdKeywords.VOCAB, EdcRequestBodyBuilder.EDC_NAMESPACE);
            String PREFIX = "json-ld" + File.separator;
            Map<String, String> FILES = Map.of(
                "https://w3id.org/tractusx/policy/v1.0.0", PREFIX + "cx-policy-v1.jsonld",
                EdcRequestBodyBuilder.ODRL_REMOTE_CONTEXT, PREFIX + "odrl.jsonld"
            );

            for (var e : FILES.entrySet()) {
                var file = mapToFile(e);
                titaniumJsonLd.registerCachedDocument(e.getKey(), file.getValue().toURI());
        }
    }

    private void allContextsBackup() {
        titaniumJsonLd = new TitaniumJsonLd(new ConsoleMonitor());
        titaniumJsonLd.registerContext(EdcRequestBodyBuilder.ODRL_REMOTE_CONTEXT);
        titaniumJsonLd.registerContext("https://w3id.org/tractusx/policy/v1.0.0");

        titaniumJsonLd.registerNamespace(JsonLdKeywords.VOCAB, EdcRequestBodyBuilder.EDC_NAMESPACE);

        titaniumJsonLd.registerNamespace("aas-semantics", EdcRequestBodyBuilder.AAS_SEMANTICS_NAMESPACE);

        titaniumJsonLd.registerNamespace("cx-common", EdcRequestBodyBuilder.CX_COMMON_NAMESPACE);

        titaniumJsonLd.registerNamespace("cx-policy", EdcRequestBodyBuilder.CX_POLICY_NAMESPACE);

        titaniumJsonLd.registerNamespace("odrl", EdcRequestBodyBuilder.ODRL_NAMESPACE);

        titaniumJsonLd.registerNamespace("cx-taxo", EdcRequestBodyBuilder.CX_TAXO_NAMESPACE);

        titaniumJsonLd.registerNamespace("tx-auth", EdcRequestBodyBuilder.TX_AUTH_NAMESPACE);

        titaniumJsonLd.registerNamespace("tx", EdcRequestBodyBuilder.TX_NAMESPACE);

        titaniumJsonLd.registerNamespace("dcat", EdcRequestBodyBuilder.DCAT_NAMESPACE);

        titaniumJsonLd.registerNamespace("dspace", EdcRequestBodyBuilder.DSPACE_NAMESPACE);

        String PREFIX = "json-ld" + File.separator;
        Map<String, String> FILES = Map.of(
            EdcRequestBodyBuilder.TX_NAMESPACE, PREFIX + "tx-v1.jsonld",
            "https://w3id.org/tractusx/policy/v1.0.0", PREFIX + "cx-policy-v1.jsonld",
            EdcRequestBodyBuilder.TX_AUTH_NAMESPACE, PREFIX + "tx-auth-v1.jsonld",
            EdcRequestBodyBuilder.EDC_NAMESPACE, PREFIX + "edc-v1.jsonld",
            EdcRequestBodyBuilder.ODRL_REMOTE_CONTEXT, PREFIX + "odrl.jsonld",

            EdcRequestBodyBuilder.DSPACE_NAMESPACE, PREFIX + "dspace.jsonld"
        );

        for (var e : FILES.entrySet()) {
            var file = mapToFile(e);
            titaniumJsonLd.registerCachedDocument(e.getKey(), file.getValue().toURI());
        }
    }

    private Map.Entry<String, File> mapToFile(Map.Entry<String, String> fileEntry) {
        return Map.entry(fileEntry.getKey(), getResourceFile(fileEntry.getValue()));
    }

    private File getResourceFile(String name) {
        try (var stream = getClass().getClassLoader().getResourceAsStream(name)) {
            if (stream == null) {
                return null;
            }
            var filename = Path.of(name).getFileName().toString();
            var parts = filename.split("\\.");
            var tempFile = Files.createTempFile(parts[0], "." + parts[1]);
            Files.copy(stream, tempFile, REPLACE_EXISTING);
            return tempFile.toFile();
        } catch (Exception e) {
            System.out.println("Failure" + e);
            return null;
        }
    }

    public static JsonObject convertObjectNodeToJsonObject(ObjectNode objectNode) {
        try {
            // Convert ObjectNode to JSON String
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(objectNode);

            // Create JsonReader from JSON String
            JsonReader jsonReader = Json.createReader(new StringReader(jsonString));

            // Get JsonObject from JsonReader
            JsonObject jsonObject = jsonReader.readObject();

            // Close the JsonReader
            jsonReader.close();

            return jsonObject;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }



    @Test
    public void expandCatalog() throws Exception{
        // GIVEN
        var catalogJson = objectMapper.readTree(test2);

        System.out.println("Input data \n" + catalogJson.toPrettyString());

        var jakartaJson = convertObjectNodeToJsonObject((ObjectNode) catalogJson);

        smallContexts();

        System.out.println("Expanded: ");
        var x= titaniumJsonLd.expand(jakartaJson).getContent();

        System.out.println(objectMapper.readTree(x.toString()).toPrettyString());

        var y  = titaniumJsonLd.compact(x).getContent();

        System.out.println("###\n".repeat(2));

        System.out.println("Compacted: ");
        System.out.println(objectMapper.readTree(y.toString()).toPrettyString());

        var z = titaniumJsonLd.expand(y).getContent();


        System.out.println("Expanded again: \n" + objectMapper.readTree(z.toString()).toPrettyString());

        System.out.println(x.toString().equals(z.toString()));
        assertEquals(x, z);

        // WHEN

        // THEN
    }

    static String test1 = "{\n" +
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

    static String test2 = "\n" +
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
