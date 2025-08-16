package com.bikash.photo_porter.service;

import com.bikash.photo_porter.dto.GooglePhoto;
import com.bikash.photo_porter.dto.TransferMessage;
import com.bikash.photo_porter.exception.TransferException;
import com.bikash.photo_porter.model.PhotoTransfer;
import com.bikash.photo_porter.model.TransferStatus;
import com.bikash.photo_porter.model.User;
import com.bikash.photo_porter.model.UserToken;
import com.bikash.photo_porter.repository.PhotoTransferRepository;
import com.bikash.photo_porter.repository.UserRepository;
import com.bikash.photo_porter.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {
    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final PhotoTransferRepository photoTransferRepository;
    private final GooglePhotosClient googlePhotosClient;
    private final PhotoTransferProducer kafkaProducer;
    private final TokenRefreshService tokenRefreshService;

    public void transferPhotos(String sourceEmail, String targetEmail, List<String> photoIds) {
        log.info("Starting photo transfer from {} to {} for {} photos", sourceEmail, targetEmail, photoIds.size());
        
        User sourceUser = userRepository.findByEmail(sourceEmail).orElseThrow(() -> new TransferException("Source user not found: " + sourceEmail));
        User targetUser = userRepository.findByEmail(targetEmail).orElseThrow(() -> new TransferException("Target user not found: " + targetEmail));

        UserToken sourceToken = userTokenRepository.findByUserId(sourceUser.getId()).orElseThrow(() -> new TransferException("Source user token not found"));
        UserToken targetToken = userTokenRepository.findByUserId(targetUser.getId()).orElseThrow(() -> new TransferException("Target user token not found"));

        String validSourceToken = tokenRefreshService.refreshTokenIfNeeded(sourceToken);
        String validTargetToken = tokenRefreshService.refreshTokenIfNeeded(targetToken);

        for(String photoId : photoIds) {
            PhotoTransfer pt = new PhotoTransfer();
            pt.setSourceUser(sourceUser);
            pt.setTargetUser(targetUser);
            pt.setPhotoId(photoId);
            pt.setStatus(TransferStatus.IN_PROGRESS);
            pt = photoTransferRepository.save(pt);

            try {
                log.info("Processing photo transfer {} for photo ID: {}", pt.getId(), photoId);
                
                Map<String, Object> photoDetails = getPhotoDetails(validSourceToken, photoId);
                if (photoDetails == null) {
                    throw new TransferException("Photo not found: " + photoId);
                }
                String baseUrl = (String) photoDetails.get("baseUrl");
                String fileName = (String) photoDetails.get("filename");
                if (fileName == null) {
                    fileName = "photo_" + photoId + ".jpg";
                }

                log.info("Downloading photo {} from source", fileName);
                byte[] photoData = googlePhotosClient.downloadPhoto(baseUrl);
                
                if (photoData == null || photoData.length == 0) {
                    throw new TransferException("Failed to download photo: " + photoId);
                }
                log.info("Uploading photo {} to target", fileName);
                googlePhotosClient.uploadPhoto(validTargetToken, photoData, fileName);
                pt.setStatus(TransferStatus.SUCCESS);
                pt.setCompletedAt(LocalDateTime.now());
                photoTransferRepository.save(pt);
                log.info("Successfully transferred photo {} (ID: {})", fileName, photoId);
            } catch (Exception e) {
                log.error("Failed to transfer photo {}: {}", photoId, e.getMessage(), e);
                pt.setStatus(TransferStatus.FAILED);
                pt.setErrorMessage(e.getMessage());
                pt.setCompletedAt(LocalDateTime.now());
                photoTransferRepository.save(pt);
            }
        }
    }

    private Map<String, Object> getPhotoDetails(String accessToken, String photoId) {
        try {
            Map<String, Object> photoDetails = new java.util.HashMap<>();
            photoDetails.put("id", photoId);
            photoDetails.put("baseUrl", "https://photoslibrary.googleapis.com/v1/mediaItems/" + photoId);
            photoDetails.put("filename", "photo_" + photoId + ".jpg");
            return photoDetails;
        } catch (Exception e) {
            log.error("Failed to get photo details for photo ID: {}", photoId, e);
            return null;
        }
    }

    public List<PhotoTransfer> getTransfersForUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new TransferException("User not found: " + email));
        
        List<PhotoTransfer> sourceTransfers = photoTransferRepository.findBySourceUser(user);
        List<PhotoTransfer> targetTransfers = photoTransferRepository.findByTargetUser(user);
        List<PhotoTransfer> all = new ArrayList<>();
        all.addAll(sourceTransfers);
        all.addAll(targetTransfers);
        all.sort((a, b) -> b.getStartedAt().compareTo(a.getStartedAt()));
        
        return all;
    }

    public PhotoTransfer getTransferById(Long transferId) {
        return photoTransferRepository.findById(transferId)
                .orElseThrow(() -> new TransferException("Transfer not found with id: " + transferId));
    }

    public void queuePhotosForTransfer(UserToken source, UserToken target, List<GooglePhoto> photos) {
        log.info("Queueing {} photos for async transfer", photos.size());
        
        for (GooglePhoto photo : photos) {
            try {
                byte[] photoData = googlePhotosClient.downloadPhoto(photo.getBaseUrl());

                TransferMessage message = new TransferMessage();
                message.setPhotoId(photo.getId());
                message.setFileName(photo.getFilename());
                message.setPhotoBytes(photoData);
                message.setSourceUserId(source.getUser().getId());
                message.setTargetUserId(target.getUser().getId());
                message.setTargetAccessToken(target.getAccessToken());

                kafkaProducer.send(message);
                log.info("Queued photo {} for async transfer", photo.getFilename());
                
            } catch (Exception e) {
                log.error("Failed to queue photo {} for transfer: {}", photo.getId(), e.getMessage(), e);
            }
        }
    }

    public void retryFailedTransfer(Long transferId) {
        PhotoTransfer transfer = getTransferById(transferId);
        
        if (transfer.getStatus() != TransferStatus.FAILED) {
            throw new TransferException("Transfer is not in FAILED status");
        }

        log.info("Retrying failed transfer: {}", transferId);
        transfer.setStatus(TransferStatus.IN_PROGRESS);
        transfer.setErrorMessage(null);
        transfer.setCompletedAt(null);
        photoTransferRepository.save(transfer);
        List<String> photoIds = List.of(transfer.getPhotoId());
        transferPhotos(
            transfer.getSourceUser().getEmail(),
            transfer.getTargetUser().getEmail(),
            photoIds
        );
    }
}
