
package org.eclipse.tractusx.puris.backend.masterdata.logic.dto.parttypeinformation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.tractusx.puris.backend.common.util.PatternStore;

import java.util.Objects;
import java.util.Optional;

/**
 * Generated class for Part Sites Information as Planned Entity. Describes the
 * ID, function and validity date of a site for the associated part in the
 * AsPlanned context.
 */
@Getter
@Setter
@NoArgsConstructor
public class PartSitesInformationAsPlannedEntity {

	@NotNull
	@Pattern(regexp = PatternStore.BPNS_STRING)

	private String catenaXsiteId;

	@NotNull
	private FunctionCharacteristic function;
	private Optional<@Pattern(regexp = "^(?:[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(?:[.][0-9]+)?Z|[0-9]{4}-[0-9]{2}-[0-9]{2}(?:T[0-9]{2}:[0-9]{2}:[0-9]{2}(?:[.][0-9]+)?(?:Z|[+-][0-9]{2}:[0-9]{2}))?)$") String> functionValidFrom;
	private Optional<@Pattern(regexp = "^(?:[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(?:[.][0-9]+)?Z|[0-9]{4}-[0-9]{2}-[0-9]{2}(?:T[0-9]{2}:[0-9]{2}:[0-9]{2}(?:[.][0-9]+)?(?:Z|[+-][0-9]{2}:[0-9]{2}))?)$") String> functionValidUntil;

	@JsonCreator
	public PartSitesInformationAsPlannedEntity(@JsonProperty(value = "catenaXsiteId") String catenaXsiteId,
                                               @JsonProperty(value = "function") FunctionCharacteristic function,
                                               @JsonProperty(value = "functionValidFrom") Optional<String> functionValidFrom,
                                               @JsonProperty(value = "functionValidUntil") Optional<String> functionValidUntil) {
		this.catenaXsiteId = catenaXsiteId;
		this.function = function;
		this.functionValidFrom = functionValidFrom;
		this.functionValidUntil = functionValidUntil;
	}

	/**
	 * Returns Catena-X site identifier
	 *
	 * @return {@link #catenaXsiteId}
	 */
	public String getCatenaXsiteId() {
		return this.catenaXsiteId;
	}

	/**
	 * Returns Function
	 *
	 * @return {@link #function}
	 */
	public FunctionCharacteristic getFunction() {
		return this.function;
	}

	/**
	 * Returns Function valid from
	 *
	 * @return {@link #functionValidFrom}
	 */
	public Optional<String> getFunctionValidFrom() {
		return this.functionValidFrom;
	}

	/**
	 * Returns Function valid until
	 *
	 * @return {@link #functionValidUntil}
	 */
	public Optional<String> getFunctionValidUntil() {
		return this.functionValidUntil;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final PartSitesInformationAsPlannedEntity that = (PartSitesInformationAsPlannedEntity) o;
		return Objects.equals(catenaXsiteId, that.catenaXsiteId) && Objects.equals(function, that.function)
				&& Objects.equals(functionValidFrom, that.functionValidFrom)
				&& Objects.equals(functionValidUntil, that.functionValidUntil);
	}

	@Override
	public int hashCode() {
		return Objects.hash(catenaXsiteId, function, functionValidFrom, functionValidUntil);
	}
}
