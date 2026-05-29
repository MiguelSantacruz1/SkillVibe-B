package com.skillvibe.tutoring.scheduler;

import com.skillvibe.tutoring.model.ClassStatus;
import com.skillvibe.tutoring.model.TutoringClass;
import com.skillvibe.tutoring.repository.TutoringClassRepository;
import com.skillvibe.tutoring.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Scheduler que corre cada 5 minutos y envía recordatorios por email
 * a tutores y estudiantes cuya clase empiece en los próximos 10-20 minutos.
 */
@Slf4j
@Component
public class ReminderScheduler {

    private final TutoringClassRepository tutoringClassRepository;
    private final EmailService emailService;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' hh:mm a");

    public ReminderScheduler(TutoringClassRepository tutoringClassRepository,
                             EmailService emailService) {
        this.tutoringClassRepository = tutoringClassRepository;
        this.emailService = emailService;
    }

    @Scheduled(fixedRate = 300000) // Corre cada 5 minutos (300,000 ms)
    public void sendClassReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.plusMinutes(10);
        LocalDateTime to = now.plusMinutes(20);

        List<TutoringClass> upcomingClasses = tutoringClassRepository
                .findByStatusAndScheduledAtBetween(ClassStatus.PROGRAMMED, from, to);

        if (upcomingClasses.isEmpty()) {
            log.debug("[Scheduler] No hay clases próximas en los siguientes 10-20 min.");
            return;
        }

        log.info("[Scheduler] Enviando recordatorios para {} clase(s) próximas.", upcomingClasses.size());

        for (TutoringClass tc : upcomingClasses) {
            String scheduledStr = tc.getScheduledAt().format(FORMATTER);

            // Notificar al ESTUDIANTE
            emailService.sendClassReminderEmail(
                    tc.getStudent().getEmail(),
                    tc.getStudent().getFullName(),
                    tc.getSubject(),
                    scheduledStr,
                    tc.getMeetingLink()
            );

            // Notificar al TUTOR
            emailService.sendClassReminderEmail(
                    tc.getTutor().getEmail(),
                    tc.getTutor().getFullName(),
                    tc.getSubject(),
                    scheduledStr,
                    tc.getMeetingLink()
            );

            log.info("[Scheduler] Recordatorio enviado para clase ID={} ({})",
                    tc.getId(), tc.getSubject());
        }
    }
}
