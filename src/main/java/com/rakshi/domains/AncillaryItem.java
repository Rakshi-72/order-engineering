package com.rakshi.domains;

import java.util.UUID;

import lombok.Getter;

/**
 * An ancillary (non-flight) product inside an Order.
 *
 * <p>
 * Legacy EMDs are gone. Bags, meals, Wi-Fi — they are just another item
 * in the same cart, same payment, same lifecycle.
 * </p>
 *
 * <p>
 * {@link #linkedFlightItemId} is nullable by design: a meal is per-segment
 * (linked), but travel insurance is order-wide (not linked).
 * </p>
 */
@Getter
public class AncillaryItem extends OrderItem {

    private final String name;
    private final AncillaryType type;
    private final UUID linkedFlightItemId; // null → order-level ancillary

    // -----------------------------------------------------------------
    // Construction (private — use Builder)
    // -----------------------------------------------------------------

    private AncillaryItem(UUID itemId, ItemStatus status, Price price,
            String name, AncillaryType type, UUID linkedFlightItemId) {
        super(itemId, status, price);

        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Ancillary name must not be blank.");
        if (type == null)
            throw new IllegalArgumentException("Ancillary type must not be null.");

        this.name = name;
        this.type = type;
        this.linkedFlightItemId = linkedFlightItemId;
    }

    // -----------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------

    public String getName() {
        return name;
    }

    public AncillaryType getType() {
        return type;
    }

    public UUID getLinkedFlightItemId() {
        return linkedFlightItemId;
    }

    /** true when this ancillary is tied to a specific flight segment. */
    public boolean isLinkedToFlight() {
        return linkedFlightItemId != null;
    }

    // -----------------------------------------------------------------
    // Builder
    // -----------------------------------------------------------------

    public static class Builder {
        private UUID itemId = newItemId();
        private ItemStatus status = ItemStatus.ACTIVE;
        private Price price;
        private String name;
        private AncillaryType type;
        private UUID linkedFlightItemId; // defaults to null

        public Builder itemId(UUID itemId) {
            this.itemId = itemId;
            return this;
        }

        public Builder status(ItemStatus status) {
            this.status = status;
            return this;
        }

        public Builder price(Price price) {
            this.price = price;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(AncillaryType type) {
            this.type = type;
            return this;
        }

        public Builder linkedFlightItemId(UUID linkedFlightItemId) {
            this.linkedFlightItemId = linkedFlightItemId;
            return this;
        }

        public AncillaryItem build() {
            return new AncillaryItem(itemId, status, price, name, type, linkedFlightItemId);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
