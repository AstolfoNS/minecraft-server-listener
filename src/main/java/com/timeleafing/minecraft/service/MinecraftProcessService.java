package com.timeleafing.minecraft.service;

import com.timeleafing.minecraft.common.validation.annotation.RequireServerId;
import com.timeleafing.minecraft.model.vo.MinecraftServer;

import java.io.IOException;
import java.util.List;

public interface MinecraftProcessService {

    void sendCmd(@RequireServerId String serverId, String cmd) throws IOException;

    void startServer(@RequireServerId String serverId) throws IOException;

    void stopServer(@RequireServerId String serverId);

    MinecraftServer getServer(@RequireServerId String serverId);

    List<MinecraftServer> listAllServer();
}
