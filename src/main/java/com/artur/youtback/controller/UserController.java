package com.artur.youtback.controller;

import com.artur.youtback.exception.AlreadyExistException;
import com.artur.youtback.exception.NotFoundException;
import com.artur.youtback.model.user.User;
import com.artur.youtback.model.user.UserCreateRequest;
import com.artur.youtback.model.user.UserUpdateRequest;
import com.artur.youtback.service.EmailService;
import com.artur.youtback.service.UserService;
import com.artur.youtback.utils.AuthenticationUtils;
import com.artur.youtback.utils.Utils;
import jakarta.annotation.security.RolesAllowed;
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



    @GetMapping("")
    public ResponseEntity<?> find(@RequestParam(required = false) String id){
        try{
            if(id != null){
                User user = userService.findById(id);
                return ResponseEntity.ok(user);
            }
            else {
                return ResponseEntity.ok(userService.findAll());
            }
        }catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/admin")
    @RolesAllowed("ROLE_ADMIN")
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

    @GetMapping("/videos")
    public ResponseEntity<?> getUserVideos(@RequestParam(name = "userId") String userId, @RequestParam(required = false, name = "sortOption") Integer sortOption){
        try {
            return ResponseEntity.ok(userService.getAllUserVideos(userId,sortOption != null ?  Utils.processSortOptions(sortOption) : null));
        } catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/liked")
    public ResponseEntity<?> hasUserLikedVideo(@RequestParam(name = "userId") String userId, @RequestParam(name = "videoId")Long videoId){
        try {
            return ResponseEntity.ok(userService.hasUserLikedVideo(userId,videoId));
        } catch(NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/user-info")
    public ResponseEntity<?> getUserInfo(@RequestParam String id){
        try {
            return ResponseEntity.ok(userService.findById(id));
        } catch (NotFoundException e) {
            logger.warn("User with id: [" + id + "] was not found", e);
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/subscribes")
    public ResponseEntity<?> getUserSubscribes(@RequestParam String userId){
        try {
            return ResponseEntity.ok(userService.getUserSubscribes(userId));
        } catch (NotFoundException e){
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/likes")
    public ResponseEntity<?> getUserLikes(@RequestParam String userId){
        try {
            return ResponseEntity.ok(userService.getUserLikes(userId));
        } catch (NotFoundException e){
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/picture/{name}")
    public ResponseEntity<?> getPicture(@PathVariable String name){
        try {
            return ResponseEntity.ok(userService.getPicture(name));
        } catch (Exception e) {
            logger.error("Could not retrieve user picture: " + name, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/search-history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSearchHistory(Authentication authentication){
        try {
           return ResponseEntity.ok(userService.findById(AuthenticationUtils.getUserId(authentication)).getSearchHistory());
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }


    @GetMapping("/watch-history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getWatchHistory(){
        try{
            return ResponseEntity.ok(userService.getWatchHistory(AuthenticationUtils.getUserId()));
        } catch(NotFoundException e){
            logger.error(e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }


    @PreAuthorize("isAuthenticated()")
    @GetMapping("/subscribed")
    public ResponseEntity<?> hasUserSubscribedChannel(@RequestParam(name = "userId") String userId, @RequestParam(name = "channelId")String channelId){
        try {
            return ResponseEntity.ok(userService.hasUserSubscribedChannel(userId,channelId));
        } catch(NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }


    @PostMapping("/not-interested")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> notInterested(@RequestParam String userId, @RequestParam Long videoId){
        try{
            userService.notInterested(videoId, userId);
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

    @PostMapping("/user-info")
    public ResponseEntity<?> postUser(@ModelAttribute UserCreateRequest userCreateRequest){
        try{
            userService.registerUser(userCreateRequest);
            return ResponseEntity.ok(null);
        } catch (AlreadyExistException e){
          return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        } catch(Exception e){
            logger.error("Could not create user", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> likeVideoById(@RequestParam(name = "videoId") Long videoId, @RequestParam(name = "userId") String userId){
        try{
            userService.likeVideo(userId, videoId);
            return ResponseEntity.ok(null);
        } catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/search-history")
    public ResponseEntity<?> addSearchOptionToUserById(Authentication authentication, @RequestParam String value){
        try{
            userService.addSearchOption(AuthenticationUtils.getUserId(authentication), value);
            return ResponseEntity.status(HttpStatus.OK).body("Search option added");
        }catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }


    @PostMapping("/admin/add")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> addUsers(@RequestParam("a") Integer value){
        try{
            return ResponseEntity.ok(userService.addUsers(value));
        } catch(Exception e){
            logger.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("")
    public ResponseEntity<String> update(@ModelAttribute UserUpdateRequest user, Authentication authentication){
        try{
            userService.update(user, AuthenticationUtils.getUserId(authentication));
            return ResponseEntity.ok(null);
        }catch(NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN).body(e.getMessage());
        }
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/subscribe")
    public ResponseEntity<?> subscribeById(@RequestParam(name = "userId") String userId, @RequestParam(name = "channelId") String subscribedChannel){
        try {
            userService.subscribeById(userId, subscribedChannel);
            return ResponseEntity.ok(null);
        } catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribeById(@RequestParam(name = "userId") String userId, @RequestParam(name = "channelId") String subscribedChannel){
        try {
            userService.unsubscribeById(userId, subscribedChannel);
            return ResponseEntity.ok(null);
        } catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("")
    public ResponseEntity<String> deleteById(@RequestParam(name = "userId") String id){
        try{
            userService.deleteById(id);
            return ResponseEntity.ok(null);
        }catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("search-history")
    public ResponseEntity<?> deleteSearchOption(@Autowired Authentication authentication, @RequestParam String searchOptionValue){
        try{
            userService.deleteSearchOption(AuthenticationUtils.getUserId(authentication), searchOptionValue);
            return ResponseEntity.ok(null);
        }catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/dislike")
    public ResponseEntity<?> dislikeVideoById(@RequestParam(name = "videoId") Long videoId, @RequestParam(name = "userId", required = false) String userId){
        try{
            userService.dislikeVideo(userId, videoId);
            return ResponseEntity.ok(null);
        } catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
}
