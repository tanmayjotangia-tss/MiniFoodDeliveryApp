package com.fooddeliveryapp.models.users;

public class Admin extends User {

    private static Admin instance;

    private Admin(String name,
                  String email,
                  String phone,
                  String password) {

        super(name, email, phone, password);
    }

    public static Admin getInstance(String name,
                                    String email,
                                    String phone,
                                    String password) {

        if (instance == null) {
            instance = new Admin(name, email, phone, password);
        }
        return instance;
    }

    public static Admin getInstance() {
        return instance;
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }
}
