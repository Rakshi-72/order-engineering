package com.rakshi.domains;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.ToString;

/**
 * Aggregate Root. Single source of truth for a customer's travel purchase.
 *
 * <p>
 * Replaces the legacy PNR + E-Ticket + EMD trio with one cohesive object.
 * All mutations to child {@link OrderItem}s go through this class — that is the
 * core Aggregate Root rule.
 * </p>
 *
 * <h3>ID strategy</h3>
 * 8-character Crockford Base32 string (≈1.1 trillion possibilities).
 * Human-readable, phone-friendly. Uniqueness is enforced by the persistence
 * layer (see {@link OrderRepository}), not here.
 */
@Getter
@ToString
public class Order {

    // -----------------------------------------------------------------
    // ID generation
    // -----------------------------------------------------------------

    /** Crockford charset — no 0/O/I/L to avoid misreading. */
    private static final String ID_CHARSET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final int ID_LENGTH = 8;

    /**
     * Package-private: {@link OrderRepository} calls this on a duplicate-key retry.
     */
    static String generateOrderId() {
        RandomGenerator rng = RandomGenerator.getDefault();
        StringBuilder sb = new StringBuilder(ID_LENGTH);
        for (int i = 0; i < ID_LENGTH; i++)
            sb.append(ID_CHARSET.charAt(rng.nextInt(ID_CHARSET.length())));
        return sb.toString();
    }

    // -----------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------

    private final String orderId;
    private final String customerId;
    private final String customerEmail;
    private OrderStatus status;
    private final List<OrderItem> items; // mutable internally; exposed as unmodifiable
    private final Instant createdAt;
    private Instant updatedAt;

    // -----------------------------------------------------------------
    // Constructors (private / package-private — use Builder)
    // -----------------------------------------------------------------

    /** Normal path: auto-generates the order ID. */
    private Order(String customerId, String customerEmail) {
        this(generateOrderId(), customerId, customerEmail);
    }

    /**
     * Retry path: accepts an externally-provided ID.
     * Package-private — only {@link OrderRepository} should use it.
     */
    Order(String orderId, String customerId, String customerEmail) {
        if (orderId == null || orderId.isBlank())
            throw new IllegalArgumentException("orderId must not be blank.");
        if (customerId == null || customerId.isBlank())
            throw new IllegalArgumentException("customerId must not be blank.");
        if (customerEmail == null || customerEmail.isBlank())
            throw new IllegalArgumentException("customerEmail must not be blank.");

        this.orderId = orderId;
        this.customerId = customerId;
        this.customerEmail = customerEmail;
        this.status = OrderStatus.CREATED;
        this.items = new ArrayList<>();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // -----------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Returns an <b>unmodifiable</b> view. External code cannot bypass
     * {@link #addItem} / {@link #cancelItem} validation this way.
     */
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    // -----------------------------------------------------------------
    // Aggregate root — item management
    // -----------------------------------------------------------------

    public void addItem(OrderItem item) {
        assertNotCancelled();
        if (item == null)
            throw new IllegalArgumentException("Cannot add a null item.");
        items.add(item);
        updatedAt = Instant.now();
    }

    /**
     * Cancels a single item. The Order itself stays in its current status.
     */
    public void cancelItem(UUID itemId) {
        findItemById(itemId).transitionStatus(ItemStatus.CANCELLED);
        updatedAt = Instant.now();
    }

    // -----------------------------------------------------------------
    // Aggregate root — order-level status transitions
    // -----------------------------------------------------------------

    /** CREATED → PENDING_PAYMENT. Refuses if there are no active items. */
    public void initiatePayment() {
        assertNotCancelled();
        if (getActiveItems().isEmpty())
            throw new IllegalStateException("Cannot initiate payment on [" + orderId + "]: no active items.");
        transitionStatus(OrderStatus.PENDING_PAYMENT);
    }

    /** PENDING_PAYMENT → CONFIRMED. */
    public void confirm() {
        transitionStatus(OrderStatus.CONFIRMED);
    }

    /** Cancels the order AND cascades to every non-cancelled item. */
    public void cancel() {
        assertNotCancelled();
        items.stream()
                .filter(i -> i.getStatus() != ItemStatus.CANCELLED)
                .forEach(i -> i.transitionStatus(ItemStatus.CANCELLED));
        status = OrderStatus.CANCELLED;
        updatedAt = Instant.now();
    }

    // -----------------------------------------------------------------
    // Rich domain queries
    // -----------------------------------------------------------------

    /**
     * Derived total — always computed fresh from active items.
     * Avoids the classic "stored total drifts out of sync" bug.
     */
    public Price calculateTotal() {
        List<OrderItem> active = getActiveItems();
        if (active.isEmpty())
            return Price.zero(Currency.getInstance("USD"));

        Currency cur = active.get(0).getPrice().getCurrency();
        if (active.stream().anyMatch(i -> !i.getPrice().getCurrency().equals(cur)))
            throw new IllegalStateException("Mixed currencies in order [" + orderId + "]. Not yet supported.");

        return active.stream().map(OrderItem::getPrice).reduce(Price.zero(cur), Price::add);
    }

    public List<OrderItem> getActiveItems() {
        return items.stream().filter(OrderItem::isActive).collect(Collectors.toList());
    }

    public List<FlightItem> getFlightItems() {
        return items.stream().filter(i -> i instanceof FlightItem).map(i -> (FlightItem) i)
                .collect(Collectors.toList());
    }

    public List<AncillaryItem> getAncillaryItems() {
        return items.stream().filter(i -> i instanceof AncillaryItem).map(i -> (AncillaryItem) i)
                .collect(Collectors.toList());
    }

    // -----------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------

    private void transitionStatus(OrderStatus next) {
        if (!status.canTransitionTo(next))
            throw new IllegalStateException("Invalid transition [" + orderId + "]: " + status + " -> " + next);
        status = next;
        updatedAt = Instant.now();
    }

    private void assertNotCancelled() {
        if (status == OrderStatus.CANCELLED)
            throw new IllegalStateException("Order [" + orderId + "] is CANCELLED. No further operations allowed.");
    }

    private OrderItem findItemById(UUID id) {
        return items.stream().filter(i -> i.getItemId().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No item [" + id + "] in order [" + orderId + "]."));
    }

    // -----------------------------------------------------------------
    // Builder
    // -----------------------------------------------------------------

    public static class Builder {
        private String orderId; // null → auto-generate
        private String customerId;
        private String customerEmail;

        /** Retry path only. Do not call from application code. */
        public Builder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder customerEmail(String customerEmail) {
            this.customerEmail = customerEmail;
            return this;
        }

        public Order build() {
            return (orderId != null)
                    ? new Order(orderId, customerId, customerEmail) // retry path
                    : new Order(customerId, customerEmail); // normal path
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
