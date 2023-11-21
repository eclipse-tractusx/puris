package org.eclipse.tractusx.puris.backend.common.security;

import org.eclipse.tractusx.puris.backend.common.security.domain.ApiKeyAuthentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockApiKeySecurityContextFactory implements WithSecurityContextFactory<WithMockApiKey> {

    @Override
    public SecurityContext createSecurityContext(WithMockApiKey annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        ApiKeyAuthentication auth = new ApiKeyAuthentication(annotation.apiKey(), true);
        context.setAuthentication(auth);

        return context;
    }
}
