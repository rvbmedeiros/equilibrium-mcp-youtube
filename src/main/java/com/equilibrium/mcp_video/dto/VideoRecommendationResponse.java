package com.equilibrium.mcp_video.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response com recomendações de vídeos do YouTube
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoRecommendationResponse {
    
    private List<VideoRecommendation> recommendations;
    private String insights;
    private List<String> suggestions;
    private Long processingTimeMs;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoRecommendation {
        private String category;
        private List<RecommendedVideo> videos;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendedVideo {
        private String videoId;
        private String title;
        private String description;
        private String thumbnailUrl;
        private String contentUrl;
        private Integer durationSeconds;
        private String channelTitle;
        private String reason; // Por que foi recomendado
        private Integer matchScore; // 0-100
        private List<String> tags;
    }
}
