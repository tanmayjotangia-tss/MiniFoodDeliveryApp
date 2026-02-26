package com.fooddeliveryapp.services.DeliveryAssignment;

import com.fooddeliveryapp.models.order.Order;
import com.fooddeliveryapp.models.users.DeliveryPartner;

import java.util.List;

public interface DeliveryAssignmentStrategy {

    DeliveryPartner assign(Order order, List<DeliveryPartner> partners);
}
