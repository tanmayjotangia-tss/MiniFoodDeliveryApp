package com.fooddeliveryapp.controllers;

import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.users.DeliveryPartner;
import com.fooddeliveryapp.services.DeliveryPartnerService;
import com.fooddeliveryapp.services.OrderService;
import com.fooddeliveryapp.utils.InputUtil;

import java.util.List;

public class DeliveryPartnerController {

    private final OrderService orderService;
    private final DeliveryPartnerService partnerService;

    public DeliveryPartnerController(OrderService orderService, DeliveryPartnerService partnerService) {
        this.orderService = orderService;
        this.partnerService = partnerService;
    }

    public void start() {

        String partnerId = InputUtil.readString("Enter your Partner ID: ");

        DeliveryPartner partner = partnerService.findById(partnerId);

        if (partner == null) {
            System.out.println("Invalid Partner ID.");
            return;
        }

        while (true) {

            System.out.println("\n=== DELIVERY PARTNER PANEL ===");
            System.out.println("1. View Assigned Orders");
            System.out.println("2. Accept Order");
            System.out.println("3. Mark Delivered");
            System.out.println("4. Back");

            int choice = InputUtil.readInt("Enter choice: ");

            try {

                switch (choice) {

                    case 1 -> viewOrders(partner);

                    case 2 -> acceptOrder(partner);

                    case 3 -> deliverOrder(partner);

                    case 4 -> {
                        return;
                    }

                    default -> System.out.println("Invalid choice.");
                }

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void viewOrders(DeliveryPartner partner) {

        List<Order> orders = orderService.getOrdersByPartner(partner.getId());

        if (orders.isEmpty()) {
            System.out.println("No assigned orders.");
            return;
        }

        System.out.println("\nAssigned Orders:");

        for (int i = 0; i < orders.size(); i++) {

            Order o = orders.get(i);

            System.out.println((i + 1) + ". ID: " + o.getId() + " | Status: " + o.getStatus() + " | Amount: " + o.getTotalAmount());
        }
    }

    private void acceptOrder(DeliveryPartner partner) {
        List<Order> orders = orderService.getOrdersByPartner(partner.getId()).stream().filter(o -> o.getStatus().equals("ASSIGNED")).toList();

        if (orders.isEmpty()) {
            System.out.println("No orders available to accept.");
            return;
        }

        Order selected = selectOrderFromList(orders);

        if (selected == null) return;

        orderService.acceptOrder(selected.getId(), partner.getId());

        System.out.println("Order accepted. Out for delivery.");
    }


    private void deliverOrder(DeliveryPartner partner) {
        List<Order> orders = orderService.getOrdersByPartner(partner.getId()).stream().filter(o -> o.getStatus().equals("OUT_FOR_DELIVERY")).toList();

        if (orders.isEmpty()) {
            System.out.println("No orders ready for delivery.");
            return;
        }

        Order selected = selectOrderFromList(orders);

        if (selected == null) return;

        orderService.deliverOrder(selected.getId(), partner.getId());

        System.out.println("Order delivered successfully.");
        partner.setAvailable(true);
    }

    private Order selectOrderFromList(List<Order> orders) {

        System.out.println("\nSelect Order:");

        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            System.out.println((i + 1) + ". ID: " + o.getId() + " | Status: " + o.getStatus());
        }

        int choice = InputUtil.readInt("Enter choice (0 to cancel): ");

        if (choice == 0) return null;

        if (choice < 1 || choice > orders.size()) {
            System.out.println("Invalid selection.");
            return null;
        }

        return orders.get(choice - 1);
    }


    private DeliveryPartner selectPartner() {

        List<DeliveryPartner> partners = partnerService.findAll();

        if (partners.isEmpty()) {
            return null;
        }

        System.out.println("\n=== SELECT DELIVERY PARTNER ===");

        for (int i = 0; i < partners.size(); i++) {
            DeliveryPartner p = partners.get(i);
            System.out.println((i + 1) + ". " + p.getName() + " | Available: " + p.isAvailable());
        }

        int choice = InputUtil.readInt("Enter choice (0 to cancel): ");

        if (choice == 0) return null;

        if (choice < 1 || choice > partners.size()) {
            System.out.println("Invalid selection.");
            return null;
        }

        return partners.get(choice - 1);
    }
}