package org.eclipse.tractusx.puris.backend.masterdata.logic.service;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;

import java.util.List;
import java.util.Map;
import java.util.UUID;


public interface MaterialPartnerRelationService {
    MaterialPartnerRelation create(MaterialPartnerRelation materialPartnerRelation);

    MaterialPartnerRelation update(MaterialPartnerRelation materialPartnerRelation);

    MaterialPartnerRelation find(Material material, Partner partner);

    List<Material> findAllMaterialsThatPartnerSupplies(Partner partner);

    List<Material> findAllProductsThatPartnerBuys(Partner partner);

    List<MaterialPartnerRelation> findAll();

    Map<String, String> getBPNL_To_MaterialNumberMap(String ownMaterialNumber);

    MaterialPartnerRelation find(String ownMaterialNumber, UUID partnerUuid);

    List<Partner> findAllSuppliersForOwnMaterialNumber(String ownMaterialNumber);

    List<Partner> findAllCustomersForOwnMaterialNumber(String ownMaterialNumber);

    List<Partner> findAllSuppliersForMaterial(Material material);

    List<Material> findAllByPartnerMaterialNumber(String partnerMaterialNumber);

    boolean partnerSuppliesMaterial(Material material, Partner partner);

    boolean partnerOrdersProduct(Material material, Partner partner);
}
