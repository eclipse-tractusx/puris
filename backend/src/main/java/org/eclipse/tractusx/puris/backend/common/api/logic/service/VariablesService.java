package org.eclipse.tractusx.puris.backend.common.api.logic.service;

import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.datatype.DT_ApiMethodEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.Getter;


@Getter
@Service
public class VariablesService {

    @Value("${puris.apiversion}")
    private String purisApiVersion;

    @Value("${puris.demonstrator.role}")
    private String purisDemonstratorRole;

    @Value("${request.apiassetid}")
    private String requestApiAssetId;

    @Value("${response.apiassetid}")
    private String responseApiAssetId;

    @Value("${own.bpnl}")
    private String ownBpnl;

    @Value("${edc.idsUrl}")
    private String ownEdcIdsUrl;

    /**
     * Returns the asset-id as defined in the properties file for the given api method
     * under request.apiassetid or response.apiassetid respectively.
     * @param method
     * @return the asset-id
     */
    public String getApiAssetId(DT_ApiMethodEnum method) {
        if(responseApiAssetId == null || requestApiAssetId == null) {
            throw new RuntimeException("You must define request.apiassetid and response.apiassetid in properties file");
        }
        switch (method) {
            case REQUEST: return requestApiAssetId;
            case RESPONSE: return responseApiAssetId;
            default: throw new RuntimeException("Unknown Api Method: " + method);
        }
    }
    
}
