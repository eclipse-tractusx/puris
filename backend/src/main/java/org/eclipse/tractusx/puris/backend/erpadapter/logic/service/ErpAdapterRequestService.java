/*
 * Copyright (c) 2024 Volkswagen AG
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.puris.backend.erpadapter.logic.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.erpadapter.domain.model.ErpAdapterRequest;
import org.eclipse.tractusx.puris.backend.erpadapter.domain.repository.ErpAdapterRequestRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ErpAdapterRequestService {

    private final ErpAdapterRequestRepository repository;

    private final ErpAdapterRequestClient erpAdapterRequestClient;

    public ErpAdapterRequest create(ErpAdapterRequest erpAdapterRequest) {
        if (erpAdapterRequest.getId() != null && repository.existsById(erpAdapterRequest.getId())) {
            log.error("ErpAdapterRequest with id {} already exists", erpAdapterRequest.getId());
            return null;
        }
        return repository.save(erpAdapterRequest);
    }

    public void createAndSend(ErpAdapterRequest erpAdapterRequest) {
        erpAdapterRequest = create(erpAdapterRequest);
        if (erpAdapterRequest != null) {
            Integer responseCode = erpAdapterRequestClient.sendRequest(erpAdapterRequest);
            if (responseCode != null) {
                if (responseCode >= 200 && responseCode < 400) {
                    log.info("Successfully sent request to ERP Adapter");
                } else {
                    log.warn("Received status code {} from ERP Adapter ", responseCode);
                }
                erpAdapterRequest.setResponseCode(responseCode);
                update(erpAdapterRequest);
            } else {
                log.error("Failed to send request to ERP Adapter");
            }
        }
    }

    public ErpAdapterRequest get(UUID id) {
        // TODO: Remove when mock is removed
        return repository.findById(id).orElse(repository.findAll().getFirst());
//        return repository.findById(id).orElse(null);
    }

    public ErpAdapterRequest update(ErpAdapterRequest erpAdapterRequest) {
        if (repository.existsById(erpAdapterRequest.getId())) {
            return repository.save(erpAdapterRequest);
        }
        log.error("ErpAdapterRequest with id {} did not exist, could not update entity", erpAdapterRequest.getId());
        return null;
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
