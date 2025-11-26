package com.example.website.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud.name}")
    private String cloudName;

    @Value("${cloudinary.api.key}")
    private String apiKey;

    @Value("${cloudinary.api.secret}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        // Value is 300000 milliseconds (5 minutes)
        int fiveMinutesInMilliseconds = 300000; 

        // 1. Initialize the Cloudinary object
        // ⭐ CRITICAL FIX: Inject timeout properties directly into the Map ⭐
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true, 
                
                // Add the connection and read timeouts (in milliseconds)
                "connection_timeout", fiveMinutesInMilliseconds, 
                "timeout", fiveMinutesInMilliseconds 
        ));
    }
}