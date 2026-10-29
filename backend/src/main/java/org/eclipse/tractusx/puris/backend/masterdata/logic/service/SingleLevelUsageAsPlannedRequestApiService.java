/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
 * AI Disclosure: This file was largely AI-generated. The AI-generated 
 * portions are made available under CC0-1.0 and not subject to the
 * project's licence. The human contributor has reviewed and verified
 * that the code is correct.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Assisted-by: Anthropic Claude (claude-opus-5-5)
 */
package org.eclipse.tractusx.puris.backend.masterdata.logic.service;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.logic.adapter.SingleLevelUsageAsPlannedSammMapper;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.singlelevelusageasplanned.SingleLevelUsageAsPlanned;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for handling SingleLevelUsageAsPlanned submodel requests.
 */
@Service
@Slf4j
public class SingleLevelUsageAsPlannedRequestApiService {
    @Autowired
    private MaterialService materialService;

    @Autowired
    private SingleLevelUsageAsPlannedSammMapper sammMapper;

    /**
     * Handles a SingleLevelUsageAsPlanned submodel request.
     * This endpoint is meant for self-access only via EDC.
     *
     * @param materialNumberCx the CatenaX material number (UUID) of the product
     * @return the SingleLevelUsageAsPlanned SAMM, or {@code null} if access is denied
     *         or the material is not found
     */
    public SingleLevelUsageAsPlanned handleSingleLevelUsageAsPlannedSubmodelRequest(String materialNumberCx) {

        Material material = materialService.findByMaterialNumberCx(materialNumberCx);
        if (material == null) {
            log.warn("Material not found for CX number: {}", materialNumberCx);
            return null;
        }

        log.info("Self-request for SingleLevelUsageAsPlanned on {}", materialNumberCx);
        return sammMapper.productToSingleLevelUsageAsPlannedSamm(material);
    }
}
