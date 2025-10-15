package com.equilibrium.mcp_video.service;

import com.equilibrium.mcp_video.dto.UserState;
import com.equilibrium.mcp_video.dto.VideoRecommendationRequest;
import com.equilibrium.mcp_video.dto.VideoRecommendationResponse;
import com.equilibrium.mcp_video.dto.VideoRecommendationResponse.RecommendedVideo;
import com.equilibrium.mcp_video.dto.VideoRecommendationResponse.VideoRecommendation;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço para buscar e recomendar vídeos do YouTube baseado no perfil do usuário
 * Integra com YouTube Data API v3 para buscar vídeos reais
 */
@Slf4j
@Service
public class YouTubeRecommendationService {

    @Value("${youtube.api.key:}")
    private String apiKey;

    private YouTube youtube;

    /**
     * Inicializa o cliente do YouTube
     */
    private YouTube getYouTubeService() {
        if (youtube == null) {
            try {
                youtube = new YouTube.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    null
                )
                .setApplicationName("Equilibrium-YouTube-MCP")
                .build();
            } catch (Exception e) {
                log.error("Erro ao inicializar YouTube Service", e);
                throw new RuntimeException("Erro ao inicializar YouTube Service", e);
            }
        }
        return youtube;
    }

    /**
     * Gera recomendações de vídeos baseado no estado completo do usuário
     * Este é o método principal que coordena todo o fluxo de recomendação
     */
    public VideoRecommendationResponse recommendVideos(VideoRecommendationRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("🎯 Iniciando recomendação de vídeos para usuário");
            
            UserState userState = request.getUserState();
            
            // 1. Analisar estado do usuário e definir estratégia de busca
            List<String> searchQueries = buildSearchQueries(userState, request.getCategory());
            log.info("📝 Queries de busca geradas: {}", searchQueries);
            
            // 2. Buscar vídeos para cada query
            List<RecommendedVideo> allVideos = new ArrayList<>();
            for (String query : searchQueries) {
                List<RecommendedVideo> videos = searchYouTubeVideos(query, request);
                allVideos.addAll(videos);
            }
            
            // 3. Filtrar e ranquear vídeos baseado no perfil do usuário
            List<RecommendedVideo> rankedVideos = rankVideos(allVideos, userState, request);
            
            // 4. Agrupar por categoria
            Map<String, List<RecommendedVideo>> videosByCategory = groupByCategory(rankedVideos);
            
            // 5. Criar recomendações por categoria
            List<VideoRecommendation> recommendations = videosByCategory.entrySet().stream()
                .map(entry -> new VideoRecommendation(
                    entry.getKey(),
                    entry.getValue().stream().limit(3).collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
            
            // 6. Gerar insights e sugestões
            String insights = generateInsights(userState);
            List<String> suggestions = generateSuggestions(userState);
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("✅ Recomendações geradas em {}ms", processingTime);
            
            return new VideoRecommendationResponse(
                recommendations,
                insights,
                suggestions,
                processingTime
            );
            
        } catch (Exception e) {
            log.error("❌ Erro ao gerar recomendações", e);
            throw new RuntimeException("Erro ao gerar recomendações", e);
        }
    }

    /**
     * Constrói queries de busca personalizadas baseado no estado do usuário
     * Este método cria prompts ricos em detalhes para a YouTube API
     */
    private List<String> buildSearchQueries(UserState userState, String category) {
        List<String> queries = new ArrayList<>();
        String language = "português"; // ou inglês baseado em configuração
        
        // Análise profunda do estado emocional para queries personalizadas
        if (userState.getStressLevel() != null && userState.getStressLevel() > 7) {
            // Stress alto: foco em relaxamento profundo
            queries.add("meditação guiada stress ansiedade reduzir " + language);
            queries.add("música relaxante dormir profundo ondas cerebrais");
            queries.add("sons da natureza chuva floresta relaxamento 4K");
            queries.add("yoga nidra relaxamento profundo guiado");
        } else if (userState.getAnxietyLevel() != null && userState.getAnxietyLevel() > 6) {
            // Ansiedade alta: técnicas de grounding e respiração
            queries.add("exercícios respiração ansiedade guiado");
            queries.add("meditação mindfulness presente momento");
            queries.add("sons calmantes ansiedade relaxar mente");
        } else if (userState.getEnergyLevel() != null && userState.getEnergyLevel() < 4) {
            // Energia baixa: vídeos energizantes e motivacionais
            queries.add("yoga energizante manhã despertar");
            queries.add("música motivacional energia positiva");
            queries.add("meditação energia vital chakra");
            queries.add("exercícios respiração energizantes pranayama");
        }
        
        // Análise de objetivos de saúde
        if ("wellness".equals(userState.getHealthGoal())) {
            queries.add("bem-estar holístico meditação saúde mental");
            queries.add("estilo vida saudável relaxamento equilíbrio");
        } else if ("lose".equals(userState.getHealthGoal())) {
            queries.add("meditação perda peso visualização");
            queries.add("relaxamento após exercício recuperação");
        }
        
        // Análise de qualidade do sono
        if ("poor".equals(userState.getSleepQuality()) || 
            (userState.getAverageSleepHours() != null && userState.getAverageSleepHours() < 6)) {
            queries.add("música dormir insônia sono profundo");
            queries.add("meditação guiada dormir rápido");
            queries.add("sons relaxantes dormir bebê 432hz");
        }
        
        // Baseado na categoria solicitada (se especificada)
        if (category != null) {
            switch (category) {
                case "nature":
                    queries.add("sons da natureza relaxamento 4K ultra HD");
                    queries.add("floresta tropical chuva meditação 10 horas");
                    queries.add("oceano ondas praia relaxar dormir");
                    queries.add("pássaros cantando manhã natureza");
                    break;
                case "meditation":
                    queries.add("meditação guiada " + language + " atenção plena");
                    queries.add("mindfulness meditação iniciantes");
                    queries.add("body scan relaxamento progressivo");
                    queries.add("meditação chakras equilíbrio energia");
                    break;
                case "music":
                    queries.add("música relaxante instrumental piano");
                    queries.add("música ambiente meditação spa");
                    queries.add("música clássica relaxar estudar");
                    queries.add("lofi relaxante jazz suave");
                    break;
                case "breathing":
                    queries.add("exercícios respiração guiada pranayama");
                    queries.add("respiração 4-7-8 técnica dormir");
                    queries.add("respiração profunda relaxamento stress");
                    queries.add("wim hof método respiração energia");
                    break;
            }
        }
        
        // Queries baseadas em gamificação (motivação)
        if (userState.getCurrentStreak() != null && userState.getCurrentStreak() > 7) {
            queries.add("meditação avançada mindfulness profundo");
            queries.add("yoga intermediário relaxamento força");
        } else {
            queries.add("meditação iniciantes guiada simples");
            queries.add("relaxamento básico começar agora");
        }
        
        // Queries padrão de alta qualidade se não houver nenhuma específica
        if (queries.isEmpty()) {
            queries.add("meditação relaxamento " + language + " guiada");
            queries.add("música calma instrumental sono");
            queries.add("natureza sons relaxantes 4K");
        }
        
        // Limitar a 5 queries para não exceder quota da API
        return queries.stream().distinct().limit(5).collect(Collectors.toList());
    }

    /**
     * Busca vídeos no YouTube usando a API v3
     * Implementa busca real com filtros avançados
     */
    private List<RecommendedVideo> searchYouTubeVideos(String query, VideoRecommendationRequest request) {
        try {
            // Verificar se API Key está configurada
            if (apiKey == null || apiKey.trim().isEmpty()) {
                log.warn("⚠️ YouTube API Key não configurada, retornando lista vazia");
                return new ArrayList<>();
            }
            
            YouTube.Search.List search = getYouTubeService().search().list(Collections.singletonList("id,snippet"));
            search.setKey(apiKey);
            search.setQ(query);
            search.setType(Collections.singletonList("video"));
            search.setMaxResults(Long.valueOf(request.getMaxResults() != null ? request.getMaxResults() : 5));
            search.setOrder("relevance");
            search.setVideoDuration(getDurationFilter(request.getPreferredDuration()));
            search.setRelevanceLanguage(request.getLanguage());
            search.setSafeSearch("moderate");
            search.setVideoDefinition("any"); // HD ou SD
            search.setVideoEmbeddable("true"); // Apenas vídeos que podem ser embedados
            
            SearchListResponse searchResponse = search.execute();
            List<SearchResult> searchResults = searchResponse.getItems();
            
            if (searchResults == null || searchResults.isEmpty()) {
                log.warn("Nenhum vídeo encontrado para query: {}", query);
                return new ArrayList<>();
            }
            
            // Obter IDs dos vídeos
            String videoIds = searchResults.stream()
                .map(sr -> sr.getId().getVideoId())
                .collect(Collectors.joining(","));
            
            // Buscar detalhes completos dos vídeos
            YouTube.Videos.List videosList = getYouTubeService().videos()
                .list(Collections.singletonList("snippet,contentDetails,statistics"));
            videosList.setKey(apiKey);
            videosList.setId(Collections.singletonList(videoIds));
            
            VideoListResponse videosResponse = videosList.execute();
            List<Video> videos = videosResponse.getItems();
            
            // Converter para RecommendedVideo
            return videos.stream()
                .map(this::convertToRecommendedVideo)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Erro ao buscar vídeos do YouTube para query: {}", query, e);
            return new ArrayList<>();
        }
    }

    /**
     * Converte Video do YouTube para RecommendedVideo
     */
    private RecommendedVideo convertToRecommendedVideo(Video video) {
        RecommendedVideo rv = new RecommendedVideo();
        rv.setVideoId(video.getId());
        rv.setTitle(video.getSnippet().getTitle());
        rv.setDescription(video.getSnippet().getDescription());
        rv.setThumbnailUrl(video.getSnippet().getThumbnails().getHigh().getUrl());
        rv.setContentUrl("https://www.youtube.com/watch?v=" + video.getId());
        rv.setChannelTitle(video.getSnippet().getChannelTitle());
        rv.setDurationSeconds(parseDuration(video.getContentDetails().getDuration()));
        rv.setTags(video.getSnippet().getTags() != null ? 
            video.getSnippet().getTags() : new ArrayList<>());
        rv.setMatchScore(75); // Score padrão, será ajustado no ranking
        rv.setReason("Selecionado baseado no seu perfil e necessidades atuais");
        
        return rv;
    }

    /**
     * Parse duração ISO 8601 para segundos
     */
    private Integer parseDuration(String isoDuration) {
        try {
            Duration duration = Duration.parse(isoDuration);
            return (int) duration.getSeconds();
        } catch (Exception e) {
            log.warn("Erro ao parsear duração: {}", isoDuration);
            return 0;
        }
    }

    /**
     * Retorna filtro de duração para YouTube API
     */
    private String getDurationFilter(String preferredDuration) {
        if (preferredDuration == null) return "any";
        
        switch (preferredDuration) {
            case "short": return "short"; // < 4 minutos
            case "medium": return "medium"; // 4-20 minutos
            case "long": return "long"; // > 20 minutos
            default: return "any";
        }
    }

    /**
     * Ranqueia vídeos baseado no perfil detalhado do usuário
     */
    private List<RecommendedVideo> rankVideos(List<RecommendedVideo> videos, UserState userState, VideoRecommendationRequest request) {
        videos.forEach(video -> {
            int score = 50; // Score base
            
            // Ajustar score baseado em duração preferida
            int durationMinutes = video.getDurationSeconds() / 60;
            if (request.getPreferredDuration() != null) {
                switch (request.getPreferredDuration()) {
                    case "short":
                        if (durationMinutes < 15) score += 20;
                        break;
                    case "medium":
                        if (durationMinutes >= 15 && durationMinutes <= 45) score += 20;
                        break;
                    case "long":
                        if (durationMinutes > 45) score += 20;
                        break;
                }
            }
            
            // Ajustar baseado em palavras-chave no título (análise semântica)
            String titleLower = video.getTitle().toLowerCase();
            if (userState.getStressLevel() != null && userState.getStressLevel() > 7) {
                if (titleLower.contains("relaxa") || titleLower.contains("calma")) score += 15;
                if (titleLower.contains("stress") || titleLower.contains("ansiedade")) score += 10;
                if (titleLower.contains("profundo") || titleLower.contains("deep")) score += 8;
            }
            
            if (userState.getEnergyLevel() != null && userState.getEnergyLevel() < 4) {
                if (titleLower.contains("energia") || titleLower.contains("motiv")) score += 15;
                if (titleLower.contains("despertar") || titleLower.contains("energi")) score += 10;
            }
            
            if (userState.getAnxietyLevel() != null && userState.getAnxietyLevel() > 6) {
                if (titleLower.contains("ansiedade") || titleLower.contains("anxiety")) score += 15;
                if (titleLower.contains("respira") || titleLower.contains("breath")) score += 10;
            }
            
            // Boost para vídeos de alta qualidade (HD, 4K)
            if (titleLower.contains("4k") || titleLower.contains("hd") || titleLower.contains("ultra")) {
                score += 5;
            }
            
            // Boost para vídeos guiados
            if (titleLower.contains("guiada") || titleLower.contains("guided")) {
                score += 8;
            }
            
            video.setMatchScore(Math.min(100, score));
            
            // Atualizar razão personalizada
            video.setReason(generateVideoReason(video, userState));
        });
        
        // Ordenar por score e remover duplicatas
        return videos.stream()
            .sorted(Comparator.comparingInt(RecommendedVideo::getMatchScore).reversed())
            .distinct()
            .limit(request.getMaxResults() != null ? request.getMaxResults() : 10)
            .collect(Collectors.toList());
    }

    /**
     * Gera razão personalizada e detalhada para recomendação do vídeo
     */
    private String generateVideoReason(RecommendedVideo video, UserState userState) {
        List<String> reasons = new ArrayList<>();
        
        if (userState.getStressLevel() != null && userState.getStressLevel() > 7) {
            reasons.add("ajuda a reduzir o stress elevado");
        }
        
        if (userState.getAnxietyLevel() != null && userState.getAnxietyLevel() > 6) {
            reasons.add("promove calma e tranquilidade para ansiedade");
        }
        
        if (userState.getEnergyLevel() != null && userState.getEnergyLevel() < 4) {
            reasons.add("ajuda a aumentar a energia e vitalidade");
        }
        
        int durationMinutes = video.getDurationSeconds() / 60;
        if (durationMinutes < 15) {
            reasons.add("duração perfeita para uma pausa rápida");
        } else if (durationMinutes > 30) {
            reasons.add("ideal para relaxamento profundo e imersivo");
        }
        
        if ("poor".equals(userState.getSleepQuality())) {
            reasons.add("pode melhorar a qualidade do sono");
        }
        
        if (reasons.isEmpty()) {
            return "Recomendado para seu bem-estar e equilíbrio";
        }
        
        return "Recomendado porque " + String.join(", ", reasons);
    }

    /**
     * Agrupa vídeos por categoria baseado em análise semântica
     */
    private Map<String, List<RecommendedVideo>> groupByCategory(List<RecommendedVideo> videos) {
        Map<String, List<RecommendedVideo>> grouped = new HashMap<>();
        
        for (RecommendedVideo video : videos) {
            String category = categorizeVideo(video);
            grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(video);
        }
        
        return grouped;
    }

    /**
     * Categoriza vídeo baseado em análise de título, descrição e tags
     */
    private String categorizeVideo(RecommendedVideo video) {
        String titleLower = video.getTitle().toLowerCase();
        String descLower = video.getDescription() != null ? video.getDescription().toLowerCase() : "";
        
        if (titleLower.contains("natureza") || titleLower.contains("nature") || 
            titleLower.contains("floresta") || titleLower.contains("oceano") ||
            titleLower.contains("chuva") || titleLower.contains("pássaro") ||
            descLower.contains("sons da natureza")) {
            return "nature";
        }
        
        if (titleLower.contains("meditação") || titleLower.contains("meditation") ||
            titleLower.contains("mindfulness") || titleLower.contains("guiada") ||
            descLower.contains("meditação")) {
            return "meditation";
        }
        
        if (titleLower.contains("respiração") || titleLower.contains("breathing") ||
            titleLower.contains("pranayama") || titleLower.contains("respira") ||
            descLower.contains("exercícios de respiração")) {
            return "breathing";
        }
        
        return "music";
    }

    /**
     * Gera insights profundos sobre o usuário
     */
    private String generateInsights(UserState userState) {
        StringBuilder insights = new StringBuilder();
        
        if (userState.getStressLevel() != null && userState.getStressLevel() > 7) {
            insights.append("Detectamos níveis elevados de stress (").append(userState.getStressLevel()).append("/10). ");
        }
        
        if (userState.getCurrentStreak() != null && userState.getCurrentStreak() > 7) {
            insights.append("Parabéns por manter sua rotina de bem-estar há ").append(userState.getCurrentStreak()).append(" dias! ");
        }
        
        if (userState.getEnergyLevel() != null && userState.getEnergyLevel() < 4) {
            insights.append("Sua energia está baixa (").append(userState.getEnergyLevel()).append("/10). Vídeos energizantes podem ajudar. ");
        }
        
        if ("poor".equals(userState.getSleepQuality())) {
            insights.append("Qualidade do sono pode melhorar com relaxamento antes de dormir. ");
        }
        
        if (insights.length() == 0) {
            insights.append("Continue sua jornada de bem-estar com conteúdo personalizado para você.");
        }
        
        return insights.toString();
    }

    /**
     * Gera sugestões personalizadas de bem-estar
     */
    private List<String> generateSuggestions(UserState userState) {
        List<String> suggestions = new ArrayList<>();
        
        if (userState.getWaterIntake() != null && userState.getWaterIntake() < 2000) {
            suggestions.add("💧 Lembre-se de se hidratar adequadamente (meta: 2L/dia)");
        }
        
        if (userState.getPhysicalActivityMinutes() != null && userState.getPhysicalActivityMinutes() < 30) {
            suggestions.add("🏃 Considere adicionar atividade física leve à sua rotina");
        }
        
        if (userState.getAverageSleepHours() != null && userState.getAverageSleepHours() < 7) {
            suggestions.add("😴 Priorize uma boa noite de sono (7-9 horas) para melhor recuperação");
        }
        
        if (userState.getStressLevel() != null && userState.getStressLevel() > 7) {
            suggestions.add("🧘 Reserve 10-15 minutos diários para meditação guiada");
        }
        
        suggestions.add("🙏 Pratique gratidão e reflexão pessoal diariamente");
        suggestions.add("🌱 Mantenha consistência em sua rotina de bem-estar");
        
        return suggestions;
    }
}
