package com.skillvibe.tutoring.controller;

import com.skillvibe.tutoring.model.TutorProfile;
import com.skillvibe.tutoring.model.User;
import com.skillvibe.tutoring.repository.TutorProfileRepository;
import com.skillvibe.tutoring.repository.UserRepository;
import com.skillvibe.tutoring.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@SuppressWarnings("null")
class TutorControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TutorProfileRepository tutorProfileRepository;

    @Autowired
    private JwtService jwtService;

    private String studentToken;
    private String tutorToken;

    @BeforeEach
    void setUp() {
        // Clean up
        tutorProfileRepository.deleteAll();
        userRepository.deleteAll();

        // Create a student
        User student = new User();
        student.setEmail("student@skillvibe.com");
        student.setPassword("pass");
        student.setFullName("Student");
        student.setRole(com.skillvibe.tutoring.model.Role.STUDENT);
        userRepository.save(student);
        studentToken = jwtService.generateToken(student);

        // Create a tutor
        User tutor = new User();
        tutor.setEmail("tutor@skillvibe.com");
        tutor.setPassword("pass");
        tutor.setFullName("Tutor");
        tutor.setRole(com.skillvibe.tutoring.model.Role.TUTOR);
        userRepository.save(tutor);
        tutorToken = jwtService.generateToken(tutor);

        // Create tutor profile
        TutorProfile profile = new TutorProfile();
        profile.setUser(tutor);
        profile.setHourlyRate(25.0);
        profile.setYearsOfExperience(5);
        profile.setBio("Math Tutor");
        profile.setSubjects(List.of("Math", "Physics"));
        profile.setIsVerified(true);
        profile.setIdentityCardUrl("http://example.com/id.jpg");
        profile.setDegreeUrl("http://example.com/degree.pdf");
        tutorProfileRepository.save(profile);
    }

    @Test
    void searchTutors_returnsPaginatedResults() throws Exception {
        mockMvc.perform(get("/api/tutor/search")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].email").value("tutor@skillvibe.com"));
    }

    @Test
    void getMyProfile_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/tutor/profile")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyProfile_withStudentToken_returns403() throws Exception {
        mockMvc.perform(get("/api/tutor/profile")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyProfile_withTutorToken_returns200() throws Exception {
        mockMvc.perform(get("/api/tutor/profile")
                .header("Authorization", "Bearer " + tutorToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hourlyRate").value(25.0));
    }
}
