package com.template.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SessionInfo {
    private String sessionId;
    private String username;
    private String fullname;
    private String ipAddress;
    private String browser;
    private LocalDateTime loginTime;
    private boolean currentSession;
}
