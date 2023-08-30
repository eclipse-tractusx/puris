package org.eclipse.tractusx.puris.backend.common.edc.logic.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EndpointDataReferenceService;

/**
 * An internal, immutable Dto class used by the {@link EndpointDataReferenceService}
 * It contains an authKey, authCode and endpoint.
 */
@AllArgsConstructor
@Getter
public class EDR_Dto {

    /**
     * This defines the key, under which the
     * authCode is to be sent to the data plane.
     * For example: "Authorization"
     */
    private final String authKey;

    /**
     * This is the secret key to be sent
     * to the data plane.
     */
    private final String authCode;

    /**
     * The address of the data plane that has
     * to handle the consumer pull.
     */
    private final String endpoint;
    
}
