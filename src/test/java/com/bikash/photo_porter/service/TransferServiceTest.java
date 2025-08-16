package com.bikash.photo_porter.service;

import com.bikash.photo_porter.exception.TransferException;
import com.bikash.photo_porter.model.PhotoTransfer;
import com.bikash.photo_porter.model.TransferStatus;
import com.bikash.photo_porter.model.User;
import com.bikash.photo_porter.model.UserToken;
import com.bikash.photo_porter.repository.PhotoTransferRepository;
import com.bikash.photo_porter.repository.UserRepository;
import com.bikash.photo_porter.repository.UserTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserTokenRepository userTokenRepository;

    @Mock
    private PhotoTransferRepository photoTransferRepository;

    @Mock
    private GooglePhotosClient googlePhotosClient;

    @Mock
    private PhotoTransferProducer kafkaProducer;

    @Mock
    private TokenRefreshService tokenRefreshService;

    @InjectMocks
    private TransferService transferService;

    private User sourceUser;
    private User targetUser;
    private UserToken sourceToken;
    private UserToken targetToken;

    @BeforeEach
    void setUp() {
        sourceUser = new User();
        sourceUser.setId(1L);
        sourceUser.setEmail("source@example.com");

        targetUser = new User();
        targetUser.setId(2L);
        targetUser.setEmail("target@example.com");

        sourceToken = new UserToken();
        sourceToken.setId(1L);
        sourceToken.setUser(sourceUser);
        sourceToken.setAccessToken("source-token");

        targetToken = new UserToken();
        targetToken.setId(2L);
        targetToken.setUser(targetUser);
        targetToken.setAccessToken("target-token");
    }

    @Test
    void testTransferPhotos_Success() {
        // Given
        List<String> photoIds = List.of("photo1", "photo2");
        
        when(userRepository.findByEmail("source@example.com")).thenReturn(Optional.of(sourceUser));
        when(userRepository.findByEmail("target@example.com")).thenReturn(Optional.of(targetUser));
        when(userTokenRepository.findByUserId(1L)).thenReturn(Optional.of(sourceToken));
        when(userTokenRepository.findByUserId(2L)).thenReturn(Optional.of(targetToken));
        when(tokenRefreshService.refreshTokenIfNeeded(sourceToken)).thenReturn("valid-source-token");
        when(tokenRefreshService.refreshTokenIfNeeded(targetToken)).thenReturn("valid-target-token");
        when(photoTransferRepository.save(any(PhotoTransfer.class))).thenAnswer(invocation -> {
            PhotoTransfer transfer = invocation.getArgument(0);
            transfer.setId(1L);
            return transfer;
        });

        // When
        assertDoesNotThrow(() -> transferService.transferPhotos("source@example.com", "target@example.com", photoIds));

        // Then
        verify(photoTransferRepository, times(2)).save(any(PhotoTransfer.class));
    }

    @Test
    void testTransferPhotos_SourceUserNotFound() {
        // Given
        when(userRepository.findByEmail("source@example.com")).thenReturn(Optional.empty());

        // When & Then
        TransferException exception = assertThrows(TransferException.class, 
            () -> transferService.transferPhotos("source@example.com", "target@example.com", List.of("photo1")));
        
        assertEquals("Source user not found: source@example.com", exception.getMessage());
    }

    @Test
    void testGetTransfersForUser_Success() {
        // Given
        PhotoTransfer transfer = new PhotoTransfer();
        transfer.setId(1L);
        transfer.setSourceUser(sourceUser);
        transfer.setStatus(TransferStatus.SUCCESS);
        transfer.setStartedAt(LocalDateTime.now());

        when(userRepository.findByEmail("source@example.com")).thenReturn(Optional.of(sourceUser));
        when(photoTransferRepository.findBySourceUser(sourceUser)).thenReturn(List.of(transfer));
        when(photoTransferRepository.findByTargetUser(sourceUser)).thenReturn(List.of());

        // When
        List<PhotoTransfer> result = transferService.getTransfersForUser("source@example.com");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(transfer.getId(), result.get(0).getId());
    }

    @Test
    void testGetTransferById_Success() {
        // Given
        PhotoTransfer transfer = new PhotoTransfer();
        transfer.setId(1L);
        transfer.setSourceUser(sourceUser);
        transfer.setTargetUser(targetUser);

        when(photoTransferRepository.findById(1L)).thenReturn(Optional.of(transfer));

        // When
        PhotoTransfer result = transferService.getTransferById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testGetTransferById_NotFound() {
        // Given
        when(photoTransferRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        TransferException exception = assertThrows(TransferException.class, 
            () -> transferService.getTransferById(1L));
        
        assertEquals("Transfer not found with id: 1", exception.getMessage());
    }
} 