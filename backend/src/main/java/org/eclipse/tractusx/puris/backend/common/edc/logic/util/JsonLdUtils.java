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

package org.eclipse.tractusx.puris.backend.common.edc.logic.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.json.Json;
import jakarta.json.JsonReader;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.spi.monitor.Monitor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class JsonLdUtils {

    private final static ObjectMapper MAPPER = new ObjectMapper();

    private final static TitaniumJsonLd TITANIUM_JSON_LD = new TitaniumJsonLd(new MonitorAdapter());

    static {
        TITANIUM_JSON_LD.registerContext(EdcRequestBodyBuilder.ODRL_REMOTE_CONTEXT);
        TITANIUM_JSON_LD.registerContext(EdcRequestBodyBuilder.TX_POLICY_CONTEXT);
        TITANIUM_JSON_LD.registerNamespace(JsonLdKeywords.VOCAB, EdcRequestBodyBuilder.EDC_NAMESPACE);
        final String prefix = "json-ld" + File.separator;
        Map<String, String> filesMap = Map.of(
            EdcRequestBodyBuilder.TX_POLICY_CONTEXT, prefix + "cx-policy-v1.jsonld",
            EdcRequestBodyBuilder.ODRL_REMOTE_CONTEXT, prefix + "odrl.jsonld"
        );

        Function<String, URI> uriFunction = fileName -> {
            Resource resource = new ClassPathResource(fileName);
            try {
                return resource.getURI();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        for (var entry : filesMap.entrySet()) {
            TITANIUM_JSON_LD.registerCachedDocument(entry.getKey(), uriFunction.apply(entry.getValue()));
        }
    }

    public static ObjectNode expand(JsonNode node) {
        JsonReader jsonReader = Json.createReader(new StringReader(node.toString()));
        var jakartaJson = jsonReader.readObject();
        var expandedJakartaJson = TITANIUM_JSON_LD.expand(jakartaJson).getContent();
        try {
            return (ObjectNode) MAPPER.readTree(expandedJakartaJson.toString());
        } catch (Exception e) {
            log.error("Failure expanding json node: \n{}", node.toPrettyString(), e);
            return null;
        }
    }

    public static ObjectNode compact(JsonNode node) {
        JsonReader jsonReader = Json.createReader(new StringReader(node.toString()));
        var jakartaJson = jsonReader.readObject();
        var expandedJakartaJson = TITANIUM_JSON_LD.compact(jakartaJson).getContent();
        try {
            return (ObjectNode) MAPPER.readTree(expandedJakartaJson.toString());
        } catch (Exception e) {
            log.error("Failure compacting json node: \n{}", node.toPrettyString(), e);
            return null;
        }
    }



    private static class MonitorAdapter implements Monitor {

        @Override
        public void severe(Supplier<String> supplier, Throwable... errors) {
            log.error(supplier.get());
            if (errors != null) {
                for (var error : errors) {
                    log.error("{} \n{}", error.getMessage(), error.getStackTrace());
                }
            }
        }

        @Override
        public void severe(Map<String, Object> data) {
            for(var entry : data.entrySet()){
                log.error(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public void warning(Supplier<String> supplier, Throwable... errors) {
            log.warn(supplier.get());
            if (errors != null) {
                for (var error : errors) {
                    log.warn("{} \n{}", error.getMessage(), error.getStackTrace());
                }
            }
        }

        @Override
        public void info(Supplier<String> supplier, Throwable... errors) {
            log.info(supplier.get(), (Object[]) errors);
        }

        @Override
        public void debug(Supplier<String> supplier, Throwable... errors) {
            log.debug(supplier.get(), (Object[]) errors);
        }


    }
}
