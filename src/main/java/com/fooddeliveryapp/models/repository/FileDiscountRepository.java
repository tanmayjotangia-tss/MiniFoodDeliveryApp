package com.fooddeliveryapp.models.repository;

import com.fooddeliveryapp.services.discount.TieredPercentageDiscount;
import java.util.List;

public class FileDiscountRepository
        extends AbstractFileRepository<TieredPercentageDiscount> {

    public FileDiscountRepository(String filePath) {
        super(filePath);
    }

    public TieredPercentageDiscount load() {

        List<TieredPercentageDiscount> list = loadFromFile();

        if (list.isEmpty()) {
            return new TieredPercentageDiscount();
        }

        return list.get(0);
    }

    public void save(TieredPercentageDiscount discount) {
        saveToFile(List.of(discount));
    }
}