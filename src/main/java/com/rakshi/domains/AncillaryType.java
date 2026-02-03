package com.rakshi.domains;

/**
 * Catalogue-level discriminator for ancillary products.
 * New types (LOUNGE_ACCESS, PRIORITY_BOARDING …) are added here as the product grows.
 */
public enum AncillaryType {
    BAGGAGE,
    MEAL,
    WIFI,
    SEAT_UPGRADE,
    TRAVEL_INSURANCE
}
