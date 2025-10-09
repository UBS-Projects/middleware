package com.middleware.backend.notification.service;

import com.middleware.backend.notification.dto.ChannelConfigDto;
import com.middleware.backend.notification.dto.NotificationActionsLogsDto;
import com.middleware.backend.notification.mapper.ChannelMapper;
import com.middleware.backend.notification.model.ChannelConfig;
import com.middleware.backend.notification.model.NotificationActionsLogs;
import com.middleware.backend.notification.repository.NotificationActionsLogsRepository;
import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Service for managing notification action logs.
 * <p>
 * Handles saving logs, retrieving logs with filtering and pagination,
 * exporting logs to CSV or Excel, and fetching individual log entries.
 * </p>
 */
@Service
@AllArgsConstructor
public class NotificationActionsLogsService {

    private final NotificationActionsLogsRepository repo;

    /**
     * Saves a notification action log entry.
     *
     * @param body the {@link NotificationActionsLogs} entity to save
     */
    public void save(NotificationActionsLogs body){
        repo.save(body);
    }

    /**
     * Retrieves paginated logs according to a specification.
     *
     * @param spec the specification for filtering logs
     * @param pageable the pagination information
     * @return a {@link Page} of {@link NotificationActionsLogsDto}
     */
    public Page<NotificationActionsLogsDto> findAll(Specification<NotificationActionsLogs> spec, Pageable pageable) {
        return repo.findAll(spec, pageable).map(e ->
                NotificationActionsLogsDto.builder()
                        .id(e.getId())
                        .action(e.getAction())
                        .details(e.getDetails())
                        .email(e.getEmail())
                        .eventTime(e.getEventTime())
                        .build()
        );
    }

    /**
     * Retrieves a single log by its ID.
     *
     * @param id the log ID
     * @return the corresponding {@link NotificationActionsLogsDto}, or null if not found
     */
    public NotificationActionsLogsDto getById(long id) {
        return repo.findById(id).map(e ->
                NotificationActionsLogsDto.builder()
                        .id(e.getId())
                        .action(e.getAction())
                        .details(e.getDetails())
                        .email(e.getEmail())
                        .eventTime(e.getEventTime())
                        .build()
        ).orElse(null);
    }

    /**
     * Exports logs according to the provided specification and pageable
     * into CSV or Excel formats.
     *
     * @param spec the specification for filtering logs
     * @param pageable the pagination information
     * @param type the export type: CSV or Excel/XLSX
     * @return a byte array containing the exported file
     */
    public byte[] exportFile(Specification<NotificationActionsLogs> spec, Pageable pageable, String type) {
        List<NotificationActionsLogs> data;
        try {
            Page<NotificationActionsLogs> res = repo.findAll(spec, pageable);
            data = res.getContent();
        } catch (Exception e) {
            e.printStackTrace();
            data = Collections.emptyList();
        }

        try {
            if ("CSV".equalsIgnoreCase(type)) {
                return convertToCSV(data).getBytes(StandardCharsets.UTF_8);
            } else if ("Excel".equalsIgnoreCase(type) || "XLSX".equalsIgnoreCase(type)) {
                return convertToExcel(data);
            } else {
                throw new IllegalArgumentException("Unsupported export type: " + type);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to export file", e);
        }
    }

    // ================= CSV Export =================

    /**
     * Converts a list of logs to CSV format.
     *
     * @param records the list of {@link NotificationActionsLogs}
     * @return a CSV string
     */
    private String convertToCSV(List<NotificationActionsLogs> records) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Action,Details,Email,EventTime\n");

        for (NotificationActionsLogs record : records) {
            sb.append(record.getId()).append(",");
            sb.append(escapeCsv(record.getAction())).append(",");
            sb.append(escapeCsv(record.getDetails())).append(",");
            sb.append(escapeCsv(record.getEmail())).append(",");
            sb.append(record.getEventTime() != null ? record.getEventTime().toString() : "").append("\n");
        }

        return sb.toString();
    }

    /**
     * Escapes a CSV value to properly handle commas, quotes, and newlines.
     *
     * @param value the original string
     * @return the escaped string
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    // ================= Excel Export =================

    /**
     * Converts a list of logs to an Excel (XLSX) byte array.
     *
     * @param records the list of {@link NotificationActionsLogs}
     * @return a byte array representing the Excel file
     * @throws IOException if writing to the workbook fails
     */
    private byte[] convertToExcel(List<NotificationActionsLogs> records) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Notification Logs");

            // Header row
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("Action");
            header.createCell(2).setCellValue("Details");
            header.createCell(3).setCellValue("Email");
            header.createCell(4).setCellValue("EventTime");

            // Data rows
            int rowIdx = 1;
            for (NotificationActionsLogs record : records) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(record.getId());
                row.createCell(1).setCellValue(record.getAction() != null ? record.getAction() : "");
                row.createCell(2).setCellValue(record.getDetails() != null ? record.getDetails() : "");
                row.createCell(3).setCellValue(record.getEmail() != null ? record.getEmail() : "");
                row.createCell(4).setCellValue(record.getEventTime() != null ? record.getEventTime().toString() : "");
            }

            // Auto-size columns
            for (int i = 0; i <= 4; i++) {
                sheet.autoSizeColumn(i);
            }

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                workbook.write(bos);
                return bos.toByteArray();
            }
        }
    }
}
