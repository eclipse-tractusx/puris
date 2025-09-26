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
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.PolicyProfileVersionEnumeration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.StringReader;
import java.net.URI;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Service
public class JsonLdUtils {

    private final ObjectMapper MAPPER = new ObjectMapper();
    private final TitaniumJsonLd TITANIUM_JSON_LD = new TitaniumJsonLd(new MonitorAdapter() {
    });

   {
        var profile2405 = PolicyProfileVersionEnumeration.POLICY_PROFILE_2405.getConstants();
        var profile2509 = PolicyProfileVersionEnumeration.POLICY_PROFILE_2509.getConstants();
        // register contexts for profile 24.05
        TITANIUM_JSON_LD.registerContext(profile2405.CX_POLICY_CONTEXT);
        // register contexts for profile 25.09
        TITANIUM_JSON_LD.registerContext(profile2509.CX_POLICY_CONTEXT);
        // register common contexts
        TITANIUM_JSON_LD.registerContext(profile2509.ODRL_REMOTE_CONTEXT);
        TITANIUM_JSON_LD.registerContext(profile2509.DCAT_NAMESPACE);
        TITANIUM_JSON_LD.registerContext(profile2509.DSPACE_NAMESPACE);
        TITANIUM_JSON_LD.registerContext(profile2509.EDC_NAMESPACE);
        TITANIUM_JSON_LD.registerContext(profile2509.TX_AUTH_NAMESPACE);
        TITANIUM_JSON_LD.registerContext(profile2509.TX_NAMESPACE);

        final String prefix = "json-ld" + File.separator;
        Map<String, String> filesMap = Map.of(
            profile2405.CX_POLICY_CONTEXT, prefix + "cx-policy-2405.jsonld",
            profile2509.CX_POLICY_CONTEXT, prefix + "cx-policy-2509.jsonld",
            profile2509.ODRL_REMOTE_CONTEXT, prefix + "odrl.jsonld",
            profile2509.DCAT_NAMESPACE, prefix + "dcat.jsonld",
            profile2509.DSPACE_NAMESPACE, prefix + "dspace.jsonld",
            profile2509.EDC_NAMESPACE, prefix + "edc-v1.jsonld",
            profile2509.TX_AUTH_NAMESPACE, prefix + "tx-auth-v1.jsonld",
            profile2509.TX_NAMESPACE, prefix + "tx-v1.jsonld"
        );

        Function<String, URI> uriFunction = fileName -> {
            Resource resource = new ClassPathResource(fileName);
            try {
                return resource.getURI();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        for (var entry : filesMap.entrySet()) {
            TITANIUM_JSON_LD.registerCachedDocument(entry.getKey(), uriFunction.apply(entry.getValue()));
        }
    }

    public ObjectNode expand(JsonNode node) {
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

    public ObjectNode compact(JsonNode node) {
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
