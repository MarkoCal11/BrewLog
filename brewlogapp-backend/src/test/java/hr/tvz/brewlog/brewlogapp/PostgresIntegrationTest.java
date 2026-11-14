package hr.tvz.brewlog.brewlogapp;

import hr.tvz.brewlog.brewlogapp.dto.BrewsPerDayPoint;
import hr.tvz.brewlog.brewlogapp.enums.PeriodType;
import hr.tvz.brewlog.brewlogapp.enums.ReportSource;
import hr.tvz.brewlog.brewlogapp.enums.Role;
import hr.tvz.brewlog.brewlogapp.model.Report;
import hr.tvz.brewlog.brewlogapp.model.User;
import hr.tvz.brewlog.brewlogapp.repository.ReportRepository;
import hr.tvz.brewlog.brewlogapp.repository.UserRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.flyway.baseline-on-migrate=false",
        "spring.h2.console.enabled=false",
        "jwt.base64-secret=dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQ=",
        "spring.security.oauth2.client.registration.google.client-id=test",
        "spring.security.oauth2.client.registration.google.client-secret=test"
})
@Testcontainers(disabledWithoutDocker = true)
class PostgresIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired private Flyway flyway;
    @Autowired private UserRepository userRepository;
    @Autowired private ReportRepository reportRepository;

    @Test
    void flywayAppliesV1AndHibernateValidatesSchema() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
    }

    @Test
    void reportJsonColumnsRoundTripOnPostgres() {
        User user = userRepository.save(User.builder()
                .username("tc-user")
                .password("not-used")
                .email("tc@example.com")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build());

        LocalDate monday = LocalDate.of(2026, 11, 2);
        Report saved = reportRepository.save(Report.builder()
                .user(user)
                .periodType(PeriodType.WEEKLY)
                .periodStart(monday)
                .periodEnd(monday.plusDays(6))
                .generatedAt(LocalDateTime.now())
                .source(ReportSource.SCHEDULED)
                .brewCount(3)
                .brewsPerDay(List.of(new BrewsPerDayPoint(monday, 3)))
                .build());

        Report loaded = reportRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getBrewsPerDay()).containsExactly(new BrewsPerDayPoint(monday, 3));
    }
}