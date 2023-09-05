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
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.MaterialDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.PartnerDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.PartnerProductStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.Stock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.measurement.MeasurementUnit;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.PartnerProductStockDto;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.samm.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

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

        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(stock.getLastUpdatedOn());
        XMLGregorianCalendar lastUpdatedOn = null;
        try {
            lastUpdatedOn =
                    DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
        } catch (DatatypeConfigurationException e) {
            log.error(String.format("Could not create XML Gregorian " +
                            "Calender from PartnerProductStock.lastUpdatedOn: %s",
                    stock.getLastUpdatedOn().toString()));
            return null;
        }

        AllocatedStock allocatedStock = new AllocatedStock(
                new Quantity(stock.getQuantity(), "unit:" + stock.getMeasurementUnit()),
                new LocationId(LocationIdTypeEnum.B_P_N_S, stock.getAtSiteBpns())
        );
        List<AllocatedStock> allocatedStocks = new ArrayList<>();
        allocatedStocks.add(allocatedStock);

        Position position = new Position(
                null,
                lastUpdatedOn,
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
     * Utility method to deserialize a ProductStockSammDto on the side of the customer
     * @param samm a ProductStockSammDto received from a supplier
     * @param partner the partner you received the message from
     * @return a PartnerProductStockDto
     */
    public PartnerProductStockDto fromSamm(ProductStockSammDto samm, Partner partner) {
        // application currently only supports:
        // - an AGGREGATED Partner stock
        // - one Site per Partner

        // aggregate quantity
        double quantity = samm.getPositions().stream().
                mapToDouble(
                        pos -> pos.getAllocatedStocks().stream().
                                mapToDouble(stock ->
                                        stock.getQuantityOnAllocatedStock().getQuantityNumber()
                                ).sum()
                ).sum();

        // This is just a quickfix in line with the above-mentioned restriction that
        // we are currently supporting only an aggregated stock. It is assumed that all
        // stocks are using the same unit. If, for example, one stock uses kilograms and
        // another stock uses metric tonnes for the same material, this will of course
        // lead to faulty data.
        String measurementUnitString = samm.getPositions().stream().findFirst()
            .stream().findFirst().get().getAllocatedStocks().stream().findFirst()
            .get().getQuantityOnAllocatedStock().getMeasurementUnit();
        measurementUnitString = measurementUnitString.replace("unit:", "");
        MeasurementUnit measurementUnit = MeasurementUnit.valueOf(measurementUnitString);

        // determine material

        Material foundMaterial = materialService.findByOwnMaterialNumber(samm.getMaterialNumberCustomer());
        MaterialPartnerRelation materialPartnerRelation = mprService.find(foundMaterial, partner);
        MaterialDto foundMaterialDto = new MaterialDto(foundMaterial.isMaterialFlag(), foundMaterial.isProductFlag(),
            foundMaterial.getOwnMaterialNumber(), materialPartnerRelation.getPartnerMaterialNumber(), foundMaterial.getMaterialNumberCx(),
            foundMaterial.getName());

        // find bpns - we use the first one as we currently only have one site per partner.
        // alternative would be to inject the bpnl of the partner.
        // also we set the bpn here always to the site
        String atSiteBpns =
                samm.getPositions()
                        .stream().findFirst().get().getAllocatedStocks()
                        .stream().findFirst().get().getSupplierStockLocationId().getLocationId();

        PartnerDto supplierPartner = modelMapper.map(partner, PartnerDto.class);

        return new PartnerProductStockDto(
                foundMaterialDto,
                quantity,
                measurementUnit,
                atSiteBpns,
                supplierPartner
        );
    }
}
