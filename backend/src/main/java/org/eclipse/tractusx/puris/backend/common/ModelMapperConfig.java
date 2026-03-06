/*
 * Copyright (c) 2026 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
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
package org.eclipse.tractusx.puris.backend.common;

import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.domain.model.PartnerDataUpdateBatchRun;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.domain.model.PartnerDataUpdateBatchRunEntry;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.logic.dto.PartnerDataUpdateBatchRunDto;
import org.eclipse.tractusx.puris.backend.batch.partnerdataupdate.logic.dto.PartnerDataUpdateBatchRunEntryDto;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration.AccessLevel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Configuration
@Slf4j
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mm = new ModelMapper();

        mm.getConfiguration()
            .setFieldMatchingEnabled(true)
            .setFieldAccessLevel(AccessLevel.PRIVATE);

        Converter<PartnerDataUpdateBatchRun, PartnerDataUpdateBatchRunDto> runConverter = ctx -> {
            PartnerDataUpdateBatchRun src = ctx.getSource();
            if (src == null) return null;
            long duration = 0;
            if (src.getStartTime() != null && src.getEndTime() != null) {
                duration = Duration.between(src.getStartTime(), src.getEndTime()).getSeconds();
            }
            
            return PartnerDataUpdateBatchRunDto.builder()
                    .id(src.getId())
                    .startTime(src.getStartTime())
                    .endTime(src.getEndTime())
                    .durationInSeconds(duration)
                    .status(src.getStatus())
                    .totalEntries(src.getTotalEntries())
                    .totalErrorCount(src.getTotalErrorCount())
                    .build();
        };

        Converter<PartnerDataUpdateBatchRunEntry, PartnerDataUpdateBatchRunEntryDto> entryConverter = ctx -> {
            PartnerDataUpdateBatchRunEntry src = ctx.getSource();
            if (src == null) return null;
            return PartnerDataUpdateBatchRunEntryDto.builder()
                    .id(src.getId())
                    .ownMaterialNumber(src.getOwnMaterialNumber())
                    .partnerBpnl(src.getPartnerBpnl())
                    .partnerName(src.getPartnerName())
                    .direction(src.getDirection())
                    .informationType(src.getInformationType())
                    .status(src.getStatus())
                    .errorMessage(src.getErrorMessage())
                    .build();
        };

        mm.createTypeMap(PartnerDataUpdateBatchRun.class, PartnerDataUpdateBatchRunDto.class).setConverter(runConverter);
        mm.createTypeMap(PartnerDataUpdateBatchRunEntry.class, PartnerDataUpdateBatchRunEntryDto.class).setConverter(entryConverter);

        // mm.addConverter(runConverter);
        // mm.addConverter(entryConverter);

        return mm;
    }
}

