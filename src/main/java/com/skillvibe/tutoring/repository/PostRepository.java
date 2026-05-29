package com.skillvibe.tutoring.repository;

import com.skillvibe.tutoring.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /** Feed principal paginado, ordenado por fecha desc */
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Publicaciones destacadas (máximo 5) */
    @Query("SELECT p FROM Post p WHERE p.featured = true ORDER BY p.createdAt DESC")
    List<Post> findTop5Featured();
}
