package com.artur.youtback.controller;

import com.artur.youtback.entity.SearchHistory;
import com.artur.youtback.exception.AlreadyExistException;
import com.artur.youtback.exception.IncorrectPasswordException;
import com.artur.youtback.exception.NotFoundException;
import com.artur.youtback.model.user.User;
import com.artur.youtback.model.user.UserAuthenticationRequest;
import com.artur.youtback.model.user.UserCreateRequest;
import com.artur.youtback.model.user.UserUpdateRequest;
import com.artur.youtback.service.EmailService;
import com.artur.youtback.service.TokenService;
import com.artur.youtback.service.UserService;
import com.artur.youtback.utils.AppCookieUtils;
import com.artur.youtback.utils.Utils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/user")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;
    @Autowired
    TokenService tokenService;
    @Autowired
    EmailService emailService;



    @GetMapping("")
    public ResponseEntity<?> find(@RequestParam(required = false) Long id){
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
    public ResponseEntity<?> getUserVideos(@RequestParam(name = "userId") Long userId, @RequestParam(required = false, name = "sortOption") Integer sortOption){
        try {
            return ResponseEntity.ok(userService.getAllUserVideos(userId,sortOption != null ?  Utils.processSortOptions(sortOption) : null));
        } catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/liked")
    public ResponseEntity<?> hasUserLikedVideo(@RequestParam(name = "userId") Long userId, @RequestParam(name = "videoId")Long videoId){
        try {
            return ResponseEntity.ok(userService.hasUserLikedVideo(userId,videoId));
        } catch(NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/subscribes")
    public ResponseEntity<?> getUserSubscribes(@RequestParam Long userId){
        try {
            return ResponseEntity.ok(userService.getUserSubscribes(userId));
        } catch (NotFoundException e){
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/likes")
    public ResponseEntity<?> getUserLikes(@RequestParam Long userId){
        try {
            return ResponseEntity.ok(userService.getUserLikes(userId));
        } catch (NotFoundException e){
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/watch-history")
    public ResponseEntity<?> getWatchHistory(@RequestParam Long userId){
        try{
            return ResponseEntity.ok(userService.getWatchHistory(userId));
        } catch(NotFoundException e){
            logger.error(e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/refresh")
    public ResponseEntity<?> validateUser(@Autowired HttpServletRequest request, @Autowired HttpServletResponse response){
        Cookie[] cookies = request.getCookies();
        if(cookies == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        Optional<Cookie> optionalCookie = Arrays.stream(cookies).filter(el -> el.getName().equals(AppCookieUtils.REFRESH_TOKEN)).findFirst();
        if(optionalCookie.isPresent()){
            Cookie refreshCookie = optionalCookie.get();
            String refreshToken = refreshCookie.getValue();
            if(tokenService.isTokenValid(refreshToken)){
                try {
                    User user = userService.findById(Long.valueOf(tokenService.decode(refreshToken).getSubject()));
                    String accessToken = tokenService.generateAccessToken(user);
                    response.addHeader("accessToken", accessToken);
                    response.addHeader("Access-Control-Expose-Headers", "accessToken");
                    return ResponseEntity.ok(user);
                } catch (NotFoundException e) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
                }
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
    }

    @GetMapping("/subscribed")
    public ResponseEntity<?> hasUserSubscribedChannel(@RequestParam(name = "userId") Long userId, @RequestParam(name = "channelId")Long channelId){
        try {
            return ResponseEntity.ok(userService.hasUserSubscribedChannel(userId,channelId));
        } catch(NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/confirm-email")
    public ResponseEntity<Boolean> confirmEmail(@RequestParam("u") String emailEncoded){
        try {
            String emailDecoded = new String(Base64.getDecoder().decode(emailEncoded));
            userService.confirmEmail(emailDecoded);
            return ResponseEntity.ok(true);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(false);
        }
    }


    @PostMapping("/login")
    public ResponseEntity<?>  loginByEmailAndPassword(@RequestBody UserAuthenticationRequest authenticationRequest, @Autowired BCryptPasswordEncoder passwordEncoder, @Autowired HttpServletResponse response){
        try {
            User user = userService.findByEmail(authenticationRequest.email());
            if(!passwordEncoder.matches(authenticationRequest.password(), user.getPassword())){
                throw new IncorrectPasswordException("Incorrect password " + authenticationRequest.email());
            }
            response.addCookie(AppCookieUtils.refreshCookie(tokenService.generateRefreshToken(user)));
            response.addHeader("accessToken", tokenService.generateAccessToken(user));
            return ResponseEntity.ok(user);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch(IncorrectPasswordException e){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

    }

    @PostMapping("/not-interested")
    public ResponseEntity<?> notInterested(@RequestParam Long userId, @RequestParam Long videoId){
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

    @PostMapping("/registration")
    public ResponseEntity<?> registerUser(@ModelAttribute UserCreateRequest userCreateRequest, @Autowired HttpServletResponse response){
        try {
//            emailService.sendConfirmationEmail(email);
            User registeredUser = userService.registerUser(userCreateRequest);
            response.addCookie(AppCookieUtils.refreshCookie(tokenService.generateRefreshToken(registeredUser)));
            response.addHeader("accessToken", tokenService.generateAccessToken(registeredUser));
            return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
        }catch (AlreadyExistException | NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(null);
        } catch (ConstraintViolationException e){
            logger.error(e.toString());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        } catch (Exception e){
            logger.error(e.toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

    }

    @PostMapping("/like")
    public ResponseEntity<?> likeVideoById(@RequestParam(name = "videoId") Long videoId, @RequestParam(name = "userId") Long userId){
        try{
            userService.likeVideo(userId, videoId);
            return ResponseEntity.ok(null);
        } catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PostMapping("/search-history")
    public ResponseEntity<?> addSearchOptionToUserById(@Autowired Authentication authentication, @RequestBody SearchHistory searchOption){
        if(authentication == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        Jwt jwt =(Jwt) authentication.getPrincipal();
        try{
            userService.addSearchOption(Long.parseLong(jwt.getSubject()), searchOption.getSearchOption());
            return ResponseEntity.status(HttpStatus.OK).body("Search option added");
        }catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("imageFile") MultipartFile file, @RequestParam Long userId){
        try(InputStream fileInputStream = file.getInputStream()){
            userService.saveImage(fileInputStream, userId.toString());
            return ResponseEntity.ok(null);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/admin/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addUsers(@RequestParam("a") Integer value){
        try{
            return ResponseEntity.ok(userService.addUsers(value));
        } catch(Exception e){
            logger.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("")
    public ResponseEntity<String> update(@ModelAttribute UserUpdateRequest user){
        try{
            userService.update(user);
            return ResponseEntity.ok(null);
        }catch(NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN).body(e.getMessage());
        }
    }

    @PutMapping("/subscribe")
    public ResponseEntity<?> subscribeById(@RequestParam(name = "userId") Long userId, @RequestParam(name = "channelId") Long subscribedChannel){
        try {
            userService.subscribeById(userId, subscribedChannel);
            return ResponseEntity.ok(null);
        } catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PutMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribeById(@RequestParam(name = "userId") Long userId, @RequestParam(name = "channelId") Long subscribedChannel){
        try {
            userService.unsubscribeById(userId, subscribedChannel);
            return ResponseEntity.ok(null);
        } catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @DeleteMapping("")
    public ResponseEntity<String> deleteById(@RequestParam(name = "userId") Long id){
        try{
            userService.deleteById(id);
            return ResponseEntity.ok(null);
        }catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("search-history")
    public ResponseEntity<String> deleteSearchOption(@Autowired Authentication authentication, @RequestBody SearchHistory searchHistory){
        if(authentication == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        Jwt jwt =(Jwt) authentication.getPrincipal();
        try{
            userService.deleteSearchOption(Long.parseLong(jwt.getSubject()), searchHistory.getSearchOption());
            return ResponseEntity.ok("Deleted");
        }catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/dislike")
    public ResponseEntity<?> dislikeVideoById(@RequestParam(name = "videoId") Long videoId, @RequestParam(name = "userId", required = false) Long userId){
        try{
            userService.dislikeVideo(userId, videoId);
            return ResponseEntity.ok(null);
        } catch (NotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @DeleteMapping("/logout")
    public ResponseEntity<?> logout(@Autowired HttpServletRequest request, HttpServletResponse response){
        try {
            AppCookieUtils.removeRefreshCookie(request, response);
            return ResponseEntity.ok(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}
