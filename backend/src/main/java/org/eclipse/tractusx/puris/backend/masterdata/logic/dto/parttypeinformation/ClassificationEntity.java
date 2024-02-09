
package org.eclipse.tractusx.puris.backend.masterdata.logic.dto.parttypeinformation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * Generated class for ClassificationEntity. Encapsulates data related to the
 * classification of the part.
 */

@Getter
@Setter
@NoArgsConstructor
public class ClassificationEntity {

	@NotNull
	private String classificationStandard;

	@NotNull
	private String classificationID;
	private String classificationDescription;

	@JsonCreator
	public ClassificationEntity(@JsonProperty(value = "classificationStandard") String classificationStandard,
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

		final ClassificationEntity that = (ClassificationEntity) o;
		return Objects.equals(classificationStandard, that.classificationStandard)
				&& Objects.equals(classificationID, that.classificationID)
				&& Objects.equals(classificationDescription, that.classificationDescription);
	}

	@Override
	public int hashCode() {
		return Objects.hash(classificationStandard, classificationID, classificationDescription);
	}
}
