package hr.tvz.brewlog.brewlogapp.service;

import hr.tvz.brewlog.brewlogapp.dto.AdminReportResponse;
import hr.tvz.brewlog.brewlogapp.dto.BrewsPerDayPoint;
import hr.tvz.brewlog.brewlogapp.dto.MethodBreakdownPoint;
import hr.tvz.brewlog.brewlogapp.dto.RatingTrendPoint;
import hr.tvz.brewlog.brewlogapp.dto.ReportResponse;
import hr.tvz.brewlog.brewlogapp.enums.Method;
import hr.tvz.brewlog.brewlogapp.enums.PeriodType;
import hr.tvz.brewlog.brewlogapp.enums.ReportSource;
import hr.tvz.brewlog.brewlogapp.mapper.ReportMapper;
import hr.tvz.brewlog.brewlogapp.model.AggregateReport;
import hr.tvz.brewlog.brewlogapp.model.BrewEntry;
import hr.tvz.brewlog.brewlogapp.model.Report;
import hr.tvz.brewlog.brewlogapp.model.User;
import hr.tvz.brewlog.brewlogapp.repository.AggregateReportRepository;
import hr.tvz.brewlog.brewlogapp.repository.BrewEntryRepository;
import hr.tvz.brewlog.brewlogapp.repository.ReportRepository;
import hr.tvz.brewlog.brewlogapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final AggregateReportRepository aggregateReportRepository;
    private final BrewEntryRepository brewEntryRepository;
    private final UserRepository userRepository;
    private final ReportMapper reportMapper;
    private final ReportStreamService reportStreamService;

    private User resolveUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> findAllReports(String username) {
        User user = resolveUser(username);
        return reportRepository.findAllByUser(user).stream()
                .map(reportMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReportResponse> findReportById(Long id, String username) {
        User user = resolveUser(username);
        return reportRepository.findByIdAndUser(id, user)
                .map(reportMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminReportResponse> findAllReportsGlobal() {
        return aggregateReportRepository.findAll().stream()
                .map(reportMapper::toAdminResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdminReportResponse> findReportByIdGlobal(Long id) {
        return aggregateReportRepository.findById(id)
                .map(reportMapper::toAdminResponse);
    }

    @Override
    public void generatePrevious(PeriodType periodType) {
        ReportWindow window = previousWindow(periodType, LocalDate.now());

        List<PendingNudge> nudges = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            reconcile(user, periodType, window, true, ReportSource.SCHEDULED)
                    .ifPresent(saved -> nudges.add(
                            new PendingNudge(user.getUsername(), reportMapper.toResponse(saved))));
        }

        publishAfterCommit(nudges);
    }

    @Override
    public void generatePreviousIfMissing(PeriodType periodType) {
        publishAfterCommit(generateWindowForAllUsersIfMissing(
                periodType, previousWindow(periodType, LocalDate.now())));
    }

    @Override
    public void backfillMissing(PeriodType periodType) {
        LocalDate earliest = earliestBrewedDate();
        if (earliest == null) {
            return; // no brews anywhere — nothing to back-fill
        }
        List<PendingNudge> nudges = new ArrayList<>();
        ReportWindow window = previousWindow(periodType, LocalDate.now());
        boolean mostRecentWindow = true;
        while (!window.end().isBefore(earliest)) {
            List<PendingNudge> windowNudges = generateWindowForAllUsersIfMissing(periodType, window);
            if (mostRecentWindow) {
                // Only the just-completed period notifies; older back-filled reports are surfaced silently.
                nudges.addAll(windowNudges);
                mostRecentWindow = false;
            }
            window = earlierWindow(periodType, window);
        }
        publishAfterCommit(nudges);
    }

    private List<PendingNudge> generateWindowForAllUsersIfMissing(PeriodType periodType, ReportWindow window) {
        List<PendingNudge> nudges = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            boolean alreadyGenerated = reportRepository
                    .findByUserAndPeriodTypeAndPeriodStart(user, periodType, window.start())
                    .isPresent();
            if (alreadyGenerated) {
                continue; // scheduler (or an earlier catch-up) already produced it — don't recompute or re-notify
            }
            reconcile(user, periodType, window, true, ReportSource.SCHEDULED)
                    .ifPresent(saved -> nudges.add(
                            new PendingNudge(user.getUsername(), reportMapper.toResponse(saved))));
        }
        return nudges;
    }

    @Override
    public void reconcileReports(User user, Collection<LocalDate> affectedDates) {
        Set<String> handledWindows = new HashSet<>();
        for (LocalDate date : affectedDates) {
            for (PeriodType periodType : PeriodType.values()) {
                ReportWindow window = windowContaining(periodType, date);
                if (handledWindows.add(periodType + "@" + window.start())) {
                    // A completed period regenerates its report when brews return to a window an earlier
                    // edit had emptied, so reverting a brew's date restores the report it deleted. The
                    // in-progress period stays uncreated — first-time creation there is the scheduler's job.
                    boolean periodCompleted = window.end().isBefore(LocalDate.now());
                    reconcile(user, periodType, window, periodCompleted, ReportSource.RECONCILE);
                    // Keep the global aggregate in step with the same window (recomputed across all users),
                    // so retroactive brew edits don't leave admin stats drifting. Same completed-period gate.
                    generateAggregateForWindow(periodType, window, periodCompleted, ReportSource.RECONCILE);
                }
            }
        }
    }

    /**
     * Brings one user's report for a single period window in line with the brews currently in that
     * window: deletes the report when no brews remain, otherwise (re)computes it. When no report
     * exists and {@code createIfMissing} is false, does nothing — on-demand reconciliation only
     * refreshes reports that already exist; first-time creation for a completed period stays the
     * scheduler's job.
     *
     * <p>{@code source} is stamped only when a new report row is created; an existing report keeps
     * the source it was first created with, so a scheduler-produced report isn't relabelled when a
     * later brew edit happens to update it.
     *
     * @return the saved report when one was created or updated; empty when it was deleted or skipped
     */
    private Optional<Report> reconcile(User user, PeriodType periodType, ReportWindow window,
                                       boolean createIfMissing, ReportSource source) {
        LocalDateTime from = window.start().atStartOfDay();
        LocalDateTime toExclusive = window.end().plusDays(1).atStartOfDay();
        List<BrewEntry> brews = brewEntryRepository
                .findByUserAndBrewedAtGreaterThanEqualAndBrewedAtLessThan(user, from, toExclusive);
        Optional<Report> existing = reportRepository
                .findByUserAndPeriodTypeAndPeriodStart(user, periodType, window.start());

        if (brews.isEmpty()) {
            existing.ifPresent(reportRepository::delete);
            return Optional.empty();
        }
        if (existing.isEmpty() && !createIfMissing) {
            return Optional.empty();
        }

        Report report = existing.orElseGet(Report::new);
        if (existing.isEmpty()) {
            report.setSource(source); // stamp origin on creation; preserve it on later updates
        }
        report.setUser(user);
        report.setPeriodType(periodType);
        report.setPeriodStart(window.start());
        report.setPeriodEnd(window.end());
        report.setGeneratedAt(LocalDateTime.now());
        report.setBrewCount(brews.size());
        report.setAverageRating(averageRating(brews));
        report.setTotalDoseGrams(totalDose(brews));
        report.setTopBeanName(topBeanName(brews));
        report.setMostUsedMethod(mostUsedMethod(brews));
        report.setBrewsPerDay(brewsPerDay(brews));
        report.setMethodBreakdown(methodBreakdown(brews));
        report.setRatingTrend(ratingTrend(brews));

        return Optional.of(reportRepository.save(report));
    }

    /**
     * Pushes the "new report" nudges to connected clients once the surrounding transaction commits,
     * so a client refetch triggered by the event sees the persisted rows. When no transaction is
     * active (e.g. a direct unit-test call), publishes immediately.
     */
    private void publishAfterCommit(List<PendingNudge> nudges) {
        if (nudges.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    nudges.forEach(n -> reportStreamService.publish(n.username(), n.report()));
                }
            });
        } else {
            nudges.forEach(n -> reportStreamService.publish(n.username(), n.report()));
        }
    }

    private record PendingNudge(String username, ReportResponse report) {}

    @Override
    public void generateAggregatePrevious(PeriodType periodType) {
        generateAggregateForWindow(periodType, previousWindow(periodType, LocalDate.now()), true, ReportSource.SCHEDULED);
    }

    @Override
    public void backfillAggregateMissing(PeriodType periodType) {
        LocalDate earliest = earliestBrewedDate();
        if (earliest == null) {
            return; // no brews anywhere — nothing to back-fill
        }
        ReportWindow window = previousWindow(periodType, LocalDate.now());
        while (!window.end().isBefore(earliest)) {
            boolean alreadyGenerated = aggregateReportRepository
                    .findByPeriodTypeAndPeriodStart(periodType, window.start())
                    .isPresent();
            if (!alreadyGenerated) {
                generateAggregateForWindow(periodType, window, true, ReportSource.SCHEDULED);
            }
            window = earlierWindow(periodType, window);
        }
    }

    private void generateAggregateForWindow(PeriodType periodType, ReportWindow window,
                                            boolean createIfMissing, ReportSource source) {
        LocalDateTime from = window.start().atStartOfDay();
        LocalDateTime toExclusive = window.end().plusDays(1).atStartOfDay();

        List<BrewEntry> brews = brewEntryRepository
                .findByBrewedAtGreaterThanEqualAndBrewedAtLessThan(from, toExclusive);
        Optional<AggregateReport> existing = aggregateReportRepository
                .findByPeriodTypeAndPeriodStart(periodType, window.start());

        if (brews.isEmpty()) {
            existing.ifPresent(aggregateReportRepository::delete);
            return;
        }
        if (existing.isEmpty() && !createIfMissing) {
            return; // on-demand reconcile only refreshes an existing aggregate; first creation stays the scheduler's job
        }

        AggregateReport report = existing.orElseGet(AggregateReport::new);
        if (existing.isEmpty()) {
            report.setSource(source); // stamp origin on creation; preserve it on later updates
        }

        report.setPeriodType(periodType);
        report.setPeriodStart(window.start());
        report.setPeriodEnd(window.end());
        report.setGeneratedAt(LocalDateTime.now());
        report.setBrewCount(brews.size());
        report.setActiveUsers(activeUsers(brews));
        report.setAverageRating(averageRating(brews));
        report.setTotalDoseGrams(totalDose(brews));
        report.setTopBeanName(topBeanName(brews));
        report.setMostUsedMethod(mostUsedMethod(brews));
        report.setBrewsPerDay(brewsPerDay(brews));
        report.setMethodBreakdown(methodBreakdown(brews));
        report.setRatingTrend(ratingTrend(brews));

        aggregateReportRepository.save(report);
    }

    @Override
    public void generateAggregatePreviousIfMissing(PeriodType periodType) {
        ReportWindow window = previousWindow(periodType, LocalDate.now());
        boolean alreadyGenerated = aggregateReportRepository
                .findByPeriodTypeAndPeriodStart(periodType, window.start())
                .isPresent();
        if (!alreadyGenerated) {
            generateAggregatePrevious(periodType);
        }
    }

    private int activeUsers(List<BrewEntry> brews) {
        return (int) brews.stream()
                .map(BrewEntry::getUser)
                .filter(Objects::nonNull)
                .map(User::getId)
                .distinct()
                .count();
    }

    private Double averageRating(List<BrewEntry> brews) {
        OptionalDouble avg = brews.stream()
                .map(BrewEntry::getRating)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average();
        return avg.isPresent() ? avg.getAsDouble() : null;
    }

    private Double totalDose(List<BrewEntry> brews) {
        return brews.stream()
                .map(BrewEntry::getDoseGrams)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    private String topBeanName(List<BrewEntry> brews) {
        return brews.stream()
                .filter(b -> b.getCoffeeBean() != null && b.getCoffeeBean().getName() != null)
                .collect(Collectors.groupingBy(b -> b.getCoffeeBean().getName(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private Method mostUsedMethod(List<BrewEntry> brews) {
        return brews.stream()
                .filter(b -> b.getMethod() != null)
                .collect(Collectors.groupingBy(BrewEntry::getMethod, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private List<BrewsPerDayPoint> brewsPerDay(List<BrewEntry> brews) {
        return brews.stream()
                .collect(Collectors.groupingBy(b -> b.getBrewedAt().toLocalDate(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new BrewsPerDayPoint(e.getKey(), e.getValue()))
                .toList();
    }

    private List<MethodBreakdownPoint> methodBreakdown(List<BrewEntry> brews) {
        return brews.stream()
                .filter(b -> b.getMethod() != null)
                .collect(Collectors.groupingBy(BrewEntry::getMethod, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<Method, Long>comparingByValue().reversed())
                .map(e -> new MethodBreakdownPoint(e.getKey(), e.getValue()))
                .toList();
    }

    private List<RatingTrendPoint> ratingTrend(List<BrewEntry> brews) {
        return brews.stream()
                .filter(b -> b.getRating() != null)
                .collect(Collectors.groupingBy(b -> b.getBrewedAt().toLocalDate(),
                        Collectors.averagingDouble(BrewEntry::getRating)))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new RatingTrendPoint(e.getKey(), e.getValue()))
                .toList();
    }

    private ReportWindow previousWindow(PeriodType type, LocalDate reference) {
        if (type == PeriodType.WEEKLY) {
            LocalDate start = reference.minusWeeks(1).with(WeekFields.ISO.dayOfWeek(), 1);
            return new ReportWindow(start, start.plusDays(6));
        }
        LocalDate firstThisMonth = reference.withDayOfMonth(1);
        return new ReportWindow(firstThisMonth.minusMonths(1), firstThisMonth.minusDays(1));
    }

    /** The window of the same type immediately before {@code window}. */
    private ReportWindow earlierWindow(PeriodType type, ReportWindow window) {
        if (type == PeriodType.WEEKLY) {
            LocalDate start = window.start().minusWeeks(1);
            return new ReportWindow(start, start.plusDays(6));
        }
        LocalDate start = window.start().minusMonths(1);
        return new ReportWindow(start, start.plusMonths(1).minusDays(1));
    }

    /** Earliest brew date across all users, or {@code null} when there are none — bounds the catch-up walk. */
    private LocalDate earliestBrewedDate() {
        return brewEntryRepository.findAll().stream()
                .map(BrewEntry::getBrewedAt)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .map(LocalDateTime::toLocalDate)
                .orElse(null);
    }

    /** The period window of the given type that contains {@code date}. */
    private ReportWindow windowContaining(PeriodType type, LocalDate date) {
        if (type == PeriodType.WEEKLY) {
            LocalDate start = date.with(WeekFields.ISO.dayOfWeek(), 1);
            return new ReportWindow(start, start.plusDays(6));
        }
        LocalDate start = date.withDayOfMonth(1);
        return new ReportWindow(start, start.plusMonths(1).minusDays(1));
    }

    private record ReportWindow(LocalDate start, LocalDate end) {}
}
