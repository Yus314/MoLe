/*
 * Copyright © 2025 Damyan Ivanov.
 * This file is part of MoLe.
 * MoLe is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * MoLe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoLe. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.ktnx.mobileledger.utils.Misc;

/**
 * Represents the display style for currency amounts.
 * Holds information about currency symbol position, spacing, decimal precision, and decimal mark.
 */
public class AmountStyle {
    private final Position commodityPosition;
    private final boolean commoditySpaced;
    private final int precision;
    private final String decimalMark;

    /**
     * Position of the currency symbol relative to the amount
     */
    public enum Position {
        BEFORE,  // Currency symbol before the amount (e.g., "$100")
        AFTER,   // Currency symbol after the amount (e.g., "100円")
        NONE     // No currency symbol
    }

    /**
     * Creates a new AmountStyle with the specified parameters
     *
     * @param commodityPosition Position of the currency symbol
     * @param commoditySpaced   Whether there is a space between currency and amount
     * @param precision         Number of decimal places to display
     * @param decimalMark       Decimal separator character ("." or ",")
     */
    public AmountStyle(@NonNull Position commodityPosition, boolean commoditySpaced,
                       int precision, @NonNull String decimalMark) {
        this.commodityPosition = commodityPosition;
        this.commoditySpaced = commoditySpaced;
        this.precision = precision;
        this.decimalMark = decimalMark;
    }

    /**
     * Creates an AmountStyle from hledger's ParsedStyle JSON object (v1.50+)
     *
     * @param parsedStyle The ParsedStyle object from hledger JSON v1.50+
     * @param currency    The currency symbol (may be null)
     * @return AmountStyle object or null if parsedStyle is null
     */
    @Nullable
    public static AmountStyle fromParsedStyle(
            @Nullable net.ktnx.mobileledger.json.v1_50.ParsedStyle parsedStyle,
            @Nullable String currency) {
        if (parsedStyle == null) {
            return null;
        }

        Position position = determinePosition(parsedStyle.getAscommodityside(), currency);
        boolean spaced = parsedStyle.isAscommodityspaced();
        int precision = parsedStyle.getAsprecision();
        String decimalMark = parsedStyle.getAsdecimalmark();

        return new AmountStyle(position, spaced, precision, decimalMark);
    }

    /**
     * Creates an AmountStyle from hledger's ParsedStyle JSON object (v1.40)
     *
     * @param parsedStyle The ParsedStyle object from hledger JSON v1.40
     * @param currency    The currency symbol (may be null)
     * @return AmountStyle object or null if parsedStyle is null
     */
    @Nullable
    public static AmountStyle fromParsedStyle(
            @Nullable net.ktnx.mobileledger.json.v1_40.ParsedStyle parsedStyle,
            @Nullable String currency) {
        if (parsedStyle == null) {
            return null;
        }

        Position position = determinePosition(parsedStyle.getAscommodityside(), currency);
        boolean spaced = parsedStyle.isAscommodityspaced();
        int precision = parsedStyle.getAsprecision();
        String decimalMark = parsedStyle.getAsdecimalmark();

        return new AmountStyle(position, spaced, precision, decimalMark);
    }

    /**
     * Creates an AmountStyle from hledger's ParsedStyle JSON object (v1.32)
     *
     * @param parsedStyle The ParsedStyle object from hledger JSON v1.32
     * @param currency    The currency symbol (may be null)
     * @return AmountStyle object or null if parsedStyle is null
     */
    @Nullable
    public static AmountStyle fromParsedStyle(
            @Nullable net.ktnx.mobileledger.json.v1_32.ParsedStyle parsedStyle,
            @Nullable String currency) {
        if (parsedStyle == null) {
            return null;
        }

        Position position = determinePosition(parsedStyle.getAscommodityside(), currency);
        boolean spaced = parsedStyle.isAscommodityspaced();
        int precision = parsedStyle.getAsprecision();
        String decimalMark = parsedStyle.getAsdecimalmark();

        return new AmountStyle(position, spaced, precision, decimalMark);
    }

