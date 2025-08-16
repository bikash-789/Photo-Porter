package com.bikash.photo_porter.service;

import com.bikash.photo_porter.dto.TransferMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhotoTransferProducer {

    @Value("${photo.kafka.topic}")
    private String topic;

    private final KafkaTemplate<String, TransferMessage> kafkaTemplate;

    public void send(TransferMessage message) {
        kafkaTemplate.send(topic, message);
        log.info("Published transfer task to Kafka: {}", message.getFileName());
    }
}
