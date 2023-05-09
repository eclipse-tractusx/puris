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

    private boolean actsAsCustomerFlag;
    private boolean actsAsSupplierFlag;

    private String edcUrl;
    private String bpnl;
    private String siteBpns;

    @Nullable
    @ToString.Exclude
    @Setter(AccessLevel.NONE)
    private Set<MaterialDto> suppliesMaterials = new HashSet<>();

    @Nullable
    @ToString.Exclude
    @Setter(AccessLevel.NONE)
    private Set<MaterialDto> ordersProducts = new HashSet<>();

    @ToString.Exclude
    @Setter(AccessLevel.NONE)
    private List<ProductStockDto> allocatedProductStocksForCustomer = new ArrayList<>();

    @ToString.Exclude
    @Setter(AccessLevel.NONE)
    private List<PartnerProductStockDto> partnerProductStocks = new ArrayList<>();

    public PartnerDto(String name, boolean actsAsCustomerFlag, boolean actsAsSupplierFlag, String edcUrl, String bpnl, String siteBpns) {
        super();
        this.name = name;
        this.actsAsCustomerFlag = actsAsCustomerFlag;
        this.actsAsSupplierFlag = actsAsSupplierFlag;
        this.edcUrl = edcUrl;
        this.bpnl = bpnl;
        this.siteBpns = siteBpns;
    }

    public void addSuppliedMaterial(MaterialDto suppliedMaterial) {
        this.suppliesMaterials.add(suppliedMaterial);
        suppliedMaterial.getSuppliedByPartners().add(this);
    }

    public void addOrderedProduct(MaterialDto orderedProduct) {
        this.ordersProducts.add(orderedProduct);
        orderedProduct.getOrderedByPartners().add(this);
    }

    public void addPartnerProductStock(PartnerProductStockDto partnerProductStockDto) {
        this.partnerProductStocks.add(partnerProductStockDto);
        partnerProductStockDto.setSupplierPartner(this);
    }

}
