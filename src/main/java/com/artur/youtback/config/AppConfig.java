package com.artur.youtback.config;

import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {


    @Bean
    @Scope("prototype")
    public LanguageDetector languageDetector(){
        return new OptimaizeLangDetector().loadModels();
    }

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
