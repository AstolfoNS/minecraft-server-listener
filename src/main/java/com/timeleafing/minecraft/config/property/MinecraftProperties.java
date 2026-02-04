package com.timeleafing.minecraft.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@Validated
@ConfigurationProperties(prefix = "minecraft")
public class MinecraftProperties {

    private List<ServerConfig> servers = new ArrayList<>();

    @Data
    public static class ServerConfig {
        private String id;

        private String workDir;

        private String runScript;
    }

    public ServerConfig byIndex(int index) {
        if (index < 0 || index >= servers.size()) throw new IllegalArgumentException("bad index " + index);
        return servers.get(index);
    }

    public ServerConfig byId(String id) {
        return servers.stream()
                .filter(s -> Objects.equals(s.getId(), id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown server id: " + id));
    }

}
