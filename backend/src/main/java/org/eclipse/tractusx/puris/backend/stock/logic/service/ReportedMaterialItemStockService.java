package org.eclipse.tractusx.puris.backend.stock.logic.service;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ReportedMaterialItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.repository.ReportedMaterialItemStockRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReportedMaterialItemStockService extends ItemStockService<ReportedMaterialItemStock> {


    public ReportedMaterialItemStockService(PartnerService partnerService, MaterialPartnerRelationService mprService, ReportedMaterialItemStockRepository repository) {
        super(partnerService, mprService, repository);
    }

    @Override
    public boolean validate(ReportedMaterialItemStock itemStock) {
        return basicValidation(itemStock) && validateMaterialItemStock(itemStock) && validateRemoteStock(itemStock);
    }
}
