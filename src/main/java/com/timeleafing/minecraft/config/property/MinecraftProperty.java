package com.timeleafing.minecraft.config.property;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "minecraft")
public class MinecraftProperty {

    @NotBlank
    private String workDir;

    @NotBlank
    private String runScript;

}
