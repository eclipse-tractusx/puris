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

package org.eclipse.tractusx.puris.backend.masterdata.logic.adapter;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.parttypeinformation.v1.PartTypeInformationLegacySAMM;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.parttypeinformation.v2.PartTypeInformationSAMM;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
public class PartTypeInformationSammMapper {

    @Autowired
    private MaterialPartnerRelationService mprService;

    /**
     * From a suppliers perspective, generate a PartTypeInformation 2.0.0 SAMM from a
     * product-material. This is the default version used internally.
     *
     * @param material  the material
     * @return          the SAMM generated from the given material
     */
    public PartTypeInformationSAMM productToSamm(Material material) {
        if (!material.isProductFlag()) {
            return null;
        }
        PartTypeInformationSAMM samm = new PartTypeInformationSAMM();
        samm.setGlobalAssetId(material.getMaterialNumberCx());
        samm.getPartTypeInformation().setManufacturerPartId(material.getOwnMaterialNumber());
        samm.getPartTypeInformation().setNameAtManufacturer(material.getName());
 
        return samm;
    }

    /**
     * From a suppliers perspective, generate a PartTypeInformation 1.0.0 SAMM from a
     * product-material. Only to be used when a partner explicitly requests the legacy version.
     *
     * @param material  the material
     * @return          the legacy SAMM generated from the given material
     */
    public PartTypeInformationLegacySAMM productToLegacySamm(Material material) {
        if (!material.isProductFlag()) {
            return null;
        }
        PartTypeInformationLegacySAMM samm = new PartTypeInformationLegacySAMM();
        samm.setCatenaXId(material.getMaterialNumberCx());
        samm.getPartTypeInformation().setManufacturerPartId(material.getOwnMaterialNumber());
        samm.getPartTypeInformation().setNameAtManufacturer(material.getName());
 
        return samm;
    }

    /**
     * This method will accept a PartTypeInformation 2.0.0 SAMM from a specific supplier partner,
     * that is meant to update the information on a specific material.
     *
     * @param material          the material
     * @param samm              the SAMM
     * @param sendingPartner    the partner, you received the SAMM from
     */
    public void updateMaterialFromSamm(@NonNull Material material, @NonNull PartTypeInformationSAMM samm,
                                       @NonNull Partner sendingPartner) {
        try {
            Objects.requireNonNull(samm.getPartTypeInformation(), "Missing PartTypeInformation");
            updateMaterialPartnerRelation(material, sendingPartner,
                samm.getGlobalAssetId(),
                samm.getPartTypeInformation().getManufacturerPartId(),
                samm.getPartTypeInformation().getNameAtManufacturer());
        } catch (Exception e) {
            log.error("Invalid PartTypeInformation 2.0.0 Samm from Partner " + sendingPartner.getBpnl() + "\n" + samm, e);
        }
    }

        /**
     * This method will accept a PartTypeInformation 1.0.0 SAMM from a specific supplier partner,
     * that is meant to update the information on a specific material.
     *
     * @param material          the material
     * @param samm              the legacy SAMM
     * @param sendingPartner    the partner, you received the SAMM from
     */
    public void updateMaterialFromLegacySamm(@NonNull Material material, @NonNull PartTypeInformationLegacySAMM samm,
                                             @NonNull Partner sendingPartner) {
        try {
            Objects.requireNonNull(samm.getPartTypeInformation(), "Missing PartTypeInformation");
            updateMaterialPartnerRelation(material, sendingPartner,
                samm.getCatenaXId(),
                samm.getPartTypeInformation().getManufacturerPartId(),
                samm.getPartTypeInformation().getNameAtManufacturer());
        } catch (Exception e) {
            log.error("Invalid PartTypeInformation 1.0.0 Samm from Partner " + sendingPartner.getBpnl() + "\n" + samm, e);
        }
    }

    /**
     * Applies the version independent content of a PartTypeInformation SAMM to the
     * MaterialPartnerRelation of the given material and partner.
     *
     * @param material              the material
     * @param sendingPartner        the partner, you received the SAMM from
     * @param cxId                  the globalAssetId (2.0.0) or catenaXId (1.0.0)
     * @param partId                the manufacturerPartId
     * @param nameAtManufacturer    the nameAtManufacturer
     */
    private void updateMaterialPartnerRelation(Material material, Partner sendingPartner, String cxId, String partId, String nameAtManufacturer) {
        var mpr = mprService.find(material, sendingPartner);
        Objects.requireNonNull(mpr, "Missing Material Partner Relation");
        Objects.requireNonNull(partId, "Missing PartId");
        Objects.requireNonNull(nameAtManufacturer, "Missing nameAtManufacturer");
        Objects.requireNonNull(cxId, "Missing CatenaXId from partner");
 
        // Receive update for Partner Material Number.
        if (!partId.equals(mpr.getPartnerMaterialNumber())) {
            // Notification if partner changed the Material Number for his product
            if (mpr.getPartnerMaterialNumber() != null) {
                // This should never happen, because partners are not expected to change the materialNumber
                // of an existing material
                log.warn("Replacing previous Partner-MaterialNumber " + mpr.getPartnerMaterialNumber() + " with " + "new PartId from SAMM: " + partId + " by Partner " + sendingPartner.getBpnl());
            }
            mpr.setPartnerMaterialNumber(partId);
        }
 
        // Receive update for Partner's Name for Material
        if (!nameAtManufacturer.equals(mpr.getNameAtManufacturer())) {
            // Notification if partner changed the Name for his product
            if (mpr.getNameAtManufacturer() != null) {
                log.warn("Replacing previous Name at Manufacturer " + mpr.getNameAtManufacturer() + " with " + "new Name from SAMM: " + nameAtManufacturer + " by Partner " + sendingPartner.getBpnl());
            }
            mpr.setNameAtManufacturer(nameAtManufacturer);
        }
 
        if (!cxId.equals(mpr.getPartnerCXNumber())) {
            if (mpr.getPartnerCXNumber() != null) {
                log.warn("Replacing previous Partner CX Number " + mpr.getPartnerCXNumber() + " with " + "new Partner CX Number from SAMM: " + cxId + " by Partner " + sendingPartner.getBpnl());
            }
            mpr.setPartnerCXNumber(cxId);
        }
        mprService.update(mpr);
    }
}
