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
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.MaterialDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.PartnerDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.PartnerProductStockDto;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.ProductStockDto;
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
    private PartnerService partnerService;

    @Autowired
    private ModelMapper modelMapper;

    public ProductStockSammDto toSamm(ProductStockDto productStockDto) {

        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(productStockDto.getLastUpdatedOn());
        XMLGregorianCalendar lastUpdatedOn = null;
        try {
            lastUpdatedOn =
                    DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
        } catch (DatatypeConfigurationException e) {
            log.error(String.format("Could not create XML Gregorian " +
                            "Calender from PartnerProductStock.lastUpdatedOn: %s",
                    productStockDto.getLastUpdatedOn().toString()));
            return null;
        }

        AllocatedStock allocatedStock = new AllocatedStock(
                new Quantity(productStockDto.getQuantity(), "unit:piece"),
                new LocationId(LocationIdTypeEnum.B_P_N_S, productStockDto.getAtSiteBpnl())
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

        return new ProductStockSammDto(
                positions,
                productStockDto.getMaterial().getMaterialNumberCustomer(),
                Optional.ofNullable(productStockDto.getMaterial().getMaterialNumberCx()),
                Optional.ofNullable(productStockDto.getMaterial().getMaterialNumberSupplier())
        );
    }


    public PartnerProductStockDto fromSamm(ProductStockSammDto samm) {
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

        // determine material
        Material foundMaterial =
                materialService.findProductByMaterialNumberCustomer(samm.getMaterialNumberCustomer());
        MaterialDto foundMaterialDto = modelMapper.map(foundMaterial, MaterialDto.class);

        // find bpns - we use the first one as we currently only have one site per partner.
        // alternative would be to inject the bpnl of the partner.
        // also we set the bpn here always to the site
        String atSiteBpns =
                samm.getPositions()
                        .stream().findFirst().get().getAllocatedStocks()
                        .stream().findFirst().get().getSupplierStockLocationId().getLocationId();

        // find partner by bpns
        Partner foundPartner = partnerService.findByBpns(atSiteBpns);
        PartnerDto supplierPartner = modelMapper.map(foundPartner, PartnerDto.class);

        return new PartnerProductStockDto(
                foundMaterialDto,
                quantity,
                atSiteBpns,
                supplierPartner
        );
    }
}
