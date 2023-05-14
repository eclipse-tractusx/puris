package org.eclipse.tractusx.puris.backend.stock.logic.dto.samm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * Generated class for Location ID. A location can be described by different
 * kinds of identifiers. Within Catena-X, a location can either be described by
 * a BPNS or BPNA.
 */
public class LocationId {

    @NotNull
    private LocationIdTypeEnum locationIdType;

    @NotNull
    private String locationId;

    @JsonCreator
    public LocationId(@JsonProperty(value = "locationIdType") LocationIdTypeEnum locationIdType,
                      @JsonProperty(value = "locationId") String locationId) {
        super(

        );
        this.locationIdType = locationIdType;
        this.locationId = locationId;
    }

    /**
     * Returns Type of Location ID
     *
     * @return {@link #locationIdType}
     */
    public LocationIdTypeEnum getLocationIdType() {
        return this.locationIdType;
    }

    /**
     * Returns Location ID
     *
     * @return {@link #locationId}
     */
    public String getLocationId() {
        return this.locationId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final LocationId that = (LocationId) o;
        return Objects.equals(locationIdType, that.locationIdType) && Objects.equals(locationId, that.locationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(locationIdType, locationId);
    }
}
