package com.middleware.backend.scheduledJobs.service;

import com.middleware.backend.scheduledJobs.DTO.JobExecutionDTO;
import com.middleware.backend.scheduledJobs.mapper.LogsMapper;
import com.middleware.backend.scheduledJobs.model.ExecutionHistory;
import com.middleware.backend.scheduledJobs.repository.LogsRepository;
import com.middleware.backend.users.dto.UserResponse;
import com.middleware.backend.users.model.User;
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
 * Service handling persistence and export of user action logs for scheduled jobs.
 */
@Service
@AllArgsConstructor
public class JobLogsService {
    private final LogsRepository repo;
    /**
     * Saves an audit entry for a scheduled job action.
     */
    public void save(JobExecutionDTO job){
        repo.save(LogsMapper.MapToEntity(job));
    }

    /**
     * Retrieves a page of audit entries matching filters.
     */
    public ResponseEntity<Page<?>> getAll(Specification<ExecutionHistory> spec, Pageable pageable) {
        Page<ExecutionHistory> page = repo.findAll(spec, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Exports audit entries to CSV or Excel bytes.
     */
    public byte[] exportFile(Specification<ExecutionHistory> spec, String type, String sortedBy, String sortDirection) throws IOException {
        if ("CSV".equalsIgnoreCase(type)) {
            return convertToCSVStreamed(spec, sortedBy, sortDirection);
        } else if ("Excel".equalsIgnoreCase(type) || "XLSX".equalsIgnoreCase(type)) {
            return convertToExcelStreamed(spec, sortedBy, sortDirection);
        } else {
            throw new IllegalArgumentException("Unsupported export type: " + type);
        }
    }


    // Convert ExecutionHistory list to CSV string
    private byte[] convertToCSVStreamed(Specification<ExecutionHistory> spec, String sortedBy, String sortDirection) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(bos, StandardCharsets.UTF_8))) {
            // Write headers
            writer.println("ID,Job Name,API Endpoint,Status,Created At");
            writer.flush();

            int pageSize = 1000;
            int pageNumber = 0;
            boolean hasMore = true;

            while (hasMore) {
                Sort sort = sortDirection.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending();
                Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
                Page<ExecutionHistory> page = repo.findAll(spec, pageable);

                for (ExecutionHistory record : page.getContent()) {
                    writer.append(String.valueOf(record.getId())).append(",");
                    writer.append(escapeCsv(record.getScheduledJobName())).append(",");
                    writer.append(escapeCsv(record.getScheduledJobPath())).append(",");
                    writer.append(record.getStatus() != null ? record.getStatus().toString() : "").append(",");
                    writer.append(record.getCreatedAt() != null ? record.getCreatedAt().toString() : "").append("\n");
                }

                writer.flush();
                hasMore = page.hasNext();
                pageNumber++;
            }

            writer.flush();
        }

        return bos.toByteArray();
    }

    // Escape CSV values
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

    // Convert ExecutionHistory list to Excel bytes
    private byte[] convertToExcelStreamed(Specification<ExecutionHistory> spec, String sortedBy, String sortDirection) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        SXSSFWorkbook workbook = new SXSSFWorkbook(100); // Keep only 100 rows in memory
        Sheet sheet = workbook.createSheet("Job Execution Logs");

        try {
            // Header row
            Row header = sheet.createRow(0);
            String[] columns = {"ID", "Job Name", "API Endpoint", "Status", "Created At"};
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            // Column widths
            int[] widths = {8, 30, 35, 15, 25};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            int rowIdx = 1;
            int pageSize = 1000;
            int pageNumber = 0;
            boolean hasMore = true;

            while (hasMore) {
                Pageable pageable = PageRequest.of(pageNumber, pageSize, sortDirection.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending());
                Page<ExecutionHistory> page = repo.findAll(spec, pageable);

                for (ExecutionHistory record : page.getContent()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(record.getId());
                    row.createCell(1).setCellValue(safeString(record.getScheduledJobName()));
                    row.createCell(2).setCellValue(safeString(record.getScheduledJobPath()));
                    row.createCell(3).setCellValue(record.getStatus() != null ? record.getStatus().toString() : "");
                    row.createCell(4).setCellValue(record.getCreatedAt() != null ? record.getCreatedAt().toString() : "");
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
}
