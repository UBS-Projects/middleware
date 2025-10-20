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
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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
    public byte[] exportFile(Specification<NotificationActionsLogs> spec, String type, String sortedBy, String sortDirection) throws IOException {
        if ("CSV".equalsIgnoreCase(type)) {
            return convertToCSVStreamed(spec, sortedBy,sortDirection);
        } else if ("Excel".equalsIgnoreCase(type) || "XLSX".equalsIgnoreCase(type)) {
            return convertToExcelStreamed(spec, sortedBy,sortDirection);
        } else {
            throw new IllegalArgumentException("Unsupported export type: " + type);
        }
    }
    private byte[] convertToCSVStreamed(Specification<NotificationActionsLogs> spec, String sortedBy, String sortDirection) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(bos, StandardCharsets.UTF_8))) {
            // Header row
            writer.println("ID,Action,Details,Email,EventTime");
            writer.flush();

            int pageSize = 1000;
            int pageNumber = 0;
            boolean hasMore = true;

            while (hasMore) {
                Sort sort = sortDirection.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending();
                Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
                Page<NotificationActionsLogs> page = repo.findAll(spec, pageable);

                for (NotificationActionsLogs record : page.getContent()) {
                    writer.append(String.valueOf(record.getId())).append(",");
                    writer.append(escapeCsv(record.getAction())).append(",");
                    writer.append(escapeCsv(record.getDetails())).append(",");
                    writer.append(escapeCsv(record.getEmail())).append(",");
                    writer.append(record.getEventTime() != null ? record.getEventTime().toString() : "").append("\n");
                }

                writer.flush();
                hasMore = page.hasNext();
                pageNumber++;
            }

            writer.flush();
        }

        return bos.toByteArray();
    }
    private byte[] convertToExcelStreamed(Specification<NotificationActionsLogs> spec, String sortedBy, String sortDirection) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        SXSSFWorkbook workbook = new SXSSFWorkbook(100); // Keep 100 rows in memory
        Sheet sheet = workbook.createSheet("Notification Logs");

        try {
            // Header row
            Row header = sheet.createRow(0);
            String[] columns = {"ID", "Action", "Details", "Email", "EventTime"};
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            int[] widths = {6, 25, 40, 30, 25};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            int rowIdx = 1;
            int pageSize = 1000;
            int pageNumber = 0;
            boolean hasMore = true;
            final int MAX_CELL_LENGTH = 32000;

            while (hasMore) {
                Sort sort = sortDirection.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending();
                Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
                Page<NotificationActionsLogs> page = repo.findAll(spec, pageable);

                for (NotificationActionsLogs record : page.getContent()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(record.getId());
                    row.createCell(1).setCellValue(safeString(record.getAction()));
                    row.createCell(2).setCellValue(truncate(record.getDetails(), MAX_CELL_LENGTH));
                    row.createCell(3).setCellValue(safeString(record.getEmail()));
                    row.createCell(4).setCellValue(record.getEventTime() != null ? record.getEventTime().toString() : "");
                }

                hasMore = page.hasNext();
                pageNumber++;
            }

            workbook.write(bos);
            return bos.toByteArray();

        } finally {
            workbook.dispose();
        }
    }
    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 3) + "...";
    }

}
