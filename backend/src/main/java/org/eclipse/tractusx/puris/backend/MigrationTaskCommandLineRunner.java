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
package org.eclipse.tractusx.puris.backend;

import java.util.List;

import org.eclipse.tractusx.puris.backend.common.ddtr.logic.DtrAdapterService;
import org.eclipse.tractusx.puris.backend.common.migration.MigrationTaskService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class MigrationTaskCommandLineRunner implements CommandLineRunner {
    @Autowired
    private DtrAdapterService dtrAdapterService;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private MaterialPartnerRelationService materialPartnerRelationService;
    @Autowired
    private MigrationTaskService migrationTaskService;
 
    @Override
    public void run(String... args) throws Exception {
        var latestTask = migrationTaskService.getLatestPendingMigrationTask();
        if (latestTask == null) {
            log.info("No pending migration task found, skipping migration");
            return;
        }
        log.info("Starting migration task for target version {}", latestTask.getTargetVersion());
        migrationTaskService.markTaskInProgress(latestTask);

        List<String> errors = updateDigitalTwins();

        if (errors.isEmpty()) {
            migrationTaskService.markTaskCompleted(latestTask);
            log.info("Migration task for target version {} completed successfully", latestTask.getTargetVersion());
        } else {
            log.error("Migration task for target version {} failed with errors: {}", latestTask.getTargetVersion(), errors);
            migrationTaskService.markTaskFailed(latestTask, errors);
        }
    }

    private List<String> updateDigitalTwins() {
        List<String> errors = List.of();
        List<Material> materials = materialService.findAll();
        if (materials.isEmpty()) {
            log.info("No materials found in database, skipping digital twin update at DTR");
            return errors;
        }
        log.info("Starting update of digital twins at DTR for {} materials", materials.size());
        for (Material material : materials) {
            try {
                List<MaterialPartnerRelation> mprs = materialPartnerRelationService.findAllByMaterial(material);
                if (material.isMaterialFlag()) {
                    for (MaterialPartnerRelation mpr : mprs) {
                        if (mpr.getPartnerCXNumber() == null) {
                            // if there is no partnerCXNumber, the material twin cannot be linked to the partner twin at the DTR, so we skip the update for this relation
                            log.warn("MaterialPartnerRelation for material {} and Partner {} has no partnerCXNumber, skipping DTR update for this relation", material.getOwnMaterialNumber(), mpr.getPartner().getBpnl());
                            continue;
                        }
                        int result = dtrAdapterService.updateMaterialAtDtr(mpr);
                        if (result < 400) {
                            log.info("Updated material ShellDescriptor at DTR for material number {} and supplier partner {}.", material.getOwnMaterialNumber(), mpr.getPartner().getBpnl());
                        } else {
                            String error = String.format(
                                "Update failed for material ShellDescriptor at DTR for material number {} and supplier partner {} with status code {}.",
                                material.getOwnMaterialNumber(), mpr.getPartner().getBpnl(), result
                            );
                            log.warn(error);
                            errors.add(error);
                        }
                    }
                }
                if (material.isProductFlag()) {
                    List<MaterialPartnerRelation> buyingMprs = mprs.stream().filter(mpr -> mpr.isPartnerBuysMaterial()).toList();
                    if (buyingMprs.isEmpty()) {
                        log.info("No buying MaterialPartnerRelation found for material {}, skipping product digital twin update at DTR", material.getOwnMaterialNumber());
                        continue;
                    }
                    int result = dtrAdapterService.updateProduct(material, buyingMprs);
                    if (result < 400) {
                        log.info("Updated product ShellDescriptor at DTR for material number {} and {} customer partners. Result: {}", material.getOwnMaterialNumber(), buyingMprs.size(), result);
                    } else {
                        String error = String.format(
                            "Update failed for product ShellDescriptor at DTR for material number {} and {} customer partners with status code {}.",
                            material.getOwnMaterialNumber(), buyingMprs.size(), result
                        );
                        log.warn(error);
                        errors.add(error);
                    }
                }
            } catch (Exception e) {
                String error = String.format("Error while updating digital twins at dDTR for material {}: {}", material.getOwnMaterialNumber(), e.getMessage());
                log.error(error);
                errors.add(error);
            }
        }
        return errors;
    }
}
