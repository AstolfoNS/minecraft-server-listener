package com.timeleafing.minecraft.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "minecraft")
@Component
public class MinecraftProperty {

    private String workDir;

    private String runScript;

}
