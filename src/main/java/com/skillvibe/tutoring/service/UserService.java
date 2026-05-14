package com.skillvibe.tutoring.service;

import com.skillvibe.tutoring.model.User;
import com.skillvibe.tutoring.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.skillvibe.tutoring.model.TutorProfile;
import com.skillvibe.tutoring.repository.TutorProfileRepository;
import com.skillvibe.tutoring.dto.TutorRegistrationRequest;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, TutorProfileRepository tutorProfileRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tutorProfileRepository = tutorProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @SuppressWarnings("null")
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
    }

    public User registerUser(com.skillvibe.tutoring.dto.RegisterRequest request) {
        // 1. Validar que el email no exista
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new com.skillvibe.tutoring.exception.BusinessLogicException("Email already exists");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole() != null ? request.getRole() : com.skillvibe.tutoring.model.Role.STUDENT);

        // 2. ENCRIPTAR la contraseña antes de guardar
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        user.setPassword(encodedPassword);

        return userRepository.save(user);
    }

    @Transactional
    public TutorProfile registerTutor(TutorRegistrationRequest request) {
        // 1. Validar que el email no exista
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new com.skillvibe.tutoring.exception.BusinessLogicException("Email already exists");
        }

        // 2. Crear el Usuario base
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setRole(com.skillvibe.tutoring.model.Role.TUTOR);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        userRepository.save(user);

        // 3. Crear el Perfil de Tutor
        TutorProfile profile = new TutorProfile();
        profile.setUser(user);
        profile.setBio(request.getBio());
        profile.setProfilePictureUrl(request.getProfilePictureUrl());
        profile.setIdentityCardUrl(request.getIdentityCardUrl());
        profile.setDegreeUrl(request.getDegreeUrl());
        profile.setHourlyRate(request.getHourlyRate());
        profile.setYearsOfExperience(request.getYearsOfExperience());
        profile.setSubjects(request.getSubjects());
        profile.setIsVerified(false); // Requiere validación manual del admin

        return tutorProfileRepository.save(profile);
    }

    // Verifica si las credenciales son correctas
    public User login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Comparamos la clave escrita con la encriptada en la base de datos
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        return user; // Si todo está bien, devolvemos el usuario
    }
}