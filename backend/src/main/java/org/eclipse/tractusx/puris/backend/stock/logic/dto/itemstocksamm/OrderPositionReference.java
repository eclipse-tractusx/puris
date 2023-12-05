
package org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * Generated class for Reference to Order Position. Encapsulates the references
 * to identify a position within an order.
 */

@Getter
@Setter
public class OrderPositionReference {

	private String supplierOrderId;

	@NotNull
	private String customerOrderId;

	@NotNull
	private String customerOrderPositionId;

	@JsonCreator
	public OrderPositionReference(@JsonProperty(value = "supplierOrderId") String supplierOrderId,
			@JsonProperty(value = "customerOrderId") String customerOrderId,
			@JsonProperty(value = "customerOrderPositionId") String customerOrderPositionId) {
		super(

		);
		this.supplierOrderId = supplierOrderId;
		this.customerOrderId = customerOrderId;
		this.customerOrderPositionId = customerOrderPositionId;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final OrderPositionReference that = (OrderPositionReference) o;
		return Objects.equals(supplierOrderId, that.supplierOrderId)
				&& Objects.equals(customerOrderId, that.customerOrderId)
				&& Objects.equals(customerOrderPositionId, that.customerOrderPositionId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(supplierOrderId, customerOrderId, customerOrderPositionId);
	}
}
