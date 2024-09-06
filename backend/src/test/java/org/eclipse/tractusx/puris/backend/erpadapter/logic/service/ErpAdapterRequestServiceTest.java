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

import org.assertj.core.api.Assertions;
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.AssetType;
import org.eclipse.tractusx.puris.backend.erpadapter.domain.model.ErpAdapterRequest;
import org.eclipse.tractusx.puris.backend.erpadapter.domain.repository.ErpAdapterRequestRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class ErpAdapterRequestServiceTest {

    @Mock
    private ErpAdapterRequestRepository erpAdapterRequestRepository;

    @Mock
    private ErpAdapterRequestClient client;

    @InjectMocks
    private ErpAdapterRequestService erpAdapterRequestService;

    private static final String matNbrCustomer = "MNR-7307-AU340474.002";

    private static final String supplierPartnerBpnl = "BPNL1234567890ZZ";

    private static final String sammVersion = "2.0";


    @ParameterizedTest
    @EnumSource(AssetType.class)
    public void unsupported_type_should_fail(AssetType assetType) throws Exception {
        if (ErpAdapterRequest.SUPPORTED_TYPES.contains(assetType)) {
            // skip supported types
            Assertions.assertThat(true).isTrue();
            return;
        }

        // given

        UUID uuid = UUID.randomUUID();
        ErpAdapterRequest erpAdapterRequest = ErpAdapterRequest.builder()
            .requestDate(new Date())
            .partnerBpnl(supplierPartnerBpnl)
            .id(uuid)
            .ownMaterialNumber(matNbrCustomer)
            .requestType(assetType)
            .sammVersion(sammVersion)
            .build();

        // then

        var storedRequest = erpAdapterRequestService.create(erpAdapterRequest);
        // Asset type validation should fail when trying to store request with unsupported type
        Assertions.assertThat(storedRequest).isNull();

    }

}
