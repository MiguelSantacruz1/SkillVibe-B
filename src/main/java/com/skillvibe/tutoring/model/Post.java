package com.skillvibe.tutoring.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una publicación del feed de la comunidad.
 * Cualquier usuario autenticado (estudiante, tutor o admin) puede publicar.
 */
@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Autor de la publicación */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /** Contenido de texto de la publicación */
    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank
    @Size(min = 1, max = 2000)
    private String content;

    /** URL de imagen opcional o base64 */
    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    /** Número de "likes" */
    @Column(nullable = false)
    @Builder.Default
    private Integer likesCount = 0;

    /** Indica si la publicación está destacada (solo admin puede marcarla) */
    @Column(nullable = false)
    @Builder.Default
    private Boolean featured = false;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostComment> comments = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
