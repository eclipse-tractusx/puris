package org.eclipse.tractusx.puris.backend.common.security;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockApiKeySecurityContextFactory.class)
public @interface WithMockApiKey {

    String apiKey() default "test";
}
