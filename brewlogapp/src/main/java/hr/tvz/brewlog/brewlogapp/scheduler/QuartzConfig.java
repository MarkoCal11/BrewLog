package hr.tvz.brewlog.brewlogapp.scheduler;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail weeklyReportJobDetail() {
        return JobBuilder.newJob(ReportGenerationJob.class)
                .withIdentity("weeklyReportJob")
                .usingJobData("periodType", "WEEKLY")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger weeklyReportTrigger(JobDetail weeklyReportJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(weeklyReportJobDetail)
                .withIdentity("weeklyReportTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 5 0 ? * MON"))
                .build();
    }

    @Bean
    public JobDetail monthlyReportJobDetail() {
        return JobBuilder.newJob(ReportGenerationJob.class)
                .withIdentity("monthlyReportJob")
                .usingJobData("periodType", "MONTHLY")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger monthlyReportTrigger(JobDetail monthlyReportJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(monthlyReportJobDetail)
                .withIdentity("monthlyReportTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 10 0 1 * ?"))
                .build();
    }

    @Bean
    public JobDetail aggregateWeeklyJobDetail() {
        return JobBuilder.newJob(AggregateReportJob.class)
                .withIdentity("aggregateWeeklyJob")
                .usingJobData("periodType", "WEEKLY")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger aggregateWeeklyTrigger(JobDetail aggregateWeeklyJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(aggregateWeeklyJobDetail)
                .withIdentity("aggregateWeeklyTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 5 0 ? * MON"))
                .build();
    }

    @Bean
    public JobDetail aggregateMonthlyJobDetail() {
        return JobBuilder.newJob(AggregateReportJob.class)
                .withIdentity("aggregateMonthlyJob")
                .usingJobData("periodType", "MONTHLY")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger aggregateMonthlyTrigger(JobDetail aggregateMonthlyJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(aggregateMonthlyJobDetail)
                .withIdentity("aggregateMonthlyTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 10 0 1 * ?"))
                .build();
    }
}
