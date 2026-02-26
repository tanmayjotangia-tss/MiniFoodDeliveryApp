package com.fooddeliveryapp.services.DeliveryAssignment;

import com.fooddeliveryapp.models.DeliveryPartner;

import java.util.List;

public interface DeliveryAssignmentStrategy {

    DeliveryPartner assign(List<DeliveryPartner> partners);
}
