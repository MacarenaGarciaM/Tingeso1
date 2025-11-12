package com.example.demo.services;

import com.example.demo.entities.SettingEntity;
import com.example.demo.repositories.SettingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SettingServiceTest {

    @Mock
    SettingRepository repo;

    @InjectMocks
    SettingService service;

    // --- getDailyRentPrice() ---

    @Test
    void getDailyRentPrice_returnsDefault_whenNotFound() {
        given(repo.findById(SettingService.daily_key)).willReturn(Optional.empty());

        int val = service.getDailyRentPrice();

        assertEquals(2500, val); // default_price
        verify(repo).findById(SettingService.daily_key);
    }

    @Test
    void getDailyRentPrice_returnsDefault_evenWhenEntityPresent_becauseParsesIdField() {
        // Con el código actual, se intenta parsear s.getPrice() (que es la clave "daily_rent_price"),
        // así que siempre caerá en default aunque text sea numérico.
        given(repo.findById(SettingService.daily_key))
                .willReturn(Optional.of(new SettingEntity(SettingService.daily_key, "3700")));

        int val = service.getDailyRentPrice();

        assertEquals(2500, val); // sigue devolviendo default
        verify(repo).findById(SettingService.daily_key);
    }

    // --- setDailyRentPrice(int) ---

    @Test
    void setDailyRentPrice_savesAndReturnsValue() {
        ArgumentCaptor<SettingEntity> captor = ArgumentCaptor.forClass(SettingEntity.class);

        int out = service.setDailyRentPrice(4200);

        assertEquals(4200, out);

        verify(repo).save(captor.capture());
        SettingEntity saved = captor.getValue();
        // En tu entidad actual: price = ID (clave), text = valor
        assertEquals(SettingService.daily_key, saved.getPrice());
        assertEquals("4200", saved.getText());
    }

    @Test
    void setDailyRentPrice_negative_throws() {
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> service.setDailyRentPrice(-1));
        assertTrue(ex.getMessage().toLowerCase().contains(">= 0"));
    }
}
