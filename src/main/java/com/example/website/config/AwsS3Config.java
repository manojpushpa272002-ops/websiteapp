package com.example.website.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.transfer.TransferManager; // <-- NEW
import com.amazonaws.services.s3.transfer.TransferManagerBuilder; // <-- NEW
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsS3Config {

    @Value("${aws.s3.accessKey}")
    private String accessKey;

    @Value("${aws.s3.secretKey}")
    private String secretKey;
    
    @Value("${aws.s3.region}")
    private String region; 

    @Bean
    public AmazonS3 amazonS3Client() {
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        return AmazonS3ClientBuilder.standard()
                .withRegion(Regions.fromName(region)) 
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();
    }
    
    // New bean for robust file handling
    @Bean
    public TransferManager transferManager(AmazonS3 amazonS3Client) {
        return TransferManagerBuilder.standard()
                .withS3Client(amazonS3Client)
                // Set the minimum part size for S3 multipart upload (5MB is standard)
                .withMinimumUploadPartSize(5 * 1024 * 1024L) 
                .build();
    }
}