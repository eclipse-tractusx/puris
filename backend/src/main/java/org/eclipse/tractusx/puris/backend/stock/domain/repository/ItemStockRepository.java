package org.eclipse.tractusx.puris.backend.stock.domain.repository;

import org.eclipse.tractusx.puris.backend.stock.domain.model.ItemStock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemStockRepository extends JpaRepository<ItemStock, ItemStock.Key> {


}
