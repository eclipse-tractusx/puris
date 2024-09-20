/*
 * Copyright (c) 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
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
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.AssetType;
import org.eclipse.tractusx.puris.backend.erpadapter.ErpAdapterConfiguration;
import org.eclipse.tractusx.puris.backend.erpadapter.domain.model.ErpAdapterRequest;
import org.eclipse.tractusx.puris.backend.erpadapter.domain.model.ErpAdapterTriggerDataset;
import org.eclipse.tractusx.puris.backend.erpadapter.domain.repository.ErpAdapterTriggerDatasetRepository;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErpAdapterTriggerService {

    @Autowired
    private ErpAdapterTriggerDatasetRepository repository;
    @Autowired
    private ErpAdapterConfiguration erpAdapterConfiguration;
    @Autowired
    private ErpAdapterRequestService erpAdapterRequestService;
    @Autowired
    private ExecutorService executorService;

    private final long daemonActivityInterval = 1 * 60 * 1000; // daemon wakes up every minute

    private Future<?> daemonObject;

    private final List<ErpAdapterTriggerDataset> datasets = new ArrayList<>();

    /**
     * protect accesses to the datasets list
     */
    private final Lock lock = new ReentrantLock();

    private final Runnable daemon = () -> {
        log.info("Daemon thread started");
        ArrayList<ErpAdapterTriggerDataset> updatedData = new ArrayList<>();
        while (true) {
            lock.lock();
            try {
                // keep the most recent information in case of conflict
                for (ErpAdapterTriggerDataset dataset : datasets) {
                    for (ErpAdapterTriggerDataset updatedDataset : updatedData) {
                        if (dataset.equals(updatedDataset)) {
                            dataset.setLastPartnerRequest(
                                Math.max(dataset.getLastPartnerRequest(), updatedDataset.getLastPartnerRequest()));
                            dataset.setNextErpRequestScheduled(
                                Math.max(dataset.getNextErpRequestScheduled(), updatedDataset.getNextErpRequestScheduled()));
                        }
                    }
                }
                updatedData.clear();
                repository.saveAll(datasets);
                datasets.clear();
            } finally {
                lock.unlock();
            }

            long timeLimit = erpAdapterConfiguration.getRefreshTimeLimit();
            var allDatasets = repository.findAll();
            long now = new Date().getTime();
            log.info("Daemon waking up, found {} datasets", allDatasets.size());
            for (var dataset : allDatasets) {
                if (dataset.getLastPartnerRequest() + timeLimit <= now) {
                    // too much time has passed since last request of this kind, so
                    // we will stop triggering further updates from the erp adapter
                    repository.delete(dataset);
                    log.info("Stopped scheduling further requests for : {}", dataset);
                } else {
                    if (dataset.getNextErpRequestScheduled() <= now) {
                        // the time has come for a new erp adapter request
                        ErpAdapterRequest request = new ErpAdapterRequest();
                        request.setOwnMaterialNumber(dataset.getOwnMaterialNumber());
                        request.setPartnerBpnl(dataset.getPartnerBpnl());
                        request.setRequestDate(new Date(now));
                        DirectionCharacteristic directionCharacteristic = dataset.getDirectionCharacteristic().isEmpty() ?
                            null : DirectionCharacteristic.valueOf(dataset.getDirectionCharacteristic());
                        request.setDirectionCharacteristic(directionCharacteristic);
                        request.setRequestType(dataset.getAssetType());
                        request.setSammVersion(dataset.getAssetType().ERP_SAMMVERSION);
                        executorService.submit(() -> erpAdapterRequestService.createAndSend(request));

                        // schedule next request
                        dataset.setNextErpRequestScheduled(now + erpAdapterConfiguration.getRefreshInterval());
                        dataset = repository.save(dataset);
                        updatedData.add(dataset);
                        log.info("Scheduled next erp adapter request: {}", dataset);
                    }
                }
            }
            try {
                // sleep for the defined interval
                Thread.sleep(daemonActivityInterval);
            } catch (InterruptedException ignore) {
            }
        }
    };

    /**
     * Send a notification about a just received request from a partner via this
     * method in order to schedule regular updates from the erp adapter.
     *
     * @param partnerBpnl           the BPNL of the requesting partner
     * @param ownMaterialNumber     the material number of the requested material
     * @param type                  the Asset/Submodel type of the request
     * @param direction             the direction characteristic (if applicable for the given asset type, may be null)
     */
    public void notifyPartnerRequest(String partnerBpnl, String ownMaterialNumber, AssetType type, DirectionCharacteristic direction) {
        if (!erpAdapterConfiguration.isErpAdapterEnabled()) {
            return;
        }
        String directionString = direction != null ? direction.name() : "";

        ErpAdapterTriggerDataset dataset = repository.findById(new ErpAdapterTriggerDataset.Key(partnerBpnl,
                ownMaterialNumber, type, directionString))
            .orElse(null);

        long now = new Date().getTime();
        if (dataset == null) {
            // unknown request specifics, so we trigger a new request right now
            ErpAdapterRequest erpAdapterRequest = new ErpAdapterRequest();
            erpAdapterRequest.setRequestDate(new Date(now));
            erpAdapterRequest.setPartnerBpnl(partnerBpnl);
            erpAdapterRequest.setOwnMaterialNumber(ownMaterialNumber);
            erpAdapterRequest.setDirectionCharacteristic(direction);
            erpAdapterRequest.setRequestType(type);
            erpAdapterRequest.setSammVersion(type.ERP_SAMMVERSION);
            executorService.submit(() -> erpAdapterRequestService.createAndSend(erpAdapterRequest));

            // create dataset for the daemon thread to schedule future erp adapter requests
            dataset = new ErpAdapterTriggerDataset(partnerBpnl, ownMaterialNumber, type, directionString, now,
                now + erpAdapterConfiguration.getRefreshInterval());
            // it seems safer when only the Daemon thread writes to the repository
            lock.lock();
            try {
                datasets.add(dataset);
            } finally {
                lock.unlock();
            }
            log.info("Created {}", dataset);
        } else {
            // we had previous requests of that kind, so we just store the timestamp of this latest request
            dataset.setLastPartnerRequest(now);
            lock.lock();
            try {
                datasets.add(dataset);
            } finally {
                lock.unlock();
            }
        }
        if (daemonObject == null) {
            daemonObject = executorService.submit(daemon);
        }
    }

}
