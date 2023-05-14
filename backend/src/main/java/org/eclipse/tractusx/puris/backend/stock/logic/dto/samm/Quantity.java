package org.eclipse.tractusx.puris.backend.stock.logic.dto.samm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * Generated class for Quantity. Comprises the number of objects and the unit of
 * measurement for the respective child objects
 */
public class Quantity {

    @NotNull
    private Double quantityNumber;

    @NotNull
    //custom: made Curie a String
    private String measurementUnit;

    @JsonCreator
    public Quantity(@JsonProperty(value = "quantityNumber") Double quantityNumber,
                    @JsonProperty(value = "measurementUnit") String measurementUnit) {
        super(

        );
        this.quantityNumber = quantityNumber;
        this.measurementUnit = measurementUnit;
    }

    /**
     * Returns Quantity Number
     *
     * @return {@link #quantityNumber}
     */
    public Double getQuantityNumber() {
        return this.quantityNumber;
    }

    /**
     * Returns Measurement Unit
     *
     * @return {@link #measurementUnit}
     */
    public String getMeasurementUnit() {
        return this.measurementUnit;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Quantity that = (Quantity) o;
        return Objects.equals(quantityNumber, that.quantityNumber)
                && Objects.equals(measurementUnit, that.measurementUnit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(quantityNumber, measurementUnit);
    }
}
