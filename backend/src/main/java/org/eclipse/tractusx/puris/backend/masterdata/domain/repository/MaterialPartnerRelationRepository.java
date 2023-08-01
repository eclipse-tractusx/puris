package org.eclipse.tractusx.puris.backend.masterdata.domain.repository;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MaterialPartnerRelationRepository extends JpaRepository<MaterialPartnerRelation, MaterialPartnerRelation.Key> {

    List<MaterialPartnerRelation> findAllByPartner_Uuid(UUID partnerUuid);

    List<MaterialPartnerRelation> findAllByMaterial_OwnMaterialNumber(String ownMaterialNumber);

    List<MaterialPartnerRelation> findAllByMaterial_OwnMaterialNumberAndPartnerSuppliesMaterialIsTrue(String ownMaterialNumber);

    List<MaterialPartnerRelation> findAllByMaterial_OwnMaterialNumberAndPartnerBuysMaterialIsTrue(String ownMaterialNumber);

    List<MaterialPartnerRelation> findAllByPartnerMaterialNumber(String partnerMaterialNumber);
}
