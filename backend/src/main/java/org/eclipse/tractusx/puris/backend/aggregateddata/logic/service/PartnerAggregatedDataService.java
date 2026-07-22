/*
Copyright (c) 2026 Volkswagen AG

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.

SPDX-License-Identifier: Apache-2.0
*/
package org.eclipse.tractusx.puris.backend.aggregateddata.logic.service;

import java.util.List;

import org.eclipse.tractusx.puris.backend.aggregateddata.domain.model.PartnerAggregatedData;
import org.eclipse.tractusx.puris.backend.aggregateddata.domain.repository.PartnerAggregatedDataRepository;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.stereotype.Service;

@Service
public class PartnerAggregatedDataService extends AggregatedDataService<PartnerAggregatedData, PartnerAggregatedDataRepository> {
    private final MaterialPartnerRelationService mprService;

    public PartnerAggregatedDataService(PartnerAggregatedDataRepository repository, PartnerService partnerService,
            MaterialPartnerRelationService mprService) {
        super(repository, partnerService);
        this.mprService = mprService;
    }

    public final List<PartnerAggregatedData> findAllByMaterial(Material material) {
        return repository.findAll().stream()
                .filter(data -> data.getMaterial().equals(material))
                .toList();
    }

    @Override
    public boolean validate(PartnerAggregatedData aggregatedData) {
        return aggregatedData.getPartner() != null &&
                aggregatedData.getMaterial() != null &&
                validateMaterialPartnerRelation(aggregatedData) &&
                validateCommonFields(aggregatedData);
    }

    private boolean validateMaterialPartnerRelation(PartnerAggregatedData aggregatedData) {
        return mprService.findAll().stream().anyMatch(mpr ->
                mpr.getPartner().equals(aggregatedData.getPartner()) &&
                mpr.getMaterial().equals(aggregatedData.getMaterial()));
    }
}
