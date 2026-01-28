package com.timeleafing.minecraft.service;

import com.timeleafing.minecraft.websocket.LogWebSocket;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class MinecraftProcessService {

    @Value("#{minecraftProperty.workDir}")
    private String workDir;

    @Value("#{minecraftProperty.runScript}")
    private String runScript;

    // 当进程较慢停止时等待的最长时间
    private static final Duration STOP_WAIT_TIMEOUT = Duration.ofSeconds(30);

    private Process process;

    private BufferedWriter writer;

    private Thread logReaderThread;

    private Thread processWatcherThread;

    private final Object lock = new Object();

    private final AtomicBoolean running = new AtomicBoolean(false);


    /**
     * 启动 Minecraft 服务（如果未运行）
     */
    public synchronized void startServer() throws IOException {
        if (running.get()) {
            log.warn("Minecraft server already running");
            return;
        }

        log.info("Starting Minecraft server (workDir={}, script={})", workDir, runScript);

        ProcessBuilder builder = new ProcessBuilder("bash", "-c", runScript);
        if (workDir != null && !workDir.isBlank()) {
            builder.directory(new File(workDir));
        }
        // 合并 stdout/stderr
        builder.redirectErrorStream(true);

        process = builder.start();

        writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        running.set(true);

        // 启动日志读取线程（从 process stdout 读取）
        logReaderThread = new Thread(this::readProcessOutput, "mc-log-reader");
        logReaderThread.setDaemon(true);
        logReaderThread.start();

        // 监控进程退出，自动清理资源
        processWatcherThread = new Thread(this::watchProcess, "mc-process-watcher");
        processWatcherThread.setDaemon(true);
        processWatcherThread.start();

        log.info("Minecraft server started successfully.");
    }

    /**
     * 向 Minecraft 控制台发送命令（同步阻塞直到写入 flush）
     */
    public synchronized void sendCommand(String command) throws IOException {
        if (!running.get() || writer == null) {
            throw new IllegalStateException("Minecraft server is not running");
        }
        writer.write(command);
        writer.newLine();
        writer.flush();
        log.info("-> Sent command to server: {}", command);
    }

    /**
     * 优雅停止服务。
     * - 先发送 "stop"
     * - 等待 STOP_WAIT_TIMEOUT ，超时则强制销毁进程
     */
    public synchronized void stopServer() throws IOException {
        if (!running.get()) {
            log.info("Minecraft server is not running, nothing to stop.");
            return;
        }

        log.info("Stopping Minecraft server gracefully...");
        try {
            // 发送 stop 命令
            sendCommand("stop");
        } catch (IOException e) {
            log.warn("Failed to send stop command, attempting to destroy process", e);
        }

        // 等待进程退出
        boolean exited = false;
        try {
            exited = process.waitFor(STOP_WAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for server to stop.");
        }

        if (!exited) {
            log.warn("Minecraft server did not exit within {}s, destroying forcibly.", STOP_WAIT_TIMEOUT.getSeconds());
            try {
                process.destroyForcibly();
            } catch (Exception ex) {
                log.error("Failed to forcibly destroy Minecraft process", ex);
            }
        } else {
            log.info("Minecraft server stopped gracefully.");
        }

        // 清理资源
        cleanupProcessResources();
    }

    /**
     * 进程输出读取逻辑：从 process.getInputStream() 读取并广播（不在此线程同步发送）
     */
    private void readProcessOutput() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 本地日志输出
                log.info("MC: {}", line);
                // 非阻塞地把日志放进 WebSocket 的广播队列
                LogWebSocket.broadcast(line);
            }
        } catch (IOException e) {
            if (running.get()) {
                log.error("Error reading Minecraft server output", e);
            } else {
                log.debug("Stop reading output because server is not running");
            }
        } finally {
            log.debug("Log reader thread exiting.");
        }
    }

    /**
     * 监控进程线程：等待 process 退出，退出后做清理
     */
    private void watchProcess() {
        try {
            int exitCode = process.waitFor();
            log.info("Minecraft process exited with code {}", exitCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Process watcher interrupted.");
        } finally {
            // 标记为非运行并清理（若其他线程没做）
            synchronized (lock) {
                running.set(false);
                cleanupProcessResources();
            }
            // 通知前端
            LogWebSocket.broadcast("[SERVER] Minecraft server has stopped (exit).");
        }
    }

    /**
     * 关闭 writer/进程流等资源
     */
    private void cleanupProcessResources() {
        // 关闭 writer
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            log.warn("Error closing writer", e);
        } finally {
            writer = null;
        }

        // 关闭/销毁 process
        try {
            if (process != null && process.isAlive()) {
                try {
                    process.destroy();
                } catch (Exception ignored) { }
            }
        } finally {
            process = null;
        }

        // 中断 logReaderThread（若仍在）
        try {
            if (logReaderThread != null && logReaderThread.isAlive()) {
                logReaderThread.interrupt();
            }
        } catch (Exception ignored) { }

        // 中断 watcher thread（若仍在）
        try {
            if (processWatcherThread != null && processWatcherThread.isAlive()) {
                processWatcherThread.interrupt();
            }
        } catch (Exception ignored) { }
    }

    @PreDestroy
    public void onDestroy() {
        log.info("Shutting down MinecraftProcessService...");
        try {
            stopServer();
        } catch (IOException e) {
            log.warn("Error while stopping server on destroy", e);
        } finally {
            // 关闭 websocket 广播器
            try {
                LogWebSocket.broadcast("[SERVER] Application shutting down, stopping log stream.");
                // 尝试优雅关闭 WebSocket broadcaster
                LogWebSocket.class.getMethod("close").invoke(null);
            } catch (NoSuchMethodException ignored) {
                // fallback: call static close if accessible
            } catch (Exception e) {
                log.warn("Failed to shutdown LogWebSocket broadcaster", e);
            }
        }
    }
}
