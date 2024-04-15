package org.eclipse.tractusx.puris.backend.common.edc.domain.repository;

import org.eclipse.tractusx.puris.backend.common.edc.domain.model.HrefMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneralHrefMappingRepository <T extends HrefMapping>  extends JpaRepository<T, String> {

    default Class<? extends HrefMapping> getType() {
        throw new UnsupportedOperationException("Implementation needed!");
    }

    default T checkedSave(HrefMapping hrefMapping) {
        if (!hrefMapping.getClass().equals(getType())) {
            throw new IllegalArgumentException("Mismatched HrefMapping type, expected " + getType() + ", got " + hrefMapping.getClass());
        }
        return save((T)hrefMapping);
    }
}
