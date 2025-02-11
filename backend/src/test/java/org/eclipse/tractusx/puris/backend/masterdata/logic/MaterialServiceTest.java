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
package org.eclipse.tractusx.puris.backend.masterdata.logic;

import org.eclipse.tractusx.puris.backend.common.ddtr.logic.DigitalTwinMappingService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.MaterialPartnerRelation;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Site;
import org.eclipse.tractusx.puris.backend.masterdata.domain.repository.MaterialRepository;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MaterialServiceTest {

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private MaterialPartnerRelationService mprService;
    @Mock
    private DigitalTwinMappingService digitalTwinMappingService;

    @InjectMocks
    private MaterialServiceImpl materialService;

    private final static String semiconductorMatNbrCustomer = "MNR-7307-AU340474.002";
    private final static String semiconductorMatNbrSupplier = "MNR-8101-ID146955.001";

    private final static UUID randomUUID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void create_WhenMaterialDoesNotExist_ReturnsCreatedMaterial() {
        // Given
        Material material = new Material(true, false, "MNR-123", "uuid-value", "Test Material", new Date());

        // When
        when(materialRepository.findById(material.getOwnMaterialNumber())).thenReturn(Optional.empty());
        when(materialRepository.save(material)).thenReturn(material);

        // Then
        Material createdMaterial = materialService.create(material);

        assertNotNull(createdMaterial);
        assertEquals(material, createdMaterial);
        verify(materialRepository, times(1)).findById(material.getOwnMaterialNumber());
        verify(materialRepository, times(1)).save(material);
    }

    @Test
    void create_WhenMaterialExists_ReturnsNull() {
        // Given
        Material material = new Material(true, false, "MNR-123", "uuid-value", "Test Material", new Date());

        // When
        when(materialRepository.findById(material.getOwnMaterialNumber())).thenReturn(Optional.of(material));

        // Then
        Material createdMaterial = materialService.create(material);

        assertNull(createdMaterial);
        verify(materialRepository, times(1)).findById(material.getOwnMaterialNumber());
        verify(materialRepository, never()).save(material);
    }

    @Test
    void identifyMaterial_WithCustomerMaterialNumber_OnSupplierSide() {
        // Given
        Material material = new Material();
        material.setProductFlag(true);
        material.setOwnMaterialNumber(semiconductorMatNbrSupplier);
        Partner customerPartner = createAndGetCustomerPartner();
        MaterialPartnerRelation materialPartnerRelation = new MaterialPartnerRelation(material, customerPartner, semiconductorMatNbrCustomer, false, true);

        // When
        when(mprService.findAllByCustomerPartnerAndPartnerMaterialNumber(customerPartner, semiconductorMatNbrCustomer)).thenReturn(List.of(materialPartnerRelation));
        when(materialRepository.findById(semiconductorMatNbrSupplier)).thenReturn(Optional.empty());

        // Then
        var result = materialService.findFromSupplierPerspective(null, semiconductorMatNbrCustomer, semiconductorMatNbrSupplier, customerPartner);
        assertEquals(result, material);
    }

    @Test
    void identifyMaterial_WithCustomerMaterialNumber_OnSupplierSide_WithoutPartner() {
        // Given
        Material material = new Material();
        material.setProductFlag(true);
        material.setOwnMaterialNumber(semiconductorMatNbrSupplier);
        Partner customerPartner = createAndGetCustomerPartner();
        MaterialPartnerRelation materialPartnerRelation = new MaterialPartnerRelation(material, customerPartner, semiconductorMatNbrCustomer, false, true);

        // When
        when(mprService.findAllByCustomerPartnerMaterialNumber(semiconductorMatNbrCustomer)).thenReturn(List.of(materialPartnerRelation));

        // Then
        var result = materialService.findFromSupplierPerspective(null, semiconductorMatNbrCustomer, null, null);
        assertEquals(result, material);
    }

    @Test
    void identifyMaterial_WithWrongId_OnSupplierSide_ShouldFail() {
        // Given
        Material material = new Material();
        material.setProductFlag(true);
        material.setOwnMaterialNumber(semiconductorMatNbrSupplier);
        Partner customerPartner = createAndGetCustomerPartner();
        MaterialPartnerRelation materialPartnerRelation = new MaterialPartnerRelation(material, customerPartner, semiconductorMatNbrCustomer, false, true);

        // When
        when(mprService.findAllByCustomerPartnerMaterialNumber(semiconductorMatNbrCustomer)).thenReturn(List.of(materialPartnerRelation));

        // Then
        var result = materialService.findFromSupplierPerspective(null, "foo", null, null);
        assertNull(result);
    }

    @Test
    void identifyMaterial_WithCatenaXNumber_OnSupplierSide() {
        // Given
        Material material = new Material();
        material.setProductFlag(true);
        material.setOwnMaterialNumber(semiconductorMatNbrSupplier);
        material.setMaterialNumberCx(randomUUID.toString());
        Partner customerPartner = createAndGetCustomerPartner();

        // When
        when(materialRepository.findByMaterialNumberCx(randomUUID.toString())).thenReturn(List.of(material));

        // Then
        var result = materialService.findFromSupplierPerspective(randomUUID.toString(), semiconductorMatNbrCustomer, semiconductorMatNbrSupplier, customerPartner);
        assertEquals(result, material);
    }

    @Test
    void identifyMaterial_WithSupplierMaterialNumber_OnSupplierSide() {
        // Given
        Material material = new Material();
        material.setProductFlag(true);
        material.setOwnMaterialNumber(semiconductorMatNbrSupplier);
        Partner customerPartner = createAndGetCustomerPartner();
        MaterialPartnerRelation materialPartnerRelation = new MaterialPartnerRelation(material, customerPartner, semiconductorMatNbrCustomer, false, true);

        // When
        when(mprService.findAllByCustomerPartnerAndPartnerMaterialNumber(customerPartner, semiconductorMatNbrCustomer)).thenReturn(List.of(materialPartnerRelation));
        when(materialRepository.findById(semiconductorMatNbrSupplier)).thenReturn(Optional.of(material));

        // Then
        var result = materialService.findFromSupplierPerspective(null, null, semiconductorMatNbrSupplier, customerPartner);
        assertEquals(result, material);
    }



    @Test
    void identifyMaterial_WithCustomerMaterialNumber_OnCustomerSide() {
        // Given
        Material material = new Material();
        material.setMaterialFlag(true);
        material.setOwnMaterialNumber(semiconductorMatNbrCustomer);
        Partner supplierPartner = createAndGetSupplierPartner();
        MaterialPartnerRelation materialPartnerRelation = new MaterialPartnerRelation(material, supplierPartner, semiconductorMatNbrSupplier, true, false);

        // When
        when(materialRepository.findById(semiconductorMatNbrCustomer)).thenReturn(Optional.of(material));

        // Then
        var result = materialService.findFromCustomerPerspective(null, semiconductorMatNbrCustomer, semiconductorMatNbrSupplier, supplierPartner);
        assertEquals(material, result);
    }

    @Test
    void identifyMaterial_WithSupplierMaterialNumber_OnCustomerSide() {
        // Given
        Material material = new Material();
        material.setMaterialFlag(true);
        material.setOwnMaterialNumber(semiconductorMatNbrCustomer);
        Partner supplierPartner = createAndGetSupplierPartner();
        MaterialPartnerRelation materialPartnerRelation = new MaterialPartnerRelation(material, supplierPartner, semiconductorMatNbrSupplier, true, false);

        // When
        when(mprService.findAllBySupplierPartnerAndPartnerMaterialNumber(supplierPartner, semiconductorMatNbrSupplier)).thenReturn(List.of(materialPartnerRelation));

        // Then
        var result = materialService.findFromCustomerPerspective(null, null, semiconductorMatNbrSupplier, supplierPartner);
        assertEquals(material, result);
    }

    @Test
    void identifyMaterial_WithCatenaXNumber_OnCustomerSide() {
        // Given
        Material material = new Material();
        material.setMaterialFlag(true);
        material.setOwnMaterialNumber(semiconductorMatNbrCustomer);
        material.setMaterialNumberCx(randomUUID.toString());
        Partner supplierPartner = createAndGetSupplierPartner();

        // When
        when(materialRepository.findByMaterialNumberCx(randomUUID.toString())).thenReturn(List.of(material));

        // Then
        var result = materialService.findFromSupplierPerspective(randomUUID.toString(), semiconductorMatNbrCustomer, semiconductorMatNbrSupplier, supplierPartner);
        assertEquals(result, material);
    }



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
        return customerPartnerEntity;
    }

    private Partner createAndGetSupplierPartner() {
        Partner supplierPartnerEntity = new Partner(
            "Semiconductor Supplier Inc.",
            "http://supplier-control-plane:9184/api/v1/dsp",
            "BPNL1234567890ZZ",
            "BPNS1234567890ZZ",
            "Semiconductor Supplier Inc. Production Site",
            "BPNA1234567890AA",
            "Wall Street 101",
            "10001 New York",
            "USA"
        );
        Site secondSite = new Site(
            "BPNS2222222222SS",
            "Semiconductor Supplier Inc. Secondary Site",
            "BPNA2222222222AA",
            "Sunset Blvd. 345",
            "90001 Los Angeles",
            "USA"
        );
        supplierPartnerEntity.getSites().add(secondSite);
        return supplierPartnerEntity;
    }
}
