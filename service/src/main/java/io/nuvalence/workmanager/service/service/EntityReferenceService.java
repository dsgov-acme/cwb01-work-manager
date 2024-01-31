package io.nuvalence.workmanager.service.service;

import io.nuvalence.workmanager.service.domain.securemessaging.EntityReference;
import io.nuvalence.workmanager.service.domain.securemessaging.EntityType;
import io.nuvalence.workmanager.service.repository.EntityReferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EntityReferenceService {
    private final EntityReferenceRepository entityReferenceRepository;

    List<EntityReference> findByEntityIdAndEntityType(UUID entityId, EntityType entityType) {
        return entityReferenceRepository.findByEntityIdAndType(entityId, entityType);
    }
}
