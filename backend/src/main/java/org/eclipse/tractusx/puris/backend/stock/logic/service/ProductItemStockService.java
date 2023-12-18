package org.eclipse.tractusx.puris.backend.stock.logic.service;


import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.repository.ProductItemStockRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ProductItemStockService extends ItemStockService<ProductItemStock> {


    public ProductItemStockService(PartnerService partnerService, MaterialPartnerRelationService mprService,
                                   ProductItemStockRepository repository) {
        super(partnerService, mprService, repository);
    }


    public boolean validate(ProductItemStock productItemStock) {
        return basicValidation(productItemStock) && validateLocalStock(productItemStock)
            && validateProductItemStock(productItemStock);
    }


}
