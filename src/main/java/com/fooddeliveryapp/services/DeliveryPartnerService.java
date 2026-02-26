package com.fooddeliveryapp.services;

import com.fooddeliveryapp.exception.EntityNotFoundException;
import com.fooddeliveryapp.models.users.DeliveryPartner;
import com.fooddeliveryapp.models.repository.Repository;

import java.util.List;

public class DeliveryPartnerService {

    private final Repository<DeliveryPartner> repository;

    public DeliveryPartnerService(Repository<DeliveryPartner> repository) {
        this.repository = repository;
    }

    public void addPartner(DeliveryPartner partner) {
        repository.save(partner);
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

    public List<DeliveryPartner> findAll() {
        return repository.findAll();
    }
}
