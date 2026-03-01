package com.fooddeliveryapp.models.notification;

import com.fooddeliveryapp.services.notification.OrderObserver;

public interface Subject {
    void addObserver(OrderObserver observer);
//    void removeObserver(Observer observer);
    void notifyObservers(String message);
}
