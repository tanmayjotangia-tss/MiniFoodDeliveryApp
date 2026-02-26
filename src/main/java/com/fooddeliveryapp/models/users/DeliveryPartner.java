package com.fooddeliveryapp.models.users;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class DeliveryPartner implements Serializable {

    private final String id;
    private final String name;
    private double basicPay;
    private boolean available = true;

    public DeliveryPartner(String name, double basicPay) {

        if (name == null || name.isBlank()) throw new IllegalArgumentException("Invalid name");

        if (basicPay < 0) throw new IllegalArgumentException("Invalid basic pay");

        this.id = UUID.randomUUID().toString();
        this.name = name.trim();
        this.basicPay = basicPay;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isAvailable() {
        return available;
    }

    public void updateBasicPay(double newPay) {
        if (newPay < 0) throw new IllegalArgumentException("Invalid pay");
        this.basicPay = newPay;
    }

    public void markUnavailable() {
        available = false;
    }

    public void markAvailable() {
        available = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeliveryPartner)) return false;
        DeliveryPartner that = (DeliveryPartner) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}