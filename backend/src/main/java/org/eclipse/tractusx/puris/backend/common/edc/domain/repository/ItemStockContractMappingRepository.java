package org.eclipse.tractusx.puris.backend.common.edc.domain.repository;

import org.eclipse.tractusx.puris.backend.common.edc.domain.model.ContractMapping;
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.ItemStockContractMapping;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemStockContractMappingRepository extends GeneralContractMappingRepository<ItemStockContractMapping> {

    @Override
    default Class<? extends ContractMapping> getType() {
        return ItemStockContractMapping.class;
    }
}
