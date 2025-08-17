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
import java.util.Optional;

@Service
@AllArgsConstructor
public class JobExecutionlogsService {
    private final JobExecutionLogsRepository repo;


    public ResponseEntity<?> getAll(Specification<JobExecutionLogs> spec, Pageable pageable) {
        Page<JobExecutionLogs> page = repo.findAll(spec, pageable);
        Page<JobExecutionLogDTO> dtoPage = page.map(JobExecutionLogDTO::fromEntity);
        return ResponseEntity.ok(dtoPage);
    }

    public ResponseEntity<?> getById(Long id) {
        Optional<JobExecutionLogs> log = repo.findById(id);
        return log.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(log.map(LogResponse::fromEntity));
    }





    public byte[] exportFile(Specification<JobExecutionLogs> spec, Pageable pageable, String type) {
        List<JobExecutionLogs> data;
        try {
            Page<JobExecutionLogs> res = repo.findAll(spec, pageable);
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
    private String convertToCSV(List<JobExecutionLogs> records) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,JobName,ApiEndpoint,Status,StartTime,EndTime,DurationMs,ResponseStatus,ErrorMessage\n");

        for (JobExecutionLogs record : records) {
            sb.append(record.getId()).append(",");
            sb.append(escapeCsv(record.getJob() != null ? record.getJob().getJobName() : "")).append(",");
            sb.append(escapeCsv(record.getJob() != null ? record.getJob().getApiEndpoint() : "")).append(",");
            sb.append(record.getStatus() != null ? record.getStatus().toString() : "").append(",");
            sb.append(record.getStartTime() != null ? record.getStartTime().toString() : "").append(",");
            sb.append(record.getEndTime() != null ? record.getEndTime().toString() : "").append(",");
            sb.append(record.getDurationMs()).append(",");
            sb.append(record.getResponseStatus()).append(",");
            sb.append(escapeCsv(record.getErrorMessage())).append("\n");
        }

        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    // ================= Excel Export =================
    private byte[] convertToExcel(List<JobExecutionLogs> records) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Job Execution Logs");

            // Header row
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("JobName");
            header.createCell(2).setCellValue("ApiEndpoint");
            header.createCell(3).setCellValue("Status");
            header.createCell(4).setCellValue("StartTime");
            header.createCell(5).setCellValue("EndTime");
            header.createCell(6).setCellValue("DurationMs");
            header.createCell(7).setCellValue("ResponseStatus");
            header.createCell(8).setCellValue("ErrorMessage");

            // Data rows
            int rowIdx = 1;
            for (JobExecutionLogs record : records) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(record.getId() != null ? record.getId() : 0);
                row.createCell(1).setCellValue(record.getJob() != null ? record.getJob().getJobName() : "");
                row.createCell(2).setCellValue(record.getJob() != null ? record.getJob().getApiEndpoint() : "");
                row.createCell(3).setCellValue(record.getStatus() != null ? record.getStatus().toString() : "");
                row.createCell(4).setCellValue(record.getStartTime() != null ? record.getStartTime().toString() : "");
                row.createCell(5).setCellValue(record.getEndTime() != null ? record.getEndTime().toString() : "");
                row.createCell(6).setCellValue(record.getDurationMs());
                row.createCell(7).setCellValue(record.getResponseStatus());
                row.createCell(8).setCellValue(record.getErrorMessage() != null ? record.getErrorMessage() : "");
            }

            // Auto-size columns
            for (int i = 0; i <= 8; i++) {
                sheet.autoSizeColumn(i);
            }

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                workbook.write(bos);
                return bos.toByteArray();
            }
        }
    }
}

