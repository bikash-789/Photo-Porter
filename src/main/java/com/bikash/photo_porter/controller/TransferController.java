package com.bikash.photo_porter.controller;

import com.bikash.photo_porter.dto.TransferRequest;
import com.bikash.photo_porter.exception.TransferException;
import com.bikash.photo_porter.model.PhotoTransfer;
import com.bikash.photo_porter.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
@Slf4j
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> transferPhotos(@Valid @RequestBody TransferRequest request) {
        try {
            log.info("Initiating transfer from {} to {} for {} photos", 
                    request.getSourceEmail(), request.getTargetEmail(), request.getPhotoIds().size());

            transferService.transferPhotos(request.getSourceEmail(), request.getTargetEmail(), request.getPhotoIds());
            
            Map<String, Object> response = Map.of(
                "message", "Transfer initiated successfully",
                "sourceEmail", request.getSourceEmail(),
                "targetEmail", request.getTargetEmail(),
                "photoCount", request.getPhotoIds().size(),
                "status", "IN_PROGRESS"
            );
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid transfer request: {}", e.getMessage());
            throw new TransferException("Invalid transfer request: " + e.getMessage());
        } catch (Exception e) {
            log.error("Transfer failed", e);
            throw new TransferException("Transfer failed: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<PhotoTransfer>> getTransferHistory(@RequestParam String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("Email parameter is required");
            }

            List<PhotoTransfer> transfers = transferService.getTransfersForUser(email);
            log.info("Retrieved {} transfers for user: {}", transfers.size(), email);
            return ResponseEntity.ok(transfers);
        } catch (Exception e) {
            log.error("Failed to get transfer history for user: {}", email, e);
            throw new TransferException("Failed to get transfer history: " + e.getMessage());
        }
    }

    @GetMapping("/{transferId}")
    public ResponseEntity<PhotoTransfer> getTransferById(@PathVariable Long transferId) {
        try {
            PhotoTransfer transfer = transferService.getTransferById(transferId);
            return ResponseEntity.ok(transfer);
        } catch (Exception e) {
            log.error("Failed to get transfer by id: {}", transferId, e);
            throw new TransferException("Failed to get transfer: " + e.getMessage());
        }
    }

    @GetMapping("/status/{transferId}")
    public ResponseEntity<Map<String, Object>> getTransferStatus(@PathVariable Long transferId) {
        try {
            PhotoTransfer transfer = transferService.getTransferById(transferId);

            Map<String, Object> status = Map.of(
                "transferId", transfer.getId(),
                "status", transfer.getStatus(),
                "startedAt", transfer.getStartedAt(),
                "completedAt", transfer.getCompletedAt(),
                "errorMessage", transfer.getErrorMessage()
            );

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Failed to get transfer status for id: {}", transferId, e);
            throw new TransferException("Failed to get transfer status: " + e.getMessage());
        }
    }

    @PostMapping("/{transferId}/retry")
    public ResponseEntity<Map<String, Object>> retryFailedTransfer(@PathVariable Long transferId) {
        try {
            log.info("Retrying failed transfer: {}", transferId);
            transferService.retryFailedTransfer(transferId);
            
            Map<String, Object> response = Map.of(
                "message", "Transfer retry initiated successfully",
                "transferId", transferId,
                "status", "RETRY_INITIATED"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to retry transfer: {}", transferId, e);
            throw new TransferException("Failed to retry transfer: " + e.getMessage());
        }
    }
}
