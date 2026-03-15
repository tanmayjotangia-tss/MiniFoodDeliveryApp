package com.fooddeliveryapp.models.users;


public class Admin extends User{

    public Admin(String name, String email, String phone, String password) {
        super(name, email, phone, password);
    }

    public Admin(String id, String name, String email, String phone, String password) {
        super(id, name, email, phone, password);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }
}
