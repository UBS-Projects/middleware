package com.middleware.backend.notification.controller;

import com.middleware.backend.notification.dto.GroupReceiversDto;
import com.middleware.backend.notification.dto.GroupReceiversDto2;
import com.middleware.backend.notification.service.NotificationGroupService;
import com.middleware.backend.notification.service.groupReceiverService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups-receivers")
@AllArgsConstructor
public class groupReceiverController {
    private final groupReceiverService service;

    @GetMapping("")
    public ResponseEntity<?> getUnlinkedGroups(){
        return ResponseEntity.ok(service.getUnlinkedGroups());

    }
    @PostMapping("")
    public ResponseEntity<?> addReceivers(@RequestBody GroupReceiversDto dto) {
        return ResponseEntity.ok(service.addReceivers(dto));
    }
    @GetMapping("/all")
    public ResponseEntity<?> getAllReceivers(
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(service.getReceivers(search));
    }


    @GetMapping("/table")
    public ResponseEntity<?> getGroupReceivers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "updatedAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Pageable pageable = PageRequest.of(page, size, sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortedBy).ascending()
                : Sort.by(sortedBy).descending());
        return ResponseEntity.ok(service.getGroupReceivers(pageable));
    }


    @GetMapping("/specific/{groupName}")
    public ResponseEntity<?> getSpecificGroupReceivers(
            @PathVariable String groupName
    ) {
        return ResponseEntity.ok(service.getGroup(groupName));
    }


    @PutMapping("/specific")
    public ResponseEntity<?> updateSpecificGroupReceivers(
            @RequestBody GroupReceiversDto body
            ) {
        return ResponseEntity.ok(service.updateGroup(body));
    }
}
