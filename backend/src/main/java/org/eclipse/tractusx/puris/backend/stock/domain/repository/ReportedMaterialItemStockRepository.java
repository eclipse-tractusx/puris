package org.eclipse.tractusx.puris.backend.stock.domain.repository;

import org.eclipse.tractusx.puris.backend.stock.domain.model.ReportedMaterialItemStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface ReportedMaterialItemStockRepository extends JpaRepository<ReportedMaterialItemStock, UUID> {
}
