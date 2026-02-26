package com.fooddeliveryapp.models;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class Admin implements Serializable {

    private final String id;
    private final String name;

    public Admin(String name) {

        if (name == null || name.isBlank()) throw new IllegalArgumentException("Admin name cannot be empty");

        this.id = UUID.randomUUID().toString();
        this.name = name.trim();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Admin)) return false;
        Admin that = (Admin) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
