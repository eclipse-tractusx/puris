package org.eclipse.tractusx.puris.backend.supply.logic;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.delivery.logic.service.OwnDeliveryService;
import org.eclipse.tractusx.puris.backend.delivery.logic.service.ReportedDeliveryService;
import org.eclipse.tractusx.puris.backend.demand.logic.services.OwnDemandService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.repository.MaterialItemStockRepository;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.eclipse.tractusx.puris.backend.stock.logic.service.MaterialItemStockService;
import org.eclipse.tractusx.puris.backend.supply.domain.model.OwnCustomerSupply;
import org.eclipse.tractusx.puris.backend.supply.domain.repository.ReportedCustomerSupplyRepository;
import org.eclipse.tractusx.puris.backend.supply.logic.service.CustomerSupplyService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SupplyServiceTest {
    @InjectMocks
    CustomerSupplyService customerSupplyService;

    @Mock
    MaterialService materialService;

    @Mock
    PartnerService partnerService;

    @Mock
    MaterialItemStockService stockService;

    @Mock
    MaterialPartnerRelationService mprService;

    @Mock 
    MaterialItemStockRepository itemStockRepository;

    @Mock
    OwnDeliveryService ownDeliveryService;

    @Mock
    ReportedDeliveryService reportedDeliveryService;

    @Mock
    OwnDemandService ownDemandService;

    @Mock
    ReportedCustomerSupplyRepository repository;

    private static final String MATERIAL_NUMBER_CX_CUSTOMER = UUID.randomUUID().toString();
    private static final String BPNL_CUSTOMER = "BPNL4444444444XX";
    private static final String BPNS_CUSTOMER = "BPNS4444444444XX";
    private static final String BPNA_CUSTOMER = "BPNA4444444444AA";

    private static final String BPNL_SUPPLIER = "BPNL1234567890ZZ";
    private static final String BPNS_SUPPLIER = "BPNS1234567890ZZ";
    private static final String BPNA_SUPPLIER = "BPNA1234567890AA";

    private Partner CUSTOMER_PARTNER;
    private Partner SUPPLIER_PARTNER;

    private static final Material TEST_MATERIAL = new Material(
            true,
            true,
            "Own-Mnr",
            MATERIAL_NUMBER_CX_CUSTOMER,
            "Test Material",
            new Date());

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        CUSTOMER_PARTNER = new Partner(
            "Test Customer",
            "http://some-edc.com",
            BPNL_CUSTOMER,
            BPNS_CUSTOMER,
            "Site Name",
            BPNA_CUSTOMER,
            "Street 10",
            "40468 Testdorf",
            "DE"
        );
        CUSTOMER_PARTNER.setUuid(UUID.randomUUID());

        SUPPLIER_PARTNER = new Partner(
            "Test Supplier",
            "http://some-edc.com",
            BPNL_SUPPLIER,
            BPNS_SUPPLIER,
            "Site Name",
            BPNA_SUPPLIER,
            "Street 10",
            "40468 Testdorf",
            "DE"
        );
        SUPPLIER_PARTNER.setUuid(UUID.randomUUID());
    }

    @Test
    void testCalculateCustomerDaysOfSupply() {
        List<Double> demandQuantities = List.of(40.0, 60.0, 50.0, 50.0, 60.0, 50.0);
        List<Double> deliveryQuantities = List.of(0.0, 60.0, 100.0, 0.0, 0.0, 40.0);
        List<Double> daysOfSupply = List.of(1.0, 1.2, 2.0, 1.0, 0.0);

        when(ownDemandService.getQuantityForDays(
            TEST_MATERIAL.getOwnMaterialNumber(),
            Optional.of(BPNL_SUPPLIER),
            Optional.empty(),
            6
        )).thenReturn(demandQuantities);

        when(ownDeliveryService.getQuantityForDays(
            TEST_MATERIAL.getOwnMaterialNumber(),
            Optional.of(BPNL_SUPPLIER),
            Optional.empty(),
            DirectionCharacteristic.INBOUND,
            6
        )).thenReturn(deliveryQuantities);

        when(reportedDeliveryService.getQuantityForDays(
            TEST_MATERIAL.getOwnMaterialNumber(),
            Optional.of(BPNL_SUPPLIER),
            Optional.empty(),
            DirectionCharacteristic.INBOUND,
            6
        )).thenReturn(List.of(0.0, 0.0, 0.0, 0.0, 0.0, 0.0));

        Optional<String> partnerBpnl = Optional.of(BPNL_SUPPLIER);
        when(partnerService.findByBpnl(partnerBpnl.get())).thenReturn(SUPPLIER_PARTNER);

        when(stockService.getInitialStockQuantity(TEST_MATERIAL.getOwnMaterialNumber(), partnerBpnl)).thenReturn(100.0);
        when(materialService.findByOwnMaterialNumber(TEST_MATERIAL.getOwnMaterialNumber())).thenReturn(TEST_MATERIAL);

        List<OwnCustomerSupply> customerSupplies = customerSupplyService.calculateDaysOfSupply(TEST_MATERIAL.getOwnMaterialNumber(), partnerBpnl, Optional.empty(), 6);

        assertEquals(5, customerSupplies.size());
        assertEquals(daysOfSupply, customerSupplies.stream().map(supply -> supply.getDaysOfSupply()).toList());
    }

}
