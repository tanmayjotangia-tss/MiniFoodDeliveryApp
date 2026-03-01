package com.fooddeliveryapp.models.users;

import java.io.Serializable;

public class Admin extends User implements Serializable {

    public Admin(String name, String email, String phone, String password) {
        super(name, email, phone, password);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }
}
