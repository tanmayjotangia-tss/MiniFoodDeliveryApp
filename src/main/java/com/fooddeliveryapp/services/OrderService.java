package com.fooddeliveryapp.services;

import com.fooddeliveryapp.models.Customer;
import com.fooddeliveryapp.models.DeliveryPartner;
import com.fooddeliveryapp.models.PaymentMode;
import com.fooddeliveryapp.models.cart.Cart;
import com.fooddeliveryapp.models.cart.CartItem;
import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.order.OrderItem;
import com.fooddeliveryapp.models.order.OrderStatus;
import com.fooddeliveryapp.models.repository.Repository;
import com.fooddeliveryapp.services.DeliveryAssignment.DeliveryAssignmentStrategy;
import com.fooddeliveryapp.services.payment.PaymentStrategy;

import java.util.List;

public class OrderService {

    private final Repository<Order> orderRepository;
    private final DiscountService discountService;
    private final Repository<DeliveryPartner> partnerRepository;
    private final DeliveryAssignmentStrategy deliveryStrategy;

    public OrderService(Repository<Order> orderRepository,
                        DiscountService discountService,
                        Repository<DeliveryPartner> partnerRepository,
                        DeliveryAssignmentStrategy deliveryStrategy) {

        this.orderRepository = orderRepository;
        this.discountService = discountService;
        this.partnerRepository = partnerRepository;
        this.deliveryStrategy = deliveryStrategy;
    }

    public Order createOrder(Customer customer) {
        return new Order(customer);
    }

    public void addItem(Order order, OrderItem item) {
        order.addItem(item);
    }

    public Order checkoutCart(Cart cart,
                              PaymentStrategy paymentStrategy,
                              PaymentMode mode) {

        if (cart.getItems().isEmpty())
            throw new IllegalStateException("Cart is empty");

        Order order = new Order(cart.getCustomer());

        for (CartItem cartItem : cart.getItems()) {

            OrderItem orderItem =
                    new OrderItem(cartItem.getItem(),
                            cartItem.getQuantity());

            order.addItem(orderItem);
        }

        double discount = discountService
                .getCurrentStrategy()
                .calculate(order.getFinalAmount());

        order.applyDiscount(discount);

        paymentStrategy.pay(order.getFinalAmount());

        order.markPaid(mode);

        List<DeliveryPartner> partners =
                partnerRepository.findAll();

        DeliveryPartner assigned =
                deliveryStrategy.assign(partners);

        order.assignDeliveryPartner(assigned);

        orderRepository.save(order);

        cart.clearCart();

        return order;
    }

    public double calculateTotalRevenue() {

        return orderRepository.findAll().stream()
                .filter(order -> order.getStatus() != OrderStatus.CREATED)
                .mapToDouble(Order::getFinalAmount)
                .sum();
    }

    public long getTotalOrders() {
        return orderRepository.findAll().size();
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
