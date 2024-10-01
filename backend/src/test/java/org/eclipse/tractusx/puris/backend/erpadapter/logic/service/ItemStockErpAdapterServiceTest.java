package org.eclipse.tractusx.puris.backend.erpadapter.logic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.AssetType;
import org.eclipse.tractusx.puris.backend.erpadapter.controller.ErpAdapterController;
import org.eclipse.tractusx.puris.backend.erpadapter.domain.model.ErpAdapterRequest;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.logic.adapter.ItemStockSammMapper;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.DirectionCharacteristic;
import org.eclipse.tractusx.puris.backend.stock.logic.service.MaterialItemStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductItemStockService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class ItemStockErpAdapterServiceTest {

    private final static ObjectMapper mapper = new ObjectMapper();

    @Mock
    private ErpAdapterRequestService erpAdapterRequestService;

    @Mock
    private MaterialPartnerRelationService mprService;

    @Mock
    private PartnerService partnerService;

    @Mock
    private MaterialService materialService;

    @Mock
    private ItemStockSammMapper sammMapper;

    @Mock
    private MaterialItemStockService materialItemStockService;

    @Mock
    private ProductItemStockService productItemStockService;

    @InjectMocks
    private ItemStockErpAdapterService itemStockErpAdapterService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    private static final Partner partner = new Partner(
            "Control Unit Creator Inc.",
                "http://customer-control-plane:8184/api/v1/dsp",
                "BPNL4444444444XX",
                "BPNS4444444444XX",
                "Control Unit Creator Production Site",
                "BPNA4444444444AA",
                "13th Street 47",
                "10011 New York",
                "USA"
    );

    private static final String ownMaterialNumber = "MNR-8101-ID146955.001";

    private static final String partnerMaterialNumber = "MNR-7307-AU340474.002";

    private static final Material material = new Material();

    private static final String partnerBpnl = partner.getBpnl();

    private static final String responseType = AssetType.ITEM_STOCK_SUBMODEL.ERP_KEYWORD;

    private static final String sammVersion = AssetType.ITEM_STOCK_SUBMODEL.ERP_SAMMVERSION;

    static {
        material.setOwnMaterialNumber(ownMaterialNumber);
        material.setProductFlag(true);
        material.setName("Semiconductor");
    }

    private static final MaterialPartnerRelation mpr = new MaterialPartnerRelation(material, partner,
        partnerMaterialNumber, false, true);

    private final static String itemStock20Sample = "{\n" +
        "    \"materialGlobalAssetId\": null,\n" +
        "    \"positions\": [\n" +
        "        {\n" +
        "            \"orderPositionReference\": {\n" +
        "                \"supplierOrderId\": \"M-Nbr-4711\",\n" +
        "                \"customerOrderId\": \"C-Nbr-4711\",\n" +
        "                \"customerOrderPositionId\": \"PositionId-01\"\n" +
        "            },\n" +
        "            \"allocatedStocks\": [\n" +
        "                {\n" +
        "                    \"isBlocked\": false,\n" +
        "                    \"stockLocationBPNA\": \"BPNA4444444444AA\",\n" +
        "                    \"lastUpdatedOnDateTime\": \"2023-04-28T14:23:00.123456+14:00\",\n" +
        "                    \"quantityOnAllocatedStock\": {\n" +
        "                        \"value\": 22.0,\n" +
        "                        \"unit\": \"unit:piece\"\n" +
        "                    },\n" +
        "                    \"stockLocationBPNS\": \"BPNS4444444444XX\"\n" +
        "                }\n" +
        "            ]\n" +
        "        },\n" +
        "        {\n" +
        "            \"orderPositionReference\": {\n" +
        "                \"supplierOrderId\": \"M-Nbr-4711\",\n" +
        "                \"customerOrderId\": \"C-Nbr-4711\",\n" +
        "                \"customerOrderPositionId\": \"PositionId-03\"\n" +
        "            },\n" +
        "            \"allocatedStocks\": [\n" +
        "                {\n" +
        "                    \"isBlocked\": false,\n" +
        "                    \"stockLocationBPNA\": \"BPNA4444444444AA\",\n" +
        "                    \"lastUpdatedOnDateTime\": \"2023-04-28T14:23:00.123456+14:00\",\n" +
        "                    \"quantityOnAllocatedStock\": {\n" +
        "                        \"value\": 66.0,\n" +
        "                        \"unit\": \"unit:piece\"\n" +
        "                    },\n" +
        "                    \"stockLocationBPNS\": \"BPNS4444444444XX\"\n" +
        "                }\n" +
        "            ]\n" +
        "        },\n" +
        "                {\n" +
        "            \"orderPositionReference\": {\n" +
        "                \"supplierOrderId\": \"M-Nbr-4711\",\n" +
        "                \"customerOrderId\": \"C-Nbr-4711\",\n" +
        "                \"customerOrderPositionId\": \"PositionId-02\"\n" +
        "            },\n" +
        "            \"allocatedStocks\": [\n" +
        "                {\n" +
        "                    \"isBlocked\": true,\n" +
        "                    \"stockLocationBPNA\": \"BPNA4444444444AA\",\n" +
        "                    \"lastUpdatedOnDateTime\": \"2023-04-28T14:23:00.123456+14:00\",\n" +
        "                    \"quantityOnAllocatedStock\": {\n" +
        "                        \"value\": 44.0,\n" +
        "                        \"unit\": \"unit:piece\"\n" +
        "                    },\n" +
        "                    \"stockLocationBPNS\": \"BPNS4444444444XX\"\n" +
        "                }\n" +
        "            ]\n" +
        "        }\n" +
        "    ],\n" +
        "    \"direction\": \"OUTBOUND\"\n" +
        "}";

    @Test
    void testReceivedMessageForExistingRequest_should_succeed() throws Exception {
        UUID requestId = UUID.randomUUID();

        // given
        ErpAdapterRequest request = ErpAdapterRequest.builder()
            .id(requestId)
            .requestType(AssetType.ITEM_STOCK_SUBMODEL)
            .sammVersion(AssetType.ITEM_STOCK_SUBMODEL.ERP_SAMMVERSION)
            .responseCode(201)
            .ownMaterialNumber(ownMaterialNumber)
            .directionCharacteristic(DirectionCharacteristic.OUTBOUND)
            .requestDate(new Date())
            .partnerBpnl(partnerBpnl)
            .build();

        ErpAdapterController.Dto dto = new ErpAdapterController.Dto(requestId, partnerBpnl, responseType, sammVersion,
            new Date(), mapper.readTree(itemStock20Sample));

        // when
        Mockito.when(erpAdapterRequestService.get(requestId)).thenReturn(request);
        Mockito.when(partnerService.findByBpnl(partnerBpnl)).thenReturn(partner);
        Mockito.when(materialService.findByOwnMaterialNumber(ownMaterialNumber)).thenReturn(material);
        Mockito.when(mprService.find(material, partner)).thenReturn(mpr);

        // then
        int result = itemStockErpAdapterService.receiveItemStockUpdate(dto);

        Assertions.assertEquals(201, result);
    }

    @Test
    void testReceivedMessageForNonExistingRequest_should_fail() throws Exception {
        UUID requestId = UUID.randomUUID();

        // given
        ErpAdapterController.Dto dto = new ErpAdapterController.Dto(requestId, partnerBpnl, responseType, sammVersion,
            new Date(), mapper.readTree(itemStock20Sample));

        // when
        Mockito.when(erpAdapterRequestService.get(requestId)).thenReturn(null);

        // then
        int result = itemStockErpAdapterService.receiveItemStockUpdate(dto);

        Assertions.assertEquals(404, result);
    }

    @Test
    void testReceivedMessageForAlreadyAnsweredRequest_should_fail() throws Exception {
        UUID requestId = UUID.randomUUID();

        // given
        ErpAdapterRequest request = ErpAdapterRequest.builder()
            .id(requestId)
            .requestType(AssetType.ITEM_STOCK_SUBMODEL)
            .sammVersion(AssetType.ITEM_STOCK_SUBMODEL.ERP_SAMMVERSION)
            .responseCode(201)
            .ownMaterialNumber(ownMaterialNumber)
            .directionCharacteristic(DirectionCharacteristic.OUTBOUND)
            .requestDate(new Date())
            .responseReceivedDate(new Date())
            .partnerBpnl(partnerBpnl)
            .build();

        ErpAdapterController.Dto dto = new ErpAdapterController.Dto(requestId, partnerBpnl, responseType, sammVersion,
            new Date(), mapper.readTree(itemStock20Sample));

        // when
        Mockito.when(erpAdapterRequestService.get(requestId)).thenReturn(request);

        // then
        int result = itemStockErpAdapterService.receiveItemStockUpdate(dto);

        Assertions.assertEquals(409, result);
    }

    @Test
    void testReceivedMessageForNotReceivedRequest_should_fail() throws Exception {
        UUID requestId = UUID.randomUUID();

        // given
        ErpAdapterRequest request = ErpAdapterRequest.builder()
            .id(requestId)
            .requestType(AssetType.ITEM_STOCK_SUBMODEL)
            .sammVersion(AssetType.ITEM_STOCK_SUBMODEL.ERP_SAMMVERSION)
            .responseCode(null)
            .ownMaterialNumber(ownMaterialNumber)
            .directionCharacteristic(DirectionCharacteristic.OUTBOUND)
            .requestDate(new Date())
            .partnerBpnl(partnerBpnl)
            .build();

        ErpAdapterController.Dto dto = new ErpAdapterController.Dto(requestId, partnerBpnl, responseType, sammVersion,
            new Date(), mapper.readTree(itemStock20Sample));

        // when
        Mockito.when(erpAdapterRequestService.get(requestId)).thenReturn(request);

        // then
        int result = itemStockErpAdapterService.receiveItemStockUpdate(dto);

        Assertions.assertEquals(404, result);
    }

    @Test
    void testReceivedMessageWithInconsistentBPNL_should_fail() throws Exception {
        UUID requestId = UUID.randomUUID();

        // given
        ErpAdapterRequest request = ErpAdapterRequest.builder()
            .id(requestId)
            .requestType(AssetType.ITEM_STOCK_SUBMODEL)
            .sammVersion(AssetType.ITEM_STOCK_SUBMODEL.ERP_SAMMVERSION)
            .responseCode(201)
            .ownMaterialNumber(ownMaterialNumber)
            .directionCharacteristic(DirectionCharacteristic.OUTBOUND)
            .requestDate(new Date())
            .partnerBpnl("BPNL1234567890")
            .build();

        ErpAdapterController.Dto dto = new ErpAdapterController.Dto(requestId, partnerBpnl, responseType, sammVersion,
            new Date(), mapper.readTree(itemStock20Sample));

        // when
        Mockito.when(erpAdapterRequestService.get(requestId)).thenReturn(request);

        // then
        int result = itemStockErpAdapterService.receiveItemStockUpdate(dto);

        Assertions.assertEquals(400, result);
    }

    @Test
    void testReceivedMessageWithDirectionMismatch_should_fail() throws Exception {
        UUID requestId = UUID.randomUUID();

        // given
        ErpAdapterRequest request = ErpAdapterRequest.builder()
            .id(requestId)
            .requestType(AssetType.ITEM_STOCK_SUBMODEL)
            .sammVersion(AssetType.ITEM_STOCK_SUBMODEL.ERP_SAMMVERSION)
            .responseCode(201)
            .ownMaterialNumber(ownMaterialNumber)
            .directionCharacteristic(DirectionCharacteristic.INBOUND)
            .requestDate(new Date())
            .partnerBpnl(partnerBpnl)
            .build();

        ErpAdapterController.Dto dto = new ErpAdapterController.Dto(requestId, partnerBpnl, responseType, sammVersion,
            new Date(), mapper.readTree(itemStock20Sample));

        // when
        Mockito.when(erpAdapterRequestService.get(requestId)).thenReturn(request);

        // then
        int result = itemStockErpAdapterService.receiveItemStockUpdate(dto);

        Assertions.assertEquals(400, result);
    }

    @Test
    void testReceivedMessageWithUnsupportedSammVersion_should_fail() throws Exception {
        UUID requestId = UUID.randomUUID();

        // given
        ErpAdapterRequest request = ErpAdapterRequest.builder()
            .id(requestId)
            .requestType(AssetType.ITEM_STOCK_SUBMODEL)
            .sammVersion(AssetType.ITEM_STOCK_SUBMODEL.ERP_SAMMVERSION)
            .responseCode(201)
            .ownMaterialNumber(ownMaterialNumber)
            .directionCharacteristic(DirectionCharacteristic.OUTBOUND)
            .requestDate(new Date())
            .partnerBpnl(partnerBpnl)
            .build();

        ErpAdapterController.Dto dto = new ErpAdapterController.Dto(requestId, partnerBpnl, responseType, "0.1",
            new Date(), mapper.readTree(itemStock20Sample));

        // when
        Mockito.when(erpAdapterRequestService.get(requestId)).thenReturn(request);

        // then
        int result = itemStockErpAdapterService.receiveItemStockUpdate(dto);

        Assertions.assertEquals(400, result);
    }

    @Test
    void testReceivedMessageForPartnerWithoutAppropriateFlag_should_fail() throws Exception {
        UUID requestId = UUID.randomUUID();

        // given
        ErpAdapterRequest request = ErpAdapterRequest.builder()
            .id(requestId)
            .requestType(AssetType.ITEM_STOCK_SUBMODEL)
            .sammVersion(AssetType.ITEM_STOCK_SUBMODEL.ERP_SAMMVERSION)
            .responseCode(201)
            .ownMaterialNumber(ownMaterialNumber)
            .directionCharacteristic(DirectionCharacteristic.OUTBOUND)
            .requestDate(new Date())
            .partnerBpnl(partnerBpnl)
            .build();

        ErpAdapterController.Dto dto = new ErpAdapterController.Dto(requestId, partnerBpnl, responseType, sammVersion,
            new Date(), mapper.readTree(itemStock20Sample));

        // NOTE: we must revert this edit after test run
        mpr.setPartnerBuysMaterial(false);

        // when
        Mockito.when(erpAdapterRequestService.get(requestId)).thenReturn(request);
        Mockito.when(partnerService.findByBpnl(partnerBpnl)).thenReturn(partner);
        Mockito.when(materialService.findByOwnMaterialNumber(ownMaterialNumber)).thenReturn(material);
        Mockito.when(mprService.find(material, partner)).thenReturn(mpr);

        // then
        int result = itemStockErpAdapterService.receiveItemStockUpdate(dto);

        Assertions.assertEquals(400, result);

        // reverting edit
        mpr.setPartnerBuysMaterial(true);
    }

}
