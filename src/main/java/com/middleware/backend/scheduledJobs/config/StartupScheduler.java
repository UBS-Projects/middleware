package com.middleware.backend.scheduledJobs.config;

import com.middleware.backend.scheduledJobs.service.ScheduledJobsService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Application startup hook that initializes the job scheduler.
 * <p>
 * When the Spring Boot application starts, this component triggers scheduling
 * of all active jobs stored in the system so they are registered with the
 * underlying scheduler (e.g., Quartz) immediately.
 */
@Component
@RequiredArgsConstructor
public class StartupScheduler implements ApplicationRunner {

    private final ScheduledJobsService schedulerService;

    /**
     * Invoked once the application context is ready.
     *
     * @param args startup arguments provided to the application (unused)
     */
    @Override
    public void run(ApplicationArguments args) {
        schedulerService.scheduleAllActiveJobs();
    }
}
