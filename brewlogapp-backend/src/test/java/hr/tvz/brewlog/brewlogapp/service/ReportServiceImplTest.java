package hr.tvz.brewlog.brewlogapp.service;

import hr.tvz.brewlog.brewlogapp.dto.ReportResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock private ReportRepository reportRepository;
    @Mock private AggregateReportRepository aggregateReportRepository;
    @Mock private BrewEntryRepository brewEntryRepository;
    @Mock private UserRepository userRepository;
    @Mock private ReportMapper reportMapper;
    @Mock private ReportStreamService reportStreamService;

    private ReportServiceImpl service;

    private User user;

    private static final ReportResponse SENTINEL = new ReportResponse(
            1L, PeriodType.WEEKLY, null, null, null, ReportSource.SCHEDULED, 1, null, null, null, null, null, null, null);

    @BeforeEach
    void setUp() {
        service = new ReportServiceImpl(reportRepository, aggregateReportRepository,
                brewEntryRepository, userRepository, reportMapper, reportStreamService);
        user = User.builder().id(1L).username("barista").build();
    }

    private BrewEntry brewInWindow() {
        BrewEntry brew = new BrewEntry();
        brew.setBrewedAt(LocalDateTime.now().minusDays(3)); // any instant; reconcile only needs it non-null
        return brew;
    }

    private BrewEntry brewOn(LocalDateTime brewedAt) {
        BrewEntry brew = new BrewEntry();
        brew.setBrewedAt(brewedAt);
        return brew;
    }

    @Nested
    class PerUserCatchUp {

        @Test
        void generatesAndNotifiesWhenReportMissing() {
            when(userRepository.findAll()).thenReturn(List.of(user));
            when(reportRepository.findByUserAndPeriodTypeAndPeriodStart(eq(user), eq(PeriodType.WEEKLY), any()))
                    .thenReturn(Optional.empty());
            when(brewEntryRepository.findByUserAndBrewedAtGreaterThanEqualAndBrewedAtLessThan(eq(user), any(), any()))
                    .thenReturn(List.of(brewInWindow()));
            when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));
            when(reportMapper.toResponse(any(Report.class))).thenReturn(SENTINEL);

            service.generatePreviousIfMissing(PeriodType.WEEKLY);

            ArgumentCaptor<Report> saved = ArgumentCaptor.forClass(Report.class);
            verify(reportRepository).save(saved.capture());
            assertEquals(ReportSource.SCHEDULED, saved.getValue().getSource());
            verify(reportStreamService).publish("barista", SENTINEL);
        }

        @Test
        void skipsUserWhoAlreadyHasReport() {
            when(userRepository.findAll()).thenReturn(List.of(user));
            when(reportRepository.findByUserAndPeriodTypeAndPeriodStart(eq(user), eq(PeriodType.WEEKLY), any()))
                    .thenReturn(Optional.of(new Report()));

            service.generatePreviousIfMissing(PeriodType.WEEKLY);

            // existing report is left untouched: no recompute, no duplicate nudge, no brew lookup
            verify(reportRepository, never()).save(any());
            verifyNoInteractions(brewEntryRepository, reportStreamService);
        }
    }

    @Nested
    class AggregateCatchUp {

        @Test
        void generatesWhenAggregateMissing() {
            when(aggregateReportRepository.findByPeriodTypeAndPeriodStart(eq(PeriodType.WEEKLY), any()))
                    .thenReturn(Optional.empty());
            when(brewEntryRepository.findByBrewedAtGreaterThanEqualAndBrewedAtLessThan(any(), any()))
                    .thenReturn(List.of(brewInWindow()));
            when(aggregateReportRepository.save(any(AggregateReport.class))).thenAnswer(inv -> inv.getArgument(0));

            service.generateAggregatePreviousIfMissing(PeriodType.WEEKLY);

            ArgumentCaptor<AggregateReport> saved = ArgumentCaptor.forClass(AggregateReport.class);
            verify(aggregateReportRepository).save(saved.capture());
            assertEquals(ReportSource.SCHEDULED, saved.getValue().getSource());
        }

        @Test
        void skipsWhenAggregateAlreadyExists() {
            when(aggregateReportRepository.findByPeriodTypeAndPeriodStart(eq(PeriodType.WEEKLY), any()))
                    .thenReturn(Optional.of(new AggregateReport()));

            service.generateAggregatePreviousIfMissing(PeriodType.WEEKLY);

            verify(aggregateReportRepository, never()).save(any());
            verifyNoInteractions(brewEntryRepository);
        }
    }

    @Nested
    class MultiWindowBackfill {

        @Test
        void backfillsEveryMissingWindowBackToEarliestBrew() {
            when(brewEntryRepository.findAll()).thenReturn(List.of(brewOn(LocalDateTime.now().minusDays(15))));
            when(userRepository.findAll()).thenReturn(List.of(user));
            when(reportRepository.findByUserAndPeriodTypeAndPeriodStart(eq(user), eq(PeriodType.WEEKLY), any()))
                    .thenReturn(Optional.empty());
            when(brewEntryRepository.findByUserAndBrewedAtGreaterThanEqualAndBrewedAtLessThan(eq(user), any(), any()))
                    .thenReturn(List.of(brewInWindow()));
            when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));
            when(reportMapper.toResponse(any(Report.class))).thenReturn(SENTINEL);

            service.backfillMissing(PeriodType.WEEKLY);

            // a ~15-day outage spans multiple weekly windows; single-window catch-up would save once
            verify(reportRepository, atLeast(2)).save(any(Report.class));
        }

        @Test
        void notifiesOnlyForTheMostRecentWindow() {
            when(brewEntryRepository.findAll()).thenReturn(List.of(brewOn(LocalDateTime.now().minusDays(15))));
            when(userRepository.findAll()).thenReturn(List.of(user));
            when(reportRepository.findByUserAndPeriodTypeAndPeriodStart(eq(user), eq(PeriodType.WEEKLY), any()))
                    .thenReturn(Optional.empty());
            when(brewEntryRepository.findByUserAndBrewedAtGreaterThanEqualAndBrewedAtLessThan(eq(user), any(), any()))
                    .thenReturn(List.of(brewInWindow()));
            when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));
            when(reportMapper.toResponse(any(Report.class))).thenReturn(SENTINEL);

            service.backfillMissing(PeriodType.WEEKLY);

            // older back-filled windows are surfaced silently — only the just-completed period nudges
            verify(reportStreamService, times(1)).publish(eq("barista"), any());
        }

        @Test
        void doesNothingWhenNoBrewsExist() {
            when(brewEntryRepository.findAll()).thenReturn(List.of());

            service.backfillMissing(PeriodType.WEEKLY);

            verify(reportRepository, never()).save(any());
            verifyNoInteractions(userRepository, reportStreamService);
        }
    }

    @Nested
    class EditReconcileCreation {

        @Test
        void recreatesReportForCompletedPeriodWhenBrewsReturn() {
            // 40 days back lands in both a completed week and a completed month
            LocalDate completedPeriodDate = LocalDate.now().minusDays(40);
            when(reportRepository.findByUserAndPeriodTypeAndPeriodStart(eq(user), any(), any()))
                    .thenReturn(Optional.empty());
            when(brewEntryRepository.findByUserAndBrewedAtGreaterThanEqualAndBrewedAtLessThan(eq(user), any(), any()))
                    .thenReturn(List.of(brewInWindow()));
            when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));

            service.reconcileReports(user, List.of(completedPeriodDate));

            // both the completed weekly and monthly windows recreate their missing report
            ArgumentCaptor<Report> saved = ArgumentCaptor.forClass(Report.class);
            verify(reportRepository, times(2)).save(saved.capture());
            // reports born from a brew edit are stamped RECONCILE so the client can keep them silent
            saved.getAllValues().forEach(r -> assertEquals(ReportSource.RECONCILE, r.getSource()));
        }

        @Test
        void preservesSourceWhenUpdatingExistingReport() {
            Report existing = new Report();
            existing.setSource(ReportSource.SCHEDULED);
            when(reportRepository.findByUserAndPeriodTypeAndPeriodStart(eq(user), any(), any()))
                    .thenReturn(Optional.of(existing));
            when(brewEntryRepository.findByUserAndBrewedAtGreaterThanEqualAndBrewedAtLessThan(eq(user), any(), any()))
                    .thenReturn(List.of(brewInWindow()));
            when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));

            service.reconcileReports(user, List.of(LocalDate.now().minusDays(40)));

            // an edit updates the existing scheduler report but must not relabel its origin
            ArgumentCaptor<Report> saved = ArgumentCaptor.forClass(Report.class);
            verify(reportRepository, times(2)).save(saved.capture());
            saved.getAllValues().forEach(r -> assertEquals(ReportSource.SCHEDULED, r.getSource()));
        }

        @Test
        void doesNotCreateReportForInProgressPeriod() {
            when(reportRepository.findByUserAndPeriodTypeAndPeriodStart(eq(user), any(), any()))
                    .thenReturn(Optional.empty());
            when(brewEntryRepository.findByUserAndBrewedAtGreaterThanEqualAndBrewedAtLessThan(eq(user), any(), any()))
                    .thenReturn(List.of(brewInWindow()));

            service.reconcileReports(user, List.of(LocalDate.now()));

            // current week and month are still in progress: an edit must not create a first-time report
            verify(reportRepository, never()).save(any());
        }

        @Test
        void reconcilesGlobalAggregateForCompletedPeriod() {
            LocalDate completedPeriodDate = LocalDate.now().minusDays(40);
            when(reportRepository.findByUserAndPeriodTypeAndPeriodStart(eq(user), any(), any()))
                    .thenReturn(Optional.empty());
            when(brewEntryRepository.findByUserAndBrewedAtGreaterThanEqualAndBrewedAtLessThan(eq(user), any(), any()))
                    .thenReturn(List.of(brewInWindow()));
            when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));
            // global (all-user) window query feeds the aggregate recompute
            when(brewEntryRepository.findByBrewedAtGreaterThanEqualAndBrewedAtLessThan(any(), any()))
                    .thenReturn(List.of(brewInWindow()));
            when(aggregateReportRepository.findByPeriodTypeAndPeriodStart(any(), any()))
                    .thenReturn(Optional.empty());
            when(aggregateReportRepository.save(any(AggregateReport.class))).thenAnswer(inv -> inv.getArgument(0));

            service.reconcileReports(user, List.of(completedPeriodDate));

            // the global aggregate is kept in step for both completed windows, not just the per-user report
            ArgumentCaptor<AggregateReport> savedAgg = ArgumentCaptor.forClass(AggregateReport.class);
            verify(aggregateReportRepository, times(2)).save(savedAgg.capture());
            // aggregates born from a brew edit are stamped RECONCILE so the admin view can keep them silent
            savedAgg.getAllValues().forEach(a -> assertEquals(ReportSource.RECONCILE, a.getSource()));
        }

        @Test
        void doesNotCreateGlobalAggregateForInProgressPeriod() {
            when(reportRepository.findByUserAndPeriodTypeAndPeriodStart(eq(user), any(), any()))
                    .thenReturn(Optional.empty());
            when(brewEntryRepository.findByUserAndBrewedAtGreaterThanEqualAndBrewedAtLessThan(eq(user), any(), any()))
                    .thenReturn(List.of(brewInWindow()));
            when(brewEntryRepository.findByBrewedAtGreaterThanEqualAndBrewedAtLessThan(any(), any()))
                    .thenReturn(List.of(brewInWindow()));
            when(aggregateReportRepository.findByPeriodTypeAndPeriodStart(any(), any()))
                    .thenReturn(Optional.empty());

            service.reconcileReports(user, List.of(LocalDate.now()));

            // in-progress period: a brew edit must not create a first-time aggregate either
            verify(aggregateReportRepository, never()).save(any());
        }
    }
}
