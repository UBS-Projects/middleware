package com.middleware.backend.scheduledJobs.config;

import com.middleware.backend.scheduledJobs.service.ScheduledJobsService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartupScheduler implements ApplicationRunner {

    private final ScheduledJobsService schedulerService;

    @Override
    public void run(ApplicationArguments args) {
        schedulerService.scheduleAllActiveJobs();
    }
}
