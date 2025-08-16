package com.bikash.photo_porter.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.*;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.photoslibrary.v1.PhotosLibrary;
import com.google.api.services.photoslibrary.v1.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GooglePhotosClient {

    private static final String APPLICATION_NAME = "GooglePhotosTransferApp";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private PhotosLibrary getPhotosLibraryClient(String accessToken) throws GeneralSecurityException, IOException {
        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
        return new PhotosLibrary.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public List<Map<String, Object>> listAlbums(String accessToken) {
        try {
            PhotosLibrary client = getPhotosLibraryClient(accessToken);
            ListAlbumsResponse response = client.albums().list().execute();
            List<Album> albums = response.getAlbums();

            return albums != null
                    ? albums.stream()
                    .map(album -> {
                        Map<String, Object> albumMap = new HashMap<>();
                        albumMap.put("id", album.getId());
                        albumMap.put("title", album.getTitle());
                        try {
                            Object mediaItemsCount = album.getClass().getMethod("getMediaItemsCount").invoke(album);
                            if (mediaItemsCount != null) {
                                albumMap.put("mediaItemsCount", mediaItemsCount);
                            }
                        } catch (Exception e) {
                            log.debug("Could not get mediaItemsCount for album: {}", album.getId());
                        }
                        albumMap.put("coverPhotoBaseUrl", album.getCoverPhotoBaseUrl());
                        albumMap.put("productUrl", album.getProductUrl());
                        return albumMap;
                    })
                    .collect(Collectors.toList())
                    : Collections.emptyList();

        } catch (Exception e) {
            log.error("Failed to fetch albums", e);
            throw new RuntimeException("Failed to fetch albums: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> listPhotosInAlbum(String accessToken, String albumId) {
        try {
            PhotosLibrary client = getPhotosLibraryClient(accessToken);

            SearchMediaItemsRequest request = new SearchMediaItemsRequest().setAlbumId(albumId);
            SearchMediaItemsResponse response = client.mediaItems().search(request).execute();

            List<MediaItem> items = response.getMediaItems();

            return items != null
                    ? items.stream()
                    .map(item -> {
                        Map<String, Object> photoMap = new HashMap<>();
                        photoMap.put("id", item.getId());
                        try {
                            Object filename = item.getClass().getMethod("getFilename").invoke(item);
                            if (filename != null) {
                                photoMap.put("filename", filename);
                            }
                        } catch (Exception e) {
                            log.debug("Could not get filename for media item: {}", item.getId());
                        }
                        photoMap.put("baseUrl", item.getBaseUrl());
                        photoMap.put("mimeType", item.getMimeType());
                        photoMap.put("mediaMetadata", item.getMediaMetadata());
                        return photoMap;
                    })
                    .collect(Collectors.toList())
                    : Collections.emptyList();

        } catch (Exception e) {
            log.error("Failed to fetch photos from album: {}", albumId, e);
            throw new RuntimeException("Failed to fetch photos: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getAlbumDetails(String accessToken, String albumId) {
        try {
            PhotosLibrary client = getPhotosLibraryClient(accessToken);
            Album album = client.albums().get(albumId).execute();

            Map<String, Object> albumDetails = new HashMap<>();
            albumDetails.put("id", album.getId());
            albumDetails.put("title", album.getTitle());
            try {
                Object mediaItemsCount = album.getClass().getMethod("getMediaItemsCount").invoke(album);
                if (mediaItemsCount != null) {
                    albumDetails.put("mediaItemsCount", mediaItemsCount);
                }
            } catch (Exception e) {
                log.debug("Could not get mediaItemsCount for album: {}", album.getId());
            }
            albumDetails.put("coverPhotoBaseUrl", album.getCoverPhotoBaseUrl());
            albumDetails.put("productUrl", album.getProductUrl());
            albumDetails.put("isWriteable", album.getIsWriteable());

            return albumDetails;

        } catch (Exception e) {
            log.error("Failed to fetch album details for album: {}", albumId, e);
            throw new RuntimeException("Failed to fetch album details: " + e.getMessage(), e);
        }
    }

    public byte[] downloadPhoto(String baseUrl) {
        try {
            URL url = new URL(baseUrl + "=d");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            try (InputStream in = connection.getInputStream()) {
                return in.readAllBytes();
            }
        } catch (Exception e) {
            log.error("Error downloading photo from: {}", baseUrl, e);
            throw new RuntimeException("Error downloading photo", e);
        }
    }

    public void uploadPhoto(String accessToken, byte[] photoData, String fileName) {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            HttpRequestFactory requestFactory = httpTransport.createRequestFactory(req -> {
                req.getHeaders().setAuthorization("Bearer " + accessToken);
                req.getHeaders().setContentType("application/octet-stream");
                req.getHeaders().set("X-Goog-Upload-File-Name", fileName);
                req.getHeaders().set("X-Goog-Upload-Protocol", "raw");
            });

            GenericUrl uploadUrl = new GenericUrl("https://photoslibrary.googleapis.com/v1/uploads");
            HttpContent content = new ByteArrayContent("application/octet-stream", photoData);
            HttpRequest request = requestFactory.buildPostRequest(uploadUrl, content);

            HttpResponse response = request.execute();
            String uploadToken = response.parseAsString();

            NewMediaItem newMediaItem = new NewMediaItem()
                    .setDescription(fileName)
                    .setSimpleMediaItem(new SimpleMediaItem().setUploadToken(uploadToken));

            BatchCreateMediaItemsRequest createRequest = new BatchCreateMediaItemsRequest()
                    .setNewMediaItems(Collections.singletonList(newMediaItem));

            PhotosLibrary client = getPhotosLibraryClient(accessToken);

            client.mediaItems().batchCreate(createRequest).execute();
            log.info("Successfully uploaded photo: {}", fileName);

        } catch (Exception e) {
            log.error("Failed to upload photo: {}", fileName, e);
            throw new RuntimeException("Failed to upload photo", e);
        }
    }
}
