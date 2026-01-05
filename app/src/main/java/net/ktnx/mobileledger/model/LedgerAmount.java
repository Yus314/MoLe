/*
 * Copyright © 2021 Damyan Ivanov.
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

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.ktnx.mobileledger.dao.AccountValueDAO;
import net.ktnx.mobileledger.db.Account;
import net.ktnx.mobileledger.db.AccountValue;
import net.ktnx.mobileledger.db.DB;

import java.util.Locale;

public class LedgerAmount {
    private final String currency;
    private final float amount;
    @Nullable
    private AmountStyle amountStyle;
    private long dbId;

    public LedgerAmount(float amount, @NonNull String currency) {
        this.currency = currency;
        this.amount = amount;
    }
    public LedgerAmount(float amount, @NonNull String currency, @Nullable AmountStyle amountStyle) {
        this.currency = currency;
        this.amount = amount;
        this.amountStyle = amountStyle;
    }
    public LedgerAmount(float amount) {
        this.amount = amount;
        this.currency = null;
    }
    static public LedgerAmount fromDBO(AccountValue dbo) {
        AmountStyle style = null;
        if (dbo.getAmountStyle() != null) {
            style = AmountStyle.deserialize(dbo.getAmountStyle());
        }
        final LedgerAmount ledgerAmount = new LedgerAmount(dbo.getValue(), dbo.getCurrency(), style);
        ledgerAmount.dbId = dbo.getId();
        return ledgerAmount;
    }
    public AccountValue toDBO(Account account) {
        final AccountValueDAO dao = DB.get()
                                      .getAccountValueDAO();
        AccountValue obj = new AccountValue();
        obj.setId(dbId);
        obj.setAccountId(account.getId());

        obj.setCurrency(currency);
        obj.setValue(amount);

        // Save amount style if present
        if (amountStyle != null) {
            obj.setAmountStyle(amountStyle.serialize());
        }

        return obj;
    }
    @Nullable
    public AmountStyle getAmountStyle() {
        return amountStyle;
    }
    public void setAmountStyle(@Nullable AmountStyle amountStyle) {
        this.amountStyle = amountStyle;
    }
    @NonNull
    public AmountStyle getEffectiveStyle() {
        if (amountStyle != null) {
            return amountStyle;
        }
        return AmountStyle.getDefault(currency);
    }
    @SuppressLint("DefaultLocale")
    @NonNull
    public String toString() {
        AmountStyle style = getEffectiveStyle();
        StringBuilder sb = new StringBuilder();

        if (currency == null) {
            // No currency, just format the amount
            sb.append(formatAmount(amount, style));
        } else {
            // Currency before amount
            if (style.getCommodityPosition() == AmountStyle.Position.BEFORE) {
                sb.append(currency);
                if (style.isCommoditySpaced()) {
                    sb.append(' ');
                }
            }

            // Format the amount
            sb.append(formatAmount(amount, style));

            // Currency after amount
            if (style.getCommodityPosition() == AmountStyle.Position.AFTER) {
                if (style.isCommoditySpaced()) {
                    sb.append(' ');
                }
                sb.append(currency);
            }
        }

        return sb.toString();
    }

    /**
     * Formats the amount value according to the given style
     */
    private String formatAmount(float amount, AmountStyle style) {
        int precision = style.getPrecision();
        String decimalMark = style.getDecimalMark();

        // Check if amount is effectively an integer
        boolean isInteger = Math.abs(amount - Math.round(amount)) < 0.001f;

        // For zero precision and integer amounts, format as integer
        if (precision == 0 && isInteger) {
            return String.format(Locale.US, "%,d", Math.round(amount));
        }

        // Format with specified precision
        String pattern = "%,." + precision + "f";
        String formatted = String.format(Locale.US, pattern, amount);

        // Replace decimal mark if needed
        if (!".".equals(decimalMark)) {
            if (",".equals(decimalMark)) {
                formatted = formatted.replace(".", "DECIMAL_PLACEHOLDER");
                formatted = formatted.replace("DECIMAL_PLACEHOLDER", decimalMark);
            } else {
                formatted = formatted.replace(".", decimalMark);
            }
        }

        return formatted;
    }
    public void propagateToAccount(@NonNull LedgerAccount acc) {
        if (currency != null)
            acc.addAmount(amount, currency);
        else
            acc.addAmount(amount);
    }
    public String getCurrency() {
        return currency;
    }
    public float getAmount() {
        return amount;
    }
}
