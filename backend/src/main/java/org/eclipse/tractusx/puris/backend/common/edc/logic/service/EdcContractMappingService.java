/*
 * Copyright (c) 2024 Volkswagen AG
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

package org.eclipse.tractusx.puris.backend.common.edc.logic.service;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.ContractMapping;
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.DtrContractMapping;
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.SubmodelType;
import org.eclipse.tractusx.puris.backend.common.edc.domain.repository.DeliveryContractMappingRepository;
import org.eclipse.tractusx.puris.backend.common.edc.domain.repository.DemandContractMappingRepository;
import org.eclipse.tractusx.puris.backend.common.edc.domain.repository.DtrContractMappingRepository;
import org.eclipse.tractusx.puris.backend.common.edc.domain.repository.GeneralContractMappingRepository;
import org.eclipse.tractusx.puris.backend.common.edc.domain.repository.ItemStockContractMappingRepository;
import org.eclipse.tractusx.puris.backend.common.edc.domain.repository.PartTypeContractMappingRepository;
import org.eclipse.tractusx.puris.backend.common.edc.domain.repository.ProductionContractMappingRepository;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Constructor;

@Service
@Slf4j
public class EdcContractMappingService {

    @Autowired
    private DtrContractMappingRepository dtrContractMappingRepository;

    @Autowired
    private ItemStockContractMappingRepository itemStockContractMappingRepository;

    @Autowired
    private ProductionContractMappingRepository productionContractMappingRepository;

    @Autowired
    private DemandContractMappingRepository demandContractMappingRepository;

    @Autowired
    private DeliveryContractMappingRepository deliveryContractMappingRepository;

    @Autowired
    private PartTypeContractMappingRepository partTypeContractMappingRepository;

    private final String SEPARATOR = "\n@\n";

    public String getContractId(Partner partner, SubmodelType type, String assetId, String dspUrl) {
        ContractMapping contractMapping = getOrCreateContractMapping(partner, type);
        return contractMapping.getAssetToContractMapping().get(assetId + SEPARATOR + dspUrl);
    }

    public void putContractId(Partner partner, SubmodelType type, String assetId, String dspUrl, String contractId) {
        ContractMapping contractMapping = getOrCreateContractMapping(partner, type);
        contractMapping.getAssetToContractMapping().put(assetId + SEPARATOR + dspUrl, contractId);
        var repository = getContractMappingRepository(type);
        repository.checkedSave(contractMapping);
    }

    public void putDtrContractData(Partner partner, String dtrAssetId, String dtrContractId) {
        ContractMapping contractMapping = getOrCreateContractMapping(partner, SubmodelType.DTR);
        contractMapping.getAssetToContractMapping().put("dtrContractId", dtrContractId);
        contractMapping.getAssetToContractMapping().put("dtrAssetId", dtrAssetId);
        dtrContractMappingRepository.save((DtrContractMapping) contractMapping);
    }

    /**
     * Returns a String [] containing the assetId at index 0 and
     * the contractId at index 1.
     *
     * @param partner the Partner
     * @return  a String array as described above
     */
    public String [] getDtrAssetAndContractId(Partner partner) {
        ContractMapping contractMapping = getOrCreateContractMapping(partner, SubmodelType.DTR);
        String assetId = contractMapping.getAssetToContractMapping().get("dtrAssetId");
        String contractId = contractMapping.getAssetToContractMapping().get("dtrContractId");
        return new String[] {assetId, contractId};
    }


    private ContractMapping getOrCreateContractMapping(Partner partner, SubmodelType type) {
        GeneralContractMappingRepository<? extends ContractMapping> repository = getContractMappingRepository(type);
        ContractMapping entity = repository.findById(partner.getBpnl()).orElse(null);
        if (entity == null) {
            try {
                Constructor<? extends ContractMapping> constructor = repository.getType().getConstructor();
                entity = constructor.newInstance();
                entity.setPartnerBpnl(partner.getBpnl());
            } catch (Exception e) {
                log.error("Error in GetOrCreateContractMapping for partner " + partner.getBpnl() +
                    " and type " + type, e);
                return null;
            }
        }
        return entity;
    }

    private GeneralContractMappingRepository<? extends ContractMapping> getContractMappingRepository(SubmodelType type) {
        GeneralContractMappingRepository<? extends ContractMapping> repository = switch (type) {
            case DTR -> dtrContractMappingRepository;
            case ITEM_STOCK -> itemStockContractMappingRepository;
            case PRODUCTION -> productionContractMappingRepository;
            case DEMAND -> demandContractMappingRepository;
            case DELIVERY -> deliveryContractMappingRepository;
            case PART_TYPE_INFORMATION -> partTypeContractMappingRepository;
        };
        return repository;
    }

}
