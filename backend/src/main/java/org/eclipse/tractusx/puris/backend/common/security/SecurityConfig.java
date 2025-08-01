/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2025 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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


import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import jakarta.servlet.DispatcherType;
import org.eclipse.tractusx.puris.backend.common.security.logic.ApiKeyAuthenticationFilter;
import org.eclipse.tractusx.puris.backend.common.security.logic.KeycloakJwtAuthenticationConverter;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@SecurityScheme(type = SecuritySchemeType.APIKEY, name = SecurityConfig.API_KEY_HEADER_NAME, in = SecuritySchemeIn.HEADER)
@OpenAPIDefinition(info = @Info(title = "PURIS FOSS Open API", version = "1.0.0"), security = {@SecurityRequirement(name = "X-API-KEY")})
public class SecurityConfig {

    @Value("${puris.idp.client.id}")
    private String authorizedParty;

    public static final String API_KEY_HEADER_NAME = "X-API-KEY";

    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    private final ObjectMapper objectMapper;

    private DtrSecurityConfiguration dtrSecurityConfiguration;

    private VariablesService variablesService;

    @Autowired
    public SecurityConfig(ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
                          ObjectMapper objectMapper,
                          VariablesService variablesService,
                          DtrSecurityConfiguration dtrSecurityConfiguration) {
        this.apiKeyAuthenticationFilter = apiKeyAuthenticationFilter;
        this.objectMapper = objectMapper;
        this.variablesService = variablesService;
        this.dtrSecurityConfiguration = dtrSecurityConfiguration;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(variablesService.getAllowedOrigins()));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * Configuration of API Key Authentication for all routes except docker
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(
                // any request in spring context
                (authorizeHttpRequests) -> authorizeHttpRequests
                    .requestMatchers(
                        "/stockView/**",
                        "/partners/**",
                        "/materials/**",
                        "/materialpartnerrelations/**",
                        "/item-stock/**",
                        "/production/**",
                        "/delivery/**",
                        "/demand/**",
                        "/demand-and-capacity-notification/**",
                        "/planned-production/**",
                        "/material-demand/**",
                        "/delivery-information/**",
                        "/days-of-supply/**",
                        "/edc/**",
                        "/erp-adapter/**",
                        "/parttypeinformation/**",
                        "/files/**"
                    )
                    .authenticated()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/health/**").permitAll()
                    .requestMatchers("/ws/**").permitAll()
                    .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
            )
            .httpBasic(
                AbstractHttpConfigurer::disable
            )
            .sessionManagement(
                (sessionManagement) -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(new KeycloakJwtAuthenticationConverter(authorizedParty))))
            .cors(Customizer.withDefaults());


        http.addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "puris.dtr.idp.enabled", havingValue = "true")
    public OAuth2ClientInterceptor oAuth2ClientInterceptor() {
        return new OAuth2ClientInterceptor(objectMapper, dtrSecurityConfiguration.getTokenUrl(), dtrSecurityConfiguration.getPurisClientId(), dtrSecurityConfiguration.getPurisClientSecret(), dtrSecurityConfiguration.getGrant_type());
    }

}
