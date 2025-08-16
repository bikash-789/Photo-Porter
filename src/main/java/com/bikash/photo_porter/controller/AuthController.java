package com.bikash.photo_porter.controller;

import com.bikash.photo_porter.exception.UnauthorizedException;
import com.bikash.photo_porter.model.User;
import com.bikash.photo_porter.model.UserToken;
import com.bikash.photo_porter.repository.UserRepository;
import com.bikash.photo_porter.repository.UserTokenRepository;
import com.bikash.photo_porter.service.TokenRefreshService;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.redirect.uri}")
    private String redirectUri;

    private static final String SCOPE = "https://www.googleapis.com/auth/photoslibrary.readonly https://www.googleapis.com/auth/photoslibrary.appendonly";
    private static final String AUTH_URI = "https://accounts.google.com/o/oauth2/auth";

    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final TokenRefreshService tokenRefreshService;

    @GetMapping("/login")
    public ResponseEntity<Void> googleLogin() throws UnsupportedEncodingException {
        String authUrl = AUTH_URI + "?" +
                "client_id=" + clientId + "&" +
                "redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) + "&" +
                "scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8) + "&" +
                "response_type=code&" +
                "access_type=offline&" +
                "prompt=consent";

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(authUrl)).build();
    }

    @GetMapping("/callback")
    public ResponseEntity<Map<String, Object>> oauthCallback(@RequestParam("code") String code) throws IOException {
        try {
            HttpTransport httpTransport = new NetHttpTransport();
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    httpTransport, jsonFactory,
                    clientId, clientSecret,
                    code, redirectUri)
                    .execute();

            String accessToken = tokenResponse.getAccessToken();
            String refreshToken = tokenResponse.getRefreshToken();

            GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
            Oauth2 oauth2 = new Oauth2.Builder(httpTransport, jsonFactory, credential).setApplicationName("Google Photos Transfer").build();
            Userinfo userInfo = oauth2.userinfo().get().execute();

            String email = userInfo.getEmail();
            User user = userRepository.findByEmail(email).orElseGet(() -> {
                User u = new User();
                u.setEmail(email);
                return userRepository.save(u);
            });

            UserToken userToken = new UserToken();
            userToken.setUser(user);
            userToken.setAccessToken(accessToken);
            userToken.setRefreshToken(refreshToken);
            userToken.setTokenType(tokenResponse.getTokenType());
            userToken.setExpiresIn(tokenResponse.getExpiresInSeconds().intValue());
            userTokenRepository.save(userToken);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Authentication successful");
            response.put("email", email);
            response.put("userId", user.getId());

            log.info("User authenticated successfully: {}", email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Authentication failed", e);
            throw new UnauthorizedException("Authentication failed: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(@RequestParam String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UnauthorizedException("User not found"));

            UserToken userToken = userTokenRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new UnauthorizedException("User token not found"));

            tokenRefreshService.refreshTokenIfNeeded(userToken);

            Map<String, Object> response = new HashMap<>();
            response.put("authenticated", true);
            response.put("email", email);
            response.put("userId", user.getId());
            response.put("tokenValid", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("authenticated", false);
            response.put("message", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}

