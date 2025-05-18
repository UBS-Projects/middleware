package com.middleware.backend.controller;

import com.middleware.backend.model.ErrorMapping;
import com.middleware.backend.repository.ErrorMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/error-mappings")
@RequiredArgsConstructor
@Slf4j
public class ErrorMappingController {

    private final ErrorMappingRepository errorMappingRepository;

    //  Get all error mappings (optionally by destinationApiId)
    @GetMapping
    public List<ErrorMapping> getAll(@RequestParam(required = false) Long destinationApiId) {

        if ( (destinationApiId != null)){
        // List<ErrorMapping> mappings = 
         return errorMappingRepository.findByDestinationApiId(destinationApiId);
    } else {
            return errorMappingRepository.findAll();
                //orElseThrow(() -> new RuntimeException("ApiEndpoint not found"));;
        //return ResponseEntity.ok(mappings);
    }

}

    //  Get error mapping by ID
    @GetMapping("/{id}")
    public ResponseEntity<ErrorMapping> getById(@PathVariable Long id) {
        return errorMappingRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    //  Create new error mapping
    @PostMapping
    public ResponseEntity<ErrorMapping> create(@RequestBody ErrorMapping errorMapping) {
        ErrorMapping saved = errorMappingRepository.save(errorMapping);
        log.info("Created ErrorMapping with ID: {}", saved.getId());
        return ResponseEntity.ok(saved);
    }

    //  Update entire error mapping by ID (PUT)
    @PutMapping("/{id}")
    public ResponseEntity<ErrorMapping> update(@PathVariable Long id, @RequestBody ErrorMapping updateData) {
        return errorMappingRepository.findById(id)
                .map(existing -> {
                    updateData.setId(existing.getId());
                    ErrorMapping saved = errorMappingRepository.save(updateData);
                    log.info("Updated ErrorMapping with ID: {}", saved.getId());
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    //  Patch error mapping (partial update)
    @PatchMapping("/{id}")
    public ResponseEntity<ErrorMapping> patch(@PathVariable Long id, @RequestBody ErrorMapping patchData) {
        return errorMappingRepository.findById(id)
                .map(existing -> {
                    if (patchData.getDestinationApi() != null) existing.setDestinationApi(patchData.getDestinationApi());
                    if (patchData.getDestinationSystemName() != null) existing.setDestinationSystemName(patchData.getDestinationSystemName());
                    if (patchData.getRawErrorSubstring() != null) existing.setRawErrorSubstring(patchData.getRawErrorSubstring());
                    if (patchData.getMatchType() != null) existing.setMatchType(patchData.getMatchType());
                    if (patchData.getMappedErrorCode() != null) existing.setMappedErrorCode(patchData.getMappedErrorCode());
                    if (patchData.getMappedMessage() != null) existing.setMappedMessage(patchData.getMappedMessage());
                    if (patchData.getErrorCategory() != null) existing.setErrorCategory(patchData.getErrorCategory());
                    if (patchData.getHttpStatusCode() != null) existing.setHttpStatusCode(patchData.getHttpStatusCode());
                    if (patchData.getLanguage() != null) existing.setLanguage(patchData.getLanguage());
                    if (patchData.getActive() != null) existing.setActive(patchData.getActive());
                    if (patchData.getCreatedBy() != null) existing.setCreatedBy(patchData.getCreatedBy());

                    ErrorMapping saved = errorMappingRepository.save(existing);
                    log.info("Patched ErrorMapping with ID: {}", saved.getId());
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    //  Delete error mapping by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!errorMappingRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        errorMappingRepository.deleteById(id);
        log.info("Deleted ErrorMapping with ID: {}", id);
        return ResponseEntity.noContent().build();
    }
}
