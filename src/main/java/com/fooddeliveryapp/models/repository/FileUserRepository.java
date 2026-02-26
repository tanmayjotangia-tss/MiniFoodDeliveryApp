package com.fooddeliveryapp.models.repository;

import com.fooddeliveryapp.models.users.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FileUserRepository extends AbstractFileRepository<User> implements UserRepository {

    private List<User> users;

    public FileUserRepository(String filePath) {
        super(filePath);
        this.users = loadFromFile();
    }

    @Override
    public void save(User user) {

        // Enforce unique email
        Optional<User> existing = findByEmail(user.getEmail());

        if (existing.isPresent() && !existing.get().getId().equals(user.getId())) {

            throw new IllegalStateException("Email already registered.");
        }

        // Update if exists
        users.removeIf(u -> u.getId().equals(user.getId()));

        users.add(user);

        saveToFile(users);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return users.stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
    }

    @Override
    public Optional<User> findById(String id) {
        return users.stream().filter(u -> u.getId().equals(id)).findFirst();
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users);
    }

    @Override
    public void delete(String id) {
        users.removeIf(u -> u.getId().equals(id));
        saveToFile(users);
    }
}
