package com.middleware.backend.notification.service;

import com.middleware.backend.notification.dto.ChannelConfigDto;
import com.middleware.backend.notification.dto.ReceiverRequest;
import com.middleware.backend.notification.enums.ChannelType;
import com.middleware.backend.notification.mapper.ChannelMapper;
import com.middleware.backend.notification.model.ChannelConfig;
import com.middleware.backend.notification.model.NotificationActionsLogs;
import com.middleware.backend.notification.repository.ChannelConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

/**
 * Service for managing notification channels.
 * <p>
 * Handles CRUD operations on {@link ChannelConfig} entities, including creating, updating,
 * retrieving, and toggling the active status of channels. Logs all actions using
 * {@link NotificationActionsLogsService}.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelConfigRepository repo;
    private final NotificationActionsLogsService loggingService;

    /**
     * Retrieves a channel by its ID.
     *
     * @param id the ID of the channel
     * @return the {@link ChannelConfigDto} for the channel
     * @throws RuntimeException if the channel is not found
     */
    public ChannelConfigDto findById(Long id) {
        return repo.findById(id).map(ChannelMapper::mapToDto)
                .orElseThrow(() -> new RuntimeException("Channel not found"));
    }

    /**
     * Retrieves a paginated list of channels matching the given specification.
     *
     * @param spec     filtering specification
     * @param pageable pagination information
     * @return a page of {@link ChannelConfigDto} objects
     */
    public Page<ChannelConfigDto> findAll(Specification<ChannelConfig> spec, Pageable pageable) {
        return repo.findAll(spec, pageable).map(ChannelMapper::mapToDto);
    }

    /**
     * Creates a new notification channel.
     *
     * @param dto the DTO containing channel data
     * @return the saved {@link ChannelConfigDto}
     * @throws RuntimeException if a channel with the same name or code already exists
     */
    public ChannelConfigDto create(ChannelConfigDto dto) {
        repo.findByName(dto.getName().trim())
                .ifPresent(c -> { throw new RuntimeException("Channel Name already exists"); });

        repo.findByCode(dto.getCode().trim())
                .ifPresent(c -> { throw new RuntimeException("Channel Code already exists"); });

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        dto.setCreatedBy(currentUser);
        dto.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        dto.setUpdatedBy(currentUser);
        dto.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        dto.setName(dto.getName().trim());
        dto.setCode(dto.getCode().trim());

        ChannelConfig saved = repo.save(ChannelMapper.mapToEntity(dto));

        // Log creation
        loggingService.save(NotificationActionsLogs.builder()
                .action("POST")
                .details("Created Channel " + dto.getName())
                .email(currentUser)
                .eventTime(new Timestamp(System.currentTimeMillis()))
                .build()
        );

        return ChannelMapper.mapToDto(saved);
    }

    /**
     * Updates an existing channel.
     *
     * @param dto the DTO containing updated channel data
     * @return the updated {@link ChannelConfigDto}
     * @throws RuntimeException if the channel does not exist
     */
    public ChannelConfigDto update(ChannelConfigDto dto) {
        ChannelConfig entity = repo.findByName(dto.getName().trim())
                .orElseThrow(() -> new RuntimeException("Channel not found"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        entity.setUpdatedBy(currentUser);
        entity.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        entity.setActive(dto.isActive());
        entity.setConfig(dto.getConfig());
        entity.setType(ChannelType.valueOf(dto.getType()));

        // Log update
        loggingService.save(NotificationActionsLogs.builder()
                .action("PUT")
                .details("Updated Channel " + dto.getName())
                .email(currentUser)
                .eventTime(new Timestamp(System.currentTimeMillis()))
                .build()
        );

        return ChannelMapper.mapToDto(repo.save(entity));
    }

    /**
     * Toggles the active status of a channel.
     *
     * @param id the ID of the channel
     * @throws RuntimeException if the channel does not exist
     */
    public void changeStatus(Long id) {
        ChannelConfig entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        loggingService.save(NotificationActionsLogs.builder()
                .action(entity.isActive() ? "Inactivating Channel" : "Activating Channel")
                .details(entity.isActive() ? "Inactive Channel " + entity.getName() : "Activating Channel " + entity.getName())
                .email(currentUser)
                .eventTime(new Timestamp(System.currentTimeMillis()))
                .build()
        );

        entity.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        entity.setUpdatedBy(currentUser);
        entity.setActive(!entity.isActive());

        repo.save(entity);
    }

    /**
     * Retrieves a list of channels for dropdown/search purposes.
     *
     * @param search optional search term
     * @return a list of {@link ReceiverRequest} containing channel IDs, names, and codes
     */
    public List<ReceiverRequest> getChannels(String search) {
        if (search == null || search.isBlank()) {
            return repo.findAllByActiveTrue(PageRequest.of(0, 3))
                    .stream()
                    .map(r -> ReceiverRequest.builder()
                            .id(r.getId())
                            .name(r.getName())
                            .code(r.getCode())
                            .build())
                    .toList();
        } else {
            return repo.findTop5ByNameContainingIgnoreCaseAndActiveTrue(search)
                    .stream()
                    .map(r -> ReceiverRequest.builder()
                            .id(r.getId())
                            .name(r.getName())
                            .code(r.getCode())
                            .build())
                    .toList();
        }
    }
}
