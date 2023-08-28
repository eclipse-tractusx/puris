package org.eclipse.tractusx.puris.backend.common.edc.logic.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EndpointDataReferenceService;

/**
 * An internal Dto class used by the {@link EndpointDataReferenceService}
 */
@AllArgsConstructor
@Getter
public class EDR_Dto {

    private final String authKey;

    private final String authCode;

    private final String endpoint;
    
}
