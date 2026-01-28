package com.timeleafing.minecraft.controller;

import com.timeleafing.minecraft.service.MinecraftProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/minecraft")
@RequiredArgsConstructor
public class MinecraftController {

    private final MinecraftProcessService minecraftProcessService;


    /** 启动服务器 */
    @PostMapping("/start")
    public String startServer() {
        try {
            minecraftProcessService.startServer();

            return "Minecraft server started.";
        } catch (Exception e) {
            log.error("Failed to start Minecraft server", e);

            return "Failed to start: %s".formatted(e.getMessage());
        }
    }

    /** 发送控制台命令 */
    @PostMapping("/cmd")
    public String sendCommand(@RequestParam String command) {
        try {
            minecraftProcessService.sendCommand(command);

            return "Cmd sent: %s".formatted(command);
        } catch (Exception e) {
            log.error("Failed to send cmd", e);

            return "Failed: %s".formatted(e.getMessage());
        }
    }

    /** 停止服务器 */
    @PostMapping("/stop")
    public String stopServer() {
        try {
            minecraftProcessService.stopServer();

            return "Minecraft server stopped.";
        } catch (Exception e) {
            log.error("Failed to stop Minecraft server", e);

            return "Failed to stop: %s".formatted(e.getMessage());
        }
    }
}
