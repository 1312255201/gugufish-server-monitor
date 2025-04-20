package cn.gugufish.config;

import cn.gugufish.task.MonitorJobBean;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz定时任务配置类
 */
@Slf4j
@Configuration
public class QuartzConfiguration {

    /**
     * 配置任务详情(JobDetail)
     * JobDetail定义了一个特定的任务，指定了要执行的具体Job类。
     * 配置了一个MonitorJobBean类型的任务，该任务用于监控服务器状态。
     * @return 配置好的JobDetail实例
     */
    @Bean
    public JobDetail jobDetailFactoryBean() {
        return JobBuilder.newJob(MonitorJobBean.class) // 指定要执行的Job类
                .withIdentity("monitor-task") // 设置任务的唯一标识
                .storeDurably() // 即使没有触发器关联，也不会被删除
                .build();
    }


    @Bean
    public Trigger cronTriggerFactoryBean(JobDetail detail) {
        // 每10秒执行一次
        CronScheduleBuilder cron = CronScheduleBuilder.cronSchedule("*/10 * * * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(detail) // 关联到上面定义的JobDetail
                .withIdentity("monitor-trigger") // 设置触发器的唯一标识
                .withSchedule(cron) // 设置调度策略
                .build();
    }
}