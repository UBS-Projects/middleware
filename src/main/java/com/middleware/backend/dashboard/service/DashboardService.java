package com.middleware.backend.dashboard.service;

import com.middleware.backend.audit_logs_interceptor.repository.AuditLogRepository;
import com.middleware.backend.dashboard.dto.ScheduledExecutedJobsListDto;
import com.middleware.backend.dashboard.dto.SummaryDto;
import com.middleware.backend.dashboard.dto.TokenListDto;
import com.middleware.backend.dashboard.dto.TransactionListDto;
import com.middleware.backend.logging.model.MiddlewareApiCallLog;
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

@Service
@AllArgsConstructor
public class DashboardService {
    private final MiddlewareApiCallLogRepository transactionRepo;
    private final AuditLogRepository auditRepo;
    private final JobExecutionLogsRepository jobRepo;
    private final TokenRepository tokenRepo;

    public SummaryDto getSummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        // Convert to Timestamp for repos using Timestamp
        Timestamp startTimestamp = Timestamp.valueOf(startOfDay);
        Timestamp endTimestamp = Timestamp.valueOf(endOfDay);

        // Transactions
        long allTransactions = transactionRepo.countByReceivedAtBetween(startOfDay, endOfDay);
        long succeededTransactions = transactionRepo.countByReceivedAtBetweenAndResponseCodeBetween(startOfDay, endOfDay, 200, 299);
        long failedTransactions = transactionRepo.countByReceivedAtBetweenAndResponseCodeBetween(startOfDay, endOfDay, 400, 599);

        // User events (audit logs)
        long userEvents = auditRepo.countAuditLogsByStartTimeBetween(startOfDay, endOfDay);

        // Scheduled jobs
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

    public SummaryDto getTransactionGraphCoordinates(LocalDate day){
        LocalDateTime startOfDay = day.atStartOfDay();
        LocalDateTime endOfDay = day.plusDays(1).atStartOfDay();
        long allTransactions = transactionRepo.countByReceivedAtBetween(startOfDay, endOfDay);
        long succeededTransactions = transactionRepo.countByReceivedAtBetweenAndResponseCodeBetween(startOfDay, endOfDay, 200, 299);
        long failedTransactions = transactionRepo.countByReceivedAtBetweenAndResponseCodeBetween(startOfDay, endOfDay, 300, 599);
        long inProgressTransactions = transactionRepo.countByReceivedAtBetweenAndStatus(startOfDay, endOfDay,"IN_PROGRESS");
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


    public SummaryDto getJobsGraphCoordinates(LocalDate day){
        LocalDateTime startOfDay = day.atStartOfDay();
        LocalDateTime endOfDay = day.plusDays(1).atStartOfDay();
        // Convert to Timestamp for repos using Timestamp
        Timestamp startTimestamp = Timestamp.valueOf(startOfDay);
        Timestamp endTimestamp = Timestamp.valueOf(endOfDay);
        long allJobs = jobRepo.countAllByStartTimeBetween(startTimestamp, endTimestamp);
        long succeededJobs = jobRepo.countAllByStatusAndStartTimeBetween(Status.SUCCESS,startTimestamp, endTimestamp);
        long failedJobs = jobRepo.countAllByStatusAndStartTimeBetween(Status.FAILURE,startTimestamp, endTimestamp);
        Map<String, String> summaryMap = Map.of(
                "Total Executed Scheduled Jobs", String.valueOf(allJobs),
                "Succeeded Executed Jobs", String.valueOf(succeededJobs),
                "Failed Executed Jobs", String.valueOf(failedJobs)
        );
        return SummaryDto.builder()
                .summary(summaryMap)
                .build();
    }


    public List<TransactionListDto> getTransactionList(){
        return transactionRepo.findTop10ByResponseCodeBetweenOrderByReceivedAtDesc(300,599)
                .stream().map(t ->
                        TransactionListDto.builder()
                                .id(t.getTransactionId())
                                .endPoint(t.getApiEndpoint())
                                .responseCode(t.getResponseCode())
                                .eventTime(t.getReceivedAt())
                                .duration(t.getDurationMs())
                                .build()).collect(Collectors.toList());
    }

    public List<ScheduledExecutedJobsListDto> getJobsList() {
        Pageable top10 = PageRequest.of(0, 10);
        return jobRepo.findTop10JobDtos(top10);
    }

    public List<TokenListDto> getTokenList(){
        Timestamp now = Timestamp.from(Instant.now());
        Timestamp sevenDaysAgo = Timestamp.from(Instant.now().minus(7, ChronoUnit.DAYS));
        Timestamp twoWeeksLater = Timestamp.from(Instant.now().plus(14, ChronoUnit.DAYS));

        // Limit to 10 tokens total
        return tokenRepo.findExpiringOrRecentlyExpiredTokensByRole(
                now,
                sevenDaysAgo,
                twoWeeksLater,
                Role.RoleType.SYSTEM_USER,
                PageRequest.of(0, 10)
        );
    }
}
