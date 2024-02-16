package org.eclipse.tractusx.puris.backend.masterdata.logic.adapter;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.parttypeinformation.PartTypeInformationSAMM;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
public class PartTypeInformationSammMapper {

    @Autowired
    private MaterialService materialService;

    @Autowired
    private MaterialPartnerRelationService mprService;

    /**
     * From a suppliers perspective, generate a PartTypeInformation SAMM from a product-material.
     *
     * @param material  the material
     * @return          the SAMM generated from the given material
     */
    public PartTypeInformationSAMM productToSamm(Material material) {
        if (!material.isProductFlag()) {
            return null;
        }
        PartTypeInformationSAMM samm = new PartTypeInformationSAMM();
        if (material.isMatNbrCxAssumedToBeUnique()) {
            samm.setCatenaXId(material.getMaterialNumberCx());
        } else {
            // TODO: Decide if a non-unique CX-Materialnumber should be passed on to customer partners
            // If yes: which one should be passed on?
        }
        samm.getPartTypeInformation().setManufacturerPartId(material.getOwnMaterialNumber());
        samm.getPartTypeInformation().setNameAtManufacturer(material.getName());

        return samm;
    }

    /**
     * This method will accept a PartTypeInformation SAMM from a specific supplier partner,
     * that is meant to update the information on a specific material.
     *
     * @param material          the material
     * @param samm              the SAMM
     * @param sendingPartner    the partner, you received the SAMM from
     */
    public void updateMaterialFromSamm(@NonNull Material material, @NonNull PartTypeInformationSAMM samm, @NonNull Partner sendingPartner) {
        try {
            var mpr = mprService.find(material, sendingPartner);
            String partId = samm.getPartTypeInformation().getManufacturerPartId();
            String nameAtManufacturer = samm.getPartTypeInformation().getNameAtManufacturer();
            String cxId = samm.getCatenaXId();
            // SAMM validity check
            Objects.requireNonNull(mpr, "Missing Material Partner Relation");
            Objects.requireNonNull(samm.getPartTypeInformation(), "Missing PartTypeInformation");
            Objects.requireNonNull(partId, "Missing PartId");
            Objects.requireNonNull(nameAtManufacturer, "Missing nameAtManufacturer");

            // Receive update for Partner Material Number.
            if (!partId.equals(mpr.getPartnerMaterialNumber())) {
                // Notification if partner changed the Material Number for his product
                if (mpr.getPartnerMaterialNumber() != null) {
                    log.warn("Replacing previous Partner-MaterialNumber " + mpr.getPartnerMaterialNumber() + " with " +
                        "new PartId from SAMM: " + partId + " by Partner " + sendingPartner.getBpnl());
                }
                mpr.setPartnerMaterialNumber(partId);
            }

            // Receive update for Partner's Name for Material
            if (!nameAtManufacturer.equals(mpr.getNameAtManufacturer())) {
                // Notification if partner changed the Name for his product
                if (mpr.getNameAtManufacturer() != null) {
                    log.warn("Replacing previous Name at Manufacturer " + mpr.getNameAtManufacturer() + " with " +
                        "new Name from SAMM: " + nameAtManufacturer + " by Partner " + sendingPartner.getBpnl());
                }
                mpr.setNameAtManufacturer(nameAtManufacturer);
            }

            if (cxId != null) {
                if (!cxId.equals(mpr.getPartnerCXNumber())) {
                    if (mpr.getPartnerCXNumber() != null) {
                        log.warn("Replacing previous Partner CX Number " + mpr.getPartnerCXNumber() + " with " +
                            "new Partner CX Number from SAMM: " + cxId + " by Partner " + sendingPartner.getBpnl());
                    }
                    mpr.setPartnerCXNumber(cxId);
                }

                if (material.getMaterialNumberCx() != null && !cxId.equals(material.getMaterialNumberCx())) {
                    log.warn("Mismatching CX Numbers on Material Entity. CatenaX Number can no longer be considered unique");
                    material.setMatNbrCxAssumedToBeUnique(false);
                    materialService.update(material);
                    // TODO: Decide how to handle conflicting CX Numbers from different suppliers
                    // If we allowed for the possibility that a received PartTypeInformation SAMM
                    // could bring one of our Material entities into an inconsistent state, that could
                    // severely damage important parts of our business logic.

                    materialService.update(material);
                } else {
                    material.setMaterialNumberCx(cxId);
                    materialService.update(material);
                }
            }
            mprService.update(mpr);

        } catch (Exception e) {
            log.error("Invalid Samm from Partner " + sendingPartner.getBpnl() + "\n" + samm, e);
        }
    }


}
