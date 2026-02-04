package com.rakshi.domains;

import java.util.UUID;

import lombok.Getter;

/**
 * Abstract base for every purchasable item inside an {@link Order}.
 *
 * <p>
 * Shared state (id, status, price) and the status-transition guard live here.
 * Subclasses only add the fields that make them unique.
 * </p>
 *
 * <p>
 * equals / hashCode are on {@code itemId} only — this is <b>entity</b>
 * equality,
 * not value equality. Two items with the same base fields but different types
 * must never compare as equal.
 * </p>
 */
@Getter
public abstract class OrderItem {

    private final UUID itemId;
    private ItemStatus status;
    private final Price price;

    // -----------------------------------------------------------------
    // Construction (called by subclass constructors via super())
    // -----------------------------------------------------------------

    protected OrderItem(UUID itemId, ItemStatus status, Price price) {
        if (itemId == null)
            throw new IllegalArgumentException("itemId must not be null.");
        if (status == null)
            throw new IllegalArgumentException("status must not be null.");
        if (price == null)
            throw new IllegalArgumentException("price must not be null.");

        this.itemId = itemId;
        this.status = status;
        this.price = price;
    }

    // -----------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------

    public UUID getItemId() {
        return itemId;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public Price getPrice() {
        return price;
    }

    // -----------------------------------------------------------------
    // Rich domain behaviour
    // -----------------------------------------------------------------

    /**
     * Transitions this item's status, delegating validation to the
     * {@link ItemStatus} state machine.
     */
    public void transitionStatus(ItemStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus))
            throw new IllegalStateException(
                    "Invalid item status transition [" + itemId + "]: " + this.status + " -> " + newStatus);
        this.status = newStatus;
    }

    /** Convenience: is this item still contributing to the order? */
    public boolean isActive() {
        return status == ItemStatus.ACTIVE;
    }

    // -----------------------------------------------------------------
    // Identity contract
    // -----------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        return itemId.equals(((OrderItem) o).itemId);
    }

    @Override
    public int hashCode() {
        return itemId.hashCode();
    }

    // -----------------------------------------------------------------
    // Helper for subclass builders
    // -----------------------------------------------------------------

    protected static UUID newItemId() {
        return UUID.randomUUID();
    }
}
