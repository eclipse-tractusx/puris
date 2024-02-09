package org.eclipse.tractusx.puris.backend.masterdata.logic.adapter;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.parttypeinformation.PartTypeInformationSAMM;
import org.springframework.stereotype.Service;

@Service
public class PartTypeInformationSammMapper {

    public PartTypeInformationSAMM materialToSamm(Material material) {
        if (!material.isMaterialFlag()) {
            // Can only generate SAMM from a supplier's perspective
            return null;
        }
        PartTypeInformationSAMM samm = new PartTypeInformationSAMM();
        samm.setCatenaXId(material.getMaterialNumberCx());
        samm.getPartTypeInformation().setManufacturerPartId(material.getOwnMaterialNumber());
        samm.getPartTypeInformation().setNameAtManufacturer(material.getName());
        return samm;
    }

}
