/*
Copyright (c) 2026 Volkswagen AG

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.

SPDX-License-Identifier: Apache-2.0
*/
package org.eclipse.tractusx.puris.backend.dataexchangeapproval.logic.service;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.model.DataExchangeApproval;
import org.eclipse.tractusx.puris.backend.dataexchangeapproval.domain.repository.DataExchangeApprovalRepository;

public abstract class DataExchangeApprovalService<TEntity extends DataExchangeApproval, TRepository extends DataExchangeApprovalRepository<TEntity>> {
    protected final TRepository repository;
    protected final Function<TEntity, Boolean> validator;
    public DataExchangeApprovalService(TRepository repository) {
        this.repository = repository;
        this.validator = this::validate;
    }

    public final List<TEntity> findAll() {
        return repository.findAll();
    }

    public final TEntity findById(UUID uuid) {
        return repository.findById(uuid).orElse(null);
    }

    public final TEntity findByDataExchangeRequest_Uuid(UUID requestId) {
        return repository.findByDataExchangeRequest_Uuid(requestId).orElse(null);
    }

    public final TEntity findByApprovalId(String approvalId) {
        return repository.findByApprovalId(approvalId).orElse(null);
    }

    protected boolean basicValidation(DataExchangeApproval dataExchangeApproval) {
        return (dataExchangeApproval.getUuid() == null || dataExchangeApproval.getTimestamp() != null) &&
            dataExchangeApproval.getApprovedTypes() != null &&
            !dataExchangeApproval.getApprovedTypes().isEmpty();
    }

    public abstract boolean validate(TEntity approval);
    
}
