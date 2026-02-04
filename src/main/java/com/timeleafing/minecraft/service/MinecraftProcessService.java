package com.timeleafing.minecraft.service;

import com.timeleafing.minecraft.model.vo.MinecraftServer;

import java.io.IOException;
import java.util.List;

public interface MinecraftProcessService {

    void sendCmd(String serverId, String cmd) throws IOException;

    void startServer(String serverId) throws IOException;

    void stopServer(String serverId);

    MinecraftServer getServer(String serverId);

    List<MinecraftServer> listAllServer();

}
