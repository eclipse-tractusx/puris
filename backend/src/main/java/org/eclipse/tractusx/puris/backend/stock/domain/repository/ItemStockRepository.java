package org.eclipse.tractusx.puris.backend.stock.domain.repository;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ItemStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItemStockRepository<T extends ItemStock> extends JpaRepository<T, UUID> {

    default List<T> find(Partner partner, Material material) {
        // default implementation prevents Jpa from trying to
        // auto-generate this method. 
        throw new UnsupportedOperationException("Implementation needed");
    }

}
