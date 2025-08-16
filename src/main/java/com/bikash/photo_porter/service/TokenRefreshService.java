package com.bikash.photo_porter.service;

import com.bikash.photo_porter.model.UserToken;
import com.bikash.photo_porter.repository.UserTokenRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenRefreshService {

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    private final UserTokenRepository userTokenRepository;

    public String refreshTokenIfNeeded(UserToken userToken) {
        if (isTokenExpired(userToken)) {
            try {
                return refreshToken(userToken);
            } catch (Exception e) {
                log.error("Failed to refresh token for user: {}", userToken.getUser().getEmail(), e);
                throw new RuntimeException("Token refresh failed", e);
            }
        }
        return userToken.getAccessToken();
    }

    private boolean isTokenExpired(UserToken userToken) {
        if (userToken.getExpiresIn() == null || userToken.getIssuedAt() == null) {
            return true;
        }
        
        LocalDateTime expirationTime = userToken.getIssuedAt().plusSeconds(userToken.getExpiresIn());
        return LocalDateTime.now().isAfter(expirationTime.minusMinutes(5));
    }

    private String refreshToken(UserToken userToken) throws IOException {
        HttpTransport httpTransport = new NetHttpTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(httpTransport)
                .setJsonFactory(jsonFactory)
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setRefreshToken(userToken.getRefreshToken());

        boolean refreshed = credential.refreshToken();
        if (!refreshed) {
            throw new RuntimeException("Failed to refresh token");
        }

        userToken.setAccessToken(credential.getAccessToken());
        userToken.setExpiresIn(credential.getExpiresInSeconds().intValue());
        userToken.setIssuedAt(LocalDateTime.now());
        userTokenRepository.save(userToken);

        log.info("Token refreshed successfully for user: {}", userToken.getUser().getEmail());
        return credential.getAccessToken();
    }
} 