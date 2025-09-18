package com.middleware.backend.notification.service;

import com.middleware.backend.notification.dto.*;
import com.middleware.backend.notification.mapper.GroupMapper;
import com.middleware.backend.notification.model.NotificationGroup;
import com.middleware.backend.notification.model.Receiver;
import com.middleware.backend.notification.repository.NotificationGroupRepository;
import com.middleware.backend.notification.repository.ReceiverRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class groupReceiverService {
    private final NotificationGroupRepository groupRepo;
    private final ReceiverRepository receiverRepo;
    
    public NotificationGroupDto addReceivers(GroupReceiversDto dto) {
        NotificationGroup entity = groupRepo.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();
        entity.setUpdatedBy(currentUser);
        entity.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        List<Receiver> receivers = new ArrayList<>();
        for(Long r : dto.getReceivers()){
            receivers.add(receiverRepo.findById(r).get());
        }
        entity.setReceivers(receivers);
        return GroupMapper.mapToDto(groupRepo.save(entity));
    }


    public List<ReceiverRequest> getReceivers(String search) {
        if (search == null || search.isBlank()) {
            return receiverRepo.findAll(PageRequest.of(0, 3)) // fetch first 5 if no search term
                    .stream()
                    .map(r->
                            ReceiverRequest.builder().id(r.getId())
                                    .name(r.getName())
                                    .build())
                    .toList();
        } else {
            return receiverRepo.findTop5ByNameContainingIgnoreCase(search).stream().map(r->
                    ReceiverRequest.builder().id(r.getId())
                            .name(r.getName())
                            .build()).toList();
        }
    }

    public List<GroupRequest> getUnlinkedGroups() {
        return groupRepo.findAllWithoutReceivers().stream().map(r ->
                GroupRequest.builder().id(r.getId()).groupName(r.getName()).build()).toList();
    }

    public Page<?> getGroupReceivers(Pageable pageable) {
        Page<GroupReceiversDto2> page = groupRepo.findAllWithReceivers(pageable)
                .map(p -> GroupReceiversDto2.builder()
                        .groupName(p.getName())
                        .receiverName(
                                p.getReceivers().stream()
                                        .map(r -> r.getName())
                                        .collect(Collectors.joining(", "))
                        )
                        .build()
                );
    return page;
    }

    public NotificationGroup getGroup(String groupName) {
        return groupRepo.findByName(groupName).get();
    }

    public Object updateGroup(GroupReceiversDto body) {
        Optional<NotificationGroup> exists = groupRepo.findById(body.getId());
        if(exists.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        exists.get().setReceivers(null);
        List<Receiver> receivers = new ArrayList<>();
        for(int i=0;i<body.getReceivers().size();i++){
            Optional<Receiver> newReceiver = receiverRepo.findById(body.getReceivers().get(i));
            receivers.add(newReceiver.get());
        }
        exists.get().setReceivers(receivers);
        return groupRepo.save(exists.get());
    }
}
