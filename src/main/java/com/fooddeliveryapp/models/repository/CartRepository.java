package com.fooddeliveryapp.models.repository;

import com.fooddeliveryapp.models.cart.Cart;

import java.util.List;
import java.util.Optional;

public interface CartRepository {

    void save(Cart cart);

    Optional<Cart> findByCustomerId(String customerId);

    List<Cart> findAll();

    void delete(String cartId);
}