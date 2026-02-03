package com.rakshi.domains;

/**
 * Per-item lifecycle states. Deliberately separate from {@link OrderStatus}:
 * a single item can be cancelled while the parent order stays CONFIRMED.
 *
 * State diagram:
 *   ACTIVE ──► MODIFICATION_PENDING ──► ACTIVE  (loop)
 *     │               │
 *     ▼               ▼
 *  CANCELLED       CANCELLED
 */
public enum ItemStatus {

    ACTIVE,
    MODIFICATION_PENDING,
    CANCELLED;

    public boolean canTransitionTo(ItemStatus target) {
        return switch (this) {
            case ACTIVE               -> target == MODIFICATION_PENDING || target == CANCELLED;
            case MODIFICATION_PENDING -> target == ACTIVE               || target == CANCELLED;
            case CANCELLED            -> false;
        };
    }
}
