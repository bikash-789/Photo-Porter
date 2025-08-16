package com.bikash.photo_porter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.bikash.photo_porter.dto.TransferMessage;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthCheckService {

    private final DataSource dataSource;
    private final KafkaTemplate<String, TransferMessage> kafkaTemplate;

    public Map<String, Object> checkHealth() {
        Map<String, Object> health = new HashMap<>();
        boolean dbHealthy = checkDatabaseHealth();
        health.put("database", dbHealthy ? "UP" : "DOWN");
        boolean kafkaHealthy = checkKafkaHealth();
        health.put("kafka", kafkaHealthy ? "UP" : "DOWN");
        boolean overallHealthy = dbHealthy && kafkaHealthy;
        health.put("status", overallHealthy ? "UP" : "DOWN");
        
        return health;
    }

    private boolean checkDatabaseHealth() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5);
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return false;
        }
    }

    private boolean checkKafkaHealth() {
        try {
            kafkaTemplate.getDefaultTopic();
            return true;
        } catch (Exception e) {
            log.error("Kafka health check failed", e);
            return false;
        }
    }
} 