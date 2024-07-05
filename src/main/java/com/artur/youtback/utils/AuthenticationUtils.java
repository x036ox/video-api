package com.artur.youtback.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class AuthenticationUtils {

    public static Long getUserId(Authentication authentication){
        if(authentication instanceof JwtAuthenticationToken){
            return Long.valueOf(
                    ((JwtAuthenticationToken) authentication).getToken().getSubject()
            );
        }
        throw new IllegalArgumentException("Not supported authentication method");
    }
}
