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
import org.eclipse.tractusx.puris.backend.common.edc.logic.service.EdcAdapterService;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.repository.MaterialPartnerRelationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class MaterialPartnerRelationServiceImpl implements MaterialPartnerRelationService {


    @Autowired
    private MaterialPartnerRelationRepository mprRepository;

    @Autowired
    private VariablesService variablesService;

    @Autowired
    private DigitalTwinMappingService dtmService;

    @Autowired
    private DtrAdapterService dtrAdapterService;

    @Autowired
    private EdcAdapterService edcAdapterService;

    @Autowired
    private ExecutorService executorService;


    /**
     * Contains all MaterialPartnerRelations, for which there are
     * currently ongoing PartTypeInformationRetrievalTasks in
     * existance. Helps to avoid duplicate tasks running simultaneously.
     */
    private Set<MaterialPartnerRelation> currentPartTypeFetches = ConcurrentHashMap.newKeySet();

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
            executorService.submit(new DtrRegistrationTask(materialPartnerRelation, 3));
            if (materialPartnerRelation.getMaterial().isMaterialFlag() && materialPartnerRelation.isPartnerSuppliesMaterial()
                && materialPartnerRelation.getPartnerCXNumber() == null) {
                log.info("Attempting CX-Id fetch for Material " + materialPartnerRelation.getMaterial().getOwnMaterialNumber() +
                    " from Supplier-Partner " + materialPartnerRelation.getPartner().getBpnl());
                executorService.submit(new PartTypeInformationRetrievalTask(materialPartnerRelation, 1));
            }
            return mprRepository.save(materialPartnerRelation);
        }
        log.error("Could not create MaterialPartnerRelation, " + materialPartnerRelation.getKey() + " already exists");
        return null;
    }

    @Override
    public void triggerPartTypeRetrievalTask(MaterialPartnerRelation mpr) {
        if (!currentPartTypeFetches.contains(mpr)) {
            executorService.submit(new PartTypeInformationRetrievalTask(mpr, 1));
        }
    }


    private class PartTypeInformationRetrievalTask implements Callable<Boolean> {
        /**
         * The MaterialPartnerRelation indicating the supplier partner we want to retrieve data from
         * and the material entity we want to fetch the partner's CatenaX-Id for.
         */
        final MaterialPartnerRelation materialPartnerRelation;
        /**
         * The number of retries that this task has currently left.
         */
        int retries;
        /**
         * The number of retries that this task was given at creation.
         */
        final int initialRetries;

        /**
         * Is set to true if all goals of this task were accomplished or otherwise
         * if the task failed and has no more retries left.
         */
        boolean done = false;

        /**
         * Constructor for a Task that tries to asynchronously fetch the CatenaX-Id from a
         * supplier partner for a material entity, as specified by the materialPartnerRelation parameter.
         *
         * @param materialPartnerRelation the MaterialPartnerRelation
         * @param retries                 a non-negative number of possible retry-attempts.
         */
        PartTypeInformationRetrievalTask(MaterialPartnerRelation materialPartnerRelation, int retries) {
            this.materialPartnerRelation = materialPartnerRelation;
            this.retries = retries;
            this.initialRetries = retries;
            currentPartTypeFetches.add(materialPartnerRelation);
        }

        /**
         * This method contains all the duties which the PartTypeInformationRetrievalTask is trying to fulfill.
         *
         * @return  true, if the task finished successfully
         */
        @Override
        public Boolean call() {
            try {
                if (retries < 0) {
                    log.warn("PartTypeInformation fetch from " + materialPartnerRelation.getPartner().getBpnl() +
                        " for " + materialPartnerRelation.getMaterial().getOwnMaterialNumber() + " failed");
                    currentPartTypeFetches.remove(materialPartnerRelation);
                    done = true;
                    return false;
                }
                if (retries < initialRetries) {
                    Thread.sleep(300);
                }
                String partnerCXId = edcAdapterService.getCxIdFromPartTypeInformation(materialPartnerRelation);
                if (partnerCXId != null && PatternStore.URN_OR_UUID_PATTERN.matcher(partnerCXId).matches()) {
                    materialPartnerRelation.setPartnerCXNumber(partnerCXId);
                    mprRepository.save(materialPartnerRelation);
                    log.info("Successfully inserted Partner CX Id for Partner " +
                        materialPartnerRelation.getPartner().getBpnl() + " and Material "
                        + materialPartnerRelation.getMaterial().getOwnMaterialNumber() +
                        " -> " + partnerCXId);
                } else {
                    log.warn("PartTypeInformation fetch from " + materialPartnerRelation.getPartner().getBpnl() +
                        " for " + materialPartnerRelation.getMaterial().getOwnMaterialNumber() + " failed. Retries left: " + retries);
                    retries--;
                    return call();
                }

                done = true;
                return true;
            } catch (Exception e) {
                log.warn("PartTypeInformation fetch from " + materialPartnerRelation.getPartner().getBpnl() +
                    " for " + materialPartnerRelation.getMaterial().getOwnMaterialNumber() + " failed. Retries left: " + retries);
                retries--;
                return call();
            } finally {
                if (done) {
                    currentPartTypeFetches.remove(materialPartnerRelation);
                }
            }
        }
    }


    private class DtrRegistrationTask implements Callable<Boolean> {

        /**
         * The MaterialPartnerRelation that was created or updated and therefore makes it necessary to
         * create or update material- and/or product AAS's in the dDTR.
         */
        MaterialPartnerRelation materialPartnerRelation;
        /**
         * The number of retries that this task has currently left.
         */
        int retries;
        /**
         * The number of retries that this task was given at creation (minimum value is 1).
         */
        final int initialRetries;
        /**
         * If the materialPartnerRelation indicates that the partner is a customer, this
         * is set to true
         */
        final boolean needProductRegistration;
        /**
         * Is set to true during runtime if a product registration call to the dDTR ran successfully
         */
        boolean completedProductRegistration = false;
        /**
         * If the materialPartnerRelation indicates that the partner is a supplier, this
         * is set to true
         */
        final boolean needMaterialRegistration;
        /**
         * Is set to true during runtime if a material registration call to the dDTR ran successfully
         */
        boolean completedMaterialRegistration = false;

        /**
         * Constructor for a task that makes sure that all potentially needed material and/or product AAS's
         * are inserted into the dDTR, when a MaterialPartnerRelation entity is created or updated.
         *
         * @param materialPartnerRelation
         * @param retries
         */
        public DtrRegistrationTask(MaterialPartnerRelation materialPartnerRelation, int retries) {
            this.materialPartnerRelation = materialPartnerRelation;
            this.initialRetries = Math.max(retries, 1);
            this.retries = initialRetries;
            this.needProductRegistration = materialPartnerRelation.isPartnerBuysMaterial();
            this.needMaterialRegistration = materialPartnerRelation.isPartnerSuppliesMaterial();
        }

        /**
         * This method contains all the duties which the DtrRegistrationTask is trying to fulfill.
         *
         * @return  true, if the task finished successfully
         */
        @Override
        public Boolean call() throws Exception {
            if (retries < 0) {
                return false;
            }
            if (retries < initialRetries) {
                Thread.sleep(2000);
            } else {
                Thread.sleep(400);
            }

            if (needProductRegistration && !completedProductRegistration) {
                var allCustomers =
                    mprRepository.findAllByMaterial_OwnMaterialNumberAndPartnerBuysMaterialIsTrue(
                        materialPartnerRelation.getMaterial().getOwnMaterialNumber());
                if (allCustomers.contains(materialPartnerRelation)) {
                    Integer result = dtrAdapterService.updateProduct(materialPartnerRelation.getMaterial(), allCustomers);
                    if (result != null) {
                        if (result < 400) {
                            log.info("Updated product ShellDescriptor at DTR for " + materialPartnerRelation.getMaterial().getOwnMaterialNumber() + " and "
                                + allCustomers.size() + " customer partners. Result: " + result);
                            completedProductRegistration = true;
                        } else {
                            if (result == 404) {
                                Integer registrationResult = dtrAdapterService.registerProductAtDtr(materialPartnerRelation.getMaterial());
                                log.info("Tried to create product AAS for " + materialPartnerRelation.getMaterial().getOwnMaterialNumber()
                                    + ", result: " + registrationResult);
                            }
                        }
                    } else {
                        log.warn("Update of product ShellDescriptor failed at DTR for " + materialPartnerRelation.getMaterial().getOwnMaterialNumber());
                    }
                } else {
                    log.warn("AllCustomers did not contain " + materialPartnerRelation.getKey());
                    log.warn("Update of product ShellDescriptor failed at DTR for " + materialPartnerRelation.getMaterial().getOwnMaterialNumber());
                }
            }

            if (needMaterialRegistration && !completedMaterialRegistration) {
                if (materialPartnerRelation.getPartnerCXNumber() == null) {
                    if (currentPartTypeFetches.contains(materialPartnerRelation)) {
                        log.info("Awaiting PartTypeInformation Fetch");
                        // await return of ongoing fetch task
                        while (currentPartTypeFetches.contains(materialPartnerRelation)) {
                            Thread.yield();
                        }
                    } else {
                        // initiate new fetch
                        log.info("Initiating new PartTypeInformation Fetch");
                        Future<Boolean> futureResult = executorService.submit(new PartTypeInformationRetrievalTask(materialPartnerRelation, 1));
                        while (!futureResult.isDone()) {
                            Thread.yield();
                        }
                    }
                    Thread.sleep(500);
                    // get result from database
                    materialPartnerRelation = find(materialPartnerRelation.getMaterial(), materialPartnerRelation.getPartner());
                    if (materialPartnerRelation.getPartnerCXNumber() == null) {
                        log.error("Missing partnerCX Number in " + materialPartnerRelation + ", retries left: " + retries);
                        retries--;
                        return call();
                    }
                }

                Integer result = dtrAdapterService.updateMaterialAtDtr(materialPartnerRelation);
                if (result != null) {
                    if (result < 400) {
                        completedMaterialRegistration = true;
                        log.info("Updated material ShellDescriptor at DTR for " + materialPartnerRelation.getMaterial().getOwnMaterialNumber() +
                            " and supplier partner " + materialPartnerRelation.getPartner().getBpnl());
                    } else {
                        if (result == 404) {
                            Integer registrationResult = dtrAdapterService.registerMaterialAtDtr(materialPartnerRelation);
                            log.info("Tried to create material AAS for " + materialPartnerRelation.getMaterial().getOwnMaterialNumber() +
                                " and partner " + materialPartnerRelation.getPartner().getBpnl() +
                                ", result: " + registrationResult);
                        }
                    }
                }
            }
            if ((needMaterialRegistration && !completedMaterialRegistration) || (needProductRegistration && !completedProductRegistration)) {
                retries--;
                String message = "DTR Registration for " + materialPartnerRelation.getMaterial().getOwnMaterialNumber() + " and " + materialPartnerRelation.getPartner().getBpnl() + " failed";
                if (needMaterialRegistration && !completedMaterialRegistration) {
                    message += ", Material Registration still needed";
                }
                if (needProductRegistration && !completedProductRegistration) {
                    message += ", Product Registration still needed";
                }
                log.warn(message);
                return call();
            }
            return true;
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
            if (materialPartnerRelation.getMaterial().isMaterialFlag() && materialPartnerRelation.isPartnerSuppliesMaterial()
                && materialPartnerRelation.getPartnerCXNumber() == null) {
                log.info("Attempting CX-Id fetch for Material " + materialPartnerRelation.getMaterial().getOwnMaterialNumber() +
                    " from Supplier-Partner " + materialPartnerRelation.getPartner().getBpnl());
                executorService.submit(new PartTypeInformationRetrievalTask(materialPartnerRelation, 3));
            }
            executorService.submit(new DtrRegistrationTask(materialPartnerRelation, 3));
            return mprRepository.save(materialPartnerRelation);
        }
        log.error("Could not update MaterialPartnerRelation, " + materialPartnerRelation.getKey() + " didn't exist before");
        return null;
    }

    private void flagConsistencyTest(MaterialPartnerRelation materialPartnerRelation) {
        Material material = materialPartnerRelation.getMaterial();
        boolean inconsistentFlag = !material.isMaterialFlag() && materialPartnerRelation.isPartnerSuppliesMaterial();
        if (inconsistentFlag) {
            log.warn(material.getOwnMaterialNumber() + " has no Material Flag, but " + materialPartnerRelation.getPartner()
                .getBpnl() + " is marked as a Supplier!");
        }
        inconsistentFlag = (!material.isProductFlag() && materialPartnerRelation.isPartnerBuysMaterial());
        if (inconsistentFlag) {
            log.warn(material.getOwnMaterialNumber() + " has no Product Flag, but " + materialPartnerRelation.getPartner()
                .getBpnl() + " is marked as a Customer!");
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

    @Override
    public MaterialPartnerRelation findByPartnerAndPartnerCXNumber(Partner partner, String partnerCXNumber) {
        var materialPartnerRelations = mprRepository.findAllByPartnerAndAndPartnerCXNumber(partner, partnerCXNumber);

        if (!materialPartnerRelations.isEmpty()) {
            if (materialPartnerRelations.size() > 1) {
                log.warn("Ambigious result for partner " + partner.getBpnl() + " and partnerCxNumber " + partnerCXNumber);
            }
            return materialPartnerRelations.get(0);
        } else {
            return null;
        }
    }
}
