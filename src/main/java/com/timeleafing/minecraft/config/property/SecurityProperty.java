package com.timeleafing.minecraft.config.property;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "security")
public class SecurityProperty {
    @NotBlank
    private String hmacSecret;

    @NotNull
    private Long maxSkewSeconds = 60L;

    @NotBlank
    private String headerTs = "X-TS";

    @NotBlank
    private String headerNonce = "X-NONCE";

    @NotBlank
    private String headerSign = "X-SIGN";

}
