package org.eclipse.tractusx.puris.backend.common.security;

import org.eclipse.tractusx.puris.backend.AuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf((csrf) -> csrf.disable())
            .addFilterBefore(new AuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .authorizeRequests().anyRequest().authenticated();

        return http.build();
    }

}
