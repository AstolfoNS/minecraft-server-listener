package com.timeleafing.minecraft.service;

import com.timeleafing.minecraft.entity.MinecraftServer;

import java.io.IOException;
import java.util.List;

public interface MinecraftProcessService {

    void sendCmd(int index, String cmd) throws IOException;

    void startServer(int index) throws IOException;

    void stopServer(int index);

    MinecraftServer getServer(int index);

    void sendCmd(String serverId, String cmd) throws IOException;

    void startServer(String serverId) throws IOException;

    void stopServer(String serverId);

    MinecraftServer getServer(String serverId);

    List<MinecraftServer> listAllServer();

}
