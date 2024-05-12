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

package org.eclipse.tractusx.puris.backend.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class OAuth2ClientInterceptorTest {

    public static final String VALUE_CLIENT_ID = "clientId";
    public static final String VALUE_CLIENT_SECRET = "clientSecret";
    public static final String VALUE_GRANT_TYPE = "client_credentials";
    public static final String VALUE_VALID_MOCK_BEARER_TOKEN = "mock-bearer-token";
    public static final String VALUE_INVALID_MOCK_BEARER_TOKEN = "initial-but-invalid-token";

    /**
     * Path to configure {@code tokenServer} url
     **/
    private final String TOKEN_URL_PATH = "/mocked/token";
    /**
     * Path to configure {@code actualRequestServer} url
     **/
    private final String SERVICE_URL_PATH = "/actual/service";

    private OAuth2ClientInterceptor oAuth2ClientInterceptor;

    /**
     * Mock Server representing the OAuth2 Server
     **/
    private MockWebServer tokenServer;
    /**
     * Mock Server representing the Server / Service that needs to be authenticated with the interceptor
     **/
    private MockWebServer actualRequestServer;

    /**
     * Interceptor chain to mock so that one can capture the request changes of the interceptor
     **/
    @Mock
    private Interceptor.Chain chain;

    @BeforeEach
    public void setup() throws IOException {
        tokenServer = new MockWebServer();
        tokenServer.start();

        actualRequestServer = new MockWebServer();
        actualRequestServer.start();

        MockitoAnnotations.openMocks(this);

        ObjectMapper objectMapper = new ObjectMapper();

        oAuth2ClientInterceptor = new OAuth2ClientInterceptor(objectMapper,
            tokenServer.url(TOKEN_URL_PATH).toString(),
            VALUE_CLIENT_ID,
            VALUE_CLIENT_SECRET,
            VALUE_GRANT_TYPE
        );
    }

    @AfterEach
    public void teardown() throws IOException {
        tokenServer.shutdown();
        actualRequestServer.shutdown();
        oAuth2ClientInterceptor = null;
    }

    /**
     * Checks that the JWT token is extracted correctly and
     * inserted as BEARER token in intercepted request's auth header
     * <p>
     * Verifies that the auth request against the OAauth2 server is correct.
     */
    @Test
    public void testInterceptor_obtainValidTokenSuccessfully() throws IOException, InterruptedException {

        // GIVEN
        // Response of OAuth2 Server
        MockResponse tokenResponse = new MockResponse()
            .setResponseCode(200)
            .setBody("{\"access_token\": \"" + VALUE_VALID_MOCK_BEARER_TOKEN + "\"}")
            .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        // Enqueue a mock response from the server
        tokenServer.enqueue(tokenResponse);

        // Create a request against the intended service that needs authentication
        Request request = new Request.Builder()
            .url(actualRequestServer.url(SERVICE_URL_PATH))
            .build();

        // needed as 401 might indicate outdated token
        MockResponse mockResponse = new MockResponse()
            .setResponseCode(200)
            .setBody("OK")
            .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        actualRequestServer.enqueue(mockResponse);

        // WHEN
        // Mock the behavior of the chain
        // return original request that needs token
        when(chain.request()).thenReturn(request);

        // capture the requests sent via chain.proceed
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        when(chain.proceed(requestCaptor.capture())).thenReturn(mock(Response.class));

        // DO -> trigger interceptor with request
        oAuth2ClientInterceptor.intercept(chain);

        //THEN
        verify(chain, times(1)).proceed(any(Request.class));

        // Verify the request sent by the interceptor
        RecordedRequest recordedRequest = tokenServer.takeRequest();

        // Get the form body from the recorded request
        Buffer requestBodyBuffer = recordedRequest.getBody();
        String requestBody = requestBodyBuffer.readUtf8();

        // Split the form body string and convert to a map using streams
        // form body like "key=value&key2=value2"
        Map<String, String> formFieldMap = Arrays.stream(requestBody.split("&"))
            .map(formField -> formField.split("="))
            .filter(keyValue -> keyValue.length == 2)
            .collect(Collectors.toMap(keyValue -> keyValue[0], keyValue -> keyValue[1]));

        // assert tokenRequest Values
        assertEquals(TOKEN_URL_PATH, recordedRequest.getPath());
        assertEquals(VALUE_CLIENT_ID, formFieldMap.get(OAuth2ClientInterceptor.KEY_CLIENT_ID));
        assertEquals(VALUE_CLIENT_SECRET, formFieldMap.get(OAuth2ClientInterceptor.KEY_CLIENT_SECRET));
        assertEquals(VALUE_GRANT_TYPE, formFieldMap.get(OAuth2ClientInterceptor.KEY_GRANT_TYPE));

        // Verify the intercepted request has the bearer token
        Request interceptedRequest = requestCaptor.getValue();
        assertEquals("Bearer " + VALUE_VALID_MOCK_BEARER_TOKEN,
            interceptedRequest.header(OAuth2ClientInterceptor.KEY_HEADER_AUTHORIZATION));
    }

    /**
     * assumes that an invalid token has been set and needs to be renewed
     */
    @Test
    public void testInterceptor_refreshTokenSuccessfully() throws Exception {

        // GIVEN
        // outdated Token already set
        setJwtToken(VALUE_INVALID_MOCK_BEARER_TOKEN);

        MockResponse tokenResponse = new MockResponse()
            .setResponseCode(200)
            .setBody("{\"access_token\": \"" + VALUE_VALID_MOCK_BEARER_TOKEN + "\"}")
            .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        // Enqueue a mock response from the server
        tokenServer.enqueue(tokenResponse);

        // Create a request against the server to be authenticated
        Request request = new Request.Builder()
            .url(actualRequestServer.url(SERVICE_URL_PATH))
            .build();

        Response notAuthorizedMockResponse = new Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_2)
            .message("Not Authorized.")
            .code(401)
            .build();

        // Mock the behavior of the chain
        // return original request that needs token
        when(chain.request()).thenReturn(request);

        // First time request returns 401 due to invalid token
        // Second is just OK
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        when(chain.proceed(requestCaptor.capture()))
            .thenReturn(notAuthorizedMockResponse)
            .thenReturn(mock(Response.class));

        // DO
        oAuth2ClientInterceptor.intercept(chain);

        // THEN
        // proceed triggered first with outdated, then with updated token
        verify(chain, times(2)).proceed(any(Request.class));

        List<Request> interceptedRequests = requestCaptor.getAllValues();

        // Verify the intercepted request has the outdated bearer token
        Request interceptedRequestWithInvalidToken = interceptedRequests.get(0);
        assertEquals("Bearer " + VALUE_INVALID_MOCK_BEARER_TOKEN,
            interceptedRequestWithInvalidToken.header(OAuth2ClientInterceptor.KEY_HEADER_AUTHORIZATION));

        // Verify the intercepted request has the updated bearer token
        Request interceptedRequestWithValidToken = interceptedRequests.get(1);
        assertEquals("Bearer " + VALUE_VALID_MOCK_BEARER_TOKEN,
            interceptedRequestWithValidToken.header("Authorization"));
        assertEquals(VALUE_VALID_MOCK_BEARER_TOKEN, getJwtToken());
    }

    /**
     * chain is interrupted with 403 in case authentication does not work
     */
    @Test
    public void testInterceptor_failObtainToken() throws IOException {

        // GIVEN
        // 401 response indicating invalid credentials
        MockResponse tokenResponse = new MockResponse()
            .setResponseCode(401)
            .setBody("""
                {
                    "error": "invalid_client",
                    "error_description": "Invalid client or Invalid client credentials"
                }""")
            .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        // Enqueue a mock response from the server
        tokenServer.enqueue(tokenResponse);

        // Create a request against service to be authenticated
        Request request = new Request.Builder()
            .url(actualRequestServer.url(SERVICE_URL_PATH))
            .build();

        // WHEN
        // Mock the behavior of the chain
        // return original request that needs token
        when(chain.request()).thenReturn(request);

        //DO - trigger chain
        Response response = oAuth2ClientInterceptor.intercept(chain);

        // THEN
        // chain is aborted with 403
        verify(chain, times(0)).proceed(any(Request.class));
        assertEquals(403, response.code());
    }

    /**
     * helper to get private field value of {@code oAuth2ClientInterceptor.jwtToken}
     *
     * @return value of the jwtToken field
     * @throws Exception if field can not be accessed (illegal, not existing)
     */
    private String getJwtToken() throws Exception {
        Field jwtTokenField = oAuth2ClientInterceptor.getClass().getDeclaredField("jwtAccessToken");
        jwtTokenField.setAccessible(true);
        return (String) jwtTokenField.get(oAuth2ClientInterceptor);
    }

    /**
     * helper to set private field value of {@code oAuth2ClientInterceptor.jwtToken}
     *
     * @throws Exception if field can not be accessed (illegal, not existing)
     */
    private void setJwtToken(String jwtToken) throws Exception {
        Field jwtTokenField = oAuth2ClientInterceptor.getClass().getDeclaredField("jwtAccessToken");
        jwtTokenField.setAccessible(true);
        jwtTokenField.set(oAuth2ClientInterceptor, jwtToken);
    }
}
