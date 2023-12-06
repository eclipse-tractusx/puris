/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.puris.backend.stock.masterdata.logic;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

public class ItemStockSAMMTest {

    private final String semiconductorMatNbrCustomer = "MNR-7307-AU340474.002";
    private final String semiconductorMatNbrSupplier = "MNR-8101-ID146955.001";


    @Test
    void parseSampleData() throws Exception {
        String sample = "{\n" +
            "  \"positions\": [\n" +
            "    {\n" +
            "      \"lastUpdatedOnDateTime\": \"2023-04-01T14:23:00\",\n" +
            "      \"orderPositionReference\": {\n" +
            "        \"supplierOrderId\": \"M-Nbr-4711\",\n" +
            "        \"customerOrderId\": \"C-Nbr-4711\",\n" +
            "        \"customerOrderPositionId\": \"PositionId-01\"\n" +
            "      },\n" +
            "      \"allocatedStocks\": [\n" +
            "        {\n" +
            "          \"isBlocked\": false,\n" +
            "          \"stockLocationBPNA\": \"BPNA1234567890\",\n" +
            "          \"quantityOnAllocatedStock\": {\n" +
            "            \"value\": 20.0,\n" +
            "            \"unit\": \"unit:piece\"\n" +
            "          },\n" +
            "          \"stockLocationBPNS\": \"BPNS1234567890ZZ\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ],\n" +
            "  \"materialGlobalAssetId\": \"urn:uuid:48878d48-6f1d-47f5-8ded-a441d0d879df\",\n" +
            "  \"materialNumberCustomer\": \"MNR-7307-AU340474.002\",\n" +
            "  \"materialNumberSupplier\": \"MNR-8101-ID146955.001\",\n" +
            "  \"direction\": \"INBOUND\"\n" +
            "}";

        ObjectMapper objectMapper = new ObjectMapper();
        ItemStockSAMM samm = objectMapper.readValue(sample, ItemStockSAMM.class);
        Assertions.assertEquals(samm.getDirection(), DirectionCharacteristic.INBOUND);
        Assertions.assertEquals(samm.getPositions().get(0).getAllocatedStocks().get(0).getIsBlocked(), false);
        Assertions.assertEquals(samm.getPositions().get(0).getOrderPositionReference().getSupplierOrderId(),"M-Nbr-4711");
        Assertions.assertEquals(samm.getPositions().get(0).getAllocatedStocks().get(0).getQuantityOnAllocatedStock().getValue(), 20.0);
        Assertions.assertEquals(samm.getPositions().get(0).getAllocatedStocks().get(0).getQuantityOnAllocatedStock().getUnit().getValue(), "unit:piece");
    }

    @Test
    void serializeAndDeserializeSamm() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ItemStockSAMM samm = new ItemStockSAMM();
        samm.setDirection(DirectionCharacteristic.INBOUND);
        samm.setMaterialNumberSupplier(semiconductorMatNbrSupplier);
        samm.setMaterialNumberCustomer(semiconductorMatNbrCustomer);

        OrderPositionReference opr = new OrderPositionReference("234", "123", "1");
        ItemQuantityEntity quantity = new ItemQuantityEntity(20.0, ItemUnitEnumeration.UNIT_PIECE);
        AllocatedStock allocatedStock = new AllocatedStock(quantity, "BPNS1234567890ZZ",
            false, "BPNA1234567890AA");
        Position position = new Position(opr, new Date(), List.of(allocatedStock));
        samm.setPositions(List.of(position));
        var jsonNode = objectMapper.readTree(objectMapper.writeValueAsString(samm));

        var readSamm = objectMapper.readValue(jsonNode.toString(), ItemStockSAMM.class);
        Assertions.assertEquals(samm, readSamm);
    }


}
