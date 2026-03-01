package com.fooddeliveryapp.models.repository;

import com.fooddeliveryapp.models.cart.Cart;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FileCartRepository
        extends AbstractFileRepository<Cart>
        implements CartRepository {

    private List<Cart> carts;

    public FileCartRepository(String filePath) {
        super(filePath);
        this.carts = loadFromFile();
    }

    @Override
    public void save(Cart cart) {
        carts.removeIf(c ->
                c.getCustomer().getId()
                        .equals(cart.getCustomer().getId())
        );
        carts.add(cart);
        saveToFile(carts);
    }

    @Override
    public Optional<Cart> findByCustomerId(String customerId) {
        return carts.stream()
                .filter(c ->
                        c.getCustomer().getId()
                                .equals(customerId))
                .findFirst();
    }

    @Override
    public List<Cart> findAll() {
        return new ArrayList<>(carts);
    }

    @Override
    public void delete(String cartId) {
        carts.removeIf(c ->
                c.getId().equals(cartId));
        saveToFile(carts);
    }

    @Override
    public void removeItemFromAllCarts(String itemId) {
        for (Cart cart : carts) {
            cart.removeItemIfExists(itemId);
        }
        saveToFile(carts);
    }
}
