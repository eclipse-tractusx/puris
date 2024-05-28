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

package org.eclipse.tractusx.puris.backend.notification.domain.model;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Site;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Entity
@ToString
public abstract class Notification {
    @Id
    @GeneratedValue
    protected UUID uuid;

    protected UUID relatedNotificationId;

    @ManyToOne()
    @JoinColumn(name = "partner_uuid")
    @ToString.Exclude
    @NotNull
    protected Partner partner;

    @ManyToMany()
    @JoinTable(
        name = "notification_material",
        joinColumns = @JoinColumn(name = "notification_uuid"),
        inverseJoinColumns = @JoinColumn(name = "material_ownMaterialNumber"))
    @ToString.Exclude
    protected List<Material> materials;

    protected String text;

    @NotNull
    protected LeadingRootCauseEnumeration leadingRootCause;

    @NotNull
    protected EffectEnumeration effect;

    @NotNull
    protected StatusEnumeration status;

    @NotNull
    protected Date startDateOfEffect;
    protected Date expectedEndDateOfEffect;

    @ManyToMany()
    @JoinTable(
        name = "notification_affected_sites_sender",
        joinColumns = @JoinColumn(name = "notification_uuid"),
        inverseJoinColumns = @JoinColumn(name = "site_bpns"))
    protected List<Site> affectedSitesSender;

    @ManyToMany()
    @JoinTable(
        name = "notification_affected_sites_recipient",
        joinColumns = @JoinColumn(name = "notification_uuid"),
        inverseJoinColumns = @JoinColumn(name = "site_bpns"))
    protected List<Site> affectedSitesRecipient;

    @NotNull
    protected Date contentChangedAt;
}
