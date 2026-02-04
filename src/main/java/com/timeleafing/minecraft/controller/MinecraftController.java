package com.timeleafing.minecraft.controller;

import com.timeleafing.minecraft.common.response.R;
import com.timeleafing.minecraft.entity.MinecraftServer;
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


    @PostMapping("/index/{index}/cmd")
    public R<Void> cmdByIndex(@PathVariable int index, @RequestBody CmdReq req) throws IOException {
        service.sendCmd(index, req.cmd());
        return R.ok();
    }

    @PostMapping("/index/{index}/start")
    public R<Void> startByIndex(@PathVariable int index) throws IOException {
        service.startServer(index);
        return R.ok();
    }

    @PostMapping("/index/{index}/stop")
    public R<Void> stopByIndex(@PathVariable int index) {
        service.stopServer(index);
        return R.ok();
    }

    @GetMapping("/server/index/{index}")
    public R<MinecraftServer> getServerByIndex(@PathVariable int index) {
        return R.ok(service.getServer(index));
    }

    @PostMapping("/{serverId}/cmd")
    public R<Void> cmd(@PathVariable String serverId, @RequestBody CmdReq req) throws IOException {
        service.sendCmd(serverId, req.cmd());
        return R.ok();
    }

    @PostMapping("/{serverId}/start")
    public R<Void> start(@PathVariable String serverId) throws IOException {
        service.startServer(serverId);
        return R.ok();
    }

    @PostMapping("/{serverId}/stop")
    public R<Void> stop(@PathVariable String serverId) {
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
