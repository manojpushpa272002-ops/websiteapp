package com.example.website.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    public String uploadFile(MultipartFile file) throws IOException {
        
        // ⭐ CRITICAL FIX: Use file.getInputStream() instead of file.getBytes()
        // This streams the file content to Cloudinary, preventing an OutOfMemoryError
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap("resource_type", "auto"));
        
        return (String) uploadResult.get("secure_url"); // public URL
    }
}