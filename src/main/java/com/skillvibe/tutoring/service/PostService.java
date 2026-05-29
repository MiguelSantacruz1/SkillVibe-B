package com.skillvibe.tutoring.service;

import com.skillvibe.tutoring.dto.CreateCommentDTO;
import com.skillvibe.tutoring.dto.CreatePostDTO;
import com.skillvibe.tutoring.dto.PostCommentResponseDTO;
import com.skillvibe.tutoring.dto.PostResponseDTO;
import com.skillvibe.tutoring.exception.BusinessLogicException;
import com.skillvibe.tutoring.model.Post;
import com.skillvibe.tutoring.model.PostComment;
import com.skillvibe.tutoring.model.User;
import com.skillvibe.tutoring.repository.PostCommentRepository;
import com.skillvibe.tutoring.repository.PostRepository;
import com.skillvibe.tutoring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostCommentRepository commentRepository;
    private final UserRepository userRepository;

    // ── Mappers ────────────────────────────────────────────────────────────────

    private PostCommentResponseDTO toCommentDTO(PostComment c) {
        return PostCommentResponseDTO.builder()
                .id(c.getId())
                .authorId(c.getAuthor().getId())
                .authorName(c.getAuthor().getFullName())
                .authorRole(c.getAuthor().getRole().name())
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private PostResponseDTO toDTO(Post post, Long requesterId) {
        List<PostCommentResponseDTO> comments = post.getComments().stream()
                .map(this::toCommentDTO)
                .collect(Collectors.toList());

        boolean likedByMe = requesterId != null && post.getLikedByUsers().contains(requesterId);

        return PostResponseDTO.builder()
                .id(post.getId())
                .authorId(post.getAuthor().getId())
                .authorName(post.getAuthor().getFullName())
                .authorRole(post.getAuthor().getRole().name())
                .content(post.getContent())
                .imageUrl(post.getImageUrl())
                .likesCount(post.getLikesCount())
                .likedByMe(likedByMe)
                .featured(post.getFeatured())
                .comments(comments)
                .createdAt(post.getCreatedAt())
                .build();
    }

    // ── Operations ─────────────────────────────────────────────────────────────

    /** Devuelve el feed paginado (todas las publicaciones, más recientes primero). */
    @Transactional(readOnly = true)
    public Page<PostResponseDTO> getFeed(int page, int size, Long requesterId) {
        return postRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(post -> toDTO(post, requesterId));
    }

    /** Devuelve las publicaciones destacadas (máximo 5). */
    @Transactional(readOnly = true)
    public List<PostResponseDTO> getFeatured(Long requesterId) {
        return postRepository.findTop5Featured()
                .stream()
                .map(post -> toDTO(post, requesterId))
                .collect(Collectors.toList());
    }

    /** Crea una nueva publicación. */
    @Transactional
    public PostResponseDTO createPost(Long authorId, CreatePostDTO dto) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new BusinessLogicException("Usuario no encontrado."));

        Post post = Post.builder()
                .author(author)
                .content(dto.getContent())
                .imageUrl(dto.getImageUrl())
                .build();

        Post saved = postRepository.save(post);
        log.info("Post creado por el usuario {} (id={})", author.getFullName(), authorId);
        return toDTO(saved, authorId);
    }

    /** Elimina una publicación. Solo el autor o un admin puede hacerlo. */
    @Transactional
    public void deletePost(Long postId, Long requesterId, boolean isAdmin) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessLogicException("Publicación no encontrada."));

        if (!isAdmin && !post.getAuthor().getId().equals(requesterId)) {
            throw new BusinessLogicException("No tienes permiso para eliminar esta publicación.");
        }

        postRepository.delete(post);
        log.info("Post {} eliminado por usuario {}", postId, requesterId);
    }

    /** Agrega un comentario a una publicación. */
    @Transactional
    public PostCommentResponseDTO addComment(Long postId, Long authorId, CreateCommentDTO dto) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessLogicException("Publicación no encontrada."));

        long userCommentsCount = post.getComments().stream()
                .filter(c -> c.getAuthor().getId().equals(authorId))
                .count();

        if (userCommentsCount >= 5) {
            throw new BusinessLogicException("Has alcanzado el límite de 5 comentarios en esta publicación.");
        }

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new BusinessLogicException("Usuario no encontrado."));

        PostComment comment = PostComment.builder()
                .post(post)
                .author(author)
                .content(dto.getContent())
                .build();

        PostComment saved = commentRepository.save(comment);
        return toCommentDTO(saved);
    }

    /** Da o quita un like a una publicación. */
    @Transactional
    public PostResponseDTO toggleLike(Long postId, Long requesterId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessLogicException("Publicación no encontrada."));

        if (post.getLikedByUsers().contains(requesterId)) {
            post.getLikedByUsers().remove(requesterId);
        } else {
            post.getLikedByUsers().add(requesterId);
        }
        
        post.setLikesCount(post.getLikedByUsers().size());
        return toDTO(postRepository.save(post), requesterId);
    }

    /** Marca o desmarca una publicación como destacada (solo admin). */
    @Transactional
    public PostResponseDTO toggleFeatured(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessLogicException("Publicación no encontrada."));

        post.setFeatured(!post.getFeatured());
        return toDTO(postRepository.save(post), requesterId);
    }
}
