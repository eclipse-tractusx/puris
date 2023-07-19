package org.eclipse.tractusx.puris.backend.common.edc.logic.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class EDR_Dto {

    private final String authKey;

    private final String authCode;

    private final String endpoint;
    
}
