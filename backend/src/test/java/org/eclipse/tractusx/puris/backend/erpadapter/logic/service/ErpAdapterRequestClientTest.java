package org.eclipse.tractusx.puris.backend.erpadapter.logic.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.Assertions;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
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
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class ErpAdapterRequestClientTest {

    private MockWebServer mockWebServer;

    @Mock
    private VariablesService variablesService;

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
        erpAdapterRequestClient = new ErpAdapterRequestClient(variablesService);

    }

    @AfterEach
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    private void prepareVariablesService() {
        Mockito.when(variablesService.getErpAdapterUrl()).thenReturn(mockWebServer.url("/").toString());
        Mockito.when(variablesService.getErpAdapterAuthKey()).thenReturn(apiKey);
        Mockito.when(variablesService.getErpAdapterAuthSecret()).thenReturn(apiSecret);
        Mockito.when(variablesService.getErpResponseUrl()).thenReturn(erpResponseUrl);
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
        prepareVariablesService();
        erpAdapterRequestClient.sendRequest(erpAdapterRequest);
        RecordedRequest request = mockWebServer.takeRequest();

        // then
        Assertions.assertThat(request.getMethod()).isEqualTo("POST");

        Assertions.assertThat(request.getHeader(apiKey)).isEqualTo(apiSecret);
        Assertions.assertThat(request.getHeader("Content-type")).contains("application/json");

        Assertions.assertThat(request.getPath()).contains(supplierPartnerBpnl);
        Assertions.assertThat(request.getPath()).contains(requestType);
        Assertions.assertThat(request.getPath()).contains(sammVersion);
        Assertions.assertThat(request.getPath()).contains(uuid.toString());

        try (InputStream stream = request.getBody().inputStream()) {
            JsonNode requestBodyNode = objectMapper.readTree(new String(stream.readAllBytes()));
            Assertions.assertThat(requestBodyNode.get("material").asText()).isEqualTo(matNbrCustomer);
            Assertions.assertThat(requestBodyNode.get("direction").asText()).isEqualTo(DirectionCharacteristic.INBOUND.toString());
            Assertions.assertThat(requestBodyNode.get("response-url").asText()).isEqualTo(erpResponseUrl);
        }


    }
}
