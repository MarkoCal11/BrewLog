package hr.tvz.brewlog.brewlogapp.repository;

import hr.tvz.brewlog.brewlogapp.enums.PeriodType;
import hr.tvz.brewlog.brewlogapp.model.AggregateReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AggregateReportRepository extends JpaRepository<AggregateReport, Long> {

    Optional<AggregateReport> findByPeriodTypeAndPeriodStart(PeriodType periodType, LocalDate periodStart);
}
