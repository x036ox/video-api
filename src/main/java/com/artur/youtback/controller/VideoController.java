package com.artur.youtback.controller;


import com.artur.common.exception.NotFoundException;
import com.artur.youtback.model.user.User;
import com.artur.youtback.model.video.Video;
import com.artur.youtback.model.video.VideoCreateRequest;
import com.artur.youtback.model.video.VideoUpdateRequest;
import com.artur.youtback.service.VideoService;
import com.artur.youtback.sort.VideoSort;
import com.artur.youtback.utils.AppConstants;
import com.artur.youtback.utils.AuthenticationUtils;
import com.artur.youtback.utils.FindOptions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/")
public class VideoController {
    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    @Autowired
    private VideoService videoService;

    @Operation(description = "Get video by id")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Video found successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Video.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Video was not found",
                            content = @Content()
                    )
            }
    )
    @GetMapping("")
    public ResponseEntity<?> find(@RequestParam Long videoId) {
        try {
            return ResponseEntity.ok(videoService.findById(videoId));
        } catch(NotFoundException exception){
            return  ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @Operation(description = """
     Get recommendations. If authorization header is present, user id will be 
     gotten from jwt token and recommendations will be tailored for this user.
     """)
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = Video.class))
                            )
                    ),
                    @ApiResponse(
                            responseCode = "406",
                            description = "If User-Languages header not present",
                            content = @Content()
                    )
            }
    )
    @GetMapping("/recs")
    public ResponseEntity<?> getRecommendations(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, name = "sortOption") Integer sortOption,
            HttpServletRequest request
    ){
        try {
            String languages = request.getHeader("User-Languages");
            if(languages == null || languages.isEmpty()) throw new IllegalArgumentException("User languages should not be empty");

            return ResponseEntity.ok(videoService.recommendations(
                    AuthenticationUtils.getUserId(),
                    page,
                    languages,
                    size,
                    VideoSort.convert(sortOption)
            ));
        } catch (IllegalArgumentException e){
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(null);
        }
    }

//    @GetMapping("/test")
//    @RolesAllowed("ADMIN")
//    public ResponseEntity<?> test(){
//        logger.trace("TEST METHOD CALLED");
//        videoService.testMethod();
//        return ResponseEntity.ok(null);
//    }

    @Operation(description = "Find videos by multiple criteria. Each option corresponds each value. Only for admins")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = Video.class))
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "If no videos was found",
                            content = @Content()
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "If range (in value field) was specified incorrectly (should be 1/10)",
                            content = @Content()
                    )
            }
    )
    @SecurityRequirement(name = "jwt", scopes = "ROLE_ADMIN")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/admin")
    public ResponseEntity<?> findByOption(
            @RequestParam List<String> option,
            @RequestParam List<String> value
    ){
        try{
            return ResponseEntity.ok(videoService.findByOption(option, value));
        }catch (NullPointerException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (IllegalArgumentException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @Operation(description = "Find videos by title")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = Video.class))
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "If no videos was found",
                            content = @Content()
                    )
            }
    )
    @GetMapping("/search")
    public ResponseEntity<?> searchVideos(@RequestParam(value = "search_query") String searchQuery){
        try{
            return ResponseEntity.ok(videoService.findByOption(
                    Collections.singletonList(FindOptions.VideoOptions.BY_TITLE.name()),
                    Collections.singletonList(searchQuery))
            );
        }catch (NullPointerException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @Operation(description = "Get index.m3u8 file")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Input stream of m3u8 file",
                            content = @Content(
                                    mediaType = "application/octet-stream",
                                    schema = @Schema(type = "string", format = "binary")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "If m3u8 file was found",
                            content = @Content()
                    )
            }
    )
    @GetMapping(value = "/{id}/index.m3u8", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<InputStreamResource> m3u8Index(@PathVariable Long id){
        HttpHeaders headers = new HttpHeaders();
        try{
            headers.set("Content-Type", "application/vnd.apple.mpegurl");
            return new ResponseEntity<>(new InputStreamResource(videoService.m3u8Index(id)), headers, HttpStatus.OK);
        } catch(NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @Operation(description = "Get ts file")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Input stream of ts file",
                            content = @Content(
                                    mediaType = "application/octet-stream",
                                    schema = @Schema(type = "string", format = "binary")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "If ts file was not found",
                            content = @Content()
                    )
            }
    )
    @GetMapping(value = "/{id}/{ts}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<InputStreamResource> ts(@PathVariable Long id, @PathVariable String ts){
        HttpHeaders headers = new HttpHeaders();
        try {
            return ResponseEntity.status(HttpStatus.OK).headers(headers).body(new InputStreamResource(videoService.ts(id, ts)));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(description = "Get video for watching by user.")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Video profile",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Video.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "If video was not found",
                            content = @Content()
                    )
            }
    )
    @GetMapping("/watch")
    public ResponseEntity<Video> watchVideoById(@RequestParam(name = "videoId") Long videoId, HttpServletRequest request, Authentication authentication){
        try{
            Video video = videoService.watchById(videoId,
                    authentication == null ? null : AuthenticationUtils.getUserId(authentication));
            return ResponseEntity.ok(video);
        }catch ( NotFoundException e){
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(description = "Create video. Only for authorized users. User id will be retrieved from jwt token")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Created successfully",
                            content = @Content()
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "If user was not found",
                            content = @Content()
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "If some error occurred",
                            content = @Content()
                    )
            }
    )
    @SecurityRequirement(name = "jwt")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("")
    public ResponseEntity<?> create(@ModelAttribute VideoCreateRequest video){
        try {
            String userId =  AuthenticationUtils.getUserId();
            videoService.create(video, userId);
            return ResponseEntity.ok(null);
        }catch(NotFoundException e){
            return ResponseEntity.notFound().build();
        } catch (Exception e){
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(description = "Create any amount of videos (for test purposes). Available only for admins")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Amount of videos created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "integer")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "If some error occurred while creating video",
                    content = @Content()
            )
    })
    @SecurityRequirement(name = "jwt",scopes = "ROLE_ADMIN")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/admin/add")
    public ResponseEntity<?> addVideos(@RequestParam("a") Integer amount){
        try {
            return ResponseEntity.ok(videoService.addVideos(amount));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(description = "Update video")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Success",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "If video was not found",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "If some error occurred",
                    content = @Content()
            )
    })
    @SecurityRequirement(name = "jwt")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("")
    public ResponseEntity<?> update(@ModelAttribute VideoUpdateRequest updateRequest){
        try{
            videoService.update(updateRequest);
            return ResponseEntity.ok(null);
        }catch(NotFoundException  e){
            return ResponseEntity.notFound().build();
        }catch (Exception e){
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(description = "Delete video")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Success",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "If video was not found",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "If some error occurred",
                    content = @Content()
            )
    })
    @SecurityRequirement(name = "jwt")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("")
    public ResponseEntity<String> deleteById(@RequestParam(name = "videoId") Long id){
        try{
            videoService.deleteById(id);
            return ResponseEntity.ok(null);
        }catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e){
            logger.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}


