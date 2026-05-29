package com.skillvibe.tutoring.controller;

import com.skillvibe.tutoring.dto.*;
import com.skillvibe.tutoring.security.UserPrincipal;
import com.skillvibe.tutoring.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "Comunidad", description = "Feed de publicaciones de la comunidad SkillVibes")
@SecurityRequirement(name = "BearerAuth")
public class PostController {

    private final PostService postService;

    @Operation(summary = "Obtener feed paginado de publicaciones")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<PostResponseDTO>>> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<PostResponseDTO> feed = postService.getFeed(page, size);
        return ResponseEntity.ok(ApiResponse.success("Feed obtenido", feed));
    }

    @Operation(summary = "Obtener publicaciones destacadas")
    @GetMapping("/featured")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PostResponseDTO>>> getFeatured() {
        List<PostResponseDTO> featured = postService.getFeatured();
        return ResponseEntity.ok(ApiResponse.success("Publicaciones destacadas obtenidas", featured));
    }

    @Operation(summary = "Crear una nueva publicación")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PostResponseDTO>> createPost(
            @Valid @RequestBody CreatePostDTO dto,
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        PostResponseDTO post = postService.createPost(principal.getId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Publicación creada exitosamente", post));
    }

    @Operation(summary = "Eliminar una publicación (autor o admin)")
    @DeleteMapping("/{postId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Long postId,
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        postService.deletePost(postId, principal.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Publicación eliminada", null));
    }

    @Operation(summary = "Agregar un comentario a una publicación")
    @PostMapping("/{postId}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PostCommentResponseDTO>> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentDTO dto,
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        PostCommentResponseDTO comment = postService.addComment(postId, principal.getId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comentario agregado", comment));
    }

    @Operation(summary = "Dar like a una publicación")
    @PutMapping("/{postId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PostResponseDTO>> likePost(@PathVariable Long postId) {
        PostResponseDTO post = postService.toggleLike(postId);
        return ResponseEntity.ok(ApiResponse.success("Like registrado", post));
    }

    @Operation(summary = "Marcar/desmarcar publicación como destacada (solo admin)")
    @PutMapping("/{postId}/featured")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PostResponseDTO>> toggleFeatured(@PathVariable Long postId) {
        PostResponseDTO post = postService.toggleFeatured(postId);
        return ResponseEntity.ok(ApiResponse.success("Estado destacado actualizado", post));
    }
}
