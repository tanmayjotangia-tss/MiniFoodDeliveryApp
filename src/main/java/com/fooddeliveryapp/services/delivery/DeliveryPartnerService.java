package com.fooddeliveryapp.services.delivery;

import com.fooddeliveryapp.exception.EntityNotFoundException;
import com.fooddeliveryapp.models.order.OrderStatus;
import com.fooddeliveryapp.models.repository.Repository;
import com.fooddeliveryapp.models.users.DeliveryPartner;
import com.fooddeliveryapp.services.order.OrderService;

import java.util.List;

public class DeliveryPartnerService {

    private final Repository<DeliveryPartner> repository;
    private final OrderService orderService;

    public DeliveryPartnerService(Repository<DeliveryPartner> repository, OrderService orderService) {
        this.repository = repository;
        this.orderService = orderService;
    }

    public void removePartner(String id) {
        repository.delete(id);
    }

    public void updateBasicPay(String id, double newPay) {

        DeliveryPartner partner = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Partner not found"));

        partner.updateBasicPay(newPay);
        repository.save(partner);
    }

    public List<DeliveryPartner> getAllPartners() {
        return repository.findAll();
    }

    public DeliveryPartner findById(String partnerId) {

        if (partnerId == null || partnerId.isBlank()) throw new IllegalArgumentException("Partner ID is required");

        return repository.findById(partnerId)
                .orElseThrow(() -> new EntityNotFoundException("Delivery partner not found with id: " + partnerId));
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
                .mapToDouble(o -> o.getTotalAmount())
                .sum();

        double incentive =
                deliveredRevenue * (partner.getIncentivePercentage() / 100);

        return partner.getBasicPay() + incentive;
    }

    public List<DeliveryPartner> findAll() {
        return repository.findAll();
    }
}
