package com.dis.repository;

import com.dis.model.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {

    default String getValueOrDefault(String key, String defaultValue) {
        return findById(key).map(SystemConfig::getConfigValue).orElse(defaultValue);
    }
}
