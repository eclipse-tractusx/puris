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
package org.eclipse.tractusx.puris.backend.stock.masterdata.domain;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.repository.MaterialRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class MaterialRepositoryTest {

    @Autowired
    private MaterialRepository materialRepository;

    @Test
    void findAllByMaterialFlagTrue_ReturnsListOfMaterials() {
        // Given
        Material material1 = new Material(true, false, "MNR-123", "uuid-value", "Test Material 1", new HashSet<>());

        Material material2 = new Material(true, false, "MNR-234", "uuid-value-2", "Test Material 2", new HashSet<>());

        // would be more realistic with relationship, but didn't add it here as we just want to test the MaterialRepo
        Material product = new Material(false, true, "MNR-456", "uuid-value-2", "Test Product 1", new HashSet<>());

        materialRepository.save(material1);
        materialRepository.save(material2);
        materialRepository.save(product);

        // When
        List<Material> materials = materialRepository.findAllByMaterialFlagTrue();

        // Then
        assertNotNull(materials);
        assertEquals(2, materials.size());
        assertTrue(materials.contains(material1));
        assertTrue(materials.contains(material2));
    }
}
