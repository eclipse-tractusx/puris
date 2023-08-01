package org.eclipse.tractusx.puris.backend.masterdata.logic.dto;

import jakarta.annotation.Nullable;
import lombok.*;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.PartnerProductStockDto;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ProductStockDto;

import java.io.Serializable;
import java.util.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class PartnerDto implements Serializable {

    private UUID uuid;
    private String name;

    private String edcUrl;
    private String bpnl;
    private String siteBpns;

    @ToString.Exclude
    @Setter(AccessLevel.NONE)
    private List<ProductStockDto> allocatedProductStocksForCustomer = new ArrayList<>();

    @ToString.Exclude
    @Setter(AccessLevel.NONE)
    private List<PartnerProductStockDto> partnerProductStocks = new ArrayList<>();

    public void addPartnerProductStock(PartnerProductStockDto partnerProductStockDto) {
        this.partnerProductStocks.add(partnerProductStockDto);
        partnerProductStockDto.setSupplierPartner(this);
    }

}
