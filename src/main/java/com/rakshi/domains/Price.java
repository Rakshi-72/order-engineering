package com.rakshi.domains;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

import lombok.Getter;

/**
 * Immutable Value Object representing a monetary amount in a specific currency.
 * Amounts are normalised to scale 2 (e.g. 199.99) using HALF_UP rounding.
 */
@Getter
public final class Price {

    private final BigDecimal amount;
    private final Currency currency;

    // -----------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------

    private Price(BigDecimal amount, Currency currency) {
        if (amount == null || currency == null)
            throw new IllegalArgumentException("Price amount and currency must not be null.");
        if (amount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Price amount cannot be negative. Received: " + amount);

        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency;
    }

    public static Price of(BigDecimal amount, Currency currency) {
        return new Price(amount, currency);
    }

    public static Price of(double amount, Currency currency) {
        return new Price(BigDecimal.valueOf(amount), currency);
    }

    public static Price zero(Currency currency) {
        return new Price(BigDecimal.ZERO, currency);
    }

    // -----------------------------------------------------------------
    // Rich domain operations
    // -----------------------------------------------------------------

    public Price add(Price other) {
        assertSameCurrency(other);
        return new Price(this.amount.add(other.amount), this.currency);
    }

    public Price subtract(Price other) {
        assertSameCurrency(other);
        return new Price(this.amount.subtract(other.amount), this.currency);
    }

    public Price multiply(int factor) {
        return new Price(this.amount.multiply(BigDecimal.valueOf(factor)), this.currency);
    }

    public boolean isGreaterThan(Price other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    // -----------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getCurrencyCode() {
        return currency.getCurrencyCode();
    }

    // -----------------------------------------------------------------
    // Value-object contract
    // -----------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Price price = (Price) o;
        return amount.compareTo(price.amount) == 0 && currency.equals(price.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }

    // -----------------------------------------------------------------
    private void assertSameCurrency(Price other) {
        if (!this.currency.equals(other.currency))
            throw new IllegalArgumentException(
                    "Cannot perform arithmetic on different currencies: "
                            + this.currency.getCurrencyCode() + " vs " + other.currency.getCurrencyCode());
    }
}
