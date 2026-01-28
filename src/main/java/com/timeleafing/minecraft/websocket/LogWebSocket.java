package com.timeleafing.minecraft.websocket;

import jakarta.annotation.PreDestroy;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.util.Set;
import java.util.concurrent.*;

/**
 * WebSocket endpoint for log streaming.
 * - 使用 CopyOnWriteArraySet 管理 session（线程安全）
 * - 内部使用单线程广播器把消息异步发送给所有 open 的 session（使用 session.getAsyncRemote()）
 * - 内部队列限容量，队列满时丢弃最旧消息以保留新消息（防止内存爆炸）
 */
@Slf4j
@Component
@ServerEndpoint("/ws/log")
public class LogWebSocket implements Closeable {

    private static final Set<Session> sessions = new CopyOnWriteArraySet<>();

    private static final int QUEUE_CAPACITY = 10_000;

    private static final BlockingQueue<String> broadcastQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    private static final ExecutorService broadcaster = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ws-broadcaster");
        t.setDaemon(true);
        return t;
    });

    // 控制广播线程生命周期
    private static volatile boolean running = true;

    static {
        broadcaster.submit(() -> {
            while (running || !broadcastQueue.isEmpty()) {
                String msg;
                try {
                    msg = broadcastQueue.poll(500, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    continue;
                }
                if (msg == null) {
                    continue;
                }
                for (Session session : sessions) {
                    sendMessageAsync(session, msg);
                }
            }
            log.info("WebSocket broadcaster stopped.");
        });
    }

    /** 负责向单个 Session 异步发送消息 */
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

    /** 处理发送后的回调结果 */
    private static void handleSendResult(Session session, SendResult r) {
        if (r.isOK()) return;

        Throwable err = r.getException();
        if (err != null) {
            log.warn("Async send failed to {}: {}", session.getId(), err.getMessage());
        } else {
            log.warn("Async send failed to {}: unknown reason", session.getId());
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        log.info("New WebSocket connection: {}", session.getId());
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        log.info("WebSocket closed: {}", session.getId());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        String id = session != null ? session.getId() : "unknown";
        log.error("WebSocket error on session {}: {}", id, throwable.getMessage(), throwable);
    }

    /**
     * 将日志加入广播队列（非阻塞）。若队列已满，则丢弃最旧消息以保留新消息（防刷屏时优先保留最新日志）。
     */
    public static void broadcast(String message) {
        if (!running) return;

        boolean offered = broadcastQueue.offer(message);
        if (!offered) {
            // 队列满：丢弃最旧的一条后再尝试插入（保留新消息）
            String dropped = broadcastQueue.poll();
            if (dropped != null) {
                log.debug("Broadcast queue full, dropped oldest message.");
            }
            // 尝试再次插入（若仍失败则直接丢弃）
            offered = broadcastQueue.offer(message);
            if (!offered) {
                log.warn("Broadcast queue full, message dropped.");
            }
        }
    }

    /**
     * 优雅关闭广播线程和清理资源（在应用关闭时调用）
     */
    @PreDestroy
    @Override
    public void close() {
        running = false;
        try {
            // 等待队列处理完或超时
            broadcaster.shutdown();
            if (!broadcaster.awaitTermination(3, TimeUnit.SECONDS)) {
                broadcaster.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            broadcaster.shutdownNow();
        }
        // 关闭所有 open 会话
        for (Session session : sessions) {
            try {
                if (session.isOpen()) session.close();
            } catch (Exception ignored) { }
        }
        sessions.clear();
        log.info("LogWebSocket shutdown complete.");
    }
}
