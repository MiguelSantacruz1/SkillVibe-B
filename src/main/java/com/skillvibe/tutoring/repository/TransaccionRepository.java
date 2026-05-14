package com.skillvibe.tutoring.repository;

import com.skillvibe.tutoring.model.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {
    List<Transaccion> findByUserIdOrderByTimestampDesc(Long userId);
}
