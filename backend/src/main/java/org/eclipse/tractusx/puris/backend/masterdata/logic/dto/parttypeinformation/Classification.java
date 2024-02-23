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

package org.eclipse.tractusx.puris.backend.masterdata.logic.dto.parttypeinformation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;

import java.util.Objects;

/**
 * Generated class for ClassificationEntity. Encapsulates data related to the
 * classification of the part.
 */

@Getter
@Setter
@NoArgsConstructor
@ToString
public class Classification {

	@NotNull
    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
	private String classificationStandard;

	@NotNull
    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
	private String classificationID;

    @Pattern(regexp = PatternStore.NON_EMPTY_NON_VERTICAL_WHITESPACE_STRING)
	private String classificationDescription;

	@JsonCreator
	public Classification(@JsonProperty(value = "classificationStandard") String classificationStandard,
                          @JsonProperty(value = "classificationID") String classificationID,
                          @JsonProperty(value = "classificationDescription") String classificationDescription) {
		this.classificationStandard = classificationStandard;
		this.classificationID = classificationID;
		this.classificationDescription = classificationDescription;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final Classification that = (Classification) o;
		return Objects.equals(classificationStandard, that.classificationStandard)
				&& Objects.equals(classificationID, that.classificationID)
				&& Objects.equals(classificationDescription, that.classificationDescription);
	}

	@Override
	public int hashCode() {
		return Objects.hash(classificationStandard, classificationID, classificationDescription);
	}
}
