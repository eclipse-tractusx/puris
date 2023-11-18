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
package org.eclipse.tractusx.puris.backend.common.security;

import org.eclipse.tractusx.puris.backend.common.security.logic.ApiKeyAuthenticationFilter;
import org.eclipse.tractusx.puris.backend.common.security.logic.ApiKeyAuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private ApiKeyAuthenticationService apiKeyAuthenticationService;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer(){
        // exclude springdoc / swagger ui
        return (web) -> web.ignoring().requestMatchers("/swagger-ui/**", "/v3/api-docs/**");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(
                // any request in spring context
                (authorizeHttpRequests) -> authorizeHttpRequests.anyRequest().authenticated()
            )
            .httpBasic(
                AbstractHttpConfigurer::disable
            )
            .sessionManagement(
                (sessionManagement) -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .cors(Customizer.withDefaults());

        http
           .addFilterBefore(new ApiKeyAuthenticationFilter(apiKeyAuthenticationService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }



}
