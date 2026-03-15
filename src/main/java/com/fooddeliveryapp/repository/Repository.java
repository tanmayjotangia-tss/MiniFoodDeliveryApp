package com.fooddeliveryapp.repository;

import java.util.List;
import java.util.Optional;

public interface Repository<T> {

    void save(T entity);

    Optional<T> findById(String id);

    List<T> findAll();

    void delete(String id);
}
