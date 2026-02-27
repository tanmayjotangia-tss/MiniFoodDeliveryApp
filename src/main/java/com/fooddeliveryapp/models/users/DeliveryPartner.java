package com.fooddeliveryapp.models.users;

public class DeliveryPartner extends User {

    private boolean available;
    private double basicPay;
    private double incentivePercentage;


    public DeliveryPartner(String name, String email, String phone, String password, double basicPay) {

        super(name, email, phone, password);
        this.available = true;
        this.basicPay = basicPay;
        this.incentivePercentage = 5;
    }

    public double getBasicPay() {
        return basicPay;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public Role getRole() {
        return Role.DELIVERY_PARTNER;
    }

    public void updateBasicPay(double newPay) {
        this.basicPay = newPay;
    }

    public double getIncentivePercentage() {
        return incentivePercentage;
    }

    public void updateIncentivePercentage(double percentage) {
        if (percentage < 0) throw new IllegalArgumentException("Invalid percentage");
        this.incentivePercentage = percentage;
    }
}