package org.eclipse.tractusx.puris.backend.common.security;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class ErpAdapterSecurityConfiguration {


    /**
     * Toggles usage of the ERP adapter
     */
    @Value("${puris.erpadapter.enabled}")
    private boolean erpAdapterEnabled;

    /**
     * The URL of the ERP adapter
     */
    @Value("${puris.erpadapter.url}")
    private String erpAdapterUrl;

    /**
     * The URL under which we expect responses from
     * the ERP adapter
     */
    @Value("${puris.baseurl}" + "catena/erp-adapter")
    private String erpResponseUrl;

    /**
     * The auth-key used when accessing the ERP adapter's
     * request interface
     */
    @Value("${puris.erpadapter.authkey}")
    private String erpAdapterAuthKey;

    /**
     * The auth-secret used when accessing the ERP adapter's
     * request interface
     */
    @Value("${puris.erpadapter.authsecret}")
    private String erpAdapterAuthSecret;
}
