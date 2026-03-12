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
package org.eclipse.tractusx.puris.backend.dataexchangerequest.domain.model;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.eclipse.tractusx.puris.backend.demandandcapacitynotification.domain.model.ReportedDemandAndCapacityNotification;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
public abstract class DataExchangeRequest {
    @Id
    @GeneratedValue
    private UUID uuid;

    @ManyToOne(optional = false)
    @JoinColumn(name = "notification_uuid", nullable = false, unique = true)
    @ToString.Exclude
    @NotNull
    protected ReportedDemandAndCapacityNotification notification;

    @NotNull
    private CriticalityEnumeration criticality;

    @NotNull
    private Date desiredStartDateTime;

    @NotNull
    private Date desiredEndDateTime;

    @NotEmpty
    protected List<RequestedTypeEnumeration> requestedTypes;

    @NotBlank
    private String text;

    @NotNull
    @Column(nullable = false, updatable = false)
    private Date timestamp;

    @PrePersist
    protected void onCreate() {
        timestamp = new Date();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DataExchangeRequest that = (DataExchangeRequest) o;
        return this.getNotification().getUuid().equals(that.getNotification().getUuid()) &&
        this.getNotification().getUuid().equals(that.getNotification().getUuid()) &&
            Objects.equals(this.getCriticality().getValue(), that.getCriticality().getValue()) &&
            Objects.equals(toInstant(this.getDesiredStartDateTime()), toInstant(that.getDesiredStartDateTime())) &&
            Objects.equals(toInstant(this.getDesiredEndDateTime()), toInstant(that.getDesiredEndDateTime())) &&
            Objects.equals(this.getRequestedTypes(), that.getRequestedTypes()) && Objects.equals(this.getText(), that.getText());
    }

    @Override
    public int hashCode() {
        return Objects.hash(notification, criticality, desiredStartDateTime, desiredEndDateTime, requestedTypes, text);
    }

    private static Instant toInstant(Date d) {
        return d == null ? null : d.toInstant();
    }
}
