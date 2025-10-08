package com.middleware.backend.dashboard.service;

import com.middleware.backend.audit_logs_interceptor.repository.AuditLogRepository;
import com.middleware.backend.dashboard.dto.ScheduledExecutedJobsListDto;
import com.middleware.backend.dashboard.dto.SummaryDto;
import com.middleware.backend.dashboard.dto.TokenListDto;
import com.middleware.backend.dashboard.dto.TransactionListDto;
import com.middleware.backend.logging.repository.MiddlewareApiCallLogRepository;
import com.middleware.backend.scheduledJobs.enums.Status;
import com.middleware.backend.scheduledJobs.repository.JobExecutionLogsRepository;
import com.middleware.backend.users.Roles.model.Role;
import com.middleware.backend.users.tokens.repository.TokenRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service responsible for handling business logic and data aggregation for the dashboard module.
 * <p>
 * Provides operations to retrieve summary metrics, graphical data, and lists of recent transactions,
 * executed jobs, and tokens.
 */
@Service
@AllArgsConstructor
public class DashboardService {

    private final MiddlewareApiCallLogRepository transactionRepo;
    private final AuditLogRepository auditRepo;
    private final JobExecutionLogsRepository jobRepo;
    private final TokenRepository tokenRepo;

    /**
     * Retrieves the overall daily summary for the dashboard, including metrics such as
     * transactions, audit events, and executed jobs.
     *
     * @return {@link SummaryDto} containing a map of summarized dashboard metrics
     */
    public SummaryDto getSummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        Timestamp startTimestamp = Timestamp.valueOf(startOfDay);
        Timestamp endTimestamp = Timestamp.valueOf(endOfDay);

        long allTransactions = transactionRepo.countByReceivedAtBetween(startOfDay, endOfDay);
        long succeededTransactions = transactionRepo.countByReceivedAtBetweenAndResponseCodeBetween(startOfDay, endOfDay, 200, 299);
        long failedTransactions = transactionRepo.countByReceivedAtBetweenAndResponseCodeBetween(startOfDay, endOfDay, 400, 599);
        long userEvents = auditRepo.countAuditLogsByStartTimeBetween(startOfDay, endOfDay);
        long allJobs = jobRepo.countAllByStartTimeBetween(startTimestamp, endTimestamp);

        Map<String, String> summaryMap = Map.of(
                "allTransactions", String.valueOf(allTransactions),
                "succeededTransactions", String.valueOf(succeededTransactions),
                "failedTransactions", String.valueOf(failedTransactions),
                "userEvents", String.valueOf(userEvents),
                "allJobs", String.valueOf(allJobs)
        );

        return SummaryDto.builder()
                .summary(summaryMap)
                .build();
    }

    /**
     * Retrieves summary metrics for transaction activity on a specific day.
     * <p>
     * Includes totals for succeeded, failed, and in-progress transactions.
     *
     * @param day the date for which transaction metrics are retrieved
     * @return {@link SummaryDto} containing transaction statistics for the given day
     */
    public SummaryDto getTransactionGraphCoordinates(LocalDate day) {
        LocalDateTime startOfDay = day.atStartOfDay();
        LocalDateTime endOfDay = day.plusDays(1).atStartOfDay();

        long allTransactions = transactionRepo.countByReceivedAtBetween(startOfDay, endOfDay);
        long succeededTransactions = transactionRepo.countByReceivedAtBetweenAndResponseCodeBetween(startOfDay, endOfDay, 200, 299);
        long failedTransactions = transactionRepo.countByReceivedAtBetweenAndResponseCodeBetween(startOfDay, endOfDay, 300, 599);
        long inProgressTransactions = transactionRepo.countByReceivedAtBetweenAndStatus(startOfDay, endOfDay, "IN_PROGRESS");

        Map<String, String> summaryMap = Map.of(
                "Total Transactions", String.valueOf(allTransactions),
                "Succeeded Transactions", String.valueOf(succeededTransactions),
                "Failed Transactions", String.valueOf(failedTransactions),
                "In Progress Transactions", String.valueOf(inProgressTransactions)
        );

        return SummaryDto.builder()
                .summary(summaryMap)
                .build();
    }

    /**
     * Retrieves summary metrics for scheduled job executions on a specific day.
     * <p>
     * Includes totals for all, succeeded, and failed jobs.
     *
     * @param day the date for which job metrics are retrieved
     * @return {@link SummaryDto} containing job execution statistics for the given day
     */
    public SummaryDto getJobsGraphCoordinates(LocalDate day) {
        LocalDateTime startOfDay = day.atStartOfDay();
        LocalDateTime endOfDay = day.plusDays(1).atStartOfDay();

        Timestamp startTimestamp = Timestamp.valueOf(startOfDay);
        Timestamp endTimestamp = Timestamp.valueOf(endOfDay);

        long allJobs = jobRepo.countAllByStartTimeBetween(startTimestamp, endTimestamp);
        long succeededJobs = jobRepo.countAllByStatusAndStartTimeBetween(Status.SUCCESS, startTimestamp, endTimestamp);
        long failedJobs = jobRepo.countAllByStatusAndStartTimeBetween(Status.FAILURE, startTimestamp, endTimestamp);

        Map<String, String> summaryMap = Map.of(
                "Total Executed Scheduled Jobs", String.valueOf(allJobs),
                "Succeeded Executed Jobs", String.valueOf(succeededJobs),
                "Failed Executed Jobs", String.valueOf(failedJobs)
        );

        return SummaryDto.builder()
                .summary(summaryMap)
                .build();
    }

    /**
     * Retrieves the 10 most recent failed or error transactions.
     *
     * @return list of {@link TransactionListDto} representing the latest failed transactions
     */
    public List<TransactionListDto> getTransactionList() {
        return transactionRepo.findTop10ByResponseCodeBetweenOrderByReceivedAtDesc(300, 599)
                .stream()
                .map(t -> TransactionListDto.builder()
                        .id(t.getTransactionId())
                        .endPoint(t.getApiEndpoint())
                        .responseCode(t.getResponseCode())
                        .eventTime(t.getReceivedAt())
                        .duration(t.getDurationMs())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the 10 most recently executed scheduled jobs.
     *
     * @return list of {@link ScheduledExecutedJobsListDto} representing recent job executions
     */
    public List<ScheduledExecutedJobsListDto> getJobsList() {
        Pageable top10 = PageRequest.of(0, 10);
        return jobRepo.findTop10JobDtos(top10);
    }

    /**
     * Retrieves up to 10 tokens that have recently expired or are about to expire.
     * <p>
     * Filters include:
     * <ul>
     *     <li>Tokens expired within the last 7 days</li>
     *     <li>Tokens expiring within the next 14 days</li>
     * </ul>
     *
     * @return list of {@link TokenListDto} containing token details and expiry information
     */
    public List<TokenListDto> getTokenList() {
        Timestamp now = Timestamp.from(Instant.now());
        Timestamp sevenDaysAgo = Timestamp.from(Instant.now().minus(7, ChronoUnit.DAYS));
        Timestamp twoWeeksLater = Timestamp.from(Instant.now().plus(14, ChronoUnit.DAYS));

        return tokenRepo.findExpiringOrRecentlyExpiredTokensByRole(
                now,
                sevenDaysAgo,
                twoWeeksLater,
                Role.RoleType.SYSTEM_USER,
                PageRequest.of(0, 10)
        );
    }
}
