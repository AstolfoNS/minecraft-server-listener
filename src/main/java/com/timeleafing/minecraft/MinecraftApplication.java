package com.timeleafing.minecraft;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan("com.timeleafing.minecraft")
@SpringBootApplication
public class MinecraftApplication {

	public static void main(String[] args) {
		SpringApplication.run(MinecraftApplication.class, args);
	}

}
