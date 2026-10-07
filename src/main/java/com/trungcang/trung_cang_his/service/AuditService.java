package com.trungcang.trung_cang_his.service;

import com.trungcang.trung_cang_his.domain.AuditEvent;
import com.trungcang.trung_cang_his.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditEventRepository events;

    @Transactional(propagation = Propagation.REQUIRED)
    public void record(String eventType, String entityType, Object entityId, String actorUsername, String detail) {
        AuditEvent event = new AuditEvent();
        event.setEventType(eventType);
        event.setEntityType(entityType);
        event.setEntityId(entityId == null ? null : String.valueOf(entityId));
        event.setActorUsername(actorUsername == null || actorUsername.isBlank() ? "SYSTEM" : actorUsername);
        event.setOccurredAt(LocalDateTime.now());
        event.setDetail(detail);
        events.save(event);
    }
}
