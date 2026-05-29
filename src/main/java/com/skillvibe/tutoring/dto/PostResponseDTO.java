package com.skillvibe.tutoring.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PostResponseDTO {
    private Long id;
    private Long authorId;
    private String authorName;
    private String authorRole;
    private String content;
    private String imageUrl;
    private Integer likesCount;
    private Boolean featured;
    private List<PostCommentResponseDTO> comments;
    private LocalDateTime createdAt;
}
