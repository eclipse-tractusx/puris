package org.eclipse.tractusx.puris.backend;

import java.util.List;

import org.eclipse.tractusx.puris.backend.common.ddtr.logic.DtrAdapterService;
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
public class MigrationCommandLineRunner implements CommandLineRunner {
    @Autowired
    private DtrAdapterService dtrAdapterService;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private MaterialPartnerRelationService materialPartnerRelationService;
 
    @Override
    public void run(String... args) throws Exception {
        if (!updateDigitalTwins()) {
            log.warn("Failed to update digital twins at DTR");
        }
    }

    private boolean updateDigitalTwins() {
        List<Material> materials = materialService.findAll();
        if (materials.isEmpty()) {
            log.info("No materials found in database, skipping digital twin update at DTR");
            return true;
        }
        log.info("Starting update of digital twins at DTR for {} materials", materials.size());
        try {
            for (Material material : materials) {
                List<MaterialPartnerRelation> mprs = materialPartnerRelationService.findAllByMaterial(material);
                if (material.isMaterialFlag()) {
                    for (MaterialPartnerRelation mpr : mprs) {
                        if (mpr.getPartnerCXNumber() == null) {
                            // if there is no partnerCXNumber, the material twin cannot be linked to the partner twin at the DTR, so we skip the update for this relation
                            log.warn("MaterialPartnerRelation for material {} and Partner {} has no partnerCXNumber, skipping DTR update for this relation", material.getOwnMaterialNumber(), mpr.getPartner().getBpnl());
                            continue;
                        }
                        dtrAdapterService.updateMaterialAtDtr(mpr);
                    }
                }
                if (material.isProductFlag()) {
                    List<MaterialPartnerRelation> buyingMprs = mprs.stream().filter(mpr -> mpr.isPartnerBuysMaterial()).toList();
                    if (buyingMprs.isEmpty()) {
                        log.warn("No buying MaterialPartnerRelation found for material {}, skipping product digital twin update at DTR", material.getOwnMaterialNumber());
                        continue;
                    }
                    dtrAdapterService.updateProduct(material, buyingMprs);
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Error while updating digital twins at dDTR: " + e.getMessage());
            return false;
        }
    }
}
