package org.eclipse.tractusx.puris.backend.stock.logic.service;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ReportedProductItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.repository.ReportedProductItemStockRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReportedProductItemStockService extends ItemStockService<ReportedProductItemStock> {
    public ReportedProductItemStockService(PartnerService partnerService, MaterialPartnerRelationService mprService,
                                           ReportedProductItemStockRepository repository) {
        super(partnerService, mprService, repository);
    }


    @Override
    public boolean validate(ReportedProductItemStock itemStock) {
        return basicValidation(itemStock) && validateProductItemStock(itemStock) && validateRemoteStock(itemStock);
    }
}
