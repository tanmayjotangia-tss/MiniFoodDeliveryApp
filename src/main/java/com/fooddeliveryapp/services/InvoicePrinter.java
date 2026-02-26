package com.fooddeliveryapp.services;

import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.order.OrderItem;
import com.fooddeliveryapp.models.users.DeliveryPartner;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class InvoicePrinter {

    private static final String RESTAURANT_NAME = "MY RESTAURANT";
    private static final String ADDRESS = "Rajkot, Gujarat, India";
    private static final int WIDTH = 60;

    public void print(Order order) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        String dateTime = LocalDateTime.now().format(formatter);

        printLine('=');
        centerText(RESTAURANT_NAME);
        centerText(ADDRESS);
        printLine('=');

        System.out.printf("Order ID      : %s%n", order.getId());
        System.out.printf("Date & Time   : %s%n", dateTime);
        System.out.printf("Customer Name : %s%n", order.getCustomer().getName());
        printLine('-');

        printTableHeader();
        printLine('-');

        double total = 0;

        for (OrderItem item : order.getItems()) {

            String name = item.getItem().getName();
            int qty = item.getQuantity();
            double price = item.getItem().getPrice();
            double subtotal = item.subtotal();

            total += subtotal;

            System.out.printf("%-22s %-6d %-10.2f %-10.2f%n", trim(name, 22), qty, price, subtotal);
        }

        printLine('-');

        double discount = total - order.getTotalAmount();

        System.out.printf("%-40s ₹%10.2f%n", "Total Amount:", total);
        System.out.printf("%-40s ₹%10.2f%n", "Discount:", discount);
        System.out.printf("%-40s ₹%10.2f%n", "Final Amount:", order.getTotalAmount());

        printLine('-');

        System.out.printf("%-20s : %s%n", "Payment Mode", order.getPaymentMode());

        System.out.printf("%-20s : %s%n", "Order Status", order.getStatus());

        printLine('=');
        centerText("Thank You For Ordering!");
        centerText("Visit Again.");
        printLine('=');
        System.out.println();
    }

    // --------------------------
    // Helper Methods
    // --------------------------

    private static void printLine(char ch) {
        for (int i = 0; i < WIDTH; i++) {
            System.out.print(ch);
        }
        System.out.println();
    }

    private void centerText(String text) {
        int padding = (WIDTH - text.length()) / 2;
        if (padding < 0) padding = 0;
        System.out.printf("%" + (padding + text.length()) + "s%n", text);
    }

    private void printTableHeader() {
        System.out.printf("%-22s %-6s %-10s %-10s%n", "Item", "Qty", "Price", "Subtotal");
    }

    private String trim(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}