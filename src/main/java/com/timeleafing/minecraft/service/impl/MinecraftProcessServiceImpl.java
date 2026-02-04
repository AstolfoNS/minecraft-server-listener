package com.timeleafing.minecraft.service.impl;

import com.timeleafing.minecraft.common.constant.Pass;
import com.timeleafing.minecraft.common.enumeration.ServerStatus;
import com.timeleafing.minecraft.config.property.MinecraftProperties;
import com.timeleafing.minecraft.exception.BizException;
import com.timeleafing.minecraft.model.vo.MinecraftServer;
import com.timeleafing.minecraft.model.dto.ServerRuntime;
import com.timeleafing.minecraft.service.MinecraftProcessService;
import com.timeleafing.minecraft.api.websocket.LogWebSocket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class MinecraftProcessServiceImpl implements MinecraftProcessService {

    private final MinecraftProperties props;

    private final ConcurrentHashMap<String, ServerRuntime> runtimes = new ConcurrentHashMap<>();

    private static final Duration STOP_WAIT_TIMEOUT = Duration.ofSeconds(30);

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private ServerRuntime runtime(String serverId) {
        return runtimes.computeIfAbsent(serverId, k -> new ServerRuntime());
    }

    @Override
    public void sendCmd(String serverId, String cmd) throws IOException {
        var runtime = runtime(serverId);
        runtime.getLock().lock();

        try {
            if (!runtime.getRunning().get() || runtime.getWriter() == null) {
                throw new BizException("Server not running: " + serverId);
            }
            runtime.getWriter().write(cmd);
            runtime.getWriter().newLine();
            runtime.getWriter().flush();
            log.info("[{}] -> {}", serverId, cmd);
        } finally {
            runtime.getLock().unlock();
        }
    }

    @Override
    public void startServer(String serverId) throws IOException {
        var serverConfig = props.byId(serverId);
        var runtime = runtime(serverId);

        runtime.getLock().lock();

        try {
            if (runtime.getRunning().get()) {
                log.warn("[{}] already running", serverId);
                return;
            }

            log.info("[{}] Starting (workDir={}, script={})", serverId, serverConfig.getWorkDir(), serverConfig.getRunScript());

            // 使用一个标志确保进程不被多次启动
            synchronized (runtime) {
                if (runtime.getRunning().get()) {
                    return; // 防止多次启动
                }

                ProcessBuilder builder = new ProcessBuilder("bash", "-c", serverConfig.getRunScript());
                builder.directory(new File(serverConfig.getWorkDir()));
                builder.redirectErrorStream(true);

                try {
                    Process process = builder.start();
                    runtime.setProcess(process);
                    runtime.setWriter(new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)));
                    runtime.getRunning().set(true);

                    executorService.submit(() -> readProcessOutput(serverId));
                    executorService.submit(() -> watchProcess(serverId));

                    LogWebSocket.broadcast(serverId, "[SERVER] started");
                } catch (IOException e) {
                    log.error("[{}] Failed to start server", serverId, e);
                    throw new IOException("Failed to start server", e);
                }
            }
        } finally {
            runtime.getLock().unlock();
        }
    }

    @Override
    public void stopServer(String serverId) {
        var runtime = runtime(serverId);
        runtime.getLock().lock();

        try {
            if (!runtime.getRunning().get()) {
                log.info("[{}] not running", serverId);
                return;
            }
            log.info("[{}] Stopping gracefully...", serverId);

            try {
                sendCmd(serverId, "stop");
            } catch (IOException e) {
                log.error("[{}] Error sending stop command", serverId, e);
            }

            boolean exited = false;
            try {
                exited = runtime.getProcess().waitFor(STOP_WAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (!exited) {
                log.warn("[{}] stop timeout, destroying forcibly", serverId);
                runtime.getProcess().destroyForcibly();
            }

            cleanup(serverId);
            LogWebSocket.broadcast(serverId, "[SERVER] stopped");
        } finally {
            runtime.getLock().unlock();
        }
    }

    @Override
    public MinecraftServer getServer(String serverId) {
        return new MinecraftServer(serverId, getServerStatus(serverId));
    }

    @Override
    public List<MinecraftServer> listAllServer() {
        return props.getServers().stream()
                .map(serverConfig -> new MinecraftServer(serverConfig.getId(), getServerStatus(serverConfig.getId())))
                .collect(Collectors.toList());
    }

    private ServerStatus getServerStatus(String serverId) {
        return ServerRuntime.isRunning(runtimes.get(serverId)) ? ServerStatus.RUNNING : ServerStatus.STOPPED;
    }

    private void readProcessOutput(String serverId) {
        var runtime = runtime(serverId);
        Process p = runtime.getProcess();
        if (p == null) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[{}] {}", serverId, line);
                LogWebSocket.broadcast(serverId, line);
            }
        } catch (IOException e) {
            log.error("[{}] read output err", serverId, e);
        }
    }

    private void watchProcess(String serverId) {
        var runtime = runtime(serverId);
        Process p = runtime.getProcess();
        if (p == null) {
            return;
        }
        try {
            int exitCode = p.waitFor();
            log.info("[{}] exited code={}", serverId, exitCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            synchronized (runtime.getLock()) {
                runtime.getRunning().set(false);
                cleanup(serverId);
            }
            LogWebSocket.broadcast(serverId, "[SERVER] exit");
        }
    }

    private void cleanup(String serverId) {
        var rt = runtime(serverId);
        closeWriter(rt);
        destroyProcess(rt);
        interruptThreads(rt);
    }

    private void closeWriter(ServerRuntime rt) {
        try (BufferedWriter ignored = rt.getWriter()) {
            Pass.pass();
        } catch (IOException e) {
            log.error("[{}] Failed to close writer: {}", rt.getServerId(), e.getMessage());
        } finally {
            log.info("[{}] Writer closed successfully.", rt.getServerId());
            rt.setWriter(null);
        }
    }

    private void destroyProcess(ServerRuntime rt) {
        try {
            if (rt.getProcess() != null && rt.getProcess().isAlive()) {
                rt.getProcess().destroy();
            }
        } catch (Exception e) {
            log.error("[{}] Failed to destroy process: {}", rt.getServerId(), e.getMessage());
        }
        rt.setProcess(null);
    }

    private void interruptThreads(ServerRuntime rt) {
        try {
            if (rt.getLogReaderThread() != null) {
                rt.getLogReaderThread().interrupt();
            }
        } catch (Exception ignored) {}

        try {
            if (rt.getProcessWatcherThread() != null) {
                rt.getProcessWatcherThread().interrupt();
            }
        } catch (Exception ignored) {}

        rt.setLogReaderThread(null);
        rt.setProcessWatcherThread(null);
    }
}
