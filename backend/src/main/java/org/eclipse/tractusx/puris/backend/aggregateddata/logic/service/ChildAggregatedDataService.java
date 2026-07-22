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

import org.eclipse.tractusx.puris.backend.aggregateddata.domain.model.ChildAggregatedData;
import org.eclipse.tractusx.puris.backend.aggregateddata.domain.repository.ChildAggregatedDataRepository;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.springframework.stereotype.Service;

@Service
public class ChildAggregatedDataService extends AggregatedDataService<ChildAggregatedData, ChildAggregatedDataRepository> {
    public ChildAggregatedDataService(ChildAggregatedDataRepository repository, PartnerService partnerService) {
        super(repository, partnerService);
    }

    @Override
    public boolean validate(ChildAggregatedData aggregatedData) {
        return aggregatedData.getExternalMaterialNumber() != null &&
                !aggregatedData.getExternalMaterialNumber().isBlank() &&
                aggregatedData.getExternalMaterialName() != null &&
                !aggregatedData.getExternalMaterialName().isBlank() &&
                aggregatedData.getParentData() != null &&
                validateCommonFields(aggregatedData);
    }
    
}
