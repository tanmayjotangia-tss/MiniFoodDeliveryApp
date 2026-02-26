package com.fooddeliveryapp.models.users;

public class Customer extends User {
    private String notificationPreference;

    public Customer(String name,
                    String email,
                    String phone,
                    String password,
                    String preference) {

        super(name, email, phone, password);
        this.notificationPreference = preference;
    }

    @Override
    public String getRole() {
        return "CUSTOMER";
    }

    public String getNotificationPreference() {
        return notificationPreference;
    }
}