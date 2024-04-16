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
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.HrefMapping;
import org.eclipse.tractusx.puris.backend.common.edc.domain.model.SubmodelType;
import org.eclipse.tractusx.puris.backend.common.edc.domain.repository.*;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Constructor;
import java.util.Optional;

@Service
@Slf4j
public class EdcContractMappingService {

    @Autowired
    private DtrContractMappingRepository dtrContractMappingRepository;

    @Autowired
    private ItemStockContractMappingRepository itemStockContractMappingRepository;
    @Autowired
    private ItemStockHrefMappingRepositoryRepository itemStockHrefMappingRepository;

    @Autowired
    private PartTypeContractMappingRepository partTypeContractMappingRepository;
    @Autowired
    private PartTypeHrefMappingRepository partTypeHrefMappingRepository;
    

    public ContractMapping getContractMapping(Partner partner, SubmodelType type) {
        return getOrCreateContractMapping(partner, type);
    }

    private ContractMapping getOrCreateContractMapping(Partner partner, SubmodelType type) {
        GeneralContractMappingRepository<? extends ContractMapping> repository = getContractMappingRepository(type);
        ContractMapping entity = repository.findById(partner.getBpnl()).orElse(null);
        if (entity == null) {
            try {
                Constructor<? extends ContractMapping> constructor = repository.getType().getConstructor();
                entity = constructor.newInstance();
                entity.setPartnerBpnl(partner.getBpnl());

                if (type == SubmodelType.DTR) {
                    entity.setPartnerDspUrl(partner.getEdcUrl());
                }
            } catch (Exception e) {
                log.error("Error in GetOrCreateContractMapping for partner " + partner.getBpnl() +
                    " and type " + type, e);
                return null;
            }
        }
        return entity;
    }

    public void saveContractMapping(Partner partner, SubmodelType type, ContractMapping contractMapping) {
        if (!partner.getBpnl().equals(contractMapping.getPartnerBpnl())) {
            throw new IllegalArgumentException("Mismatched partner BPNL");
        }
        GeneralContractMappingRepository<? extends ContractMapping> repository = getContractMappingRepository(type);

        if (type == SubmodelType.DTR) {
            // DTR always uses the default EDC URL from the Partner entity.
            contractMapping.setPartnerDspUrl(partner.getEdcUrl());
        }

        if (contractMapping.getContractId() == null && contractMapping.getAssetId() == null && contractMapping.getPartnerBpnl() == null) {
            throw new IllegalArgumentException("Missing contract data, cannot save: \n" + contractMapping);
        }
        repository.checkedSave(contractMapping);
    }

    public void invalidateContractMapping(Partner partner, SubmodelType type) {
        GeneralContractMappingRepository<? extends ContractMapping> repository = getContractMappingRepository(type);
        repository.deleteById(partner.getBpnl());
    }



    public void saveHrefMapping(Partner partner, SubmodelType type, String key, String value) {
        if (type == SubmodelType.DTR) {
            throw new IllegalArgumentException("No HREF Mapping for DTR type");
        }
        HrefMapping hrefMapping = getOrCreateHrefMapping(partner, type);
        if (hrefMapping != null) {
            hrefMapping.getMaterialToHrefMapping().put(key, value);
            var repository = getHrefMappingRepository(type);
            repository.checkedSave(hrefMapping);
        } else {
            log.error("Failed to save the HREF mapping for " + partner.getBpnl() + " type " + type + " key " + key +
                " value " + value);
        }
    }

    public void invalidateHrefMapping(Partner partner, SubmodelType type, String key) {
        saveHrefMapping(partner, type, key, null);
    }

    public String getHrefMapping(Partner partner, SubmodelType type, String key) {
        HrefMapping hrefMapping = getOrCreateHrefMapping(partner, type);
        if (hrefMapping != null) {
            return hrefMapping.getMaterialToHrefMapping().get(key);
        }
        return null;
    }

    private HrefMapping getOrCreateHrefMapping(Partner partner, SubmodelType type) {
        GeneralHrefMappingRepository<? extends HrefMapping> repository = getHrefMappingRepository(type);
        HrefMapping hrefMapping;
        Optional<? extends HrefMapping> searchResult = repository.findById(partner.getBpnl());
        if (searchResult.isEmpty()) {
            try {
                Constructor<? extends HrefMapping> constructor = repository.getType().getConstructor();
                hrefMapping = constructor.newInstance();
                hrefMapping.setPartnerBpnl(partner.getBpnl());
            } catch (Exception e) {
                log.error("Error in GetOrCreateHrefMapping", e);
                return null;
            }
        } else {
            hrefMapping = searchResult.get();
        }
        return repository.checkedSave(hrefMapping);
    }

    private GeneralHrefMappingRepository<? extends HrefMapping> getHrefMappingRepository(SubmodelType type) {
        GeneralHrefMappingRepository<? extends HrefMapping> repository = switch (type) {
            case DTR -> throw new IllegalArgumentException("No HREF Mapping for DTR type");
            case ITEMSTOCK -> itemStockHrefMappingRepository;
            case PART_TYPE_INFORMATION -> partTypeHrefMappingRepository;
        };
        return repository;
    }

    private GeneralContractMappingRepository<? extends ContractMapping> getContractMappingRepository(SubmodelType type) {
        GeneralContractMappingRepository<? extends ContractMapping> repository = switch (type) {
            case DTR -> dtrContractMappingRepository;
            case ITEMSTOCK -> itemStockContractMappingRepository;
            case PART_TYPE_INFORMATION -> partTypeContractMappingRepository;
        };
        return repository;
    }

}
