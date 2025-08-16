package com.bikash.photo_porter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GooglePhoto {
    private String id;
    private String filename;
    private String baseUrl;
}