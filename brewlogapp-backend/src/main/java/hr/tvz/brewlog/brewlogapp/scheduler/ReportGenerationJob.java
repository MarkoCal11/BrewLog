package hr.tvz.brewlog.brewlogapp.scheduler;

import hr.tvz.brewlog.brewlogapp.enums.PeriodType;
import hr.tvz.brewlog.brewlogapp.service.ReportService;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

public class ReportGenerationJob extends QuartzJobBean {

    private final ReportService reportService;

    private static final Logger log = LoggerFactory.getLogger(ReportGenerationJob.class);

    public ReportGenerationJob(ReportService reportService) {
        this.reportService = reportService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        PeriodType periodType = PeriodType.valueOf(
                context.getMergedJobDataMap().getString("periodType"));
        log.info("ReportGenerationJob FIRED for {} at {}", periodType, java.time.LocalDateTime.now());
        reportService.generatePrevious(periodType);
    }
}
