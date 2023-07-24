package org.eclipse.tractusx.puris.backend.common.api.logic.service;

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
    
}
