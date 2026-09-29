/*
 * Copyright (c) 2026 Volkswagen AG
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
package org.eclipse.tractusx.puris.backend.irs.logic.service;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.eclipse.tractusx.puris.backend.irs.IrsAdapterConfiguration;
import org.eclipse.tractusx.puris.backend.irs.domain.model.IrsQueuedRequestMethodEnumeration;
import org.eclipse.tractusx.puris.backend.irs.logic.service.IrsRequestService.IrsResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IrsRequestServiceTest {

    private MockWebServer mockWebServer;

    @Mock
    private IrsAdapterConfiguration irsAdapterConfiguration;

    private IrsRequestService irsRequestService;

    private static final String AUTH_KEY = "x-api-key";
    private static final String AUTH_SECRET = "test-secret";

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        irsRequestService = new IrsRequestService(irsAdapterConfiguration);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    // --- isEnabled ---

    @Test
    void isEnabled_WhenConfigTrue_ReturnsTrue() {
        when(irsAdapterConfiguration.isIrsAdapterEnabled()).thenReturn(true);
        assertThat(irsRequestService.isEnabled()).isTrue();
    }

    @Test
    void isEnabled_WhenConfigFalse_ReturnsFalse() {
        when(irsAdapterConfiguration.isIrsAdapterEnabled()).thenReturn(false);
        assertThat(irsRequestService.isEnabled()).isFalse();
    }

    // --- disabled guard ---

    @Test
    void get_WhenAdapterDisabled_ThrowsIllegalStateException() {
        when(irsAdapterConfiguration.isIrsAdapterEnabled()).thenReturn(false);
        assertThrows(IllegalStateException.class,
            () -> irsRequestService.execute(IrsQueuedRequestMethodEnumeration.GET, "/test", null, null, false));
    }

    @Test
    void post_WhenAdapterDisabled_ThrowsIllegalStateException() {
        when(irsAdapterConfiguration.isIrsAdapterEnabled()).thenReturn(false);
        assertThrows(IllegalStateException.class,
            () -> irsRequestService.execute(IrsQueuedRequestMethodEnumeration.POST, "/test", null, "{}", false));
    }

    @Test
    void put_WhenAdapterDisabled_ThrowsIllegalStateException() {
        when(irsAdapterConfiguration.isIrsAdapterEnabled()).thenReturn(false);
        assertThrows(IllegalStateException.class,
            () -> irsRequestService.execute(IrsQueuedRequestMethodEnumeration.PUT, "/test", null, "{}", false));
    }

    @Test
    void delete_WhenAdapterDisabled_ThrowsIllegalStateException() {
        when(irsAdapterConfiguration.isIrsAdapterEnabled()).thenReturn(false);
        assertThrows(IllegalStateException.class,
            () -> irsRequestService.execute(IrsQueuedRequestMethodEnumeration.DELETE, "/test", null, null, false));
    }

    // --- GET ---

    @Test
    void get_WhenEnabled_SendsGetRequest() throws Exception {
        stubEnabled();
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        IrsResponse response = irsRequestService.execute(IrsQueuedRequestMethodEnumeration.GET, "/irs/policies", null, null, false);

        RecordedRequest request = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getHeader(AUTH_KEY)).isEqualTo(AUTH_SECRET);
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.isSuccessful()).isTrue();
    }

    @Test
    void get_WithQueryParams_AppendsParamsToUrl() throws Exception {
        stubEnabled();
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        irsRequestService.execute(IrsQueuedRequestMethodEnumeration.GET, "/irs/policies", Map.of("state", "COMPLETED", "limit", "10"), null, false);

        RecordedRequest request = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        assertThat(request.getPath()).contains("state=COMPLETED");
        assertThat(request.getPath()).contains("limit=10");
    }

    // --- POST ---

    @Test
    void post_WhenEnabled_SendsBodyAsJson() throws Exception {
        stubEnabled();
        mockWebServer.enqueue(new MockResponse().setResponseCode(201).setBody("{\"id\":\"abc\"}"));

        String jsonBody = "{\"globalAssetId\":\"urn:uuid:test\"}";
        IrsResponse response = irsRequestService.execute(IrsQueuedRequestMethodEnumeration.POST, "/irs/policies", null, jsonBody, false);

        RecordedRequest request = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getHeader("Content-Type")).contains("application/json");
        assertThat(request.getBody().readUtf8()).isEqualTo(jsonBody);
        assertThat(response.getStatusCode()).isEqualTo(201);
        assertThat(response.isSuccessful()).isTrue();
    }

    @Test
    void post_WithNullBody_SendsEmptyJsonObject() throws Exception {
        stubEnabled();
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        irsRequestService.execute(IrsQueuedRequestMethodEnumeration.POST, "/irs/policies", null, null, false);

        RecordedRequest request = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        assertThat(request.getBody().readUtf8()).isEqualTo("{}");
    }

    // --- PUT ---

    @Test
    void put_WhenEnabled_SendsPutWithBody() throws Exception {
        stubEnabled();
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        String jsonBody = "{\"state\":\"CANCELED\"}";
        IrsResponse response = irsRequestService.execute(IrsQueuedRequestMethodEnumeration.PUT, "/irs/policies", null, jsonBody, false);

        RecordedRequest request = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        assertThat(request.getMethod()).isEqualTo("PUT");
        assertThat(request.getBody().readUtf8()).isEqualTo(jsonBody);
        assertThat(response.isSuccessful()).isTrue();
    }

    // --- DELETE ---

    @Test
    void delete_WhenEnabled_SendsDeleteRequest() throws Exception {
        stubEnabled();
        mockWebServer.enqueue(new MockResponse().setResponseCode(204));

        IrsResponse response = irsRequestService.execute(IrsQueuedRequestMethodEnumeration.DELETE, "/irs/policies/abc", null, null, false);

        RecordedRequest request = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        assertThat(request.getMethod()).isEqualTo("DELETE");
        assertThat(response.getStatusCode()).isEqualTo(204);
    }

    @Test
    void delete_WithQueryParams_AppendsParamsToUrl() throws Exception {
        stubEnabled();
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        irsRequestService.execute(IrsQueuedRequestMethodEnumeration.DELETE, "/irs/policies", Map.of("id", "policy-id-123"), null, false);

        RecordedRequest request = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        assertThat(request.getPath()).contains("id=policy-id-123");
    }

    // --- response code mapping ---

    @Test
    void execute_OnSuccessResponse_ReturnsSuccessfulIrsResponse() throws Exception {
        stubEnabled();
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("success-body"));

        IrsResponse response = irsRequestService.execute(IrsQueuedRequestMethodEnumeration.GET, "/test", null, null, false);

        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getResponseBody()).isEqualTo("success-body");
    }

    @Test
    void execute_OnErrorResponse_ReturnsUnsuccessfulIrsResponse() throws Exception {
        stubEnabled();
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("internal server error"));

        IrsResponse response = irsRequestService.execute(IrsQueuedRequestMethodEnumeration.GET, "/test", null, null, false);

        assertThat(response.isSuccessful()).isFalse();
        assertThat(response.getStatusCode()).isEqualTo(500);
        assertThat(response.getResponseBody()).isEqualTo("internal server error");
    }

    @Test
    void execute_On404Response_ReturnsUnsuccessfulIrsResponse() throws Exception {
        stubEnabled();
        mockWebServer.enqueue(new MockResponse().setResponseCode(404).setBody("not found"));

        IrsResponse response = irsRequestService.execute(IrsQueuedRequestMethodEnumeration.GET, "/irs/policiess/nonexistent", null, null, false);

        assertThat(response.isSuccessful()).isFalse();
        assertThat(response.getStatusCode()).isEqualTo(404);
    }

    @Test
    void execute_OnEmptyBody_ReturnsEmptyString() throws Exception {
        stubEnabled();
        mockWebServer.enqueue(new MockResponse().setResponseCode(204));

        IrsResponse response = irsRequestService.execute(IrsQueuedRequestMethodEnumeration.DELETE, "/irs/policies/abc", null, null, false);

        assertThat(response.getResponseBody()).isEmpty();
    }

    // --- network error / timeout ---

    @Test
    void execute_OnNetworkError_ReturnsFailedResponse() throws Exception {
        stubEnabled();
        // Shut down the server so the connection is refused
        mockWebServer.shutdown();

        IrsResponse response = irsRequestService.execute(IrsQueuedRequestMethodEnumeration.GET, "/test", null, null, false);

        assertThat(response.isSuccessful()).isFalse();
        assertThat(response.getStatusCode()).isEqualTo(500);
        assertThat(response.getResponseBody()).contains("/test");

        // Prevent double-shutdown in @AfterEach
        mockWebServer = new MockWebServer();
    }

    @Test
    void execute_OnTimeout_ReturnsFailedResponse() throws Exception {
        stubEnabled();
        try (MockWebServer fastMockServer = new MockWebServer()) {
            fastMockServer.start();
            fastMockServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

            // Reconfigure service to point at the fast server
            when(irsAdapterConfiguration.getIrsAdapterUrl()).thenReturn(fastMockServer.url("/").toString());

            IrsResponse response = irsRequestService.execute(IrsQueuedRequestMethodEnumeration.GET, "/test", null, null, false);

            assertThat(response.isSuccessful()).isFalse();
            assertThat(response.getStatusCode()).isEqualTo(500);

            fastMockServer.shutdown();
        }
    }

    // --- buildUrl edge cases ---

    @Test
    void buildUrl_WithNullPath_UsesBaseUrlOnly() throws Exception {
        stubEnabled();
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        irsRequestService.execute(IrsQueuedRequestMethodEnumeration.GET, null, null, null, false);

        RecordedRequest request = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        // Path should be just the root '/'
        assertThat(request.getPath()).isEqualTo("/");
    }

    @Test
    void buildUrl_WithLeadingSlashPath_ParsedCorrectly() throws Exception {
        stubEnabled();
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        irsRequestService.execute(IrsQueuedRequestMethodEnumeration.GET, "/irs/jobs", null, null, false);

        RecordedRequest request = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        assertThat(request.getPath()).isEqualTo("/irs/jobs");
    }

    @Test
    void buildUrl_WithoutLeadingSlash_ParsedCorrectly() throws Exception {
        stubEnabled();
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        irsRequestService.execute(IrsQueuedRequestMethodEnumeration.GET, "irs/jobs", null, null, false);

        RecordedRequest request = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        assertThat(request.getPath()).isEqualTo("/irs/jobs");
    }

    @Test
    void buildUrl_WithNullQueryParams_NoQueryString() throws Exception {
        stubEnabled();
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        irsRequestService.execute(IrsQueuedRequestMethodEnumeration.GET, "/irs/jobs", null, null, false);

        RecordedRequest request = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        assertThat(request.getPath()).doesNotContain("?");
    }

    // --- auth header ---

    @Test
    void execute_AlwaysInjectsAuthHeader() throws Exception {
        stubEnabled();
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        irsRequestService.execute(IrsQueuedRequestMethodEnumeration.POST, "/test", null, "{}", false);

        RecordedRequest request = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        assertThat(request.getHeader(AUTH_KEY)).isEqualTo(AUTH_SECRET);
    }

    // --- helpers ---

    private void stubEnabled() {
        when(irsAdapterConfiguration.isIrsAdapterEnabled()).thenReturn(true);
        when(irsAdapterConfiguration.getIrsAdapterUrl()).thenReturn(mockWebServer.url("/").toString());
        when(irsAdapterConfiguration.getIrsAdapterAuthKey()).thenReturn(AUTH_KEY);
        when(irsAdapterConfiguration.getIrsAdapterAuthSecret()).thenReturn(AUTH_SECRET);
    }
}
