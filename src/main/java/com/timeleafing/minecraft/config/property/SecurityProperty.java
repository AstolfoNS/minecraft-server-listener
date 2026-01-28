package com.timeleafing.minecraft.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "security")
@Component
public class SecurityProperty {

    private String apiKey;

    private String headerName;

}
