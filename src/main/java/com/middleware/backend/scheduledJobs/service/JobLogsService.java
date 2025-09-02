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

@Service
@AllArgsConstructor
public class JobLogsService {
    private final LogsRepository repo;
    public void save(JobExecutionDTO job){
        repo.save(LogsMapper.MapToEntity(job));
    }

    public ResponseEntity<?> getAll(Specification<ExecutionHistory> spec, Pageable pageable) {
        Page<ExecutionHistory> page = repo.findAll(spec, pageable);
        return ResponseEntity.ok(page);
    }

    public byte[] exportFile(Specification<ExecutionHistory> spec, Pageable pageable, String type) throws IOException {
        List<ExecutionHistory> data;
        try {
            Page<ExecutionHistory> res = repo.findAll(spec, pageable);
            data = res.getContent();
        } catch (Exception e) {
            // Log the error and return empty file
            e.printStackTrace();
            data = Collections.emptyList();
        }

        if ("CSV".equalsIgnoreCase(type)) {
            return convertToCSV(data).getBytes(StandardCharsets.UTF_8);
        } else if ("Excel".equalsIgnoreCase(type) || "XLSX".equalsIgnoreCase(type)) {
            return convertToExcel(data);
        } else {
            throw new IllegalArgumentException("Unsupported export type: " + type);
        }
    }


    // Convert ExecutionHistory list to CSV string
    private String convertToCSV(List<ExecutionHistory> records) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,JobName,ApiEndpoint,Status,CreatedAt\n");

        for (ExecutionHistory record : records) {
            sb.append(record.getId()).append(",");
            sb.append(escapeCsv(record.getScheduledJobName())).append(",");
            sb.append(escapeCsv(record.getScheduledJobPath())).append(",");
            sb.append(record.getStatus() != null ? record.getStatus().toString() : "").append(",");
            sb.append(record.getCreatedAt() != null ? record.getCreatedAt().toString() : "").append("\n");
        }

        return sb.toString();
    }

    // Escape CSV values
    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\""); // escape quotes
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\""; // wrap in quotes if needed
        }
        return escaped;
    }

    // Convert ExecutionHistory list to Excel bytes
    private byte[] convertToExcel(List<ExecutionHistory> records) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Execution History");

            // Header row
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("JobName");
            header.createCell(2).setCellValue("ApiEndpoint");
            header.createCell(3).setCellValue("Status");
            header.createCell(4).setCellValue("CreatedAt");

            // Data rows
            int rowIdx = 1;
            for (ExecutionHistory record : records) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(record.getId());
                row.createCell(1).setCellValue(record.getScheduledJobName());
                row.createCell(2).setCellValue(record.getScheduledJobPath());
                row.createCell(3).setCellValue(record.getStatus() != null ? record.getStatus().toString() : "");
                row.createCell(4).setCellValue(record.getCreatedAt() != null ? record.getCreatedAt().toString() : "");
            }

            // Auto-size columns
            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to byte[]
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                workbook.write(bos);
                return bos.toByteArray();
            }
        }
    }
}
