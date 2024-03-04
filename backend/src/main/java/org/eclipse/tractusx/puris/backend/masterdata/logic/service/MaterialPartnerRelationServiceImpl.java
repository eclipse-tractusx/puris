/*
 * Copyright (c) 2023, 2024 Volkswagen AG
 * Copyright (c) 2023, 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
 * Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.ddtr.logic.DigitalTwinMappingService;
import org.eclipse.tractusx.puris.backend.common.ddtr.logic.DtrAdapterService;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.repository.MaterialPartnerRelationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class MaterialPartnerRelationServiceImpl implements MaterialPartnerRelationService {

    private QueuingHelper queuingHelper = new QueuingHelper();

    @Autowired
    private MaterialPartnerRelationRepository mprRepository;

    @Autowired
    private VariablesService variablesService;

    @Autowired
    private DigitalTwinMappingService dtmService;

    @Autowired
    private DtrAdapterService dtrAdapterService;

    {
        // Initializer block
        Thread thread = new Thread(queuingHelper);
        thread.setDaemon(true);
        thread.start();
    }


    /**
     * Stores the given relation to the database.
     *
     * @param materialPartnerRelation
     * @return the stored relation or null, if the given relation was already in existence.
     */
    @Override
    public MaterialPartnerRelation create(MaterialPartnerRelation materialPartnerRelation) {
        flagConsistencyTest(materialPartnerRelation);
        var searchResult = find(materialPartnerRelation.getMaterial(), materialPartnerRelation.getPartner());
        if (searchResult == null) {
            dtmService.update(materialPartnerRelation);
            queuingHelper.addToQueue(materialPartnerRelation);
            return mprRepository.save(materialPartnerRelation);
        }
        log.error("Could not create MaterialPartnerRelation, " + materialPartnerRelation.getKey() + " already exists");
        return null;
    }

    /**
     * This class is an internal service that helps to make sure that calls to the dDTR are executed in
     * the order that they were caused and will help to avoid race conditions for write actions on the dDTR.
     * One important reason why this is necessary, is because any creation or update event of MPR's can only work,
     * after the given Material was registered at the dDTR. If the registration of the Material was not complete,
     * then one or more retries may be necessary to properly insert the data from the MPR.
     *
     */
    private class QueuingHelper implements Runnable {

        private enum RegistrationType {

            /**
             * case: Partner acts as a supplier in the context of a given MPR
             */
            REGISTER_AS_SUPPLIER,

            /**
             * case: Partner acts as a customer in the context of a given MPR
             */
            REGISTER_AS_CUSTOMER;
        }
        private ConcurrentLinkedDeque<RegistrationTask> queue = new ConcurrentLinkedDeque<>();

        private record RegistrationTask (RegistrationType type, MaterialPartnerRelation mpr){}

        /**
         * The maximum number of retries that will be attempted for an MPR, that was put into the queue.
         * If the maximum number is exceeded, then a new series of retries can be triggered manually.
         */
        private int retries = 3;

        /**
         * The minimum number of milliseconds between two write-calls to the dDTR.
         */
        private long interval = 1000L;

        public void addToQueue(MaterialPartnerRelation materialPartnerRelation) {
            if (materialPartnerRelation.getMaterial().isProductFlag() && materialPartnerRelation.isPartnerBuysMaterial()) {
                queue.addLast(new RegistrationTask(RegistrationType.REGISTER_AS_CUSTOMER, materialPartnerRelation));
            }
            if (materialPartnerRelation.getMaterial().isMaterialFlag() && materialPartnerRelation.isPartnerSuppliesMaterial()) {
                queue.addLast(new RegistrationTask(RegistrationType.REGISTER_AS_SUPPLIER, materialPartnerRelation));
            }

        }

        @Override
        public void run() {
            HashMap<RegistrationTask, Integer> failCount = new HashMap<>();
            while (true) {
                try {
                    var task = queue.removeFirst();
                    boolean success = switch (task.type()) {
                        case REGISTER_AS_SUPPLIER -> true;
                        case REGISTER_AS_CUSTOMER -> dtrAdapterService.updateProductForMaterialPartnerRelationWithCustomer(task.mpr());
                    };
                    if (!success) {
                        log.error("Failed to update for " + task);
                        Integer count = failCount.get(task);
                        if (count == null) {
                            count = 1;
                        } else {
                            count++;
                        }
                        failCount.put(task, count);
                        if (count <= retries) {
                            queue.addLast(task);
                            log.info("Will retry for " + task + " Retries left: " + (retries - count + 1));
                        } else {
                            failCount.put(task, null);
                        }
                    } else {
                        log.info("Successfully updated for " + task);
                        failCount.put(task, null);
                    }
                } catch (Exception e) {
                    if (!(e instanceof NoSuchElementException)) {
                        log.error("Error in QueuingHelper ", e);
                    }
                }
                Thread.yield();
                try {
                    Thread.sleep(interval);
                } catch (Exception e) {

                }

            }
        }
    }

    /**
     * Updates an existing MaterialPartnerRelation
     *
     * @param materialPartnerRelation
     * @return the updated relation or null, if the given relation didn't exist before.
     */
    @Override
    public MaterialPartnerRelation update(MaterialPartnerRelation materialPartnerRelation) {
        flagConsistencyTest(materialPartnerRelation);
        var foundEntity = mprRepository.findById(materialPartnerRelation.getKey());
        if (foundEntity.isPresent()) {
            dtmService.update(materialPartnerRelation);
            queuingHelper.addToQueue(materialPartnerRelation);
            return mprRepository.save(materialPartnerRelation);
        }
        log.error("Could not update MaterialPartnerRelation, " + materialPartnerRelation.getKey() + " didn't exist before");
        return null;
    }

    private void flagConsistencyTest(MaterialPartnerRelation materialPartnerRelation) {
        Material material = materialPartnerRelation.getMaterial();
        boolean test = material.isMaterialFlag() && materialPartnerRelation.isPartnerSuppliesMaterial();
        test = test || (material.isProductFlag() && materialPartnerRelation.isPartnerBuysMaterial());
        if (!test) {
            log.warn("Flags of " + materialPartnerRelation + " are not consistent with flags of " + material.getOwnMaterialNumber());
        }
    }

    /**
     * Find the MaterialPartnerRelation containing the material and the partner.
     *
     * @param material
     * @param partner
     * @return the relation, if it exists or else null;
     */
    @Override
    public MaterialPartnerRelation find(Material material, Partner partner) {
        return find(material.getOwnMaterialNumber(), partner.getUuid());
    }

    /**
     * Returns a list of all materials that the given partner supplies to you.
     *
     * @param partner the partner
     * @return a list of material entities
     */
    @Override
    public List<Material> findAllMaterialsThatPartnerSupplies(Partner partner) {
        return mprRepository
            .findAllByPartner_UuidAndPartnerSuppliesMaterialIsTrue(partner.getUuid())
            .stream()
            .map(mpr -> mpr.getMaterial())
            .collect(Collectors.toList());
    }

    /**
     * Returns a list of all products that the given partner buys from you.
     *
     * @param partner the partner
     * @return a list of product entities
     */
    @Override
    public List<Material> findAllProductsThatPartnerBuys(Partner partner) {
        return mprRepository
            .findAllByPartner_UuidAndPartnerBuysMaterialIsTrue(partner.getUuid())
            .stream()
            .map(mpr -> mpr.getMaterial())
            .collect(Collectors.toList());
    }

    /**
     * @return a list of all existing MaterialPartnerRelations
     */
    @Override
    public List<MaterialPartnerRelation> findAll() {
        return mprRepository.findAll();
    }

    /**
     * Generates a Map of key-value-pairs. Each key represents the BPNL of a
     * partner (and yourself), each corresponding value is the materialNumber
     * that the owner of the BPNL is using in his own house to define the given Material.
     *
     * @param ownMaterialNumber
     * @return a Map with the content described above or an empty map if no entries with the given ownMaterialNumber could be found.
     */
    @Override
    public Map<String, String> getBPNL_To_MaterialNumberMap(String ownMaterialNumber) {
        var relationsList = mprRepository.findAllByMaterial_OwnMaterialNumber(ownMaterialNumber);
        HashMap<String, String> output = new HashMap<>();
        if (relationsList.isEmpty()) {
            return output;
        }
        output.put(variablesService.getOwnBpnl(), ownMaterialNumber);
        for (var relation : relationsList) {
            output.put(relation.getPartner().getBpnl(), relation.getPartnerMaterialNumber());
        }
        return output;
    }

    /**
     * Find the MaterialPartnerRelation containing the material with the given
     * ownMaterialNumber and the uuid referencing a partner in your database.
     *
     * @param ownMaterialNumber
     * @param partnerUuid
     * @return the relation, if it exists or else null
     */
    @Override
    public MaterialPartnerRelation find(String ownMaterialNumber, UUID partnerUuid) {
        var searchResult = mprRepository.findById(new MaterialPartnerRelation.Key(ownMaterialNumber, partnerUuid));
        if (searchResult.isPresent()) {
            return searchResult.get();
        }
        return null;
    }

    /**
     * Returns a list containing all Partners that are registered as suppliers for
     * the material with the given ownMaterialNumber
     *
     * @param ownMaterialNumber
     * @return a list of partners as described above
     */
    @Override
    public List<Partner> findAllSuppliersForOwnMaterialNumber(String ownMaterialNumber) {
        return mprRepository.findAllByMaterial_OwnMaterialNumberAndPartnerSuppliesMaterialIsTrue(ownMaterialNumber)
            .stream()
            .map(mpr -> mpr.getPartner())
            .collect(Collectors.toList());
    }

    /**
     * Returns a list containing all Partners that are registered as customers for
     * the material with the given ownMaterialNumber
     *
     * @param ownMaterialNumber
     * @return a list of partners as described above
     */
    @Override
    public List<Partner> findAllCustomersForOwnMaterialNumber(String ownMaterialNumber) {
        return mprRepository.findAllByMaterial_OwnMaterialNumberAndPartnerBuysMaterialIsTrue(ownMaterialNumber)
            .stream()
            .map(mpr -> mpr.getPartner())
            .collect(Collectors.toList());
    }

    /**
     * Returns a list containing all Partners that are registered as suppliers for
     * the material with the given material
     *
     * @param material
     * @return a list of partners as described above
     */
    @Override
    public List<Partner> findAllSuppliersForMaterial(Material material) {
        return findAllSuppliersForOwnMaterialNumber(material.getOwnMaterialNumber());
    }

    /**
     * Returns a list containing all Partners that are registered as customers for
     * the material with the given material
     *
     * @param material
     * @return a list of partners as described above
     */
    public List<Partner> findAllCustomersForMaterial(Material material) {
        return findAllCustomersForOwnMaterialNumber(material.getOwnMaterialNumber());
    }

    /**
     * Returns a list of all Materials, for which a MaterialPartnerRelation exists,
     * where the partner is using the given partnerMaterialNumber.
     *
     * @param partnerMaterialNumber
     * @return a list of Materials
     */
    @Override
    public List<Material> findAllByPartnerMaterialNumber(String partnerMaterialNumber) {
        return mprRepository.findAllByPartnerMaterialNumber(partnerMaterialNumber)
            .stream()
            .map(mpr -> mpr.getMaterial())
            .collect(Collectors.toList());
    }

    /**
     * @param material
     * @param partner
     * @return true, if the given partner is registered as supplier for the given material, else false
     */
    @Override
    public boolean partnerSuppliesMaterial(Material material, Partner partner) {
        if (material.isMaterialFlag()) {
            MaterialPartnerRelation mpr = find(material, partner);
            return mpr != null && mpr.isPartnerSuppliesMaterial();
        }
        return false;
    }

    /**
     * @param material
     * @param partner
     * @return true, if the given partner is registered as customer for the given material, else false
     */
    @Override
    public boolean partnerOrdersProduct(Material material, Partner partner) {
        if (material.isProductFlag()) {
            MaterialPartnerRelation mpr = find(material, partner);
            return mpr != null && mpr.isPartnerBuysMaterial();
        }
        return false;
    }

    @Override
    public List<MaterialPartnerRelation> findAllBySupplierPartnerMaterialNumber(String partnerMaterialNumber) {
        return mprRepository.findAllByPartnerMaterialNumberAndPartnerSuppliesMaterialIsTrue(partnerMaterialNumber);
    }

    @Override
    public List<MaterialPartnerRelation> findAllByCustomerPartnerMaterialNumber(String partnerMaterialNumber) {
        return mprRepository.findAllByPartnerMaterialNumberAndPartnerBuysMaterialIsTrue(partnerMaterialNumber);
    }

    @Override
    public List<MaterialPartnerRelation> findAllBySupplierPartnerAndPartnerMaterialNumber(Partner partner, String partnerMaterialNumber) {
        return mprRepository.findAllByPartnerAndPartnerMaterialNumberAndPartnerSuppliesMaterialIsTrue(partner, partnerMaterialNumber);
    }

    @Override
    public List<MaterialPartnerRelation> findAllByCustomerPartnerAndPartnerMaterialNumber(Partner partner, String partnerMaterialNumber) {
        return mprRepository.findAllByPartnerAndPartnerMaterialNumberAndPartnerBuysMaterialIsTrue(partner, partnerMaterialNumber);
    }

}
