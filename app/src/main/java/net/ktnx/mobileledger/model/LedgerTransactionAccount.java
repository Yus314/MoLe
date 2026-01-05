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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.ktnx.mobileledger.db.TransactionAccount;
import net.ktnx.mobileledger.utils.Misc;

import java.util.Locale;

public class LedgerTransactionAccount {
    private String accountName;
    private String shortAccountName;
    private float amount;
    private boolean amountSet = false;
    @Nullable
    private String currency;
    private String comment;
    private boolean amountValid = true;
    private long dbId;
    @Nullable
    private AmountStyle amountStyle;
    public LedgerTransactionAccount(String accountName, float amount, String currency,
                                    String comment) {
        this(accountName, amount, currency, comment, null);
    }
    public LedgerTransactionAccount(String accountName, float amount, String currency,
                                    String comment, @Nullable AmountStyle amountStyle) {
        this.setAccountName(accountName);
        this.amount = amount;
        this.amountSet = true;
        this.amountValid = true;
        this.currency = Misc.emptyIsNull(currency);
        this.comment = Misc.emptyIsNull(comment);
        this.amountStyle = amountStyle;
    }
    public LedgerTransactionAccount(String accountName) {
        this.accountName = accountName;
    }
    public LedgerTransactionAccount(String accountName, String currency) {
        this.accountName = accountName;
        this.currency = Misc.emptyIsNull(currency);
    }
    public LedgerTransactionAccount(LedgerTransactionAccount origin) {
        // copy constructor
        setAccountName(origin.getAccountName());
        setComment(origin.getComment());
        if (origin.isAmountSet())
            setAmount(origin.getAmount());
        amountValid = origin.amountValid;
        currency = origin.getCurrency();
        amountStyle = origin.amountStyle;
    }
    public LedgerTransactionAccount(TransactionAccount dbo) {
        this(dbo.getAccountName(), dbo.getAmount(), Misc.emptyIsNull(dbo.getCurrency()),
                Misc.emptyIsNull(dbo.getComment()),
                dbo.getAmountStyle() != null ? AmountStyle.deserialize(dbo.getAmountStyle()) :
                        null);
        amountSet = true;
        amountValid = true;
        dbId = dbo.getId();
    }
    public String getComment() {
        return comment;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }
    public String getAccountName() {
        return accountName;
    }
    public void setAccountName(String accountName) {
        this.accountName = accountName;
        shortAccountName = accountName.replaceAll("(?<=^|:)(.)[^:]+(?=:)", "$1");
    }
    public String getShortAccountName() {
        return shortAccountName;
    }
    public float getAmount() {
        if (!amountSet)
            throw new IllegalStateException("Account amount is not set");

        return amount;
    }
    public void setAmount(float account_amount) {
        this.amount = account_amount;
        this.amountSet = true;
        this.amountValid = true;
    }
    public void resetAmount() {
        this.amountSet = false;
        this.amountValid = true;
    }
    public void invalidateAmount() {
        this.amountValid = false;
    }
    public boolean isAmountSet() {
        return amountSet;
    }
    public boolean isAmountValid() { return amountValid; }
    @Nullable
    public String getCurrency() {
        return currency;
    }
    public void setCurrency(String currency) {
        this.currency = Misc.emptyIsNull(currency);
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
    @NonNull
    public String toString() {
        if (!amountSet)
            return "";

        AmountStyle style = getEffectiveStyle();
        StringBuilder sb = new StringBuilder();

        // Currency before amount
        if (currency != null && style.getCommodityPosition() == AmountStyle.Position.BEFORE) {
            sb.append(currency);
            if (style.isCommoditySpaced()) {
                sb.append(' ');
            }
        }

        // Format the amount
        sb.append(formatAmount(amount, style));

        // Currency after amount
        if (currency != null && style.getCommodityPosition() == AmountStyle.Position.AFTER) {
            if (style.isCommoditySpaced()) {
                sb.append(' ');
            }
            sb.append(currency);
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
            // When decimal mark is comma, we need to be careful not to replace thousand separators
            // First replace decimal point with a placeholder, then replace commas, then restore decimal mark
            if (",".equals(decimalMark)) {
                formatted = formatted.replace(".", "DECIMAL_PLACEHOLDER");
                // Thousand separator remains as comma (no change needed)
                formatted = formatted.replace("DECIMAL_PLACEHOLDER", decimalMark);
            } else {
                formatted = formatted.replace(".", decimalMark);
            }
        }

        return formatted;
    }
    public TransactionAccount toDBO() {
        TransactionAccount dbo = new TransactionAccount();
        dbo.setAccountName(accountName);
        if (amountSet)
            dbo.setAmount(amount);
        dbo.setComment(comment);
        dbo.setCurrency(Misc.nullIsEmpty(currency));
        dbo.setId(dbId);

        // Save amount style if present
        if (amountStyle != null) {
            dbo.setAmountStyle(amountStyle.serialize());
        }

        return dbo;
    }
}