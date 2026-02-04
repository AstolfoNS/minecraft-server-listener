package com.timeleafing.minecraft.api.controller;

import com.timeleafing.minecraft.common.response.R;
import com.timeleafing.minecraft.model.vo.MinecraftServer;
import com.timeleafing.minecraft.service.MinecraftProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping
public class MinecraftController {

    private final MinecraftProcessService service;

    public record CmdReq(String cmd) {}


    @PostMapping("/{serverId}/cmd")
    public R<Void> sendCmd(@PathVariable String serverId, @RequestBody CmdReq req) throws IOException {
        service.sendCmd(serverId, req.cmd());
        return R.ok();
    }

    @PostMapping("/{serverId}/start")
    public R<Void> startServer(@PathVariable String serverId) throws IOException {
        service.startServer(serverId);
        return R.ok();
    }

    @PostMapping("/{serverId}/stop")
    public R<Void> stopServer(@PathVariable String serverId) {
        service.stopServer(serverId);
        return R.ok();
    }

    @GetMapping("/server/{serverId}")
    public R<MinecraftServer> getServer(@PathVariable String serverId) {
        return R.ok(service.getServer(serverId));
    }

    @GetMapping("/servers")
    public R<List<MinecraftServer>> listAllServer() {
        return R.ok(service.listAllServer());
    }

}
