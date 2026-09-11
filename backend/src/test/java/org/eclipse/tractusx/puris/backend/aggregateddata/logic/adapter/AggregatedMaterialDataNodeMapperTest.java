/*
Copyright (c) 2026 Volkswagen AG

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.

SPDX-License-Identifier: Apache-2.0
*/
package org.eclipse.tractusx.puris.backend.aggregateddata.logic.adapter;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.aggregateddata.domain.model.AggregatedMaterialData;
import org.eclipse.tractusx.puris.backend.common.domain.model.measurement.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.delivery.domain.model.EventTypeEnumeration;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.PolicyProfileVersionEnumeration;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class AggregatedMaterialDataNodeMapperTest {
    private static final String GLOBAL_ASSET_ID = "urn:uuid:6c311d29-5753-46d4-b32c-19b918ea93b0";
    private static final String ANONYMIZED_MATERIAL = "3f4a1b8c9d2e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0";
    private static final String ANONYMIZED_BPNS_1 = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2";
    private static final String ANONYMIZED_BPNS_2 = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a2c4";

    private static final Material MATERIAL;

    static {
        MATERIAL = Material.builder().materialFlag(true).ownMaterialNumber("OWN1").build();
    }

    private static final String AGGREGATED_DATA_JSON = """
        {
          "job": {
            "globalAssetId": "urn:uuid:6c311d29-5753-46d4-b32c-19b918ea93b0"
          },
          "result": {
            "childItems": [
              {
                "materialNumber": "PARTNER-MNR",
                "materialName": "Central Control Unit",
                "quantity": {
                  "value": 1.0,
                  "unit": "unit:piece"
                },
                "items": [
                  {
                    "aspect": "urn:samm:io.catenax.delivery_information_anonymized:1.0.0#DeliveryInformationAnonymized",
                    "items": {
                      "deliveries": [
                        {
                          "deliveryQuantity": {
                            "value": 20.0,
                            "unit": "unit:piece"
                          },
                          "lastUpdatedOnDateTime": "2023-04-28T14:23:00.123456+14:00",
                          "transitEvents": [
                            {
                              "dateTimeOfEvent": "2023-04-29T14:23:00.123456+14:00",
                              "eventType": "estimated-departure"
                            }
                          ],
                          "originBpnsAnonymized": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2",
                          "destinationBpnsAnonymized": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a2c4"
                        }
                      ],
                      "materialGlobalAssetIdAnonymized": "3f4a1b8c9d2e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0"
                    }
                  },
                  {
                    "aspect": "urn:samm:io.catenax.item_stock_anonymized:1.0.0#ItemStockAnonymized",
                    "items": {
                      "allocatedStocksAnonymized": [
                        {
                          "quantityOnAllocatedStock": {
                            "value": 20.0,
                            "unit": "unit:piece"
                          },
                          "stockLocationBPNSAnonymized": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2",
                          "isBlocked": false,
                          "lastUpdatedOnDateTime": "2023-04-28T14:23:00.123456+14:00"
                        }
                      ],
                      "materialGlobalAssetIdAnonymized": "3f4a1b8c9d2e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0",
                      "direction": "OUTBOUND"
                    }
                  },
                  {
                    "aspect": "urn:samm:io.catenax.planned_production_output_anonymized:1.0.0#PlannedProductionOutputAnonymized",
                    "items": {
                      "allocatedPlannedProductionOutputs": [
                        {
                          "plannedProductionQuantity": {
                            "value": 20.0,
                            "unit": "unit:piece"
                          },
                          "productionSiteBpnsAnonymized": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2",
                          "estimatedTimeOfCompletion": "2023-04-29T14:23:00.123456+14:00",
                          "lastUpdatedOnDateTime": "2023-04-28T14:23:00.123456+14:00"
                        }
                      ],
                      "materialGlobalAssetIdAnonymized": "3f4a1b8c9d2e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0"
                    }
                  }
                ],
                "childItems": [
                  {
                    "materialNumber": "MNR-7307-AU340474.002",
                    "materialName": "Semiconductor",
                    "quantity": {
                      "value": 2.0,
                      "unit": "unit:piece"
                    },
                    "items": [
                      {
                        "aspect": "urn:samm:io.catenax.planned_production_output_anonymized:1.0.0#PlannedProductionOutputAnonymized",
                        "items": {
                          "allocatedPlannedProductionOutputs": [
                            {
                              "plannedProductionQuantity": {
                                "value": 5.0,
                                "unit": "unit:piece"
                              },
                              "productionSiteBpnsAnonymized": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a2c4",
                              "estimatedTimeOfCompletion": "2023-04-29T14:23:00.123456+14:00",
                              "lastUpdatedOnDateTime": "2023-04-28T14:23:00.123456+14:00"
                            }
                          ],
                          "materialGlobalAssetIdAnonymized": "3f4a1b8c9d2e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0"
                        }
                      }
                    ],
                    "childItems": [
                      {
                        "materialNumber": "MNR-4177-S",
                        "materialName": "Transistor",
                        "quantity": {
                          "value": 1.0,
                          "unit": "unit:piece"
                        },
                        "items": [],
                        "childItems": []
                      }
                    ]
                  }
                ]
              }
            ]
          }
        }
        """;

    private static final String MINIMAL_JSON = """
        {
          "job": { "globalAssetId": "%s" },
          "result": {
            "childItems": [
              {
                "materialNumber": "PARTNER-MNR",
                "materialName": "Central Control Unit",
                "quantity": { "value": 1.0, "unit": "unit:piece" },
                "items": [],
                "childItems": []
              }
            ]
          }
        }
        """;

    @InjectMocks
    AggregatedMaterialDataNodeMapper mapper;

    @Mock
    MaterialService materialService;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void jsonToAggregatedMaterialData_success() throws Exception {
        JsonNode json = objectMapper.readTree(AGGREGATED_DATA_JSON);
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(MATERIAL);

        AggregatedMaterialData result = mapper.jsonToAggregatedMaterialData(json);

        assertNotNull(result);
        assertSame(MATERIAL, result.getMaterial());

        assertEquals(1, result.getChildMaterialData().size());
        var rootNode = result.getChildMaterialData().get(0);
        assertNull(rootNode.getParentNode());
        assertSame(result, rootNode.getAggregatedMaterialData());
        assertEquals("PARTNER-MNR", rootNode.getExternalMaterialNumber());
        assertEquals("Central Control Unit", rootNode.getExternalMaterialName());
        assertEquals(1.0, rootNode.getQuantity());
        assertEquals(ItemUnitEnumeration.UNIT_PIECE, rootNode.getMeasurementUnit());

        // Delivery
        assertEquals(1, rootNode.getDeliveries().size());
        var delivery = rootNode.getDeliveries().iterator().next();
        assertEquals(20.0, delivery.getQuantity());
        assertEquals(ItemUnitEnumeration.UNIT_PIECE, delivery.getMeasurementUnit());
        assertEquals(EventTypeEnumeration.ESTIMATED_DEPARTURE, delivery.getDepartureType());
        assertEquals(dateFromIso("2023-04-29T14:23:00.123456+14:00"), delivery.getDateOfDeparture());
        assertNull(delivery.getArrivalType());
        assertNull(delivery.getDateOfArrival());
        assertEquals(ANONYMIZED_BPNS_1, delivery.getOriginBpnsAnonymized());
        assertEquals(ANONYMIZED_BPNS_2, delivery.getDestinationBpnsAnonymized());
        assertEquals(dateFromIso("2023-04-28T14:23:00.123456+14:00"), delivery.getLastUpdatedOnDateTime());

        // Stock
        assertEquals(1, rootNode.getStocks().size());
        var stock = rootNode.getStocks().iterator().next();
        assertEquals(20.0, stock.getQuantity());
        assertEquals(ItemUnitEnumeration.UNIT_PIECE, stock.getMeasurementUnit());
        assertEquals(ANONYMIZED_BPNS_1, stock.getStockLocationBpnsAnonymized());
        assertFalse(stock.isBlocked());
        assertEquals(dateFromIso("2023-04-28T14:23:00.123456+14:00"), stock.getLastUpdatedOnDateTime());

        // Production
        assertEquals(1, rootNode.getProductions().size());
        var production = rootNode.getProductions().iterator().next();
        assertEquals(20.0, production.getQuantity());
        assertEquals(ItemUnitEnumeration.UNIT_PIECE, production.getMeasurementUnit());
        assertEquals(ANONYMIZED_BPNS_1, production.getProductionSiteBpnsAnonymized());
        assertEquals(ANONYMIZED_MATERIAL, production.getMaterialGlobalAssetIdAnonymized());
        assertEquals(dateFromIso("2023-04-29T14:23:00.123456+14:00"), production.getEstimatedTimeOfCompletion());
        assertEquals(dateFromIso("2023-04-28T14:23:00.123456+14:00"), production.getLastUpdatedOnDateTime());

        // Child
        assertEquals(1, rootNode.getChildMaterialData().size());
        var child = rootNode.getChildMaterialData().get(0);
        assertEquals("MNR-7307-AU340474.002", child.getExternalMaterialNumber());
        assertEquals("Semiconductor", child.getExternalMaterialName());
        assertEquals(2.0, child.getQuantity());
        assertEquals(ItemUnitEnumeration.UNIT_PIECE, child.getMeasurementUnit());
        assertSame(rootNode, child.getParentNode());
        assertSame(result, child.getAggregatedMaterialData());
        assertTrue(child.getDeliveries().isEmpty());
        assertTrue(child.getStocks().isEmpty());
        assertEquals(1, child.getProductions().size());
        var childProduction = child.getProductions().iterator().next();
        assertEquals(5.0, childProduction.getQuantity());
        assertEquals(ANONYMIZED_BPNS_2, childProduction.getProductionSiteBpnsAnonymized());

        // Grandchild
        assertEquals(1, child.getChildMaterialData().size());
        var grandChild = child.getChildMaterialData().get(0);
        assertEquals("MNR-4177-S", grandChild.getExternalMaterialNumber());
        assertEquals("Transistor", grandChild.getExternalMaterialName());
        assertEquals(1.0, grandChild.getQuantity());
        assertEquals(ItemUnitEnumeration.UNIT_PIECE, grandChild.getMeasurementUnit());
        assertSame(child, grandChild.getParentNode());
        assertSame(result, grandChild.getAggregatedMaterialData());
        assertTrue(grandChild.getProductions().isEmpty());
        assertTrue(grandChild.getDeliveries().isEmpty());
        assertTrue(grandChild.getStocks().isEmpty());
        assertTrue(grandChild.getChildMaterialData().isEmpty());
    }

    @Test
    void jsonToAggregatedMaterialData_multipleChildItems_mapsAllAsRootChildren() throws Exception {
        JsonNode json = objectMapper.readTree("""
            {
              "job": { "globalAssetId": "%s" },
              "result": {
                "childItems": [
                  {
                    "materialNumber": "PARTNER-MNR-1",
                    "materialName": "First",
                    "quantity": { "value": 1.0, "unit": "unit:piece" },
                    "items": [],
                    "childItems": []
                  },
                  {
                    "materialNumber": "PARTNER-MNR-2",
                    "materialName": "Second",
                    "quantity": { "value": 3.0, "unit": "unit:piece" },
                    "items": [],
                    "childItems": []
                  }
                ]
              }
            }
            """.formatted(GLOBAL_ASSET_ID));
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(MATERIAL);

        AggregatedMaterialData result = mapper.jsonToAggregatedMaterialData(json);

        assertNotNull(result);
        assertEquals(2, result.getChildMaterialData().size());
        assertEquals("PARTNER-MNR-1", result.getChildMaterialData().get(0).getExternalMaterialNumber());
        assertNull(result.getChildMaterialData().get(0).getParentNode());
        assertEquals("PARTNER-MNR-2", result.getChildMaterialData().get(1).getExternalMaterialNumber());
        assertNull(result.getChildMaterialData().get(1).getParentNode());
    }

    @Test
    void jsonToAggregatedMaterialData_missingChildItems_returnsEmptyChildren() throws Exception {
        JsonNode json = objectMapper.readTree("""
            {
              "job": { "globalAssetId": "%s" },
              "result": {}
            }
            """.formatted(GLOBAL_ASSET_ID));
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(MATERIAL);

        AggregatedMaterialData result = mapper.jsonToAggregatedMaterialData(json);

        assertNotNull(result);
        assertSame(MATERIAL, result.getMaterial());
        assertTrue(result.getChildMaterialData().isEmpty());
    }

    @Test
    void jsonToAggregatedMaterialData_missingResult_returnsEmptyChildren() throws Exception {
        JsonNode json = objectMapper.readTree("""
            {
              "job": { "globalAssetId": "%s" }
            }
            """.formatted(GLOBAL_ASSET_ID));
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(MATERIAL);

        AggregatedMaterialData result = mapper.jsonToAggregatedMaterialData(json);

        assertNotNull(result);
        assertSame(MATERIAL, result.getMaterial());
        assertTrue(result.getChildMaterialData().isEmpty());
    }

    @Test
    void jsonToAggregatedMaterialData_unknownMaterial_returnsNull() throws Exception {
        JsonNode json = objectMapper.readTree(MINIMAL_JSON.formatted(GLOBAL_ASSET_ID));

        assertNull(mapper.jsonToAggregatedMaterialData(json));
    }

    @Test
    void jsonToAggregatedMaterialData_missingGlobalAssetId_returnsNull() throws Exception {
        JsonNode json = objectMapper.readTree("""
            {
              "job": {},
              "result": { "childItems": [] }
            }
            """);

        assertNull(mapper.jsonToAggregatedMaterialData(json));
    }

    @Test
    void jsonToAggregatedMaterialData_missingJob_returnsNull() throws Exception {
        JsonNode json = objectMapper.readTree("""
            {
              "result": { "childItems": [] }
            }
            """);

        assertNull(mapper.jsonToAggregatedMaterialData(json));
    }

    @Test
    void jsonToAggregatedMaterialData_unexpectedAspect_throws() throws Exception {
        JsonNode json = objectMapper.readTree("""
            {
              "job": { "globalAssetId": "%s" },
              "result": {
                "childItems": [
                  {
                    "materialNumber": "PARTNER-MNR",
                    "materialName": "Central Control Unit",
                    "quantity": { "value": 1.0, "unit": "unit:piece" },
                    "items": [
                      { "aspect": "urn:samm:io.catenax.item_stock:2.0.0#ItemStock", "items": {} }
                    ],
                    "childItems": []
                  }
                ]
              }
            }
            """.formatted(GLOBAL_ASSET_ID));
        when(materialService.findByMaterialNumberCx(GLOBAL_ASSET_ID)).thenReturn(MATERIAL);

        assertThrows(IllegalArgumentException.class, () -> mapper.jsonToAggregatedMaterialData(json));
    }

    private static Date dateFromIso(String iso) {
        return Date.from(OffsetDateTime.parse(iso).toInstant());
    }
}