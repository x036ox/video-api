package com.artur.youtback.controller;

import com.artur.youtback.model.ImageUploadRequest;
import com.artur.youtback.service.ImageService;
import com.artur.youtback.utils.MediaUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.FileNotFoundException;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/image")
public class ImageController {
    @Autowired
    private ImageService imageService;


    @PostMapping("/user")
    public ResponseEntity<?> postUserPicture(@ModelAttribute ImageUploadRequest imageUploadRequest){
        try {
            return ResponseEntity.ok(
                    ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/api/image/")
                            .path(imageService.uploadUserPicture(imageUploadRequest))
                            .toUriString()
            );
        } catch (Exception e) {
            log.error("Could not upload user picture", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/thumbnail")
    public ResponseEntity<?> postThumbnail(@ModelAttribute ImageUploadRequest imageUploadRequest){
        try {
            return ResponseEntity.ok(imageService.uploadThumbnail(imageUploadRequest));
        } catch (Exception e) {
            log.error("Could not upload thumbnail", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(description = "Get image by its path.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Input stream of the picture",
                    content = @Content(
                            mediaType = "image/*",
                            schema = @Schema(type = "byte[]")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Picture was not found or some error occurred",
                    content = @Content()
            )
    })
    @GetMapping(value = "/{folder}/{id}/{name}", produces = "image/*")
    public ResponseEntity<InputStreamResource> getUserPicture(@PathVariable String folder, @PathVariable String id, @PathVariable String name){
        try {
            String path = folder + "/" + id + "/" + name;
            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(MediaUtils.getMediaTypeForFile(
                            Objects.requireNonNull(StringUtils.getFilenameExtension(path))
                    ))
                    .body(new InputStreamResource(imageService.getImage(path)));
        } catch (Exception e) {
            log.error("Could not get the picture {}", name, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = "/user/default")
    public ResponseEntity<InputStreamResource> getDefaultUserPicture(){
        try {
            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(MediaType.IMAGE_PNG)
                    .body(new InputStreamResource(imageService.getDefaultPicture()));
        } catch (FileNotFoundException e) {
            log.error("Default user picture was not found");
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Could not get default user picture");
            return ResponseEntity.internalServerError().build();
        }
    }
    @DeleteMapping(value = "/{folder}/{id}/{name}")
    public ResponseEntity<?> deleteImage(@PathVariable String folder, @PathVariable String id, @PathVariable String name){
        try {
            imageService.deleteImage(folder + "/" + id + "/" + name);
            return ResponseEntity.ok(null);
        } catch (Exception e) {
            log.error("Could not delete the picture {}", name, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
