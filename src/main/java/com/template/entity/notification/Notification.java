package com.template.entity.notification;

import com.template.entity.BaseEntity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @Id
    private Long id;
    private Long userId;
    private String title;
    private String message;
    private String type;
    private Boolean isRead;
    private String link;
    private String createdBy;
    private LocalDateTime createdDate;
}
