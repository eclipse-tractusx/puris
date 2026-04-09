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

package org.eclipse.tractusx.puris.backend.common.edc.logic.util;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdErrorCode;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.loader.DocumentLoaderOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.tractusx.puris.backend.common.edc.domain.model.PolicyProfileConstants;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.PolicyProfileVersionEnumeration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class JsonLdUtils {

    private final ObjectMapper MAPPER = new ObjectMapper();
    private final DocumentLoader CACHED_LOADER_2405;
    private final DocumentLoader CACHED_LOADER_2509;
    private final JsonDocument DEFAULT_CONTEXT_DOCUMENT_2405;
    private final JsonDocument DEFAULT_CONTEXT_DOCUMENT_2509;

    /**
     * JsonLd processor having preloaded Eclipse Tractus-X related jsonLd documents.
     */
    public JsonLdUtils() {
        // Map of contexts to load
        final String prefix = "json-ld" + File.separator;
        PolicyProfileConstants profile2405 = PolicyProfileVersionEnumeration.POLICY_PROFILE_2405.getConstants();
        PolicyProfileConstants profile2509 = PolicyProfileVersionEnumeration.POLICY_PROFILE_2509.getConstants();
        Map<String, String> filesMap2405 = Map.of(
            profile2405.CX_POLICY_CONTEXT, prefix + "cx-policy-2405.jsonld",
            profile2405.ODRL_REMOTE_CONTEXT, prefix + "odrl.jsonld",
            profile2405.DCAT_NAMESPACE, prefix + "dcat.jsonld",
            profile2405.DSPACE_NAMESPACE, prefix + "dspace.jsonld",
            profile2405.EDC_NAMESPACE, prefix + "edc-v1.jsonld",
            profile2405.TX_AUTH_NAMESPACE, prefix + "tx-auth-v1.jsonld",
            profile2405.TX_NAMESPACE, prefix + "tx-v1.jsonld"
        );
        Map<String, String> filesMap2509 = Map.of(
            profile2509.CX_POLICY_CONTEXT, prefix + "cx-policy-2509.jsonld",
            profile2509.ODRL_REMOTE_CONTEXT, prefix + "odrl.jsonld",
            profile2509.DCAT_NAMESPACE, prefix + "dcat.jsonld",
            profile2509.DSPACE_NAMESPACE, prefix + "dspace.jsonld",
            profile2509.EDC_NAMESPACE, prefix + "edc-v1.jsonld",
            profile2509.TX_AUTH_NAMESPACE, prefix + "tx-auth-v1.jsonld",
            profile2509.TX_NAMESPACE, prefix + "tx-v1.jsonld"
        );

        // Load only internal documments
        this.CACHED_LOADER_2405 = new CachingDocumentLoader(filesMap2405);

        JsonObject contextObject2405 = Json.createObjectBuilder()
            .add("edc", profile2405.EDC_NAMESPACE)
            .add("dcat", profile2405.DCAT_NAMESPACE)
            .add("odrl", profile2405.ODRL_REMOTE_CONTEXT)
            .add("dspace", profile2405.DSPACE_NAMESPACE)
            .add("tx", profile2405.TX_NAMESPACE)
            .add("cx-policy", profile2405.CX_POLICY_CONTEXT)
            .build();

        JsonObject rootContext2405 = Json.createObjectBuilder()
            .add("@context", contextObject2405)
            .build();

        this.DEFAULT_CONTEXT_DOCUMENT_2405 = JsonDocument.of(rootContext2405);

        // Load only internal documments
        this.CACHED_LOADER_2509 = new CachingDocumentLoader(filesMap2509);

        JsonObject contextObject2509 = Json.createObjectBuilder()
            .add("edc", profile2509.EDC_NAMESPACE)
            .add("dcat", profile2509.DCAT_NAMESPACE)
            .add("odrl", profile2509.ODRL_REMOTE_CONTEXT)
            .add("dspace", profile2509.DSPACE_NAMESPACE)
            .add("tx", profile2509.TX_NAMESPACE)
            .add("cx-policy", profile2509.CX_POLICY_CONTEXT)
            .build();

        JsonObject rootContext2509 = Json.createObjectBuilder()
            .add("@context", contextObject2509)
            .build();

        this.DEFAULT_CONTEXT_DOCUMENT_2509 = JsonDocument.of(rootContext2509);
    }

    /**
     * expands JSON-LD using the default context and the specified policy profile
     * 
     * @param node JsonNode (jackson) to expand
     * @param profileVersion PolicyProfileVersionEnumeration to determine which default context and docuement loader to use during expansion
     */
    public JsonNode expand(JsonNode node, PolicyProfileVersionEnumeration profileVersion) {
        try (JsonReader jsonReader = Json.createReader(new StringReader(node.toString()))){
            
            // transform from jackson to jakarta
            JsonStructure jakartaJson = jsonReader.read();
            JsonDocument document = JsonDocument.of(jakartaJson);

            JsonLdOptions options = new JsonLdOptions();
            var loader = profileVersion == PolicyProfileVersionEnumeration.POLICY_PROFILE_2405 ? CACHED_LOADER_2405 : CACHED_LOADER_2509;
            options.setDocumentLoader(loader);

            var expandedJakartaJson = JsonLd.expand(document)
                .options(options)
                .get();

            // transform back from jakarto to jackson
            return MAPPER.readTree(expandedJakartaJson.toString());
        } catch (Exception e) {
            log.error("Failure expanding json node: \n{}", node.toPrettyString(), e);
        }
        return null;
    }

    /**
     * compacts JSON-LD using the default context and the specified policy profile
     * 
     * @param node JsonNode (jackson) to compact
     * @param profileVersion PolicyProfileVersionEnumeration to determine which default context and docuement loader to use during compaction
     */
    public JsonNode compact(JsonNode node, PolicyProfileVersionEnumeration profileVersion) {
        var contextDocument = profileVersion == PolicyProfileVersionEnumeration.POLICY_PROFILE_2405 ? DEFAULT_CONTEXT_DOCUMENT_2405 : DEFAULT_CONTEXT_DOCUMENT_2509;
        return compact(node, contextDocument, profileVersion);
    }

    /**
     * compacts JSON-LD using a specific context and the specified policy profile
     * 
     * @param node JsonNode (jackson) to compact
     * @param contextNode JsonNode (jackson) to use as context during compaction
     * @param profileVersion PolicyProfileVersionEnumeration to determine which document loader to use during compaction
     */
    public JsonNode compact(JsonNode node, JsonNode contextNode, PolicyProfileVersionEnumeration profileVersion) {
        try (JsonReader contextReader = Json.createReader(new StringReader(contextNode.toString()))) {
            JsonDocument contextDocument = JsonDocument.of(contextReader.read());
            return compact(node, contextDocument, profileVersion);
        }
    }

    /**
     * compacts a JsonNode using a given context document and the specified policy profile
     * 
     * @param node JsonNode (Jackson) to compact
     * @param contextDocument JsonDocument (jakarta) to use as context during compaction
     * @param profileVersion PolicyProfileVersionEnumeration to determine which document loader to use during compaction
     */
    private JsonNode compact(JsonNode node, JsonDocument contextDocument, PolicyProfileVersionEnumeration profileVersion) {
        try (JsonReader jsonReader = Json.createReader(new StringReader(node.toString()))) {
            JsonStructure jakartaJson = jsonReader.read();
            JsonDocument document = JsonDocument.of(jakartaJson);

            JsonLdOptions options = new JsonLdOptions();
            var loader = profileVersion == PolicyProfileVersionEnumeration.POLICY_PROFILE_2405 ? CACHED_LOADER_2405 : CACHED_LOADER_2509;
            options.setDocumentLoader(loader);

            var compactedObject = JsonLd.compact(document, contextDocument)
                .options(options)
                .get();

            return MAPPER.readTree(compactedObject.toString());
        } catch (JsonLdError | IOException e) {
            log.error("Failure compacting json node", e);
        }
        return null;
    }

    /**
     * Internal Custom Loader that serves Contexts from ClassPath (memory)
     * and falls back to HTTP for everything else.
     */
    private static class CachingDocumentLoader implements DocumentLoader {
        private final Map<URI, Document> cache = new HashMap<>();

        public CachingDocumentLoader(Map<String, String> filesMap) {
            for (Map.Entry<String, String> entry : filesMap.entrySet()) {
                try {
                    URI uri = URI.create(entry.getKey());
                    ClassPathResource resource = new ClassPathResource(entry.getValue());
                    try (InputStream is = resource.getInputStream()) {
                        // Parse the local file immediately into a JsonDocument
                        JsonDocument doc = JsonDocument.of(Json.createReader(is).read());
                        cache.put(uri, doc);
                    }
                } catch (Exception e) {
                    log.error("Failed to load cached JSON-LD context: {}", entry.getKey(), e);
                }
            }
        }

        @Override
        public Document loadDocument(URI url, DocumentLoaderOptions options) throws JsonLdError {
            // Return cached document, if loaded
            if (cache.containsKey(url)) {
                return cache.get(url);
            }
            throw new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED, String.format("Document \"%s\" has not been found in cache. Due to security concerns, no remote file has been loaded.", url.toString()));
          }
    }
}
