package com.fooddeliveryapp.services.delivery;

import com.fooddeliveryapp.exception.InvalidOperationException;
import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.users.DeliveryPartner;

import java.util.List;

public class FirstAvailableDeliveryAssignment implements DeliveryAssignmentStrategy{
    @Override
    public DeliveryPartner assign(Order order,
                                  List<DeliveryPartner> partners) {

        return partners.stream()
                .filter(DeliveryPartner::isAvailable)
                .findFirst()
                .orElseThrow(() ->
                        new InvalidOperationException(
                                "No delivery partner available"
                        ));
    }
}
