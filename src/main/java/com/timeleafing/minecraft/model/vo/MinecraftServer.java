package com.timeleafing.minecraft.model.vo;

import com.timeleafing.minecraft.common.enumeration.ServerStatus;

public record MinecraftServer(
        String serverId,
        ServerStatus status
) {}
