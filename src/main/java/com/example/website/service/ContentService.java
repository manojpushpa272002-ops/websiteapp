package com.example.website.service;

import com.example.website.model.Content;
import com.example.website.model.Like;
import com.example.website.model.Comment;
import com.example.website.repository.ContentRepository;
import com.example.website.repository.LikeRepository;
import com.example.website.repository.CommentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ContentService {

    private final ContentRepository contentRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final AmazonS3Service amazonS3Service; // 🟢 Dependency changed to S3 Service

    
    public ContentService(ContentRepository contentRepository, 
                          AmazonS3Service amazonS3Service, // 🟢 Parameter changed
                          LikeRepository likeRepository, 
                          CommentRepository commentRepository) {
        this.contentRepository = contentRepository;
        this.amazonS3Service = amazonS3Service; // 🟢 Field assigned
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
    }

    private String getFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            return contentType.startsWith("video") ? "video" :
                    contentType.startsWith("image") ? "image" : "other";
        }
        return "unknown";
    }

    // -----------------------------------------------------------------------------------
    // --- CRUD METHODS ---
    // -----------------------------------------------------------------------------------

    @Transactional
    public void saveContent(Content content, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("Cannot save content: Uploaded file is missing or empty.");
        }

        // 1. Upload the main file. This returns the S3 File Key (the path).
        // 🟢 Using amazonS3Service
        String fileKey = amazonS3Service.uploadFile(file);
        content.setFilePath(fileKey); 

        // 2. Determine file type
        String fileType = getFileType(file);
        content.setFileType(fileType);

        // 3. Generate and set the full public thumbnail URL
        if ("video".equals(fileType)) {
            // 🟢 S3 does not natively support on-the-fly video poster generation like ImageKit.
            // We use the S3 service's placeholder method, which typically returns the public URL of the video itself.
            // In a production scenario, you would need to pre-generate a thumbnail and upload it separately.
            String thumbnailUrl = amazonS3Service.getTransformedUrl(fileKey, "tr:w-400,c-at_max,f-jpg"); 
            content.setThumbnailUrl(thumbnailUrl);
        } else if ("image".equals(fileType)) {
            // 🟢 Use the standard S3 public URL for the image thumbnail
            content.setThumbnailUrl(amazonS3Service.getPublicUrl(fileKey));
        } else {
            content.setThumbnailUrl(null); 
        }

        content.setUploadDate(LocalDateTime.now());
        if (content.getViews() == null) content.setViews(0);
        if (content.getLikes() == null) content.setLikes(0);

        contentRepository.save(content);
    }

    @Transactional
    public void updateContent(Content updatedContent, MultipartFile file) throws IOException {

        Content existingContent = contentRepository.findById(updatedContent.getId())
                .orElseThrow(() -> new EntityNotFoundException("Content not found with ID: " + updatedContent.getId()));

        // Update metadata fields
        existingContent.setTitle(updatedContent.getTitle());
        existingContent.setDescription(updatedContent.getDescription());
        existingContent.setTags(updatedContent.getTags());

        if (file != null && !file.isEmpty()) {
            // OPTIONAL: Delete the OLD file first (uncomment if required)
            // try {
            //     amazonS3Service.deleteFile(existingContent.getFilePath());
            // } catch (Exception e) {
            //     System.err.println("Warning: Could not delete old file from S3: " + e.getMessage());
            //     // Continue with upload even if delete fails
            // }
            
            // 1. Upload new file (and get the new S3 File Key)
            // 🟢 Using amazonS3Service
            String newFileKey = amazonS3Service.uploadFile(file);
            existingContent.setFilePath(newFileKey);

            // 2. Determine new file type
            String fileType = getFileType(file);
            existingContent.setFileType(fileType);

            // 3. Update thumbnail URL if file changed
            if ("video".equals(fileType)) {
                 // 🟢 Using S3 service's placeholder/default transformed URL
                String thumbnailUrl = amazonS3Service.getTransformedUrl(newFileKey, "tr:w-400,c-at_max,f-jpg");
                existingContent.setThumbnailUrl(thumbnailUrl);
            } else if ("image".equals(fileType)) {
                existingContent.setThumbnailUrl(amazonS3Service.getPublicUrl(newFileKey));
            } else {
                existingContent.setThumbnailUrl(null);
            }
        }

        contentRepository.save(existingContent);
    }


    // -----------------------------------------------------------------------------------
    // --- VIEW & INTERACTION METHODS ---
    // -----------------------------------------------------------------------------------

    public List<Comment> getCommentsByContentId(Long contentId) {
        return commentRepository.findByContent_IdOrderByPostDateDesc(contentId);
    }

    public Content getById(Long id) {
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Content not found with ID: " + id));
        
        return content;
    }

    @Transactional
    public void delete(Long id) throws IOException { 
        Content content = getById(id);
        
        // Delete the file from the AWS S3 service first
        // 🟢 Using amazonS3Service
        amazonS3Service.deleteFile(content.getFilePath()); 
        
        // Then delete the record from the database
        contentRepository.delete(content);
    }

    @Transactional
    public void incrementViews(Long contentId) {
        contentRepository.incrementViews(contentId);
    }

    @Transactional
    public int toggleLike(Long contentId, Long userId) {
        Content content = getById(contentId);
        Optional<Like> existingLike = likeRepository.findByContentIdAndUserId(contentId, userId);

        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            content.setLikes(content.getLikes() - 1);
        } else {
            Like newLike = new Like(contentId, userId);
            likeRepository.save(newLike);
            content.setLikes(content.getLikes() + 1);
        }

        contentRepository.save(content);
        return content.getLikes();
    }

    public List<Long> getLikedContentIds(Long userId) {
        return likeRepository.findContentIdsByUserId(userId);
    }

    public boolean isLikedByUser(Long contentId, Long userId) {
        return likeRepository.findByContentIdAndUserId(contentId, userId).isPresent();
    }

    @Transactional
    public void addComment(Long contentId, String userName, String commentText) {
        Content content = contentRepository.getReferenceById(contentId);

        Comment comment = new Comment();
        comment.setContent(content);
        comment.setUserName(userName);
        comment.setText(commentText);
        comment.setPostDate(LocalDateTime.now());

        commentRepository.save(comment);
    }

    // -----------------------------------------------------------------------------------
    // --- PAGINATION / FILTERING METHODS ---
    // -----------------------------------------------------------------------------------

    public Page<Content> getPaginated(String keyword, int page, int pageSize) {
        Sort sort = Sort.by(Sort.Order.desc("uploadDate"));
        PageRequest pageable = PageRequest.of(page - 1, pageSize, sort);

        if (keyword != null && !keyword.isEmpty()) {
            return contentRepository.findByTitleContainingIgnoreCaseOrTagsContainingIgnoreCase(keyword, keyword, pageable);
        } else {
            return contentRepository.findAll(pageable);
        }
    }

    public Page<Content> getMostViewedPaginated(int page, int pageSize) {
        PageRequest pageable = PageRequest.of(page - 1, pageSize, Sort.by("views").descending());
        return contentRepository.findAll(pageable);
    }

    public Page<Content> getMostLikedPaginated(int page, int pageSize) {
        PageRequest pageable = PageRequest.of(page - 1, pageSize, Sort.by("likes").descending());
        return contentRepository.findAll(pageable);
    }

    public Page<Content> getVideosByTagPaginated(String tag, int page, int pageSize) {
        Sort sort = Sort.by(Sort.Order.desc("uploadDate"));
        PageRequest pageable = PageRequest.of(page - 1, pageSize, sort);
        return contentRepository.findByTagsContainingIgnoreCase(tag, pageable);
    }
}