package com.equilibrium.mcp_video;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

import com.equilibrium.mcp_video.controller.YouTubeMCPToolController;

@SpringBootApplication
public class EquilibriumMCPVideoApplication {

    public static void main(String[] args) {
        SpringApplication.run(EquilibriumMCPVideoApplication.class, args);
        System.out.println("🚀 Equilibrium MCP youtube iniciado com sucesso!");
    }

    @Bean
    public ToolCallbackProvider tools(@Lazy YouTubeMCPToolController youTubeMCPToolController) {
        return MethodToolCallbackProvider.builder().toolObjects(youTubeMCPToolController).build();
    }
}
