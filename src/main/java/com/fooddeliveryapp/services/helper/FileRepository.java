package com.fooddeliveryapp.services.helper;

import com.fooddeliveryapp.models.repository.Repository;

import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FileRepository<T> implements Repository<T> {

    private final String filePath;
    private final List<T> storage;

    public FileRepository(String filePath) {
        this.filePath = filePath;
        this.storage = loadFromFile();
    }

    @Override
    public void save(T entity) {

        Optional<T> existing = findById(extractId(entity));

        if (existing.isPresent()) {
            storage.remove(existing.get());
        }

        storage.add(entity);
        writeToFile();
    }

    @Override
    public Optional<T> findById(String id) {

        return storage.stream()
                .filter(entity -> extractId(entity)
                        .equals(id)).findFirst();
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(storage);
    }

    @Override
    public void delete(String id) {

        storage.removeIf(entity -> extractId(entity).equals(id));
        writeToFile();
    }

    private List<T> loadFromFile() {

        File file = new File(filePath);

        if (!file.exists()) return new ArrayList<>();

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {

            return (List<T>) ois.readObject();

        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void writeToFile() {

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {

            oos.writeObject(storage);

        } catch (IOException e) {
            throw new RuntimeException("Error writing to file", e);
        }
    }

    private String extractId(T entity) {

        try {
            Method method = entity.getClass().getMethod("getId");
            return (String) method.invoke(entity);

        } catch (Exception e) {
            throw new RuntimeException("Entity must have getId() method");
        }
    }
}