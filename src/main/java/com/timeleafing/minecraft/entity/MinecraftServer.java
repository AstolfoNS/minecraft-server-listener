package com.timeleafing.minecraft.entity;

import com.timeleafing.minecraft.common.enumeration.ServerStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class MinecraftServer {

    private String serverId;

    private ServerStatus status;

}
