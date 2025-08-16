package com.bikash.photo_porter.dto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GoogleAlbum {
    private String id;
    private String title;
    private String mediaItemsCount;
}

