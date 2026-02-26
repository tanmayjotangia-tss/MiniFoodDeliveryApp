package com.fooddeliveryapp.models.users;

public class Customer extends User {

    public Customer(String name,
                    String email,
                    String phone,
                    String password) {

        super(name, email, phone, password);
    }

    @Override
    public String getRole() {
        return "CUSTOMER";
    }
}