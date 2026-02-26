package com.fooddeliveryapp.models.users;

public class DeliveryPartner extends User {

    private boolean available;
    private double basicPay;

    public DeliveryPartner(String name,
                           String email,
                           String phone,
                           String password,
                           double basicPay) {

        super(name, email, phone, password);
        this.available = true;
        this.basicPay = basicPay;
    }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String getRole() {
        return "DELIVERY_PARTNER";
    }
}