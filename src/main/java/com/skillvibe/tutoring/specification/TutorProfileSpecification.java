package com.skillvibe.tutoring.specification;

import com.skillvibe.tutoring.model.TutorProfile;
import com.skillvibe.tutoring.model.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class TutorProfileSpecification {

    public static Specification<TutorProfile> filterByCriteria(
            String query,
            String subject,
            Double minPrice,
            Double maxPrice,
            Integer minExperience,
            Boolean onlyVerified
    ) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by full name (joining with User)
            if (query != null && !query.isEmpty()) {
                Join<TutorProfile, User> userJoin = root.join("user");
                predicates.add(cb.like(cb.lower(userJoin.get("fullName")), "%" + query.toLowerCase() + "%"));
            }

            // Filter by subject
            if (subject != null && !subject.isEmpty()) {
                predicates.add(cb.isMember(subject, root.get("subjects")));
            }

            // Filter by hourly rate
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("hourlyRate"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("hourlyRate"), maxPrice));
            }

            // Filter by experience
            if (minExperience != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("yearsOfExperience"), minExperience));
            }

            // Only verified tutors
            if (onlyVerified != null && onlyVerified) {
                predicates.add(cb.equal(root.get("isVerified"), true));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
