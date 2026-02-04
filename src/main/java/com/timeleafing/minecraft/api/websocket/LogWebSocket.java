package com.timeleafing.minecraft.api.websocket;

import jakarta.annotation.PreDestroy;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@ServerEndpoint("/ws/log/{serverId}")
public class LogWebSocket implements Closeable {

    /** 每个 serverId 对应一组 session */
    private static final ConcurrentHashMap<String, CopyOnWriteArraySet<Session>> sessionsByServer = new ConcurrentHashMap<>();

    /** 广播事件：携带 serverId，避免混流 */
    private record WsEvent(String serverId, String message) {}

    private static final int QUEUE_CAPACITY = 10_000;

    // 使用 LinkedBlockingQueue 处理消息队列
    private static final BlockingQueue<WsEvent> broadcastQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    // 使用线程池管理广播任务
    private static final ExecutorService broadcaster = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "ws-broadcaster");
        t.setDaemon(true);
        return t;
    });

    private static final AtomicBoolean running = new AtomicBoolean(true);

    static {
        broadcaster.submit(() -> {
            while (running.get() || !broadcastQueue.isEmpty()) {
                WsEvent ev;
                try {
                    ev = broadcastQueue.poll(500, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    continue;
                }
                if (ev == null) {
                    continue;
                }
                Set<Session> sessions = sessionsByServer.get(ev.serverId());
                if (sessions == null || sessions.isEmpty()) {
                    continue;
                }

                // 批量发送消息
                for (Session session : sessions) {
                    sendMessageAsync(session, ev.message());
                }
            }
            log.info("WebSocket broadcaster stopped.");
        });
    }

    private static void sendMessageAsync(Session session, String msg) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.getAsyncRemote().sendText(msg, r -> handleSendResult(session, r));
        } catch (Exception e) {
            log.error("Exception sending to {}: {}", session.getId(), e.getMessage());
        }
    }

    private static void handleSendResult(Session session, SendResult r) {
        if (r.isOK()) {
            return;
        }
        Throwable err = r.getException();
        if (err != null) {
            log.warn("Async send failed to {}: {}", session.getId(), err.getMessage());
        } else {
            log.warn("Async send failed to {}: unknown reason", session.getId());
        }
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("serverId") String serverId) {
        sessionsByServer.computeIfAbsent(serverId, k -> new CopyOnWriteArraySet<>()).add(session);
        session.getUserProperties().put("serverId", serverId);
        log.info("WS open: serverId={}, session={}", serverId, session.getId());
    }

    @OnClose
    public void onClose(Session session) {
        removeSession(session);
        log.info("WS closed: {}", session.getId());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        String id = session != null ? session.getId() : "unknown";
        log.error("WS error on session {}: {}", id, throwable.getMessage(), throwable);
        if (session != null) {
            removeSession(session);
        }
    }

    private void removeSession(Session session) {
        if (session == null) {
            return;
        }
        String serverId = (String) session.getUserProperties().get("serverId");
        if (serverId == null) {
            return;
        }
        CopyOnWriteArraySet<Session> set = sessionsByServer.get(serverId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) {
                sessionsByServer.remove(serverId);
            }
        }
    }

    /**
     * 对外广播：按 serverId 广播
     */
    public static void broadcast(String serverId, String message) {
        if (!running.get()) return;

        boolean offered = broadcastQueue.offer(new WsEvent(serverId, message));
        if (!offered) {
            // 队列满：丢弃最旧的消息后再尝试插入（保留新消息）
            WsEvent dropped = broadcastQueue.poll();
            if (dropped != null) log.debug("Broadcast queue full, dropped oldest message.");
            offered = broadcastQueue.offer(new WsEvent(serverId, message));
            if (!offered) log.warn("Broadcast queue full, message dropped.");
        }
    }

    /**
     * （可选）保留你原来的 broadcastAll(message) 作为兼容：直接发给所有 serverId 的订阅者
     */
    public static void broadcastAll(String message) {
        for (Map.Entry<String, CopyOnWriteArraySet<Session>> e : sessionsByServer.entrySet()) {
            broadcast(e.getKey(), message);
        }
    }

    @PreDestroy
    @Override
    public void close() {
        running.set(false);

        try {
            broadcaster.shutdown();
            if (!broadcaster.awaitTermination(3, TimeUnit.SECONDS)) {
                broadcaster.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            broadcaster.shutdownNow();
        }

        // 关闭所有会话
        for (CopyOnWriteArraySet<Session> set : sessionsByServer.values()) {
            for (Session session : set) {
                try {
                    if (session.isOpen()) session.close();
                } catch (Exception ignored) {}
            }
            set.clear();
        }
        sessionsByServer.clear();

        log.info("LogWebSocket shutdown complete.");
    }
}
