package com.artur.youtback.utils;

import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class AuthenticationUtils {

    public static String getUserId(@Nullable Authentication authentication){
        if(authentication == null || authentication.getClass().isAssignableFrom(AnonymousAuthenticationToken.class)){
            return null;
        }
        if(authentication instanceof JwtAuthenticationToken){
            return ((JwtAuthenticationToken) authentication).getToken().getSubject();
        }
        throw new IllegalArgumentException("Not supported authentication method");
    }

    public static String getUserId(){
        return getUserId(SecurityContextHolder.getContext().getAuthentication());
    }

    public static boolean isAuthenticated(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !authentication.getClass().isAssignableFrom(AnonymousAuthenticationToken.class);
    }
}
