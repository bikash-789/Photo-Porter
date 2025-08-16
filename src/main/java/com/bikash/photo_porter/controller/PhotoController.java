package com.bikash.photo_porter.controller;

import com.bikash.photo_porter.exception.UnauthorizedException;
import com.bikash.photo_porter.model.UserToken;
import com.bikash.photo_porter.repository.UserRepository;
import com.bikash.photo_porter.repository.UserTokenRepository;
import com.bikash.photo_porter.service.GooglePhotosClient;
import com.bikash.photo_porter.service.TokenRefreshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/photos")
@RequiredArgsConstructor
@Slf4j
public class PhotoController {

    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final GooglePhotosClient photosClient;
    private final TokenRefreshService tokenRefreshService;

    private UserToken getTokenByEmail(String email) {
        return userRepository.findByEmail(email)
                .flatMap(u -> userTokenRepository.findByUserId(u.getId()))
                .orElseThrow(() -> new UnauthorizedException("User token not found for email: " + email));
    }

    @GetMapping("/albums")
    public ResponseEntity<List<Map<String, Object>>> listAlbums(@RequestParam String email) {
        try {
            UserToken userToken = getTokenByEmail(email);
            String validToken = tokenRefreshService.refreshTokenIfNeeded(userToken);
            
            List<Map<String, Object>> albums = photosClient.listAlbums(validToken);
            log.info("Retrieved {} albums for user: {}", albums.size(), email);
            return ResponseEntity.ok(albums);
        } catch (Exception e) {
            log.error("Failed to list albums for user: {}", email, e);
            throw new RuntimeException("Failed to list albums: " + e.getMessage());
        }
    }

    @GetMapping("/albums/{albumId}/photos")
    public ResponseEntity<List<Map<String, Object>>> listPhotos(@RequestParam String email, @PathVariable String albumId) {
        try {
            UserToken userToken = getTokenByEmail(email);
            String validToken = tokenRefreshService.refreshTokenIfNeeded(userToken);
            
            List<Map<String, Object>> photos = photosClient.listPhotosInAlbum(validToken, albumId);
            log.info("Retrieved {} photos from album {} for user: {}", photos.size(), albumId, email);
            return ResponseEntity.ok(photos);
        } catch (Exception e) {
            log.error("Failed to list photos from album {} for user: {}", albumId, email, e);
            throw new RuntimeException("Failed to list photos: " + e.getMessage());
        }
    }

    @GetMapping("/albums/{albumId}")
    public ResponseEntity<Map<String, Object>> getAlbumDetails(@RequestParam String email, @PathVariable String albumId) {
        try {
            UserToken userToken = getTokenByEmail(email);
            String validToken = tokenRefreshService.refreshTokenIfNeeded(userToken);
            
            // This would need to be implemented in GooglePhotosClient
            Map<String, Object> albumDetails = photosClient.getAlbumDetails(validToken, albumId);
            return ResponseEntity.ok(albumDetails);
        } catch (Exception e) {
            log.error("Failed to get album details for album {} and user: {}", albumId, email, e);
            throw new RuntimeException("Failed to get album details: " + e.getMessage());
        }
    }
}
