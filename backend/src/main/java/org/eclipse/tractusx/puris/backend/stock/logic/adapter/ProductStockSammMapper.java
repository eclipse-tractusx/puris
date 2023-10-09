/*
 * Copyright (c) 2023 Volkswagen AG
 * Copyright (c) 2023 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
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
package org.eclipse.tractusx.puris.backend.stock.logic.adapter;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.PartnerProductStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.Stock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.measurement.MeasurementUnit;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.samm.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class ProductStockSammMapper {

    @Autowired
    private MaterialService materialService;

    @Autowired
    private MaterialPartnerRelationService mprService;

    @Autowired
    private ModelMapper modelMapper;

    /**
     * Utility method to serialize a ProductStock or PartnerProduct on the side
     * of the responding supplier;
     * @param stock MUST be either ProductStock or PartnerProductStock
     * @return the corresponding ProductStockSammDto
     */
    public ProductStockSammDto toSamm(Stock stock) {

        AllocatedStock allocatedStock = new AllocatedStock(
                new Quantity(stock.getQuantity(), stock.getMeasurementUnit()),
                new LocationId(LocationIdTypeEnum.B_P_N_S, stock.getAtSiteBpns())
        );
        List<AllocatedStock> allocatedStocks = new ArrayList<>();
        allocatedStocks.add(allocatedStock);

        Position position = new Position(
                null,
                stock.getLastUpdatedOn(),
                allocatedStocks
        );
        List<Position> positions = new ArrayList<>();
        positions.add(position);
        String materialNumberCustomer = null;
        String materialNumberSupplier = null;
        if (stock instanceof ProductStock) {
            // Partner is customer
            ProductStock productStock = (ProductStock) stock;
            Partner partner = productStock.getAllocatedToCustomerPartner();
            materialNumberCustomer =  mprService.find(stock.getMaterial(), partner).getPartnerMaterialNumber();
            materialNumberSupplier = stock.getMaterial().getOwnMaterialNumber();
        } else if (stock instanceof PartnerProductStock) {
            // Partner is supplier
            PartnerProductStock partnerProductStock = (PartnerProductStock) stock;
            Partner partner = partnerProductStock.getSupplierPartner();
            materialNumberSupplier = mprService.find(stock.getMaterial(), partner).getPartnerMaterialNumber();
            materialNumberCustomer = stock.getMaterial().getOwnMaterialNumber();
        } else {
            // stock is neither ProductStock nor PartnerProductStock,
            // therefore it is not possible to create a ProductStockSammDto
            return null;
        }

        return new ProductStockSammDto(
                positions,
                materialNumberCustomer,
                Optional.ofNullable(stock.getMaterial().getMaterialNumberCx()),
                Optional.ofNullable(materialNumberSupplier)
        );
    }

    /**
     * Utility method to deserialize a ProductStockSammDto on the side of the customer.
     * Since each ProductStockSammDto may contain multiple Positions, this method will
     * return a list of PartnerProductStocks, where each Samm-Position is mapped to an
     * instance of PartnerProductStock.
     *
     * The caller of this method has to decide how whether he wants to aggregate some of
     * the items of the returned list. Furthermore, he may want to compare the resulting
     * list to his current database and then decide which of the resulting PartnerProductStocks
     * are an update to existing entities in his database.
     *
     * @param samm a ProductStockSammDto received from a supplier
     * @param partner the partner you received the message from
     * @return a PartnerProductStockDto
     */
    public List<PartnerProductStock> sammToPartnerProductStocks(ProductStockSammDto samm, Partner partner) {
        ArrayList<PartnerProductStock> output = new ArrayList<>();
        Material material = materialService.findByOwnMaterialNumber(samm.getMaterialNumberCustomer());
        if(material == null && samm.getMaterialNumberCatenaX().isPresent()) {
            materialService.findByMaterialNumberCx(samm.getMaterialNumberCatenaX().get());
        }
        for(var position : samm.getPositions()) {
            Date lastUpdated = position.getLastUpdatedOnDateTime();
            for(var allocatedStock : position.getAllocatedStocks()) {
                double quantity = allocatedStock.getQuantityOnAllocatedStock().getQuantityNumber();
                MeasurementUnit unit = allocatedStock.getQuantityOnAllocatedStock().getMeasurementUnit();
                String locationId = allocatedStock.getSupplierStockLocationId().getLocationId();
                PartnerProductStock partnerProductStock = new PartnerProductStock(
                    material,
                    quantity,
                    unit,
                    locationId,
                    lastUpdated,
                    partner
                );
                output.add(partnerProductStock);
            }
        }
        return output;
    }
}
