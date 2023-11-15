package org.eclipse.tractusx.puris.backend.common.security.logic;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.security.domain.ApiKeyAuthentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ApiKeyAuthenticationService {

    private static final String AUTH_TOKEN_HEADER_NAME = "X-API-KEY";

    @Value("${puris.api.key}")
    private String AUTH_TOKEN;

    public Authentication getAuthentication(HttpServletRequest request) {
        String apiKey = request.getHeader(AUTH_TOKEN_HEADER_NAME);
        if (apiKey == null || !apiKey.equals(AUTH_TOKEN)) {
            log.info("API key {} is not equal to auth token {}", apiKey, AUTH_TOKEN);
            throw new BadCredentialsException("Invalid API Key");
        }

        return new ApiKeyAuthentication(apiKey, AuthorityUtils.NO_AUTHORITIES);
    }
}
