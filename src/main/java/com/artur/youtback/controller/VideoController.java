package com.artur.youtback.controller;


import com.artur.youtback.entity.VideoEntity;
import com.artur.youtback.exception.UserNotFoundException;
import com.artur.youtback.exception.VideoNotFoundException;
import com.artur.youtback.model.User;
import com.artur.youtback.model.Video;
import com.artur.youtback.repository.VideoRepository;
import com.artur.youtback.service.TokenService;
import com.artur.youtback.service.UserService;
import com.artur.youtback.service.VideoService;
import com.artur.youtback.utils.SortOption;
import com.artur.youtback.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.service.annotation.PutExchange;

import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/")
public class VideoController {

    @Autowired
    private VideoService videoService;
    @Autowired
    private UserService userService;
    @Autowired
    private TokenService tokenService;


    private ResponseEntity<List<Video>> findAll(SortOption sortOption){

        try{
            return ResponseEntity.ok(videoService.findAll(sortOption));
        }catch(VideoNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("")
    public ResponseEntity<?> find(@RequestParam(required = false) Long videoId, @RequestParam(required = false, name = "sortOption") Integer sortOption, @RequestParam(value = "option", required = false) String option, @RequestParam(value = "value", required = false)String value) {
        if(videoId != null){
            try {
                return ResponseEntity.ok(videoService.findById(videoId));
            } catch(VideoNotFoundException exception){
                return  ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } else if (option != null) {
            try{
                return ResponseEntity.ok(videoService.findByOption(option, value));
            } catch (NullPointerException e){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            } catch (IllegalArgumentException e){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
            }
        } else{
            return findAll(sortOption != null ? Utils.processSortOptions(sortOption) : null);
        }


    }

    @PostMapping("")
    public ResponseEntity<String> create(@RequestParam("title") String title, @RequestParam("duration") String duration, @RequestParam("description") String description, @RequestParam("thumbnail")MultipartFile thumbnail, HttpServletRequest request){
        try {
            String accessToken = request.getHeader("accessToken");
            if(accessToken == null || !tokenService.isTokenValid(accessToken)){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
            long userId = Long.parseLong(tokenService.decode(accessToken).getSubject());
            videoService.create(title, description, duration, thumbnail, userId);
            return ResponseEntity.status(HttpStatus.OK).body("Created");
        }catch(UserNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        } catch (Exception e){
           e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("");
        }
    }

    @PutMapping("")
    public ResponseEntity<String> update(@RequestParam(value = "videoId")Long videoId, @RequestParam(name = "duration", required = false)String duration, @RequestParam(name = "title", required = false)String title, @RequestParam(name = "description", required = false) String description, @RequestParam(name = "thumbnail", required = false)MultipartFile thumbnail){
        try{
            videoService.update(videoId, title, description, duration, thumbnail);
            return ResponseEntity.ok(null);
        }catch(VideoNotFoundException | IOException e){
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("")
    public ResponseEntity<String> deleteById(@RequestParam(name = "videoId") Long id){
        try{
            videoService.deleteById(id);
            return ResponseEntity.ok(null);
        }catch (VideoNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/watch")
    public ResponseEntity<Video> watchVideoById(@RequestParam(name = "videoId") Long videoId){
        try{
            Video video = videoService.watchById(videoId);
            return ResponseEntity.status(HttpStatus.OK).body(video);
        }catch ( VideoNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }



}


