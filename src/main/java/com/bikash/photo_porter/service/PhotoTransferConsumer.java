package com.bikash.photo_porter.service;

import com.bikash.photo_porter.dto.TransferMessage;
import com.bikash.photo_porter.model.Transfer;
import com.bikash.photo_porter.model.TransferStatus;
import com.bikash.photo_porter.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class PhotoTransferConsumer {

    private final GooglePhotosClient googlePhotosClient;
    private final TransferRepository transferRepository;

    @KafkaListener(topics = "${photo.kafka.topic}", groupId = "photo-transfer-group")
    public void consume(TransferMessage message) {
        log.info("Consuming message for photo: {}", message.getFileName());

        Transfer transfer = new Transfer();
        transfer.setPhotoId(message.getPhotoId());
        transfer.setSourceUserId(message.getSourceUserId());
        transfer.setTargetUserId(message.getTargetUserId());
        transfer.setStatus(TransferStatus.IN_PROGRESS.name());
        transfer.setStartedAt(LocalDateTime.now());
        transfer = transferRepository.save(transfer);

        try {
            googlePhotosClient.uploadPhoto(message.getTargetAccessToken(),
                    message.getPhotoBytes(),
                    message.getFileName());

            transfer.setStatus(TransferStatus.SUCCESS.name());
            transfer.setCompletedAt(LocalDateTime.now());
            log.info("Transfer successful: {}", message.getFileName());

        } catch (Exception e) {
            transfer.setStatus(TransferStatus.FAILED.name());
            transfer.setErrorMessage(e.getMessage());
            transfer.setCompletedAt(LocalDateTime.now());
            log.error("Transfer failed: {}", message.getFileName(), e);
        }

        transferRepository.save(transfer);
    }
}
