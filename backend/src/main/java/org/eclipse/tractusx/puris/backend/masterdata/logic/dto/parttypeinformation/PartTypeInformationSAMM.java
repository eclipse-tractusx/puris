
package org.eclipse.tractusx.puris.backend.masterdata.logic.dto.parttypeinformation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Generated class for Part Type Information. A Part Type Information represents
 * an item in the Catena-X Bill of Material (BOM) on a type level in a specific
 * version.
 */
@Getter
@Setter
@NoArgsConstructor
public class PartTypeInformationSAMM {

	@NotNull
	@Pattern(regexp = PatternStore.URN_STRING)
	private String catenaXId;

	@NotNull
	private PartTypeInformationBody partTypeInformation = new PartTypeInformationBody();
	private Set<PartSitesInformationAsPlannedEntity> partSitesInformationAsPlanned = new HashSet<>();

	@JsonCreator
	public PartTypeInformationSAMM(@JsonProperty(value = "catenaXId") String catenaXId,
                                   @JsonProperty(value = "partTypeInformation") PartTypeInformationBody partTypeInformation,
                                   @JsonProperty(value = "partSitesInformationAsPlanned") Set<PartSitesInformationAsPlannedEntity> partSitesInformationAsPlanned) {
		this.catenaXId = catenaXId;
		this.partTypeInformation = partTypeInformation;
		this.partSitesInformationAsPlanned = partSitesInformationAsPlanned;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final PartTypeInformationSAMM that = (PartTypeInformationSAMM) o;
		return Objects.equals(catenaXId, that.catenaXId)
				&& Objects.equals(partTypeInformation, that.partTypeInformation)
				&& Objects.equals(partSitesInformationAsPlanned, that.partSitesInformationAsPlanned);
	}

	@Override
	public int hashCode() {
		return Objects.hash(catenaXId, partTypeInformation, partSitesInformationAsPlanned);
	}
}
