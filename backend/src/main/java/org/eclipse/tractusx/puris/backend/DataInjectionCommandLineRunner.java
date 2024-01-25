/*
 * Copyright (c) 2023, 2024 Volkswagen AG
 * Copyright (c) 2023, 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
 * Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.puris.backend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.util.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Site;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.MaterialItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ReportedMaterialItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ReportedProductItemStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.measurement.ItemUnitEnumeration;
import org.eclipse.tractusx.puris.backend.stock.logic.service.MaterialItemStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductItemStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ReportedMaterialItemStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ReportedProductItemStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class DataInjectionCommandLineRunner implements CommandLineRunner {

    @Autowired
    private MaterialService materialService;

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private MaterialPartnerRelationService mprService;

    @Autowired
    private MaterialItemStockService materialItemStockService;
    
    @Autowired
    private ProductItemStockService productItemStockService;

    @Autowired
    private ReportedMaterialItemStockService reportedMaterialItemStockService;

    @Autowired
    private ReportedProductItemStockService reportedProductItemStockService;

    @Autowired
    private VariablesService variablesService;

    private ObjectMapper objectMapper;

    private final String semiconductorMatNbrCustomer = "MNR-7307-AU340474.002";
    private final String semiconductorMatNbrSupplier = "MNR-8101-ID146955.001";

    private final String semiconductorMatNbrCatenaX = "860fb504-b884-4009-9313-c6fb6cdc776b";

    private final String supplierSiteNyBpns = "BPNS1234567890ZZ";

    private final String supplierSiteLaBpns = "BPNS2222222222SS";

    public DataInjectionCommandLineRunner(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    @Override
    public void run(String... args) throws Exception {
        createOwnPartnerEntity();
        log.info("Creating setup for " + variablesService.getDemoRole().toUpperCase());
        if (variablesService.getDemoRole().equals("supplier")) {
            setupSupplierRole();
        } else if (variablesService.getDemoRole().equals(("customer"))) {
            setupCustomerRole();
        } else {
            log.info("No role specific setup was created");
        }
    }

    /**
     * Generates and persists a Partner entity that holds all
     * relevant data about the owner of the running instance of
     * the PURIS application.
     */
    private void createOwnPartnerEntity() {
        Partner mySelf;
        if (variablesService.getOwnDefaultBpns() != null && variablesService.getOwnDefaultBpns().length() != 0) {
            mySelf = new Partner(variablesService.getOwnName(),
                variablesService.getEdcProtocolUrl(),
                variablesService.getOwnBpnl(),
                variablesService.getOwnDefaultBpns(),
                variablesService.getOwnDefaultSiteName(),
                variablesService.getOwnDefaultBpna(),
                variablesService.getOwnDefaultStreetAndNumber(),
                variablesService.getOwnDefaultZipCodeAndCity(),
                variablesService.getOwnDefaultCountry());
        } else {
            mySelf = new Partner(variablesService.getOwnName(),
                variablesService.getEdcProtocolUrl(),
                variablesService.getOwnBpnl(),
                variablesService.getOwnDefaultBpna(),
                variablesService.getOwnDefaultStreetAndNumber(),
                variablesService.getOwnDefaultZipCodeAndCity(),
                variablesService.getOwnDefaultCountry()
            );
        }
        mySelf = partnerService.create(mySelf);
        log.info("Successfully created own Partner Entity: " + (partnerService.findByBpnl(mySelf.getBpnl()) != null));
        if (mySelf != null) {
            log.info(mySelf.toString());
        }
    }

    /**
     * Generates an initial set of data for a customer within the demonstration context.
     *
     * @throws JsonProcessingException
     */
    private void setupCustomerRole() throws JsonProcessingException {
        Partner supplierPartner = createAndGetSupplierPartner();
        Material semiconductorMaterial = getNewSemiconductorMaterialForCustomer();
        Partner mySelf = partnerService.getOwnPartnerEntity();

        semiconductorMaterial = materialService.create(semiconductorMaterial);
        log.info(String.format("Created material: %s", semiconductorMaterial));
        List<Material> materialsFound = materialService.findAllMaterials();
        log.info(String.format("Found Material: %s", materialsFound));
        log.info(String.format("UUID of supplier partner: %s", supplierPartner.getUuid()));
        supplierPartner = partnerService.findByUuid(supplierPartner.getUuid());
        log.info(String.format("Found supplier partner: %s", supplierPartner));
        supplierPartner = partnerService.findByBpns(supplierPartner.getSites().stream().findFirst().get().getBpns());
        log.info("Found supplier partner by bpns: " + (supplierPartner != null));


        MaterialPartnerRelation semiconductorPartnerRelation = new MaterialPartnerRelation(semiconductorMaterial,
            supplierPartner, semiconductorMatNbrSupplier, true, false);
        mprService.create(semiconductorPartnerRelation);
        semiconductorPartnerRelation = mprService.find(semiconductorMaterial, supplierPartner);
        log.info("Found Relation: " + semiconductorPartnerRelation);

        // customer + material
        Partner nonScenarioCustomer = createAndGetNonScenarioCustomer();
        Material centralControlUnitEntity = getNewCentralControlUnitMaterial();
        centralControlUnitEntity = materialService.create(centralControlUnitEntity);
        log.info(String.format("Created Product: %s", centralControlUnitEntity));

        MaterialPartnerRelation ccuPartnerRelation = new MaterialPartnerRelation(centralControlUnitEntity,
            nonScenarioCustomer, "MNR-4177-C", false, true);
        ccuPartnerRelation = mprService.create(ccuPartnerRelation);
        log.info("Found Relation: " + ccuPartnerRelation);

        log.info("All stored Relations: " + mprService.findAll());

        List<Partner> foundPartners = mprService.findAllCustomersForOwnMaterialNumber(centralControlUnitEntity.getOwnMaterialNumber());

        log.info("Customer Partner for CCU: " + foundPartners);

        List<Material> productsFound = materialService.findAllProducts();
        log.info(String.format("Found Products: %s", productsFound));

        productsFound = mprService.findAllProductsThatPartnerBuys(nonScenarioCustomer);
        log.info("Products that customer buys: " + productsFound);

        log.info(mySelf.toString());

        var builder = MaterialItemStock.builder();
        var materialItemStock = builder.partner(supplierPartner)
            .material(semiconductorMaterial)
            .lastUpdatedOnDateTime(new Date())
            .locationBpna(mySelf.getSites().first().getAddresses().first().getBpna())
            .locationBpns(mySelf.getSites().first().getBpns())
            .measurementUnit(ItemUnitEnumeration.UNIT_PIECE)
            .quantity(500)
            .partner(supplierPartner)
            .customerOrderId("CNbr-1")
            .customerOrderPositionId("C-Pos-1")
            .supplierOrderId("SNbr-1")
            .build();
        var createdMaterialItemStock = materialItemStockService.create(materialItemStock);
        log.info("Created MaterialItemStock: \n" + createdMaterialItemStock);

        var reportedMaterialItemStock = ReportedMaterialItemStock.builder()
            .material(semiconductorMaterial)
            .lastUpdatedOnDateTime(new Date())
            .locationBpns(supplierPartner.getSites().first().getBpns())
            .locationBpna(supplierPartner.getSites().first().getAddresses().first().getBpna())
            .measurementUnit(ItemUnitEnumeration.UNIT_PIECE)
            .quantity(30)
            .partner(supplierPartner)
            .build();
        reportedMaterialItemStock = reportedMaterialItemStockService.create(reportedMaterialItemStock);
        log.info("Created ReportedMaterialItemStock: \n" + reportedMaterialItemStock);
    }

    /**
     * Generates an initial set of data for a supplier within the demonstration context.
     */
    private void setupSupplierRole() {
        Partner customerPartner = createAndGetCustomerPartner();
        Material semiconductorMaterial = getNewSemiconductorMaterialForSupplier();
        Partner mySelf = partnerService.getOwnPartnerEntity();

        Site secondSite = new Site(
            supplierSiteLaBpns,
            "Semiconductor Supplier Inc. Secondary Site",
            "BPNA2222222222AA",
            "Sunset Blvd. 345",
            "90001 Los Angeles",
            "USA"
        );

        mySelf.getSites().add(secondSite);
        mySelf = partnerService.update(mySelf);
        log.info(String.format("Added Site to mySelf Partner: %s", mySelf));

        semiconductorMaterial = materialService.create(semiconductorMaterial);
        log.info(String.format("Created product: %s", semiconductorMaterial));

        MaterialPartnerRelation semiconductorPartnerRelation = new MaterialPartnerRelation(semiconductorMaterial,
            customerPartner, semiconductorMatNbrCustomer, false, true);
        semiconductorPartnerRelation = mprService.create(semiconductorPartnerRelation);

        log.info("Created Relation " + semiconductorPartnerRelation);

        semiconductorPartnerRelation = mprService.find(semiconductorMaterial, customerPartner);

        log.info("Found Relation " + semiconductorPartnerRelation);

        List<Material> materialsFound = materialService.findAllProducts();
        log.info(String.format("Found product: %s", materialsFound));

        List<Partner> customerPartners = mprService.findAllCustomersForOwnMaterialNumber(semiconductorMaterial.getOwnMaterialNumber());
        log.info(String.format("Found customer partners for semiconductor: %s", customerPartners));

        Site siteNy = mySelf.getSites().stream().filter(site -> site.getBpns().equals("BPNS1234567890ZZ")).findFirst().get();
        Site siteLa = mySelf.getSites().stream().filter(site -> site.getBpns().equals("BPNS2222222222SS")).findFirst().get();

        var productItemStock = ProductItemStock.builder()
            .locationBpna(siteNy.getAddresses().stream().findFirst().get().getBpna())
            .locationBpns(siteNy.getBpns())
            .measurementUnit(ItemUnitEnumeration.UNIT_PIECE)
            .quantity(100)
            .partner(customerPartner)
            .isBlocked(true)
            .customerOrderId("CNbr-2")
            .customerOrderPositionId("C-Pos-2")
            .supplierOrderId("SNbr-2")
            .material(semiconductorMaterial)
            .lastUpdatedOnDateTime(new Date())
            .build();
        productItemStock = productItemStockService.create(productItemStock);
        log.info("Created ProductItemStock \n" + productItemStock);

        var productItemStock2 = ProductItemStock.builder()
            .locationBpna(siteLa.getAddresses().stream().findFirst().get().getBpna())
            .locationBpns(siteLa.getBpns())
            .measurementUnit(ItemUnitEnumeration.UNIT_PIECE)
            .quantity(400)
            .partner(customerPartner)
            .customerOrderId("CNbr-2")
            .customerOrderPositionId("C-Pos-2")
            .supplierOrderId("SNbr-2")
            .material(semiconductorMaterial)
            .lastUpdatedOnDateTime(new Date())
            .build();
        productItemStock2 = productItemStockService.create(productItemStock2);
        log.info("Created ProductItemStock 2\n" + productItemStock2);

        ReportedProductItemStock reportedProductItemStock = ReportedProductItemStock.builder()
            .material(semiconductorMaterial)
            .locationBpns(customerPartner.getSites().first().getBpns())
            .locationBpna(customerPartner.getSites().first().getAddresses().first().getBpna())
            .measurementUnit(ItemUnitEnumeration.UNIT_PIECE)
            .quantity(25)
            .partner(customerPartner)
            .lastUpdatedOnDateTime(new Date())
            .build();
        reportedProductItemStock = reportedProductItemStockService.create(reportedProductItemStock);
        log.info("Created ReportedProductItemStock \n" + reportedProductItemStock);
    }

    /**
     * creates a new customer Partner entity, stores it to
     * the database and returns this entity.
     *
     * @return a reference to the newly created customer
     */
    private Partner createAndGetCustomerPartner() {
        Partner customerPartnerEntity = new Partner(
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
        customerPartnerEntity = partnerService.create(customerPartnerEntity);
        log.info(String.format("Created customer partner: %s", customerPartnerEntity));
        customerPartnerEntity = partnerService.findByUuid(customerPartnerEntity.getUuid());
        log.info(String.format("Found customer partner: %s", customerPartnerEntity));
        return customerPartnerEntity;
    }

    /**
     * creates a new supplier Partner entity, stores it to
     * the database and returns this entity.
     *
     * @return a reference to the newly created supplier
     */
    private Partner createAndGetSupplierPartner() {
        Partner supplierPartnerEntity = new Partner(
            "Semiconductor Supplier Inc.",
            "http://supplier-control-plane:9184/api/v1/dsp",
            "BPNL1234567890ZZ",
            supplierSiteNyBpns,
            "Semiconductor Supplier Inc. Production Site",
            "BPNA1234567890AA",
            "Wall Street 101",
            "10001 New York",
            "USA"
        );
        supplierPartnerEntity = partnerService.create(supplierPartnerEntity);
        log.info(String.format("Created supplier partner: %s", supplierPartnerEntity));
        supplierPartnerEntity = partnerService.findByUuid(supplierPartnerEntity.getUuid());
        log.info(String.format("Found supplier partner: %s", supplierPartnerEntity));

        Site secondSite = new Site(
            supplierSiteLaBpns,
            "Semiconductor Supplier Inc. Secondary Site",
            "BPNA2222222222AA",
            "Sunset Blvd. 345",
            "90001 Los Angeles",
            "USA"
        );

        supplierPartnerEntity.getSites().add(secondSite);
        Partner updatedSupplierPartner = partnerService.update(supplierPartnerEntity);
        log.info(String.format("Added Site to mySelf Partner: %s", updatedSupplierPartner));

        return supplierPartnerEntity;
    }

    /**
     * creates a new (non-scenario) customer entity, stores
     * it to the database and returns this entity.
     *
     * @return a reference to the newly created non-scenario customer
     */
    private Partner createAndGetNonScenarioCustomer() {
        Partner nonScenarioCustomer = new Partner(
            "Non-Scenario Customer",
            "http://nonscenario-customer.com/api/v1/dsp",
            "BPNL2222222222RR",
            "BPNS2222222222XY",
            "Non Scneario Site",
            "BPNA2222222222XZ",
            "Fichtenweg 23",
            "65432 Waldhausen",
            "Germany"
        );
        nonScenarioCustomer = partnerService.create(nonScenarioCustomer);
        log.info(String.format("Created non-scenario customer partner: %s", nonScenarioCustomer));
        nonScenarioCustomer = partnerService.findByUuid(nonScenarioCustomer.getUuid());
        log.info(String.format("Found non-scenario customer partner: %s", nonScenarioCustomer));
        return nonScenarioCustomer;
    }

    private Material getNewSemiconductorMaterialForSupplier() {
        Material material = new Material();
        material.setOwnMaterialNumber(semiconductorMatNbrSupplier);
        material.setMaterialNumberCx(semiconductorMatNbrCatenaX);
        material.setProductFlag(true);
        material.setName("Semiconductor");
        return material;
    }

    private Material getNewSemiconductorMaterialForCustomer() {
        Material material = new Material();
        material.setOwnMaterialNumber(semiconductorMatNbrCustomer);
        material.setMaterialNumberCx(semiconductorMatNbrCatenaX);
        material.setMaterialFlag(true);
        material.setName("Semiconductor");
        return material;
    }

    /**
     * creates a new central control unit Material object.
     * Note: this object is not yet stored to the database
     *
     * @return a reference to the newly created central control unit material
     */
    private Material getNewCentralControlUnitMaterial() {
        Material material = new Material();
        material.setOwnMaterialNumber("MNR-4177-S");
        material.setProductFlag(true);
        material.setName("Central Control Unit");
        return material;
    }

}
