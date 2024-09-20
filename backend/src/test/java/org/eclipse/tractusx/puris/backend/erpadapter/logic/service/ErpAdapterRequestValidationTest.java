/*
 * Copyright (c) 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.puris.backend.erpadapter.logic.service;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.AssetType;
import org.eclipse.tractusx.puris.backend.erpadapter.domain.model.ErpAdapterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ErpAdapterRequestValidationTest {

    private static final String matNbrCustomer = "MNR-7307-AU340474.002";

    private static final String supplierPartnerBpnl = "BPNL1234567890ZZ";

    private static Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @ParameterizedTest
    @EnumSource(AssetType.class)
    public void testRequestTypeValidation(AssetType type) {
        // given
        ErpAdapterRequest erpAdapterRequest = ErpAdapterRequest.builder()
            .id(UUID.randomUUID())
            .requestDate(new Date())
            .partnerBpnl(supplierPartnerBpnl)
            .ownMaterialNumber(matNbrCustomer)
            .requestType(type)
            .sammVersion(type.ERP_SAMMVERSION)
            .build();

        // when
        var violations = validator.validate(erpAdapterRequest);

        // then
        if (!ErpAdapterRequest.SUPPORTED_TYPES.contains(type)) {
            assertEquals(1, violations.size(), "Expected validation errors for unsupported type: " + type);
        } else {
            assertTrue(violations.isEmpty(), "No validation errors expected for supported type: " + type);
        }
    }

    @Test
    public void testRequestTypeValidation() {
        // given
        ErpAdapterRequest erpAdapterRequest = ErpAdapterRequest.builder()
            .id(UUID.randomUUID())
            .requestDate(null) // must not be null
            .partnerBpnl("wrong-bpnl") // should fail regex check
            .ownMaterialNumber("illegal-material-number\n") // should fail regex check
            .requestType(AssetType.ITEM_STOCK_SUBMODEL)
            .sammVersion(AssetType.ITEM_STOCK_SUBMODEL.ERP_SAMMVERSION)
            .build();

        // when
        var violations = validator.validate(erpAdapterRequest);

        // then
        assertEquals(3, violations.size());
    }


}
