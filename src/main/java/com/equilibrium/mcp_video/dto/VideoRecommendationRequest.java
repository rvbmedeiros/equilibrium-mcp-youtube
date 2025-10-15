package com.equilibrium.mcp_video.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para recomendação de vídeos do YouTube
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoRecommendationRequest {
    
    /**
     * Estado completo do usuário
     */
    private UserState userState;
    
    /**
     * Categoria desejada (opcional)
     * nature, meditation, music, breathing
     */
    private String category;
    
    /**
     * Número máximo de vídeos a retornar (padrão: 10)
     */
    private Integer maxResults = 10;
    
    /**
     * Duração preferida em minutos (opcional)
     * short: < 15 min
     * medium: 15-45 min
     * long: > 45 min
     */
    private String preferredDuration;
    
    /**
     * Idioma preferido (padrão: pt)
     */
    private String language = "pt";
}
