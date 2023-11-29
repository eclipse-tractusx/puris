package org.eclipse.tractusx.puris.backend.stock.logic.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.repository.ItemStockRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemStockService {

    private final ItemStockRepository itemStockRepository;
    private final PartnerService partnerService;
    private final MaterialPartnerRelationService mprService;

    public ItemStock create(ItemStock itemStock) {
        if(!validate(itemStock)) {
            return null;
        }
        return itemStockRepository.save(itemStock);
    }

    public ItemStock update(ItemStock itemStock) {
        if(!validate(itemStock)) {
            return null;
        }
        if(itemStockRepository.findById(itemStock.getKey()).isEmpty()){
            return null;
        }
        return itemStockRepository.save(itemStock);
    }

    public List<ItemStock> findAll() {
        return itemStockRepository.findAll();
    }

    private boolean validate(ItemStock itemStock) {
        var key = itemStock.getKey();
        try {
            Objects.requireNonNull(key.getPartnerBpnl(), "Missing PartnerBpnl");
            Objects.requireNonNull(key.getMaterialNumberCustomer(), "Missing materialNumberCustomer");
            Objects.requireNonNull(key.getMaterialNumberSupplier(), "Missing materialNumberSupplier");
            Objects.requireNonNull(key.getMaterialGlobalAssetId(), "Missing materialGlobalAssetId");
            Objects.requireNonNull(key.getDirection(), "Missing direction");
            Objects.requireNonNull(key.getSupplierOrderId(), "Missing supplierOrderId");
            Objects.requireNonNull(key.getCustomerOrderId(), "Missing customerOrderId");
            Objects.requireNonNull(key.getCustomerOrderPositionId(), "Missing customerOrderPositionId");
            Objects.requireNonNull(key.getLocationBpna(), "Missing locationBpna");
            Objects.requireNonNull(key.getLocationBpns(), "Missing locationBpns");
            Objects.requireNonNull(itemStock.getMeasurementUnit(), "Missing measurementUnit");
            Objects.requireNonNull(itemStock.getQuantityAmount(), "Missing quantityAmount");
            Objects.requireNonNull(itemStock.getLastUpdatedOnDateTime(), "Missing lastUpdatedOnTime");
            Partner partner =  partnerService.findByBpnl(key.getPartnerBpnl());
            Objects.requireNonNull(partner, "Unknown partner: " + key.getPartnerBpnl());
            Partner mySelf = partnerService.getOwnPartnerEntity();
            Partner customer = key.getDirection() == ItemStock.Direction.INBOUND ? mySelf : partner;
            Partner supplier = customer == mySelf ? partner : mySelf;
            var stockBpns = supplier.getSites().stream()
                .filter(site -> site.getBpns().equals(key.getLocationBpns())).findFirst().orElse(null);
            Objects.requireNonNull(stockBpns, "Unknown Bpns: " + key.getLocationBpns());
            var stockBpna = supplier.getSites().stream().flatMap(site -> site.getAddresses().stream())
                .filter(address -> address.getBpna().equals(key.getLocationBpna())).findFirst().orElse(null);
            Objects.requireNonNull(stockBpna, "Unknown Bpna: " + key.getLocationBpna());
            String ownMaterialNumber = mySelf == customer ? key.getMaterialNumberCustomer() : key.getMaterialNumberSupplier();
            var materialPartnerRelation = mprService.find(ownMaterialNumber, partner.getUuid());
            Objects.requireNonNull(materialPartnerRelation, "Missing MaterialPartnerRelation");
        } catch (Exception e) {
            log.error("Validation failed: " + e.getMessage());
            return false;
        }
        return true;
    }
}
