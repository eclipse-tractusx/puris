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
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Class allowing to authenticate following OAuth2 (with e.g, client credential flow) against a service.
 * <p>
 * Stores the jwt token and renews it, if outdated (via retry)
 */
@Slf4j
public class OAuth2ClientInterceptor implements Interceptor {

    public static final String KEY_GRANT_TYPE = "grant_type";
    public static final String KEY_CLIENT_ID = "client_id";
    public static final String KEY_CLIENT_SECRET = "client_secret";
    public static final String KEY_HEADER_AUTHORIZATION = "Authorization";
    private final ObjectMapper objectMapper;


    /**
     * creates OAuth2Client Interceptor that obtains jwtTokens and adds them as Bearer
     *
     * @param objectMapper to parse and read json value
     * @param tokenUrl     to authenticate against (full url including realm and protocol)
     * @param clientId     to authenticate against
     * @param clientSecret to authenticate with
     * @param grant_type   to use as flow (e.g. client_credentials)
     */
    public OAuth2ClientInterceptor(ObjectMapper objectMapper,
                                   String tokenUrl,
                                   String clientId,
                                   String clientSecret,
                                   String grant_type) {
        this.objectMapper = objectMapper;
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.grant_type = grant_type;
    }

    /**
     * contains token, if obtained; may be outdated
     **/
    private String jwtAccessToken;

    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;
    private final String grant_type;


    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {

        //do before
        Request request = chain.request();

        // perform idp call & extract jwt
        if (jwtAccessToken == null) {
            if (!obtainAccessToken()) {
                return new Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_2)
                    .code(403)
                    .message("Access token could not be obtained.")
                    .build();
            }
        }

        // append token
        Request requestWithToken = request.newBuilder()
            .header(KEY_HEADER_AUTHORIZATION, "Bearer " + jwtAccessToken)
            .build();

        Response response = chain.proceed(requestWithToken);

        // if 401, assume the token to be invalid
        if (response.code() == 401) {
            log.debug("Oauth2 Client token renewal needed.");
            obtainAccessToken();

            requestWithToken = request.newBuilder()
                .header("Authorization", "Bearer " + jwtAccessToken)
                .build();

            response = chain.proceed(requestWithToken);
        }
        return response;
    }

    /**
     * performs OAuth2 client credential request
     * <p>
     * Sets {@code this.jwtToken} to the obtained token. Sets it to null, if not obtained.
     *
     * @return true, if token was obtained, else false
     */
    private boolean obtainAccessToken() {
        // Create an OkHttpClient instance to make the token request
        OkHttpClient client = new OkHttpClient();

        // Build the request body with client credentials and grant type
        RequestBody requestBody = new FormBody.Builder()
            .add(KEY_GRANT_TYPE, this.grant_type)
            .add(KEY_CLIENT_ID, this.clientId)
            .add(KEY_CLIENT_SECRET, this.clientSecret)
            .build();

        // Build the token request
        Request tokenRequest = new Request.Builder()
            .url(this.tokenUrl)
            .post(requestBody)
            .build();

        // Execute the token request and parse the response
        try (Response tokenResponse = client.newCall(tokenRequest).execute()) {

            if (tokenResponse.isSuccessful()) {
                String responseBody = tokenResponse.body().string();

                // Extract the access token from the response
                // Assume the response body is in JSON format and has a field named "access_token"
                jwtAccessToken = objectMapper.readTree(responseBody).get("access_token").asText();
                return true;
            } else {
                jwtAccessToken = null;
                log.error("JWT could not be obtained. Please check configuration.");
                return false;
            }

        } catch (Exception e) {
            log.error("Obtaining JWT failed: {}", e.toString());
            jwtAccessToken = null;
            return false;
        }
    }
}
