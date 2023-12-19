package org.eclipse.tractusx.puris.backend.stock.logic.service;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.MaterialItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.repository.MaterialItemStockRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MaterialItemStockService extends ItemStockService<MaterialItemStock> {


    public MaterialItemStockService(PartnerService partnerService, MaterialPartnerRelationService mprService,
                                    MaterialItemStockRepository repository) {
        super(partnerService, mprService, repository);
    }

    @Override
    public boolean validate(MaterialItemStock materialItemStock) {
        return basicValidation(materialItemStock) && validateLocalStock(materialItemStock)
            && validateMaterialItemStock(materialItemStock);
    }


}