    /**
     * Creates an AmountStyle from hledger's ParsedStyle JSON object (base/old versions)
     *
     * @param parsedStyle The ParsedStyle object from hledger JSON
     * @param currency    The currency symbol (may be null)
     * @return AmountStyle object or null if parsedStyle is null
     */
    @Nullable
    public static AmountStyle fromParsedStyle(
            @Nullable net.ktnx.mobileledger.json.ParsedStyle parsedStyle, @Nullable String currency) {
        if (parsedStyle == null) {
            return null;
        }

        Position position = determinePosition(parsedStyle.getAscommodityside(), currency);
        boolean spaced = parsedStyle.isAscommodityspaced();

        // Handle decimal mark from ParsedStyle
        String decimalMark = ".";
        char decimalPoint = parsedStyle.getAsdecimalpoint();
        if (decimalPoint == ',') {
            decimalMark = ",";
        }
        else if (decimalPoint == '.') {
            decimalMark = ".";
        }

        // Get precision - default to 2 if not specified in older versions
        int precision = 2;

        return new AmountStyle(position, spaced, precision, decimalMark);
    }

    /**
     * Helper method to determine currency position from side character
     */
    private static Position determinePosition(char side, @Nullable String currency) {
        if (currency == null || currency.isEmpty()) {
            return Position.NONE;
        }
        else if (side == 'L') {
            return Position.BEFORE;
        }
        else if (side == 'R') {
            return Position.AFTER;
        }
        else {
            return Position.NONE;
        }
    }

    /**
     * Gets the default AmountStyle based on global settings
     *
     * @param currency The currency symbol (may be null)
     * @return Default AmountStyle for the given currency
     */
    @NonNull
    public static AmountStyle getDefault(@Nullable String currency) {
        Position position;
        Currency.Position globalPos = Data.currencySymbolPosition.getValue();

        if (currency == null || currency.isEmpty()) {
            position = Position.NONE;
        }
        else if (globalPos == Currency.Position.before) {
            position = Position.BEFORE;
        }
        else if (globalPos == Currency.Position.after) {
            position = Position.AFTER;
        }
        else {
            position = Position.NONE;
        }

        Boolean gap = Data.currencyGap.getValue();
        boolean spaced = (gap != null) ? gap : true;

        // Default precision is 2 decimal places
        int precision = 2;

        // Default decimal mark is period
        String decimalMark = ".";

        return new AmountStyle(position, spaced, precision, decimalMark);
    }

    /**
     * Serializes the AmountStyle to a string for database storage
     *
     * @return Serialized string representation
     */
    @NonNull
    public String serialize() {
        String posStr;
        switch (commodityPosition) {
            case BEFORE:
                posStr = "BEFORE";
                break;
            case AFTER:
                posStr = "AFTER";
                break;
            case NONE:
                posStr = "NONE";
                break;
            default:
                posStr = "NONE";
        }

        return String.format("%s:%b:%d:%s", posStr, commoditySpaced, precision,
                Misc.emptyIsNull(decimalMark) == null ? "." : decimalMark);
    }

    /**
     * Deserializes an AmountStyle from a database string
     *
     * @param serialized The serialized string
     * @return AmountStyle object or null if deserialization fails
     */
    @Nullable
    public static AmountStyle deserialize(@Nullable String serialized) {
        if (serialized == null || serialized.isEmpty()) {
            return null;
        }

        try {
            String[] parts = serialized.split(":");
            if (parts.length != 4) {
                return null;
            }

            Position position;
            switch (parts[0]) {
                case "BEFORE":
                    position = Position.BEFORE;
                    break;
                case "AFTER":
                    position = Position.AFTER;
                    break;
                case "NONE":
                    position = Position.NONE;
                    break;
                default:
                    return null;
            }

            boolean spaced = Boolean.parseBoolean(parts[1]);
            int precision = Integer.parseInt(parts[2]);
            String decimalMark = parts[3];

            return new AmountStyle(position, spaced, precision, decimalMark);
        }
        catch (Exception e) {
            return null;
        }
    }

    @NonNull
    public Position getCommodityPosition() {
        return commodityPosition;
    }

    public boolean isCommoditySpaced() {
        return commoditySpaced;
    }

    public int getPrecision() {
        return precision;
    }

    @NonNull
    public String getDecimalMark() {
        return decimalMark;
    }
}
