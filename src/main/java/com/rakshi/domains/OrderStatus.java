package com.rakshi.domains;

/**
 * Top-level lifecycle states for an {@link Order}.
 *
 * State diagram:
 *   CREATED ──► PENDING_PAYMENT ──► CONFIRMED
 *      │              │                  │
 *      ▼              ▼                  ▼
 *   CANCELLED      CANCELLED         CANCELLED
 */
public enum OrderStatus {

    CREATED,
    PENDING_PAYMENT,
    CONFIRMED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case CREATED         -> target == PENDING_PAYMENT || target == CANCELLED;
            case PENDING_PAYMENT -> target == CONFIRMED       || target == CANCELLED;
            case CONFIRMED       -> target == CANCELLED;
            case CANCELLED       -> false;
        };
    }
}
