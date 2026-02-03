package com.rakshi.domains;

/**
 * Persistence layer skeleton.  Owns the uniqueness guarantee on Order IDs.
 *
 * <p>The retry contract: if the DB returns a duplicate-key error, generate a
 * fresh ID, rebuild the Order, retry.  The caller gets back the final persisted
 * Order and never knows a collision happened.</p>
 *
 * <p>Actual DB driver calls (Couchbase / MongoDB) replace the stub methods below
 * on a later day.</p>
 */
public class OrderRepository {

    private static final int MAX_RETRIES = 5;

    /**
     * Persists an Order, retrying with a fresh ID on duplicate-key collisions.
     * <b>Use the returned Order as your source of truth</b> — its ID may differ
     * from the one you passed in.
     */
    public Order save(Order order) {
        Order candidate = order;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                persistToDatabase(candidate);
                return candidate;                          // happy path — done

            } catch (DuplicateOrderIdException e) {
                System.out.println("[OrderRepository] Collision on '" + candidate.getOrderId()
                        + "' (attempt " + attempt + "). Generating fresh ID …");

                // Rebuild with a brand-new ID.  Items will need to be re-added by
                // the caller for now; we'll carry them across once we wire real persistence.
                candidate = Order.builder()
                        .orderId(Order.generateOrderId())
                        .customerId(candidate.getCustomerId())
                        .customerEmail(candidate.getCustomerEmail())
                        .build();
            }
        }

        throw new RuntimeException(
                "Could not generate a unique Order ID after " + MAX_RETRIES + " attempts. "
                        + "Investigate RNG or ID-space exhaustion.");
    }

    // -----------------------------------------------------------------
    // Stubs — replaced with real driver on persistence day
    // -----------------------------------------------------------------

    private void persistToDatabase(Order order) {
        // Stub: always succeeds.
        // Real version: driver.insert(order); catch DB duplicate → throw below.
        System.out.println("[OrderRepository] Persisted order: " + order.getOrderId());
    }

    /** Wraps whatever the DB driver throws into a domain-neutral signal. */
    static class DuplicateOrderIdException extends RuntimeException {
        DuplicateOrderIdException(String orderId) { super("Duplicate Order ID: " + orderId); }
    }
}
