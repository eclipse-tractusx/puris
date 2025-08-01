/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Contributors to the Eclipse Foundation

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

package org.eclipse.tractusx.puris.backend.demandandcapacitynotification.logic.service;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.DemandAndCapacityNotification;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.data.jpa.repository.JpaRepository;

public abstract class DemandAndCapacityNotificationService<TEntity extends DemandAndCapacityNotification, TRepository extends JpaRepository<TEntity, UUID>> {
    protected final TRepository repository;
    protected final PartnerService partnerService;
    protected final MaterialPartnerRelationService mprService;

    protected final Function<TEntity, Boolean> validator;

    public DemandAndCapacityNotificationService(TRepository repository, PartnerService partnerService, MaterialPartnerRelationService mprService) {
        this.repository = repository;
        this.partnerService = partnerService;
        this.mprService = mprService;
        this.validator = this::validate;
    }

    public final TEntity findById(UUID uuid) {
        return repository.findById(uuid).orElse(null);
    }

    public final TEntity findByNotificationId(UUID notificationId) {
        return repository.findAll().stream().filter(notification -> notification.getNotificationId().equals(notificationId))
                .findFirst().orElse(null);
    }

    public final List<TEntity> findAll() {
        return repository.findAll();
    }

    public final List<TEntity> findAllByBpnl(String bpnl) {
        return repository.findAll().stream().filter(demand -> demand.getPartner().getBpnl().equals(bpnl))
                .toList();
    }

    public final TEntity create(TEntity notification) {
        if (!validator.apply(notification)) {
            throw new IllegalArgumentException("Invalid notification");
        }
        if (repository.findAll().stream().anyMatch(d -> d.getNotificationId().equals(notification.getNotificationId()))) {
            throw new KeyAlreadyExistsException("Notification already exists");
        }
        if (notification.getNotificationId() == null) {
            notification.setNotificationId(UUID.randomUUID());
        }
        if (notification.getSourceDisruptionId() == null) {
            notification.setSourceDisruptionId(UUID.randomUUID());
        }
        notification.setContentChangedAt(new Date());
        return repository.save(notification);
    }

    public final TEntity update(TEntity notification) {
        if (!validator.apply(notification)) {
            throw new IllegalArgumentException("Invalid notification");
        }
        if (notification.getUuid() == null || repository.findById(notification.getUuid()).isEmpty()) {
            return null;
        }
        notification.setContentChangedAt(new Date());
        return repository.save(notification);
    }

    public final void delete(UUID uuid) {
        repository.deleteById(uuid);
    }

    public abstract boolean validate(TEntity notification);
}
