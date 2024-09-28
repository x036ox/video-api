package com.artur.youtback.model.user;

import com.artur.youtback.model.video.Video;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@NoArgsConstructor
@Builder
@Getter
@EqualsAndHashCode
public class User implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(User.class);
    public static String DEFAULT_USER_PICTURE = "Prewievs/default-picture.png";

    private String id;
    private String username;
    private String email;
    private String picture;
    private String subscribers;


    private List<Video> userVideos;
    private List<String> searchHistory;

    private final transient boolean accountNonExpired = true;
    private final transient boolean accountNonLocked = true;
    private final transient boolean credentialsNonExpired = true;
    private final transient boolean enabled = true;

    private String authorities;


    @JsonCreator
    public User(@JsonProperty("id")String id,
                @JsonProperty("username")String username,
                @JsonProperty("email")String email,
                @JsonProperty("picture")String picture,
                @JsonProperty("subscribers")String subscribers,
                @JsonProperty("userVideos")List<Video> userVideos,
                @JsonProperty("searchHistory")List<String> searchHistory,
                @JsonProperty("authorities") String authorities) {
        this.id = id;
        this.username = username;
        this.picture = picture;
        this.subscribers = subscribers;
        this.userVideos = userVideos;
        this.searchHistory = searchHistory;
        this.authorities = authorities;
    }




    public static User deserialize(String serialized){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(serialized, User.class);
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    public static User create(String username, String authorities){
        return User.builder()
                .id(null)
                .username(username)
                .authorities(authorities)
                .searchHistory(new ArrayList<>())
                .userVideos(new ArrayList<>())
                .picture(null)
                .subscribers("0")
                .build();

    }

    public String serialize(){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
            return null;
        }
    }



    public void addAuthority(String authority){
        this.authorities += "," + authority;
    }
}
