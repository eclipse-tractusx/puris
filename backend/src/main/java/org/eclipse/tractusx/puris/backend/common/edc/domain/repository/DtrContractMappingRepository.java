package org.eclipse.tractusx.puris.backend.common.edc.domain.repository;

import jakarta.persistence.Table;
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.ContractMapping;
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.DtrContractMapping;
import org.springframework.stereotype.Repository;

@Repository
@Table(name = "dtr_contract_mapping")
public interface DtrContractMappingRepository extends GeneralContractMappingRepository<DtrContractMapping> {

    @Override
    default Class<? extends ContractMapping> getType() {
        return DtrContractMapping.class;
    }
}
