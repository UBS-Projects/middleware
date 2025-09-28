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

@Service
@AllArgsConstructor
public class NotificationActionsLogsService {
    private final NotificationActionsLogsRepository repo;

    public void save(NotificationActionsLogs body){
        repo.save(body);
    }

    public Page<NotificationActionsLogsDto> findAll(Specification<NotificationActionsLogs> spec, Pageable pageable) {
        return repo.findAll(spec, pageable).map(e->
                        NotificationActionsLogsDto.builder()
                                .id(e.getId())
                                .action(e.getAction())
                                .details(e.getDetails())
                                .email(e.getEmail())
                                .eventTime(e.getEventTime())
                                .build()
                );
    }

    public NotificationActionsLogsDto getById(long id) {
        return repo.findById(id).map(
                e-> NotificationActionsLogsDto.builder()
                        .id(e.getId())
                        .action(e.getAction())
                        .details(e.getDetails())
                        .email(e.getEmail())
                        .eventTime(e.getEventTime())
                        .build()
        ).orElse(null);
    }

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

    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    // ================= Excel Export =================
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
