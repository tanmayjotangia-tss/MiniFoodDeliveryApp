package com.fooddeliveryapp.services;

import com.fooddeliveryapp.exception.EntityNotFoundException;
import com.fooddeliveryapp.models.DeliveryPartner;
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
}
