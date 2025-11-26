package com.example.website.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "content")
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Mandatory Fields (Assuming these are NOT NULL in your DB) ---
    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String fileType;

    // ⭐ NEW FIELD: Stores the public URL/path for the video thumbnail ⭐
    @Column(nullable = true) // Can be null for non-video content
    private String thumbnailUrl;
    
    @Column(nullable = false)
    private LocalDateTime uploadDate;

    @Column(nullable = false)
    private Integer views = 0;

    @Column(nullable = false)
    private Integer likes = 0;

    // --- Optional/Other Fields ---
    @Column(columnDefinition = "TEXT") // Good for potentially long text descriptions
    private String description;
    
    // For searching/filtering
    private String tags; 

    // 💡 CRITICAL FIX: Relationship for Comments
    // This defines a One-to-Many relationship where one Content item has many Comments.
    // cascade = CascadeType.ALL ensures comments are saved/deleted with the content.
    // orphanRemoval = true ensures comments are deleted if removed from the collection.
    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>(); 
    // --- Constructor (Optional, but useful) ---
    public Content() {}
    
    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    // ⭐ NEW Getter for thumbnailUrl ⭐
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    // ⭐ NEW Setter for thumbnailUrl ⭐
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public Integer getViews() {
        return views;
    }

    public void setViews(Integer views) {
        this.views = views;
    }

    public Integer getLikes() {
        return likes;
    }

    public void setLikes(Integer likes) {
        this.likes = likes;
    }

    // 💡 CRITICAL: Getter for the comments list
    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }
    
    /**
     * Helper method to manage the bidirectional relationship when adding a comment.
     * It ensures both sides of the relationship are linked.
     */
    public void addComment(Comment comment) {
        comments.add(comment);
        // ⭐ CRITICAL FIX: Explicitly set the 'content' reference on the Comment side
        comment.setContent(this); 
    }
}