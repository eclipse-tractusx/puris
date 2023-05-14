package org.eclipse.tractusx.puris.backend.stock.logic.dto.samm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Generated class for Stock of Products of a Supplier. This aspect represents
 * the latest quantities of a supplier's products that are on stock. The stock
 * represent the build-to-order (BTO) stocks already available for the customer.
 */
@Getter
@Setter
public class ProductStock {

    @NotNull
    private Collection<Position> positions;

    @NotNull
    private String materialNumberCustomer;

    @Pattern(regexp = "(^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$)|(^urn:uuid:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$)")
    private Optional<String> materialNumberCatenaX;

    private Optional<String> materialNumberSupplier;

    @JsonCreator
    public ProductStock(@JsonProperty(value = "positions") Collection<Position> positions,
                        @JsonProperty(value = "materialNumberCustomer") String materialNumberCustomer,
                        @JsonProperty(value = "materialNumberCatenaX") Optional<String> materialNumberCatenaX,
                        @JsonProperty(value = "materialNumberSupplier") Optional<String> materialNumberSupplier) {
        super(

        );
        this.positions = positions;
        this.materialNumberCustomer = materialNumberCustomer;
        this.materialNumberCatenaX = materialNumberCatenaX;
        this.materialNumberSupplier = materialNumberSupplier;
    }

    /**
     * Returns Positions
     *
     * @return {@link #positions}
     */
    public Collection<Position> getPositions() {
        return this.positions;
    }

    /**
     * Returns Customer Material Number
     *
     * @return {@link #materialNumberCustomer}
     */
    public String getMaterialNumberCustomer() {
        return this.materialNumberCustomer;
    }

    /**
     * Returns Material UUID Used in Catena-X
     *
     * @return {@link #materialNumberCatenaX}
     */
    public Optional<String> getMaterialNumberCatenaX() {
        return this.materialNumberCatenaX;
    }

    /**
     * Returns Supplier Material Number
     *
     * @return {@link #materialNumberSupplier}
     */
    public Optional<String> getMaterialNumberSupplier() {
        return this.materialNumberSupplier;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ProductStock that = (ProductStock) o;
        return Objects.equals(positions, that.positions)
                && Objects.equals(materialNumberCustomer, that.materialNumberCustomer)
                && Objects.equals(materialNumberCatenaX, that.materialNumberCatenaX)
                && Objects.equals(materialNumberSupplier, that.materialNumberSupplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(positions, materialNumberCustomer, materialNumberCatenaX, materialNumberSupplier);
    }
}
