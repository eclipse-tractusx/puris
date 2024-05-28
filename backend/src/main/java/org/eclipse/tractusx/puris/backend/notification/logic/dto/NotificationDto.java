/*
Copyright (c) 2024 Volkswagen AG
Copyright (c) 2024 Contributors to the Eclipse Foundation

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

package org.eclipse.tractusx.puris.backend.notification.logic.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.common.util.PatternStore;
import org.eclipse.tractusx.puris.backend.notification.domain.model.EffectEnumeration;
import org.eclipse.tractusx.puris.backend.notification.domain.model.LeadingRootCauseEnumeration;
import org.eclipse.tractusx.puris.backend.notification.domain.model.StatusEnumeration;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class NotificationDto implements Serializable {
    private UUID uuid;

    private UUID relatedNotificationId;

    @Pattern(regexp = PatternStore.BPNL_STRING)
    private String partnerBpnl;

    private List<@Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING) String> affectedMaterialNumbers;

    private String text;

    private LeadingRootCauseEnumeration leadingRootCause;
    private EffectEnumeration effect;

    private StatusEnumeration status;

    private Date startDateOfEffect;
    private Date expectedEndDateOfEffect;

    private List<@Pattern(regexp = PatternStore.BPNS_STRING) String> affectedSitesBpnsSender;
    private List<@Pattern(regexp = PatternStore.BPNS_STRING) String> affectedSitesBpnsRecipient;

    private boolean isReported;
}
