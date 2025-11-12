package com.example.demo.services;

import com.example.demo.entities.SettingEntity;
import com.example.demo.repositories.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettingService {

    public static final String daily_key = "daily_rent_price";
    private static final int default_price = 2500;

    private final SettingRepository repo;

    @Transactional(readOnly = true)
    public int getDailyRentPrice() {
        return repo.findById(daily_key)
                .map(s -> {
                    try { return Integer.parseInt(s.getPrice()); }
                    catch (Exception e) { return default_price; }
                })
                .orElse(default_price);
    }

    @Transactional
    public int setDailyRentPrice(int value) {
        if (value < 0) throw new IllegalArgumentException("Daily price must be >= 0");
        repo.save(new SettingEntity(daily_key, String.valueOf(value)));
        return value;
    }
}
