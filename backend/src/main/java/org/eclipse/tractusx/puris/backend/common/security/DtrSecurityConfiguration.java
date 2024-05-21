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

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the DTR clients:
 * <ul>
 * <li>PURIS client using the DTR directly with write access</li>
 * <li>EDC client allowing read access</li>
 * </ul>
 */
@Getter
@Configuration
public class DtrSecurityConfiguration {

    /**
     * if true, then DTR is configured with IDP
     **/
    @Value("${puris.dtr.idp.enabled:false}")
    private boolean oauth2InterceptorEnabled;
    /**
     * token url of the OAuth2 identity provider
     **/
    @Value("${puris.dtr.idp.tokenurl}")
    private String tokenUrl;
    /**
     * client id of the puris client with write access for DTR
     **/
    @Value("${puris.dtr.idp.puris-client.id}")
    private String purisClientId;
    /**
     * client secret of the puris client with write access for DTR
     **/
    @Value("${puris.dtr.idp.puris-client.secret}")
    private String purisClientSecret;
    /**
     * grant_type. Currently only client_credentials is supported
     **/
    private final String grant_type = "client_credentials";

    /**
     * client id of the edc client with read access for DTR
     **/
    @Value("${puris.dtr.idp.edc-client.id}")
    private String edcClientId;
    /**
     * vault alias for the client secret of the edc client with read access for DTR
     **/
    @Value("${puris.dtr.idp.edc-client.secret.alias}")
    private String edcClientSecretAlias;
}
