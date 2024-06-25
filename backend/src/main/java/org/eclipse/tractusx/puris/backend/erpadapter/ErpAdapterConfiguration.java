package org.eclipse.tractusx.puris.backend.erpadapter;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class ErpAdapterConfiguration {

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
    @Value("${puris.baseurl}" + "${server.servlet.context-path}" + "/erp-adapter")
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

    @Value("${puris.erpadapter.refreshinterval}")
    @Getter(AccessLevel.NONE)
    private long refreshInterval;

    @Value("${puris.erpadapter.timelimit}")
    @Getter(AccessLevel.NONE)
    private long refreshTimeLimit;

    /**
     * @return The refresh time limit in milliseconds
     */
    public long getRefreshTimeLimit() {
        // translate days to milliseconds
        return refreshTimeLimit * 24 * 60 * 60 * 1000;
    }

    /**
     * @return The refresh interval in milliseconds
     */
    public long getRefreshInterval() {
        // translate minutes to milliseconds
        return refreshInterval * 60 * 1000;
    }
}
