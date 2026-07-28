package com.template.service.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SessionStore {

    private final Map<String, SessionMeta> store = new ConcurrentHashMap<>();

    public void register(String sessionId, String username, String ip, String browser) {
        store.put(sessionId, new SessionMeta(username, ip, browser, LocalDateTime.now()));
    }

    public SessionMeta get(String sessionId) {
        return store.get(sessionId);
    }

    public void remove(String sessionId) {
        store.remove(sessionId);
    }

    public Map<String, SessionMeta> getAll() {
        return Map.copyOf(store);
    }

    public record SessionMeta(String username, String ip, String browser, LocalDateTime loginTime) {}

    @EventListener
    public void onSessionDestroyed(HttpSessionDestroyedEvent event) {
        String sessionId = event.getSession().getId();
        SessionMeta meta = store.remove(sessionId);
        if (meta != null) {
            log.debug("Session destroyed: {} user={}", sessionId, meta.username());
        }
    }
}
