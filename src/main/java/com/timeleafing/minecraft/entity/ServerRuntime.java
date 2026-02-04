package com.timeleafing.minecraft.entity;

import com.timeleafing.minecraft.common.enumeration.ServerStatus;
import lombok.Data;

import java.io.BufferedWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Data
public class ServerRuntime {

    private String serverId;

    private Process process;

    private BufferedWriter writer;

    private Thread logReaderThread;

    private Thread processWatcherThread;

    // 使用 AtomicBoolean 来保持线程安全的运行状态标识
    private final AtomicBoolean running = new AtomicBoolean(false);

    // 使用 ReentrantLock 来替代 synchronized 锁
    private final Lock lock = new ReentrantLock();


    public static Boolean isRunning(ServerRuntime runtime) {
        return runtime != null && runtime.getRunning().get();
    }

}
