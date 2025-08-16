package com.bikash.photo_porter.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class TransferRequest {
    @NotNull(message = "Source email is required")
    @Email(message = "Source email must be a valid email address")
    private String sourceEmail;

    @NotNull(message = "Target email is required")
    @Email(message = "Target email must be a valid email address")
    private String targetEmail;

    @NotEmpty(message = "Photo IDs list cannot be empty")
    private List<@NotNull(message = "Photo ID cannot be null") String> photoIds;
}
