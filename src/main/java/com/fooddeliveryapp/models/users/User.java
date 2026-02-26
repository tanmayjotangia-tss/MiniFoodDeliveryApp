package com.fooddeliveryapp.models.users;

import java.io.Serializable;
import java.util.UUID;

public abstract class User implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String id;
    protected String name;
    protected String email;
    protected String phone;
    protected String password;

    protected User(String name,
                   String email,
                   String phone,
                   String password) {

        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Name required");

        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Email required");

        if (password == null || password.isBlank())
            throw new IllegalArgumentException("Password required");

        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getName() { return name; }

    public abstract String getRole();
}
