package com.rakshi.domains;

import java.time.Instant;
import java.util.Currency;

/**
 * Runnable entry point.  Exercises the full Day-1 domain model end-to-end.
 *
 * Compile & run (from the project root):
 *   javac -d out  src/com/amadeus/oms/domain/*.java
 *   java  -cp out com.amadeus.oms.domain.Main
 */
public class Main {

    private static final Currency USD = Currency.getInstance("USD");

    public static void main(String[] args) {

        // =====================================================================
        // 1.  Create an Order via the repository (handles ID uniqueness)
        // =====================================================================
        OrderRepository repo  = new OrderRepository();
        Order           order = repo.save(
                Order.builder()
                        .customerId("CUST-98234")
                        .customerEmail("jane.doe@email.com")
                        .build());

        printSep("1. Order Created");
        System.out.println("  Order ID      : " + order.getOrderId());
        System.out.println("  Status        : " + order.getStatus());
        System.out.println("  Customer      : " + order.getCustomerId() + " <" + order.getCustomerEmail() + ">");

        // =====================================================================
        // 2.  Add an outbound flight  LHR → JFK
        // =====================================================================
        FlightItem outbound = FlightItem.builder()
                .price(Price.of(549.99, USD))
                .origin("LHR")
                .destination("JFK")
                .flightNumber("BA178")
                .departureTime(Instant.parse("2026-07-15T11:00:00Z"))
                .arrivalTime(  Instant.parse("2026-07-15T14:30:00Z"))
                .build();

        order.addItem(outbound);
        printSep("2. Flight Added  LHR → JFK");
        System.out.println("  Item ID       : " + outbound.getItemId());
        System.out.println("  Price         : " + outbound.getPrice());

        // =====================================================================
        // 3.  Add a return flight  JFK → LHR
        // =====================================================================
        FlightItem ret = FlightItem.builder()
                .price(Price.of(479.00, USD))
                .origin("JFK")
                .destination("LHR")
                .flightNumber("BA177")
                .departureTime(Instant.parse("2026-07-22T16:00:00Z"))
                .arrivalTime(  Instant.parse("2026-07-23T05:45:00Z"))
                .build();

        order.addItem(ret);
        printSep("3. Flight Added  JFK → LHR");
        System.out.println("  Item ID       : " + ret.getItemId());
        System.out.println("  Price         : " + ret.getPrice());

        // =====================================================================
        // 4.  Add ancillaries  (the old EMD world — now just items in the cart)
        // =====================================================================
        AncillaryItem baggage = AncillaryItem.builder()
                .price(Price.of(45.00, USD))
                .name("Extra Baggage 23 kg")
                .type(AncillaryType.BAGGAGE)
                .linkedFlightItemId(outbound.getItemId())   // per-segment
                .build();

        AncillaryItem meal = AncillaryItem.builder()
                .price(Price.of(28.50, USD))
                .name("Vegetarian Meal")
                .type(AncillaryType.MEAL)
                .linkedFlightItemId(outbound.getItemId())   // per-segment
                .build();

        AncillaryItem wifi = AncillaryItem.builder()
                .price(Price.of(15.00, USD))
                .name("Wi-Fi Pass (Full Trip)")
                .type(AncillaryType.WIFI)
                // no linkedFlightItemId → order-level
                .build();

        order.addItem(baggage);
        order.addItem(meal);
        order.addItem(wifi);

        printSep("4. Ancillaries Added");
        System.out.println("  Baggage       : " + baggage.getPrice() + "  (linked to outbound leg)");
        System.out.println("  Meal          : " + meal.getPrice()    + "   (linked to outbound leg)");
        System.out.println("  Wi-Fi         : " + wifi.getPrice()    + "   (order-level)");

        // =====================================================================
        // 5.  Snapshot the order
        // =====================================================================
        printSep("5. Order Snapshot");
        System.out.println("  Total items   : " + order.getItems().size());
        System.out.println("  Flight legs   : " + order.getFlightItems().size());
        System.out.println("  Ancillaries   : " + order.getAncillaryItems().size());
        System.out.println("  Total price   : " + order.calculateTotal());   // 1 117.49 USD

        // =====================================================================
        // 6.  Cancel the meal  (item-level cancel — order stays untouched)
        // =====================================================================
        order.cancelItem(meal.getItemId());

        printSep("6. Meal Cancelled (item-level)");
        System.out.println("  Meal status   : " + meal.getStatus());          // CANCELLED
        System.out.println("  Order status  : " + order.getStatus());         // still CREATED
        System.out.println("  Active items  : " + order.getActiveItems().size());
        System.out.println("  New total     : " + order.calculateTotal());    // 1 088.99 USD

        // =====================================================================
        // 7.  Progress the order lifecycle  CREATED → PENDING_PAYMENT → CONFIRMED
        // =====================================================================
        order.initiatePayment();
        printSep("7a. Payment Initiated");
        System.out.println("  Status        : " + order.getStatus());

        order.confirm();
        printSep("7b. Order Confirmed");
        System.out.println("  Status        : " + order.getStatus());

        // =====================================================================
        // 8.  Price value-object equality demo
        // =====================================================================
        Price a = Price.of(100.00, USD);
        Price b = Price.of(100.00, USD);
        Price c = Price.of(100.00, Currency.getInstance("EUR"));

        printSep("8. Price Value-Object Equality");
        System.out.println("  USD 100 == USD 100 ?  " + a.equals(b));   // true
        System.out.println("  USD 100 == EUR 100 ?  " + a.equals(c));   // false
        System.out.println("  USD 100 + USD 50   =  " + a.add(Price.of(50, USD)));

        // =====================================================================
        // 9.  Illegal-state demos  (caught and reported cleanly)
        // =====================================================================
        printSep("9. Illegal-State Demos");
        tryAndReport("Cancel an already-cancelled item",
                () -> order.cancelItem(meal.getItemId()));

        tryAndReport("Initiate payment on a CONFIRMED order",
                () -> order.initiatePayment());

        tryAndReport("Add USD + EUR",
                () -> Price.of(10, USD).add(Price.of(10, Currency.getInstance("EUR"))));

        tryAndReport("FlightItem with origin == destination",
                () -> FlightItem.builder()
                        .price(Price.of(100, USD)).origin("JFK").destination("JFK")
                        .flightNumber("XX1").departureTime(Instant.now()).build());

        System.out.println("\n=== All demos complete ===");
    }

    // -----------------------------------------------------------------
    // Formatting helpers
    // -----------------------------------------------------------------

    private static void printSep(String title) {
        System.out.println("\n┌─── " + title + " ───");
    }

    private static void tryAndReport(String label, Runnable action) {
        try {
            action.run();
            System.out.println("  [" + label + "]  — unexpectedly succeeded!");
        } catch (Exception e) {
            System.out.println("  [" + label + "]");
            System.out.println("    → " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
