/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.puris.backend.common.edc.logic.service;

import java.util.HashMap;

import org.eclipse.tractusx.puris.backend.common.edc.logic.dto.EDR_Dto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
/**
 * This class stores authCodes which are generated in the course of 
 * the contracting for the request or response api. Since authCodes
 * expire after a very short period, all stored items will be deleted 
 * after a number of minutes specified in the parameter own.authcodes.deletiontimer. 
 */
@Service
@Slf4j
public class EndpointDataReferenceService {

    /** AuthCodes expire after a very short period and the data is quite voluminous, 
     *  therefore it's not really useful to persist them in the database. 
     *  The key is the transferId, the value is the authCode
     */ 
    final private HashMap<String, EDR_Dto> nonpersistantRepository = new HashMap<>();


    @Value("${own.edr.deletiontimer}")
    private long minutesUntilDeletion;

    /**
     * Stores transferId and authCode as a key/value-pair. 
     * Please note that any data will only be stored for a period of 5
     * minutes. 
     * @param transferId
     * @param authCode
     */
    public void save(String transferId, EDR_Dto edr_Dto) {
        nonpersistantRepository.put(transferId, edr_Dto);
        final long timer = minutesUntilDeletion * 60 * 1000;
        // Start timer for deletion
        new Thread(()-> {
            try {
                Thread.sleep(timer);
            } catch (InterruptedException e) {
                log.error("EndpointDataReferenceService Deletion Thread: Sleep interrupted", e);
            }
            nonpersistantRepository.remove(transferId);
            log.info("Deleted authcode for transferId " + transferId);
        }).start();
    }

    /**
     * 
     * @param transferId The key under which the Dto is supposed to be stored
     * @return the Dto or null, if there is no authCode recorded under the given parameter
     */
    public EDR_Dto findByTransferId(String transferId) {
        return nonpersistantRepository.get(transferId);
    }
    
}
