package com.middleware.backend.system_settings.controller;

import com.middleware.backend.system_settings.dto.ConfigDto;
import com.middleware.backend.system_settings.mapper.ConfigMapper;
import com.middleware.backend.system_settings.model.Config;
import com.middleware.backend.system_settings.service.ConfigService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/configs")
@AllArgsConstructor
public class ConfigController {

    private final ConfigService configService;


    @GetMapping("/module")
    public ResponseEntity<List<String>> getAllModules(){
        return ResponseEntity.ok(configService.getModules());
    }
    @GetMapping("/{module}")
    public Map<String, String> getModuleConfigs(
            @PathVariable String module,
            @RequestParam(required = false) String codePrefix) {
        return configService.getModuleConfig(module, codePrefix);
    }

    @GetMapping("/{module}/{id}")
    public ResponseEntity<?> getModuleConfig(
            @PathVariable String module,
            @PathVariable UUID id){
        return ResponseEntity.ok(configService.getModuleSpecificConfig(module, id));
    }

    @GetMapping
    public ResponseEntity<Page<?>> getAllConfigs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Pageable pageable = PageRequest.of(page, size,
                sortDirection.equalsIgnoreCase("asc") ? Sort.by(sortedBy).ascending() : Sort.by(sortedBy).descending());

        return configService.findAll(pageable);
    }

    @PostMapping
    public ResponseEntity<?> createConfig(@RequestBody ConfigDto dto) {
        Config saved = configService.saveConfig(dto);
        if (saved==null)
            return ResponseEntity.badRequest().body("Key Already Exists");
        return ResponseEntity.ok(ConfigMapper.toDTO(saved));
    }

    @PutMapping("/{module}/{key}")
    public ResponseEntity<?> updateConfig(
            @PathVariable String module,
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        String value = body.get("value");
        ConfigDto updated = configService.updateConfig(module, key, value);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.badRequest().body("Not Found");
    }
}
