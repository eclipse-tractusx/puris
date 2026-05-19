/*
 * Copyright (c) 2026 Volkswagen AG
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
package org.eclipse.tractusx.puris.backend.common.migration;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MigrationTask implements Comparable<MigrationTask> {
    @Id
    private UUID id;
    @NotNull
    private String targetVersion;
    @NotNull
    private MigrationTaskStatusEnumeration status;
    private String logs;

    /**
     * Compares this task to another by semantic version (MAJOR.MINOR.PATCH).
     * Falls back to lexicographic comparison when either version does not
     * conform to semantic versioning (e.g. contains non-numeric segments or
     * is null).
     */
    @Override
    public int compareTo(MigrationTask other) {
        if (this.targetVersion == null && other.targetVersion == null) return 0;
        if (this.targetVersion == null) return -1;
        if (other.targetVersion == null) return 1;

        String[] thisParts = this.targetVersion.split("\\.");
        String[] otherParts = other.targetVersion.split("\\.");
        int length = Math.max(thisParts.length, otherParts.length);
        for (int i = 0; i < length; i++) {
            String thisSegment = i < thisParts.length ? thisParts[i] : "0";
            String otherSegment = i < otherParts.length ? otherParts[i] : "0";
            try {
                int thisPart = Integer.parseInt(thisSegment);
                int otherPart = Integer.parseInt(otherSegment);
                if (thisPart != otherPart) {
                    return Integer.compare(thisPart, otherPart);
                }
            } catch (NumberFormatException e) {
                // Non-numeric segment — fall back to lexicographic comparison for the whole version
                return this.targetVersion.compareTo(other.targetVersion);
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MigrationTask other = (MigrationTask) obj;
        return targetVersion.equals(other.targetVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, targetVersion);
    }
    
}
