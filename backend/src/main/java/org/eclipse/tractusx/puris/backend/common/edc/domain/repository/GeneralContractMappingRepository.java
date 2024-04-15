package org.eclipse.tractusx.puris.backend.common.edc.domain.repository;

import org.eclipse.tractusx.puris.backend.common.edc.domain.model.ContractMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneralContractMappingRepository <T extends ContractMapping> extends JpaRepository<T, String> {
    default Class<? extends ContractMapping> getType() {
        throw new UnsupportedOperationException("Implementation needed!");
    }

    default T checkedSave(ContractMapping contractMapping) {
        if (!contractMapping.getClass().equals(getType())) {
            throw new IllegalArgumentException("Mismatched ContractMapping type, expected: " + getType() + ", got: "
                + contractMapping.getClass());
        }
        return save((T)contractMapping);
    }


}
