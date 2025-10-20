package com.middleware.backend.scheduledJobs.service;

import com.middleware.backend.scheduledJobs.DTO.JobExecutionLogDTO;
import com.middleware.backend.scheduledJobs.DTO.LogResponse;
import com.middleware.backend.scheduledJobs.model.ExecutionHistory;
import com.middleware.backend.scheduledJobs.model.JobExecutionLogs;
import com.middleware.backend.scheduledJobs.repository.JobExecutionLogsRepository;
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
import java.util.Optional;

/**
 * Service for querying and exporting job execution logs.
 */
@Service
@AllArgsConstructor
public class JobExecutionlogsService {
    private final JobExecutionLogsRepository repo;

    /**
     * Returns a page of execution logs mapped to lightweight DTOs.
     */
    public ResponseEntity<?> getAll(Specification<JobExecutionLogs> spec, Pageable pageable) {
        Page<JobExecutionLogs> page = repo.findAll(spec, pageable);
        Page<JobExecutionLogDTO> dtoPage = page.map(JobExecutionLogDTO::fromEntity);
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Fetches a single execution log by id and maps it to detailed response DTO.
     */
    public ResponseEntity<?> getById(Long id) {
        Optional<JobExecutionLogs> log = repo.findById(id);
        return log.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(log.map(LogResponse::fromEntity));
    }





    /**
     * Exports execution logs to CSV or Excel bytes according to type.
     *
     * @param spec filters for data
     * @param pageable pagination to cap exported size
     * @param type CSV or Excel/XLSX
     * @return file content bytes
     */
    public byte[] exportFile(Specification<JobExecutionLogs> spec, String type, String sortedBy, String sortDirection) throws IOException {
        if ("CSV".equalsIgnoreCase(type)) {
            return convertToCSVStreamed(spec, sortedBy,sortDirection);
        } else if ("Excel".equalsIgnoreCase(type) || "XLSX".equalsIgnoreCase(type)) {
            return convertToExcelStreamed(spec, sortedBy,sortDirection);
        } else {
            throw new IllegalArgumentException("Unsupported export type: " + type);
        }
    }


    // ================= CSV Export =================
    private byte[] convertToCSVStreamed(Specification<JobExecutionLogs> spec, String sortedBy, String sortDirection) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(bos, StandardCharsets.UTF_8))) {
            // Headers
            writer.println("ID,Job Name,API Endpoint,Status,Start Time,End Time,Duration (ms),Response Status,Error Message");
            writer.flush();

            int pageSize = 1000;
            int pageNumber = 0;
            boolean hasMore = true;

            while (hasMore) {
                Sort sort = sortDirection.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending();
                Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
                Page<JobExecutionLogs> page = repo.findAll(spec, pageable);

                for (JobExecutionLogs record : page.getContent()) {
                    writer.append(String.valueOf(record.getId())).append(",");
                    writer.append(escapeCsv(record.getJob() != null ? record.getJob().getJobName() : "")).append(",");
                    writer.append(escapeCsv(record.getJob() != null ? record.getJob().getApiEndpoint() : "")).append(",");
                    writer.append(record.getStatus() != null ? record.getStatus().toString() : "").append(",");
                    writer.append(record.getStartTime() != null ? record.getStartTime().toString() : "").append(",");
                    writer.append(record.getEndTime() != null ? record.getEndTime().toString() : "").append(",");
                    writer.append(String.valueOf(record.getDurationMs())).append(",");
                    writer.append(String.valueOf(record.getResponseStatus())).append(",");
                    writer.append(escapeCsv(record.getErrorMessage())).append("\n");
                }

                writer.flush();
                hasMore = page.hasNext();
                pageNumber++;
            }

            writer.flush();
        }

        return bos.toByteArray();
    }

    private byte[] convertToExcelStreamed(Specification<JobExecutionLogs> spec, String sortedBy, String sortDirection) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        SXSSFWorkbook workbook = new SXSSFWorkbook(100); // Keep 100 rows in memory
        Sheet sheet = workbook.createSheet("Job Execution Logs");

        try {
            // Header row
            Row header = sheet.createRow(0);
            String[] columns = {
                    "ID", "Job Name", "API Endpoint", "Status",
                    "Start Time", "End Time", "Duration (ms)",
                    "Response Status", "Error Message"
            };
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            // Column widths
            int[] widths = {6, 25, 30, 15, 20, 20, 15, 18, 35};
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
                Page<JobExecutionLogs> page = repo.findAll(spec, pageable);

                for (JobExecutionLogs record : page.getContent()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(record.getId());
                    row.createCell(1).setCellValue(safeString(record.getJob() != null ? record.getJob().getJobName() : ""));
                    row.createCell(2).setCellValue(safeString(record.getJob() != null ? record.getJob().getApiEndpoint() : ""));
                    row.createCell(3).setCellValue(safeString(record.getStatus() != null ? record.getStatus().toString() : ""));
                    row.createCell(4).setCellValue(safeString(record.getStartTime() != null ? record.getStartTime().toString() : ""));
                    row.createCell(5).setCellValue(safeString(record.getEndTime() != null ? record.getEndTime().toString() : ""));
                    row.createCell(6).setCellValue(String.valueOf(record.getDurationMs()));
                    row.createCell(7).setCellValue(String.valueOf(record.getResponseStatus()));
                    row.createCell(8).setCellValue(truncate(record.getErrorMessage(), MAX_CELL_LENGTH));
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

