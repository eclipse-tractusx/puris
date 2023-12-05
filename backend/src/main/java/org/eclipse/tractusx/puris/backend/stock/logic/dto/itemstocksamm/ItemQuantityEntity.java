
package org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * Generated class for Item Quantity Entity. Entity for common measurements of
 * an item (mass, count, linear, area, volume, misc) with an unit and a value.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ItemQuantityEntity {

    @NotNull
	private Double value;

	@NotNull
	private ItemUnitEnumeration unit;

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final ItemQuantityEntity that = (ItemQuantityEntity) o;
		return Objects.equals(value, that.value) && Objects.equals(unit, that.unit);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value, unit);
	}
}
