/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.puris.backend.common.security.annotation;

import org.eclipse.tractusx.puris.backend.common.security.domain.ApiKeyAuthentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockApiKeySecurityContextFactory implements WithSecurityContextFactory<WithMockApiKey> {

    @Value("${puris.api.key}")
    String apiKeyConfig;

    @Override
    public SecurityContext createSecurityContext(WithMockApiKey annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        if (this.apiKeyConfig.equals((annotation.apiKey()))){
            ApiKeyAuthentication auth = new ApiKeyAuthentication(annotation.apiKey(), true);
            context.setAuthentication(auth);
        } else {
            ApiKeyAuthentication auth = new ApiKeyAuthentication(annotation.apiKey(), false);
            context.setAuthentication(auth);
        }

        return context;
    }
}
