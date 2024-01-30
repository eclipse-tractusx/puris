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

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.repository.MaterialRepository;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MaterialServiceTest {

    @Mock
    private MaterialRepository materialRepository;

    @InjectMocks
    private MaterialServiceImpl materialService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void create_WhenMaterialDoesNotExist_ReturnsCreatedMaterial() {
        // Given
        Material material = new Material(true, false, "MNR-123", "uuid-value", "Test Material");

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
        Material material = new Material(true, false, "MNR-123", "uuid-value", "Test Material");

        // When
        when(materialRepository.findById(material.getOwnMaterialNumber())).thenReturn(Optional.of(material));

        // Then
        Material createdMaterial = materialService.create(material);

        assertNull(createdMaterial);
        verify(materialRepository, times(1)).findById(material.getOwnMaterialNumber());
        verify(materialRepository, never()).save(material);
    }
}
