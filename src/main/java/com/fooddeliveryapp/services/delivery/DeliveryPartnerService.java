package com.fooddeliveryapp.services.delivery;

import com.fooddeliveryapp.exception.EntityNotFoundException;
import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.order.OrderStatus;
import com.fooddeliveryapp.models.users.DeliveryPartner;
import com.fooddeliveryapp.repository.UserRepository;
import com.fooddeliveryapp.services.order.OrderService;

import java.util.List;

public class DeliveryPartnerService {

    private final UserRepository repository;
    private final OrderService orderService;

    public DeliveryPartnerService(UserRepository repository, OrderService orderService) {
        this.repository = repository;
        this.orderService = orderService;
    }

    public void removePartner(String id) {
        repository.delete(id);
    }


    public void updateBasicPay(String id, double newPay) {

        DeliveryPartner partner = repository.findById(id)
                .filter(user -> user instanceof DeliveryPartner)
                .map(user -> (DeliveryPartner) user)
                .orElseThrow(() -> new EntityNotFoundException("Partner not found"));

        partner.updateBasicPay(newPay);
        repository.save(partner);
    }

    public List<DeliveryPartner> getAllPartners() {
        return repository.findAll().stream()
                .filter(u -> u instanceof DeliveryPartner)
                .map(u -> (DeliveryPartner) u)
                .toList();
    }

    public DeliveryPartner findById(String id) {
        return repository.findAll().stream()
                .filter(u -> u instanceof DeliveryPartner)
                .map(u -> (DeliveryPartner) u)
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Delivery partner not found with id: " + id));
    }

    public void updateIncentivePercentage(String id, double percentage) {

        DeliveryPartner partner = findById(id);

        partner.updateIncentivePercentage(percentage);

        repository.save(partner);
    }

    public double calculateEarnings(String partnerId) {

        DeliveryPartner partner = findById(partnerId);

        double deliveredRevenue = orderService
                .getOrdersByPartner(partnerId)
                .stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .mapToDouble(Order::getFinalAmount)
                .sum();

        double incentive = deliveredRevenue * (partner.getIncentivePercentage() / 100);

        return partner.getBasicPay() + incentive;
    }

}
