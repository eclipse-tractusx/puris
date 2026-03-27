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
import com.apicatalog.jsonld.loader.SchemeRouter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import lombok.extern.slf4j.Slf4j;
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
    private final DocumentLoader CACHED_LOADER;
    private final JsonDocument DEFAULT_CONTEXT_DOCUMENT;

    /**
     * JsonLd processor having preloaded Eclipse Tractus-X related jsonLd documents.
     */
    public JsonLdUtils() {
        // Map of contexts to load
        final String prefix = "json-ld" + File.separator;
        Map<String, String> filesMap = Map.of(
            EdcRequestBodyBuilder.CX_POLICY_CONTEXT, prefix + "cx-policy-v1.jsonld",
            EdcRequestBodyBuilder.ODRL_REMOTE_CONTEXT, prefix + "odrl.jsonld",
            EdcRequestBodyBuilder.DCAT_NAMESPACE, prefix + "dcat.jsonld",
            EdcRequestBodyBuilder.DSPACE_NAMESPACE, prefix + "dspace.jsonld",
            EdcRequestBodyBuilder.EDC_NAMESPACE, prefix + "edc-v1.jsonld",
            EdcRequestBodyBuilder.TX_AUTH_NAMESPACE, prefix + "tx-auth-v1.jsonld",
            EdcRequestBodyBuilder.TX_NAMESPACE, prefix + "tx-v1.jsonld"
        );

        // Load only internal documments
        this.CACHED_LOADER = new CachingDocumentLoader(filesMap);

        JsonObject contextObject = Json.createObjectBuilder()
            .add("edc", EdcRequestBodyBuilder.EDC_NAMESPACE)
            .add("dcat", EdcRequestBodyBuilder.DCAT_NAMESPACE)
            .add("odrl", EdcRequestBodyBuilder.ODRL_REMOTE_CONTEXT)
            .add("dspace", EdcRequestBodyBuilder.DSPACE_NAMESPACE)
            .add("tx", EdcRequestBodyBuilder.TX_NAMESPACE)
            .add("cx-policy", EdcRequestBodyBuilder.CX_POLICY_CONTEXT)
            .build();

        JsonObject rootContext = Json.createObjectBuilder()
            .add("@context", contextObject)
            .build();

        this.DEFAULT_CONTEXT_DOCUMENT = JsonDocument.of(rootContext);
    }

    public JsonNode expand(JsonNode node) {
        try (JsonReader jsonReader = Json.createReader(new StringReader(node.toString()))){
            
            // transform from jackson to jakarta
            JsonStructure jakartaJson = jsonReader.read();
            JsonDocument document = JsonDocument.of(jakartaJson);

            JsonLdOptions options = new JsonLdOptions();
            options.setDocumentLoader(CACHED_LOADER);

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
     * compacts JSON-LD using the default context
     * 
     * @param node JsonNode (jackson) to compact
     */
    public JsonNode compact(JsonNode node) {
        return compact(node, DEFAULT_CONTEXT_DOCUMENT);
    }

    /**
     * compacts JSON-LD using a specific context
     * 
     * @param node JsonNode (jackson) to compact
     * @param contextNode JsonNode (jackson) to use as context during compaction
     */
    public JsonNode compact(JsonNode node, JsonNode contextNode) {
        try (JsonReader contextReader = Json.createReader(new StringReader(contextNode.toString()))) {
            JsonDocument contextDocument = JsonDocument.of(contextReader.read());
            return compact(node, contextDocument);
        }
    }

    /**
     * compacts a JsonNode
     * 
     * @param node JsonNode (Jackson) to compact
     * @param contextDocument JsonDocument (jakarta) to use as context during compaction
     */
    private JsonNode compact(JsonNode node, JsonDocument contextDocument) {
        try (JsonReader jsonReader = Json.createReader(new StringReader(node.toString()))) {
            JsonStructure jakartaJson = jsonReader.read();
            JsonDocument document = JsonDocument.of(jakartaJson);

            JsonLdOptions options = new JsonLdOptions();
            options.setDocumentLoader(CACHED_LOADER);

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
        private final DocumentLoader defaultLoader = SchemeRouter.defaultInstance();

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
