package com.fooddeliveryapp.models.users;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class Customer implements Serializable {

    private final String id;
    private final String name;
    private final String email;

    public Customer(String name, String email) {

        if (name == null || name.isBlank()) throw new IllegalArgumentException("Customer name cannot be empty");

        if (email == null || email.isBlank()) throw new IllegalArgumentException("Customer email cannot be empty");

        this.id = UUID.randomUUID().toString();
        this.name = name.trim();
        this.email = email.trim();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer)) return false;
        Customer that = (Customer) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
