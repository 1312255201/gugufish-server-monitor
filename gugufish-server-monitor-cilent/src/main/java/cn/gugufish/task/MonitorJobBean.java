package cn.gugufish.task;

import cn.gugufish.entity.RuntimeDetail;
import cn.gugufish.util.MonitorUtils;
import cn.gugufish.util.NetUtils;
import jakarta.annotation.Resource;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Component
public class MonitorJobBean extends QuartzJobBean {

    @Resource
    MonitorUtils monitor;

    @Resource
    NetUtils net;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        RuntimeDetail runtimeDetail = monitor.monitorRuntimeDetail();
        net.updateRuntimeDetails(runtimeDetail);
    }
}