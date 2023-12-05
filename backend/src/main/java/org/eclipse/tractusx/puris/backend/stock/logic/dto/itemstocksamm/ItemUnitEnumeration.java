
package org.eclipse.tractusx.puris.backend.stock.logic.dto.itemstocksamm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import org.eclipse.esmf.aspectmodel.java.exception.EnumAttributeNotFoundException;
import org.eclipse.esmf.metamodel.datatypes.Curie;

import java.util.Arrays;
import java.util.Optional;

/**
 * Generated class {@link ItemUnitEnumeration}.
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ItemUnitEnumeration {
    UNIT_PIECE(new Curie("unit:piece")),
    UNIT_SET(new Curie("unit:set")),
    UNIT_PAIR(new Curie("unit:pair")),
    UNIT_PAGE(new Curie("unit:page")),
    UNIT_CYCLE(new Curie("unit:cycle")),
    UNIT_KILOWATT_HOUR(new Curie("unit:kilowattHour")),
    UNIT_GRAM(new Curie("unit:gram")),
    UNIT_KILOGRAM(new Curie("unit:kilogram")),
    UNIT_TONNE_METRIC_TON(new Curie("unit:tonneMetricTon")),
    UNIT_TON_US_OR_SHORT_TON_UKORUS(new Curie("unit:tonUsOrShortTonUkorus")),
    UNIT_OUNCE_AVOIRDUPOIS(new Curie("unit:ounceAvoirdupois")),
    UNIT_POUND(new Curie("unit:pound")),
    UNIT_METRE(new Curie("unit:metre")),
    UNIT_CENTIMETRE(new Curie("unit:centimetre")),
    UNIT_KILOMETRE(new Curie("unit:kilometre")),
    UNIT_INCH(new Curie("unit:inch")),
    UNIT_FOOT(new Curie("unit:foot")),
    UNIT_YARD(new Curie("unit:yard")),
    UNIT_SQUARE_CENTIMETRE(new Curie("unit:squareCentimetre")),
    UNIT_SQUARE_METRE(new Curie("unit:squareMetre")),
    UNIT_SQUARE_INCH(new Curie("unit:squareInch")),
    UNIT_SQUARE_FOOT(new Curie("unit:squareFoot")),
    UNIT_SQUARE_YARD(new Curie("unit:squareYard")),
    UNIT_CUBIC_CENTIMETRE(new Curie("unit:cubicCentimetre")),
    UNIT_CUBIC_METRE(new Curie("unit:cubicMetre")),
    UNIT_CUBIC_INCH(new Curie("unit:cubicInch")),
    UNIT_CUBIC_FOOT(new Curie("unit:cubicFoot")),
    UNIT_CUBIC_YARD(new Curie("unit:cubicYard")),
    UNIT_LITRE(new Curie("unit:litre")),
    UNIT_MILLILITRE(new Curie("unit:millilitre")),
    UNIT_HECTOLITRE(new Curie("unit:hectolitre")),
    UNIT_SECOND_UNIT_OF_TIME(new Curie("unit:secondUnitOfTime")),
    UNIT_MINUTE_UNIT_OF_TIME(new Curie("unit:minuteUnitOfTime")),
    UNIT_HOUR_UNIT_OF_TIME(new Curie("unit:hourUnitOfTime")),
    UNIT_DAY(new Curie("unit:day"));

    private Curie value;

    ItemUnitEnumeration(Curie value) {
        this.value = value;
    }

    @JsonCreator
    static ItemUnitEnumeration enumDeserializationConstructor(String value) {
        return fromValue(value).orElseThrow(() -> new EnumAttributeNotFoundException(
            "Tried to parse value \"" + value + "\", but there is no enum field like that in ItemUnitEnumeration"));
    }

    @JsonValue
    public String getValue() {
        return value.getValue();
    }

    public static Optional<ItemUnitEnumeration> fromValue(String value) {
        return Arrays.stream(ItemUnitEnumeration.values()).filter(enumValue -> enumValue.getValue().equals(value)).findFirst();
    }

}
