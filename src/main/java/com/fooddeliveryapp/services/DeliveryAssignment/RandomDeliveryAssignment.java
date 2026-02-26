package com.fooddeliveryapp.services.DeliveryAssignment;


import com.fooddeliveryapp.exception.InvalidOperationException;
import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.users.DeliveryPartner;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class RandomDeliveryAssignment implements DeliveryAssignmentStrategy {

    private final Random random = new Random();

    @Override
    public DeliveryPartner assign(Order order, List<DeliveryPartner> partners) {

        List<DeliveryPartner> available =
                partners.stream()
                        .filter(DeliveryPartner::isAvailable)
                        .collect(Collectors.toList());

        if (available.isEmpty())
            throw new InvalidOperationException("No delivery partner available");

        return available.get(random.nextInt(available.size()));
    }
}
