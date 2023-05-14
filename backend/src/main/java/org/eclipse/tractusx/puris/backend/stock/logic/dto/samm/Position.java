package org.eclipse.tractusx.puris.backend.stock.logic.dto.samm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Generated class for Position. The Position can be located at several stocks.
 * A position may be anonymous or may reference an position within an order.
 */
public class Position {
    private Optional<OrderPositionReference> orderPositionReference;

    @NotNull
    private XMLGregorianCalendar lastUpdatedOnDateTime;

    @NotNull
    private Collection<AllocatedStock> allocatedStocks;

    @JsonCreator
    public Position(
            @JsonProperty(value = "orderPositionReference") Optional<OrderPositionReference> orderPositionReference,
            @JsonProperty(value = "lastUpdatedOnDateTime") XMLGregorianCalendar lastUpdatedOnDateTime,
            @JsonProperty(value = "allocatedStocks") Collection<AllocatedStock> allocatedStocks) {
        super(

        );
        this.orderPositionReference = orderPositionReference;
        this.lastUpdatedOnDateTime = lastUpdatedOnDateTime;
        this.allocatedStocks = allocatedStocks;
    }

    /**
     * Returns Order Position Reference
     *
     * @return {@link #orderPositionReference}
     */
    public Optional<OrderPositionReference> getOrderPositionReference() {
        return this.orderPositionReference;
    }

    /**
     * Returns Lasted Updated on Date
     *
     * @return {@link #lastUpdatedOnDateTime}
     */
    public XMLGregorianCalendar getLastUpdatedOnDateTime() {
        return this.lastUpdatedOnDateTime;
    }

    /**
     * Returns Allocated Stocks
     *
     * @return {@link #allocatedStocks}
     */
    public Collection<AllocatedStock> getAllocatedStocks() {
        return this.allocatedStocks;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Position that = (Position) o;
        return Objects.equals(orderPositionReference, that.orderPositionReference)
                && Objects.equals(lastUpdatedOnDateTime, that.lastUpdatedOnDateTime)
                && Objects.equals(allocatedStocks, that.allocatedStocks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderPositionReference, lastUpdatedOnDateTime, allocatedStocks);
    }
}
