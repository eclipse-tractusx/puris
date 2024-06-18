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

package org.eclipse.tractusx.puris.backend.erpadapter.logic.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.Assertions;
import org.eclipse.tractusx.puris.backend.erpadapter.ErpAdapterConfiguration;
import org.eclipse.tractusx.puris.backend.erpadapter.domain.model.ErpAdapterRequest;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
public class ErpAdapterRequestClientTest {

    private MockWebServer mockWebServer;

    @Mock
    private ErpAdapterConfiguration erpAdapterConfiguration;

    @InjectMocks
    private ErpAdapterRequestClient erpAdapterRequestClient;

    private static final String erpResponseUrl = "http://localhost:8081/catena/erpadapter";

    private static final String matNbrCustomer = "MNR-7307-AU340474.002";

    private static final String supplierPartnerBpnl = "BPNL1234567890ZZ";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String apiKey = "x-api-key";

    private static final String apiSecret = "my-secret";

    private static final String requestType = "itemstock";

    private static final String sammVersion = "2.0";

    @BeforeEach
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        erpAdapterRequestClient = new ErpAdapterRequestClient(erpAdapterConfiguration);
    }

    @AfterEach
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }


    @Test
    public void test_should_success() throws Exception {
        // given
        UUID uuid = UUID.randomUUID();
        ErpAdapterRequest erpAdapterRequest = ErpAdapterRequest.builder()
            .requestDate(new Date())
            .partnerBpnl(supplierPartnerBpnl)
            .id(uuid)
            .directionCharacteristic(DirectionCharacteristic.INBOUND)
            .ownMaterialNumber(matNbrCustomer)
            .requestType(requestType)
            .sammVersion(sammVersion)
            .build();

        // when
        Mockito.when(erpAdapterConfiguration.getErpAdapterUrl()).thenReturn(mockWebServer.url("/").toString());
        Mockito.when(erpAdapterConfiguration.getErpAdapterAuthKey()).thenReturn(apiKey);
        Mockito.when(erpAdapterConfiguration.getErpAdapterAuthSecret()).thenReturn(apiSecret);
        Mockito.when(erpAdapterConfiguration.getErpResponseUrl()).thenReturn(erpResponseUrl);
        erpAdapterRequestClient.sendRequest(erpAdapterRequest);
        RecordedRequest request = mockWebServer.takeRequest(2, TimeUnit.SECONDS);

        // then
        Assertions.assertThat(request.getMethod()).isEqualTo("POST");

        Assertions.assertThat(request.getHeader(apiKey)).isEqualTo(apiSecret);
        Assertions.assertThat(request.getHeader("Content-type")).contains("application/json");

        var pairs = request.getPath().substring(2).split("&");
        Map<String, String> parameters = Stream.of(pairs)
            .map(string -> string.split("="))
            .collect(Collectors.toMap(pair -> pair[0], pair -> pair[1]));

        Assertions.assertThat(parameters.size()).isEqualTo(5);
        Assertions.assertThat(parameters.get("bpnl")).isEqualTo(supplierPartnerBpnl);
        Assertions.assertThat(parameters.get("request-type")).isEqualTo(requestType);
        Assertions.assertThat(parameters.get("samm-version")).isEqualTo(sammVersion);
        Assertions.assertThat(parameters.get("request-timestamp")).isEqualTo(String.valueOf(erpAdapterRequest.getRequestDate().getTime()));
        Assertions.assertThat(parameters.get("request-id")).isEqualTo(uuid.toString());

        try (InputStream stream = request.getBody().inputStream()) {
            JsonNode requestBodyNode = objectMapper.readTree(new String(stream.readAllBytes()));
            Assertions.assertThat(requestBodyNode.get("material").asText()).isEqualTo(matNbrCustomer);
            Assertions.assertThat(requestBodyNode.get("direction").asText()).isEqualTo(DirectionCharacteristic.INBOUND.toString());
            Assertions.assertThat(requestBodyNode.get("responseUrl").asText()).isEqualTo(erpResponseUrl);
        }
    }
}
