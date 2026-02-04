package com.rakshi.domains;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;

/**
 * A single flight segment inside an Order.
 *
 * <p>
 * Multi-leg trips (LHR→CDG→JFK) are modelled as multiple FlightItems in the
 * same Order — one per segment. They are linked by belonging to the same Order,
 * not by a separate itinerary object (we can add that grouping later if
 * needed).
 * </p>
 *
 * <p>
 * Fields are intentionally <b>immutable</b> after construction. If a flight
 * changes, the correct domain action is: cancel this item, create a new one.
 * </p>
 */
@Getter
public class FlightItem extends OrderItem {

    private final String origin;
    private final String destination;
    private final String flightNumber;
    private final Instant departureTime;
    private final Instant arrivalTime; // nullable — we may not always know it upfront

    // -----------------------------------------------------------------
    // Construction (private — use Builder)
    // -----------------------------------------------------------------

    private FlightItem(UUID itemId, ItemStatus status, Price price,
            String origin, String destination,
            String flightNumber, Instant departureTime, Instant arrivalTime) {
        super(itemId, status, price);

        if (origin == null || origin.isBlank() || origin.length() != 3)
            throw new IllegalArgumentException("origin must be a 3-letter IATA code. Got: " + origin);
        if (destination == null || destination.isBlank() || destination.length() != 3)
            throw new IllegalArgumentException("destination must be a 3-letter IATA code. Got: " + destination);
        if (origin.equalsIgnoreCase(destination))
            throw new IllegalArgumentException("origin and destination cannot be the same airport.");
        if (flightNumber == null || flightNumber.isBlank())
            throw new IllegalArgumentException("flightNumber must not be blank.");
        if (departureTime == null)
            throw new IllegalArgumentException("departureTime must not be null.");
        if (arrivalTime != null && !arrivalTime.isAfter(departureTime))
            throw new IllegalArgumentException("arrivalTime must be after departureTime.");

        this.origin = origin.toUpperCase();
        this.destination = destination.toUpperCase();
        this.flightNumber = flightNumber;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
    }

    // -----------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public Instant getDepartureTime() {
        return departureTime;
    }

    public Instant getArrivalTime() {
        return arrivalTime;
    }

    // -----------------------------------------------------------------
    // Builder
    // -----------------------------------------------------------------

    public static class Builder {
        private UUID itemId = newItemId();
        private ItemStatus status = ItemStatus.ACTIVE;
        private Price price;
        private String origin;
        private String destination;
        private String flightNumber;
        private Instant departureTime;
        private Instant arrivalTime;

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

        public Builder origin(String origin) {
            this.origin = origin;
            return this;
        }

        public Builder destination(String destination) {
            this.destination = destination;
            return this;
        }

        public Builder flightNumber(String flightNumber) {
            this.flightNumber = flightNumber;
            return this;
        }

        public Builder departureTime(Instant departureTime) {
            this.departureTime = departureTime;
            return this;
        }

        public Builder arrivalTime(Instant arrivalTime) {
            this.arrivalTime = arrivalTime;
            return this;
        }

        public FlightItem build() {
            return new FlightItem(itemId, status, price, origin, destination,
                    flightNumber, departureTime, arrivalTime);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
