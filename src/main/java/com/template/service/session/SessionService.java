package com.template.service.session;

import com.template.dto.session.SessionInfo;
import com.template.entity.user.User;
import com.template.mapper.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRegistry sessionRegistry;
    private final SessionStore sessionStore;
    private final UserMapper userMapper;

    public List<SessionInfo> getActiveSessions(String currentUsername) {
        List<SessionInfo> sessions = new ArrayList<>();
        var sessionMetaMap = sessionStore.getAll();

        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof org.springframework.security.core.userdetails.User userDetails) {
                List<SessionInformation> sessionInfos = sessionRegistry.getAllSessions(principal, false);

                for (SessionInformation si : sessionInfos) {
                    String username = userDetails.getUsername();
                    String fullname = username;

                    User user = userMapper.findByUsername(username);
                    if (user != null) {
                        fullname = user.getFullname();
                    }

                    var meta = sessionMetaMap.get(si.getSessionId());

                    sessions.add(SessionInfo.builder()
                            .sessionId(si.getSessionId())
                            .username(username)
                            .fullname(fullname)
                            .ipAddress(meta != null ? meta.ip() : "N/A")
                            .browser(meta != null ? meta.browser() : "N/A")
                            .loginTime(meta != null ? meta.loginTime() :
                                    LocalDateTime.ofInstant(
                                            Instant.ofEpochMilli(si.getLastRequest().getTime()),
                                            ZoneId.systemDefault()))
                            .currentSession(username.equals(currentUsername))
                            .build());
                }
            }
        }

        return sessions;
    }

    public void forceLogout(String sessionId) {
        SessionInformation sessionInfo = sessionRegistry.getSessionInformation(sessionId);
        if (sessionInfo != null) {
            sessionInfo.expireNow();
        }
    }
}
