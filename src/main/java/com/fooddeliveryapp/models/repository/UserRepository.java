package com.fooddeliveryapp.models.repository;

import com.fooddeliveryapp.models.users.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends Repository<User>{

    void save(User user);

    Optional<User> findByEmail(String email);

    Optional<User> findById(String id);

    List<User> findAll();

    void delete(String id);
}