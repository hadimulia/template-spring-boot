package com.template.entity.audit;

import javax.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Table(name = "audit_logs")
public class AuditLog {
    private Long id;
    private Long tenantId;
    private String entityType;
    private Long entityId;
    private String action;
    private String oldValue;
    private String newValue;
    private String description;
    private String performedBy;
    private LocalDateTime performedAt;
    private String ipAddress;
}
