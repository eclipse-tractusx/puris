/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
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
package org.eclipse.tractusx.puris.backend.common.api.logic.service;

import org.eclipse.tractusx.puris.backend.common.api.domain.model.ProductStockRequest;
import org.eclipse.tractusx.puris.backend.common.api.domain.model.datatype.DT_RequestStateEnum;
import org.eclipse.tractusx.puris.backend.common.api.domain.repository.ProductStockRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class RequestServiceImpl implements RequestService {
    @Autowired
    private ProductStockRequestRepository productStockRequestRepository;

    @Override
    public ProductStockRequest createRequest(ProductStockRequest productStockRequest) {
        return productStockRequestRepository.save(productStockRequest);
    }

    @Override
    public ProductStockRequest updateRequest(ProductStockRequest productStockRequest) {
        Optional<ProductStockRequest> existingRequest = productStockRequestRepository.findById(productStockRequest.getUuid());

        if (existingRequest.isPresent()) {
            return productStockRequestRepository.save(existingRequest.get());
        } else return null;
    }

    @Override
    public ProductStockRequest updateState(ProductStockRequest productStockRequest, DT_RequestStateEnum state) {
        Optional<ProductStockRequest> existingRequest = productStockRequestRepository.findById(productStockRequest.getUuid());

        if (existingRequest.isPresent()) {
            existingRequest.get().setState(state);
            return productStockRequestRepository.save(existingRequest.get());
        } else return null;
    }

    @Override
    public ProductStockRequest findByInternalUuid(UUID requestInternalUuid) {
        Optional<ProductStockRequest> existingRequest = productStockRequestRepository.findById(requestInternalUuid);

        if (existingRequest.isPresent()) {
            return existingRequest.get();
        } else return null;
    }

    @Override
    public ProductStockRequest findRequestByHeaderUuid(UUID headerUuid) {
        Optional<ProductStockRequest> existingRequest = productStockRequestRepository.findFirstByHeader_RequestId(headerUuid);

        if (existingRequest.isPresent()) {
            return existingRequest.get();
        } else return null;
    }
}
