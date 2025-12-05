package com.example.website.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File; // <-- NEW IMPORT
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class AmazonS3Service {

    @Autowired
    private AmazonS3 amazonS3Client; 

    // --- S3 Specific Properties ---
    @Value("${aws.s3.bucketName}")
    private String bucketName;

    @Value("${aws.s3.publicBaseUrl}") 
    private String publicBaseUrl; 

    private static final String UPLOAD_FOLDER = "website-content/";

    /**
     * Uploads a MultipartFile to AWS S3 storage.
     * Uses a temporary File conversion to ensure the stream is repeatable for S3 retries.
     * @param file The file to upload.
     * @return The unique key/path of the file in the S3 bucket.
     * @throws IOException If file streaming or S3 API communication fails.
     */
    public String uploadFile(MultipartFile file) throws IOException {
        
        // 1. Determine the file name and path (key)
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String uniqueFileName = UPLOAD_FOLDER + UUID.randomUUID().toString() + fileExtension;
        
        // 2. Prepare metadata
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());
        
        // 3. Create a temporary File from the MultipartFile
        // This is the most reliable way to provide a repeatable stream source to S3.
        File tempFile = null;
        String uploadedFileKey = null;

        try {
            // Create a temp file on the local file system (Spring/Tomcat default temp directory)
            tempFile = File.createTempFile("s3upload-", originalFilename);
            file.transferTo(tempFile); // Copy MultipartFile content to the temp file

            // 4. Upload the File directly to S3
            // When using the File object, the SDK handles stream repeatability automatically.
            PutObjectRequest putRequest = new PutObjectRequest(
                bucketName, 
                uniqueFileName, 
                tempFile
            )
            .withMetadata(metadata); // Apply metadata

            amazonS3Client.putObject(putRequest);
            
            uploadedFileKey = uniqueFileName;
            System.out.println("S3: File uploaded successfully: " + uniqueFileName);

        } catch (Exception e) {
            e.printStackTrace(); 
            throw new IOException("AWS S3 API error during upload for file " + originalFilename + ": " + e.getMessage(), e);
        } finally {
            // 5. Clean up the temporary file
            if (tempFile != null) {
                tempFile.delete();
            }
        }
        
        return uploadedFileKey; 
    }

    // --- Other methods (getPublicUrl, getTransformedUrl, deleteFile) remain the same ---
    
    /**
     * Generates the public CDN URL for a file key/path.
     * @param fileKey The unique key/path stored in the database.
     * @return The complete public CDN URL.
     */
    public String getPublicUrl(String fileKey) {
        if (fileKey == null || fileKey.isEmpty()) {
            return publicBaseUrl;
        }
        
        String baseUrl = publicBaseUrl.endsWith("/") ? publicBaseUrl : publicBaseUrl + "/";
        String key = fileKey.startsWith("/") ? fileKey.substring(1) : fileKey;
        
        return baseUrl + key;
    }
    
    /**
     * IMPORTANT: AWS S3 does not have built-in URL transformations like ImageKit.
     */
    public String getTransformedUrl(String fileKey, String transformation) {
        System.out.println("WARNING: S3 does not natively support dynamic transformations. Returning original URL.");
        return getPublicUrl(fileKey);
    }

    /**
     * Deletes a file from the S3 bucket using its unique key.
     * @param fileKey The unique key/path saved in the database.
     * @throws IOException If S3 API communication fails.
     */
    public void deleteFile(String fileKey) throws IOException {
        if (fileKey == null || fileKey.isEmpty()) {
            System.out.println("S3: Skip delete - fileKey is null or empty.");
            return;
        }
        
        try {
            DeleteObjectRequest deleteRequest = new DeleteObjectRequest(bucketName, fileKey);
            amazonS3Client.deleteObject(deleteRequest);
            System.out.println("S3: File deleted successfully: " + fileKey);
        } catch (Exception e) {
            throw new IOException("AWS S3 API error during delete for key " + fileKey + ": " + e.getMessage(), e);
        }
    }
}