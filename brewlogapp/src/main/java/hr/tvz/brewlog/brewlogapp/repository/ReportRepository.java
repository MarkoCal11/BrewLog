package hr.tvz.brewlog.brewlogapp.repository;

import hr.tvz.brewlog.brewlogapp.enums.PeriodType;
import hr.tvz.brewlog.brewlogapp.model.Report;
import hr.tvz.brewlog.brewlogapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findAllByUser(User user);

    Optional<Report> findByIdAndUser(Long id, User user);

    Optional<Report> findByUserAndPeriodTypeAndPeriodStart(
            User user, PeriodType periodType, LocalDate periodStart);
}
