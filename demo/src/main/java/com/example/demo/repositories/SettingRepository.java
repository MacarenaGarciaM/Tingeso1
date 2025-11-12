package com.example.demo.repositories;

import com.example.demo.entities.SettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingRepository extends JpaRepository<SettingEntity, String> {}
