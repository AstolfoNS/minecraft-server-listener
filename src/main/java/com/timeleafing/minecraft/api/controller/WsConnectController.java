package com.timeleafing.minecraft.api.controller;

import com.timeleafing.minecraft.api.websocket.LogWebSocket;
import com.timeleafing.minecraft.common.response.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequestMapping("/ws")
@RestController
public class WsConnectController {

    @GetMapping("/online")
    public R<Map<String, Integer>> online() {
        return R.ok(LogWebSocket.snapshotOnline());
    }

}
