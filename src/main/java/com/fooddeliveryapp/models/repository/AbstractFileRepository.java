package com.fooddeliveryapp.models.repository;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractFileRepository<T> {

    private final String filePath;

    protected AbstractFileRepository(String filePath) {
        this.filePath = filePath;
    }

    protected void saveToFile(List<T> data) {
        try (ObjectOutputStream oos =
                     new ObjectOutputStream(
                             new FileOutputStream(filePath))) {

            oos.writeObject(data);

        } catch (IOException e) {
            throw new RuntimeException("Error saving file", e);
        }
    }

    @SuppressWarnings("unchecked")
    protected List<T> loadFromFile() {

        File file = new File(filePath);

        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (ObjectInputStream ois =
                     new ObjectInputStream(
                             new FileInputStream(filePath))) {

            return (List<T>) ois.readObject();

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Error loading file", e);
        }
    }
}