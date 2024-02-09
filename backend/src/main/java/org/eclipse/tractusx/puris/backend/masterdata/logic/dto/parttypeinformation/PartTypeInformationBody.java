
package org.eclipse.tractusx.puris.backend.masterdata.logic.dto.parttypeinformation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * Generated class for Part Type Information Entity. Encapsulation for data
 * related to the part type.
 */

@Getter
@Setter
@NoArgsConstructor
public class PartTypeInformationBody {

	@NotNull
	private String manufacturerPartId;

	@NotNull
	private String nameAtManufacturer;
	private LinkedHashSet<ClassificationEntity> partClassification = new LinkedHashSet<>();

	@JsonCreator
	public PartTypeInformationBody(@JsonProperty(value = "manufacturerPartId") String manufacturerPartId,
                                   @JsonProperty(value = "nameAtManufacturer") String nameAtManufacturer,
                                   @JsonProperty(value = "partClassification") LinkedHashSet<ClassificationEntity> partClassification) {
		this.manufacturerPartId = manufacturerPartId;
		this.nameAtManufacturer = nameAtManufacturer;
		this.partClassification = partClassification;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final PartTypeInformationBody that = (PartTypeInformationBody) o;
		return Objects.equals(manufacturerPartId, that.manufacturerPartId)
				&& Objects.equals(nameAtManufacturer, that.nameAtManufacturer)
				&& Objects.equals(partClassification, that.partClassification);
	}

	@Override
	public int hashCode() {
		return Objects.hash(manufacturerPartId, nameAtManufacturer, partClassification);
	}
}
