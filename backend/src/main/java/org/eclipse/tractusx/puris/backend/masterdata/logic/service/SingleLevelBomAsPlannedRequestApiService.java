/*
 * Copyright (c) 2026 Volkswagen AG
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.puris.backend.masterdata.logic.service;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.logic.adapter.SingleLevelBomAsPlannedSammMapper;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelbomasplanned.SingleLevelBomAsPlanned;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for handling SingleLevelBomAsPlanned submodel requests.
 */
@Service
@Slf4j
public class SingleLevelBomAsPlannedRequestApiService {

    @Autowired
    private MaterialService materialService;

    @Autowired
    private SingleLevelBomAsPlannedSammMapper sammMapper;

    @Autowired
    private VariablesService variablesService;

    /**
     * Handles a SingleLevelBomAsPlanned submodel request.
     * This endpoint is meant for self-access only via EDC.
     *
     * @param bpnl             the BPNL of the requesting partner (must match own BPNL)
     * @param materialNumberCx the CatenaX material number (UUID) of the product
     * @return the SingleLevelBomAsPlanned SAMM, or {@code null} if access is denied
     *         or the material is not found
     */
    public SingleLevelBomAsPlanned handleSingleLevelBomAsPlannedSubmodelRequest(String bpnl, String materialNumberCx) {
        if (!bpnl.equals(variablesService.getOwnBpnl())) {
            log.warn("Access denied: {} attempted to access SingleLevelBomAsPlanned (self-access only)", bpnl);
            return null;
        }

        Material material = materialService.findByMaterialNumberCx(materialNumberCx);
        if (material == null || !material.isProductFlag()) {
            log.warn("Material not found or not a product for CX number: {}", materialNumberCx);
            return null;
        }

        log.info("Self-request for SingleLevelBomAsPlanned on {}", materialNumberCx);
        return sammMapper.materialToSamm(material);
    }
}
