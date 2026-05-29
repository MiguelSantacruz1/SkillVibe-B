package com.skillvibe.tutoring.service;

import com.skillvibe.tutoring.dto.RegisterRequest;
import com.skillvibe.tutoring.dto.TutorRegistrationRequest;
import com.skillvibe.tutoring.exception.BusinessLogicException;
import com.skillvibe.tutoring.exception.UnauthorizedException;
import com.skillvibe.tutoring.model.Role;
import com.skillvibe.tutoring.model.TutorProfile;
import com.skillvibe.tutoring.model.User;
import com.skillvibe.tutoring.repository.TutorProfileRepository;
import com.skillvibe.tutoring.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(UserRepository userRepository,
                       TutorProfileRepository tutorProfileRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.tutorProfileRepository = tutorProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // ── Basic Queries ────────────────────────────────────────────────────────

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
    }

    // ── Student Registration ─────────────────────────────────────────────────

    public User registerUser(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessLogicException("El correo ya está registrado");
        }

        String token = generateToken();

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole() != null ? request.getRole() : Role.STUDENT);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerified(false);
        user.setVerificationToken(token);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));

        User saved = userRepository.save(user);

        // Envío asíncrono: no bloquea la respuesta HTTP
        emailService.sendVerificationEmail(saved.getEmail(), saved.getFullName(), token);

        return saved;
    }

    // ── Tutor Registration ───────────────────────────────────────────────────

    @Transactional
    public TutorProfile registerTutor(TutorRegistrationRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessLogicException("El correo ya está registrado");
        }

        String token = generateToken();

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setRole(Role.TUTOR);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerified(false);
        user.setVerificationToken(token);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));

        userRepository.save(user);

        TutorProfile profile = new TutorProfile();
        profile.setUser(user);
        profile.setBio(request.getBio());
        profile.setProfilePictureUrl(request.getProfilePictureUrl());
        profile.setIdentityCardUrl(request.getIdentityCardUrl());
        profile.setDegreeUrl(request.getDegreeUrl());
        profile.setHourlyRate(request.getHourlyRate());
        profile.setYearsOfExperience(request.getYearsOfExperience());
        profile.setSubjects(request.getSubjects());
        profile.setIsVerified(false);

        TutorProfile saved = tutorProfileRepository.save(profile);

        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), token);

        return saved;
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    public User login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthorizedException("Credenciales inválidas");
        }

        // Bloquear login si el correo no ha sido verificado
        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            throw new UnauthorizedException("Debes verificar tu correo antes de iniciar sesión. Revisa tu bandeja de entrada.");
        }

        return user;
    }

    // ── Email Verification ───────────────────────────────────────────────────

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BusinessLogicException("Token de verificación inválido o expirado"));

        if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BusinessLogicException("El enlace de verificación ha expirado. Solicita uno nuevo.");
        }

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BusinessLogicException("Este correo ya fue verificado anteriormente");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);

        log.info("Correo verificado para: {}", user.getEmail());
    }

    // Reenviar el email de verificación
    @Transactional
    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessLogicException("No existe una cuenta con ese correo"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BusinessLogicException("Este correo ya fue verificado");
        }
        
        if (user.getVerificationTokenExpiry() != null && user.getVerificationTokenExpiry().isAfter(LocalDateTime.now())) {
            throw new BusinessLogicException("Ya se ha enviado un correo de verificación reciente. Por favor revisa tu bandeja de entrada o spam.");
        }

        String token = generateToken();
        user.setVerificationToken(token);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), token);
    }

    // ── Password Recovery ────────────────────────────────────────────────────

    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessLogicException("No existe una cuenta con ese correo"));

        if (user.getPasswordResetTokenExpiry() != null && user.getPasswordResetTokenExpiry().isAfter(LocalDateTime.now())) {
            throw new BusinessLogicException("Ya has solicitado un restablecimiento de contraseña recientemente. Revisa tu correo o spam.");
        }

        String token = generateToken();
        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), token);
        log.info("Token de reset enviado a: {}", email);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new BusinessLogicException("Token inválido o expirado"));

        if (user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BusinessLogicException("El enlace de recuperación ha expirado. Solicita uno nuevo.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);

        log.info("Contraseña restablecida para: {}", user.getEmail());
    }

    // ── Utilities ────────────────────────────────────────────────────────────

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}