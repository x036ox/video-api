package com.artur.youtback.controller;

import com.artur.common.exception.NotFoundException;
import com.artur.youtback.exception.AlreadyExistException;
import com.artur.youtback.model.user.User;
import com.artur.youtback.model.user.UserCreateRequest;
import com.artur.youtback.model.user.UserUpdateRequest;
import com.artur.youtback.model.video.Video;
import com.artur.youtback.service.EmailService;
import com.artur.youtback.service.UserService;
import com.artur.youtback.sort.VideoSort;
import com.artur.youtback.utils.AuthenticationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;
    @Autowired
    EmailService emailService;



    @Operation(description = "Get user by id")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "User found successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = User.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "User was not found",
                            content = @Content()
                    )
            }
    )
    @GetMapping("/user-info")
    public ResponseEntity<?> getUserInfo(@RequestParam String id){
        try {
            return ResponseEntity.ok(userService.findById(id));
        } catch (NotFoundException e) {
            logger.warn("User with id: [" + id + "] was not found", e);
            return ResponseEntity.notFound().build();
        }
    }

    @SecurityRequirement(name = "jwt", scopes = "ROLE_ADMIN")
    @Operation(description = """
        Finds users by specified option (search by multiple criteria).
        Available only for admins
    """)
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Users was found successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = User.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No users was found with specified options",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "If range in value param specified incorrectly (should be 1/10)",
                    content = @Content()
            )
    })
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> findByOption(
            @RequestParam List<String> option,
            @RequestParam List<String> value
    ){
        try{
            return ResponseEntity.ok(userService.findByOption(option, value));
        }catch (NullPointerException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (IllegalArgumentException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(description = "Get all user videos")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(schema = @Schema(implementation = Video.class))
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "User was not found",
                            content = @Content()
                    )
            }
    )
    @GetMapping("/videos")
    public ResponseEntity<?> getUserVideos(@RequestParam(name = "userId") String userId, @RequestParam(required = false, name = "sortOption") Integer sortOption){
        try {
            return ResponseEntity.ok(userService.getAllUserVideos(userId,sortOption != null ?  VideoSort.convert(sortOption) : null));
        } catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @SecurityRequirement(name = "jwt")
    @Operation(description = "Check if user liked video")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Response returned successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "boolean")
                    )
            )
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/liked")
    public ResponseEntity<?> hasUserLikedVideo(@RequestParam(name = "userId") String userId, @RequestParam(name = "videoId")Long videoId){
        try {
            return ResponseEntity.ok(userService.hasUserLikedVideo(userId,videoId));
        } catch(NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @Operation(description = "Get all channels user subscribed to")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Response returned successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = User.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User with specified id was not found",
                    content = @Content()
            )
    })
    @SecurityRequirement(name = "jwt")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/subscribes")
    public ResponseEntity<?> getUserSubscribes(@RequestParam String userId){
        try {
            return ResponseEntity.ok(userService.getUserSubscribes(userId));
        } catch (NotFoundException e){
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(description = "Get all videos that user liked")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of videos that user liked",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = Video.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User with specified id was not found",
                    content = @Content()
            )
    })
    @SecurityRequirement(name = "jwt")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/likes")
    public ResponseEntity<?> getUserLikes(@RequestParam String userId){
        try {
            return ResponseEntity.ok(userService.getUserLikes(userId));
        } catch (NotFoundException e){
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(description = "Get user`s search history. User id will be retrieved from jwt token")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of strings representing search history",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(type = "string"))
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User with id from jwt was not found",
                    content = @Content()
            )
    })
    @SecurityRequirement(name = "jwt")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/search-history")
    public ResponseEntity<?> getSearchHistory(Authentication authentication){
        try {
           return ResponseEntity.ok(userService.findById(AuthenticationUtils.getUserId(authentication)).getSearchHistory());
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(description = "Get user`s watch history. User id will be retrieved from jwt token")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of videos",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = Video.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User with id from jwt was not found",
                    content = @Content()
            )
    })
    @SecurityRequirement(name = "jwt")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/watch-history")
    public ResponseEntity<?> getWatchHistory(){
        try{
            return ResponseEntity.ok(userService.getWatchHistory(AuthenticationUtils.getUserId()));
        } catch(NotFoundException e){
            logger.error(e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(description = "Checks if user subscribed specified channel. User`s id will be retrieved from jwt token.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "True if user subscribed, false if not",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(type = "boolean"))
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Either user or channel was not found",
                    content = @Content()
            )
    })
    @SecurityRequirement(name = "jwt")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/subscribed")
    public ResponseEntity<?> hasUserSubscribedChannel(@RequestParam(name = "channelId")String channelId){
        try {
            return ResponseEntity.ok(userService.hasUserSubscribedChannel(AuthenticationUtils.getUserId(),channelId));
        } catch(NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }


    @Operation(description = "Mark specified video as not interested. User`s id will be retrieved from jwt token.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Either user or video was not found",
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
    @PostMapping("/not-interested")
    public ResponseEntity<?> notInterested(@RequestParam Long videoId){
        try{
            userService.notInterested(videoId, AuthenticationUtils.getUserId());
            return ResponseEntity.ok(null);
        } catch(NotFoundException e){
            logger.warn(e.toString());
            return ResponseEntity.notFound().build();
        }
        catch (Exception e){
            logger.error(e.toString());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(description = "Create user. This endpoint should be called only if user did a registration on authorization server")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User created successfully",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "406",
                    description = "If user this already exists",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "If some error occurred",
                    content = @Content()
            )
    })
    @PostMapping("/user-info")
    public ResponseEntity<?> postUser(@ModelAttribute UserCreateRequest userCreateRequest){
        try{
            userService.registerUser(userCreateRequest);
            return ResponseEntity.ok(null);
        } catch (AlreadyExistException e){
            logger.warn("Error occurred while creating user", e);
          return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        } catch(Exception e){
            logger.error("Could not create user", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @Operation(description = "User liked video. User id will be retrieved from jwt token")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User liked video successfully",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Either user or video was not found",
                    content = @Content()
            )
    })
    @SecurityRequirement(name = "jwt")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/like")
    public ResponseEntity<?> likeVideoById(@RequestParam(name = "videoId") Long videoId){
        try{
            userService.likeVideo(AuthenticationUtils.getUserId(), videoId);
            return ResponseEntity.ok(null);
        } catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @Operation(description = "Save search query. User id will be retrieved from jwt token")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Search query successfully saved",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User was not found",
                    content = @Content()
            )
    })
    @SecurityRequirement(name = "jwt")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/search-history")
    public ResponseEntity<?> addSearchOptionToUserById(Authentication authentication, @RequestParam String value){
        try{
            userService.addSearchOption(AuthenticationUtils.getUserId(authentication), value);
            return ResponseEntity.status(HttpStatus.OK).body(null);
        }catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @Operation(description = "Create any amount of users (for test purposes). Available only for admins")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Amount of users created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "integer")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "If some error occurred while creating users",
                    content = @Content()
            )
    })
    @SecurityRequirement(name = "jwt",scopes = "ROLE_ADMIN")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/admin/add")
    public ResponseEntity<?> addUsers(@RequestParam("a") Integer value){
        try{
            return ResponseEntity.ok(userService.addUsers(value));
        } catch(Exception e){
            logger.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Operation(description = "Update user profile. User id will be retrieved from jwt token")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Updated successfully",
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
    })
    @SecurityRequirement(name = "jwt")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("")
    public ResponseEntity<String> update(@RequestBody UserUpdateRequest user, Authentication authentication){
        try{
            userService.update(user, AuthenticationUtils.getUserId(authentication));
            return ResponseEntity.ok(null);
        }catch(NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(description = "User subscribed channel. User id will be retrieved from jwt token")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Success",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "If user was not found",
                    content = @Content()
            )
    })
    @SecurityRequirement(name = "jwt")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/subscribe")
    public ResponseEntity<?> subscribeById(@RequestParam(name = "channelId") String subscribedChannel){
        try {
            userService.subscribeById(AuthenticationUtils.getUserId(), subscribedChannel);
            return ResponseEntity.ok(null);
        } catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @Operation(description = "User unsubscribed channel. User id will be retrieved from jwt token")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Success",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "If user was not found",
                    content = @Content()
            )
    })
    @SecurityRequirement(name = "jwt")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribeById(@RequestParam(name = "channelId") String subscribedChannel){
        try {
            userService.unsubscribeById(AuthenticationUtils.getUserId(), subscribedChannel);
            return ResponseEntity.ok(null);
        } catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @Operation(description = "Delete user`s self account. User id will be retrieved from jwt token")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Success",
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
    })
    @SecurityRequirement(name = "jwt")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("")
    public ResponseEntity<String> deleteById(@RequestParam String userId){
        try{
            userService.deleteById(userId);
            return ResponseEntity.ok(null);
        }catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
    }

    @Operation(description = "Remove search option. User id will be retrieved from jwt token")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Success",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "If user was not found",
                    content = @Content()
            ),
    })
    @SecurityRequirement(name = "jwt")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("search-history")
    public ResponseEntity<?> deleteSearchOption(@Autowired Authentication authentication, @RequestParam String searchOptionValue){
        try{
            userService.deleteSearchOption(AuthenticationUtils.getUserId(authentication), searchOptionValue);
            return ResponseEntity.ok(null);
        }catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @Operation(description = "User disliked video. User id will be retrieved from jwt token")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Success",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "If user was not found",
                    content = @Content()
            ),
    })
    @SecurityRequirement(name = "jwt")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/dislike")
    public ResponseEntity<?> dislikeVideoById(@RequestParam(name = "videoId") Long videoId){
        try{
            userService.dislikeVideo(AuthenticationUtils.getUserId(), videoId);
            return ResponseEntity.ok(null);
        } catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
}
