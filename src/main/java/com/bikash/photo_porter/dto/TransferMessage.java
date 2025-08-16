package com.bikash.photo_porter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferMessage {
    private String photoId;
    private String fileName;
    private byte[] photoBytes;
    private Long sourceUserId;
    private Long targetUserId;
    private String targetAccessToken;
}
