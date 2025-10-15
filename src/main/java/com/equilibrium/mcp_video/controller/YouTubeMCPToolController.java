package com.equilibrium.mcp_video.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import com.equilibrium.mcp_video.dto.VideoRecommendationRequest;
import com.equilibrium.mcp_video.dto.VideoRecommendationResponse;
import com.equilibrium.mcp_video.dto.UserState;
import com.equilibrium.mcp_video.service.YouTubeRecommendationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCP Tool Controller para recomendações de vídeos do YouTube
 * 
 * Este controller implementa o padrão MCP (Model Context Protocol) Tool
 * permitindo que outros serviços invoquem as recomendações de vídeo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YouTubeMCPToolController {

    private final YouTubeRecommendationService youtubeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Tool: recommend_youtube_videos
     * 
     * Recomenda vídeos do YouTube baseado no perfil completo do usuário
     * 
     * @param prompt String contendo dados completos do usuário incluindo perfil, nutrição, humor em formato texto natural
     * @return Recomendações personalizadas de vídeos do YouTube em formato JSON string
     */
    @Tool(description = """
            Recomenda vídeos do YouTube baseado no perfil completo do usuário.
            
            O prompt deve conter informações detalhadas do usuário como:
            - Perfil físico: idade, peso, altura, nível de atividade, objetivo de saúde
            - Estado emocional: humor atual, nível de stress (1-10), ansiedade (1-10), energia (1-10)
            - Dados de gamificação: nível atual, streak, XP total
            - Nutrição: calorias médias, macronutrientes, hidratação, refeições por dia
            - Atividade física: minutos de exercício
            - Sono: horas de sono, qualidade
            - Preferências: categoria (nature/meditation/music/breathing), duração (short/medium/long), idioma
            
            Retorna vídeos do YouTube categorizados com thumbnails, descrições, razões de recomendação e match scores.
            """)
    public String recommendYouTubeVideos(String prompt) {
        
        log.info("🎬 [MCP Tool] recommend_youtube_videos invocado com prompt");
        log.debug("📝 Prompt recebido: {}", prompt);
        
        // Defensive null/empty check: return structured JSON error if prompt is missing
        if (prompt == null || prompt.isBlank()) {
            log.warn("⚠️ Prompt recebido nulo ou vazio para recommend_youtube_videos");
            try {
                Map<String, Object> errorResponse = Map.of(
                    "error", true,
                    "message", "Prompt ausente ou inválido",
                    "recommendations", List.of(),
                    "insights", "Prompt inválido - verifique a requisição",
                    "suggestions", List.of("Forneça um prompt textual com o perfil do usuário"),
                    "processingTimeMs", 0L
                );
                return objectMapper.writeValueAsString(errorResponse);
            } catch (Exception ex) {
                return "{\"error\":true,\"message\":\"Prompt ausente ou inválido\"}";
            }
        }
        
        try {
            // 1. Extrair dados do usuário do prompt rico em detalhes
            UserState userState = extractUserStateFromPrompt(prompt);
            log.info("📊 UserState extraído - Stress: {}, Energia: {}, Humor: {}", 
                userState.getStressLevel(),
                userState.getEnergyLevel(),
                userState.getCurrentMood());
            
            // 2. Extrair preferências adicionais do prompt
            String category = extractCategory(prompt);
            String preferredDuration = extractPreferredDuration(prompt);
            String language = extractLanguage(prompt);
            Integer maxResults = extractMaxResults(prompt);
            
            // 3. Criar request estruturado
            VideoRecommendationRequest request = new VideoRecommendationRequest();
            request.setUserState(userState);
            request.setCategory(category);
            request.setPreferredDuration(preferredDuration);
            request.setLanguage(language);
            request.setMaxResults(maxResults);
            
            log.info("🎯 Request criado - Categoria: {}, Duração: {}, Idioma: {}, Max: {}", 
                category, preferredDuration, language, maxResults);
            
            // 4. Obter recomendações do serviço
            VideoRecommendationResponse response = youtubeService.recommendVideos(request);
            
            // 5. Converter resposta para JSON string
            String jsonResponse = objectMapper.writeValueAsString(response);
            
            log.info("✅ [MCP Tool] {} vídeos recomendados em {}ms",
                response.getRecommendations().stream()
                    .mapToInt(cat -> cat.getVideos().size())
                    .sum(),
                response.getProcessingTimeMs());
            
            return jsonResponse;
            
        } catch (Exception e) {
            log.error("❌ [MCP Tool] Erro ao processar recomendações", e);
            
            // Retornar erro em formato JSON
            try {
                Map<String, Object> errorResponse = Map.of(
                    "error", true,
                    "message", "Erro ao gerar recomendações: " + e.getMessage(),
                    "recommendations", List.of(),
                    "insights", "Não foi possível processar sua solicitação no momento.",
                    "suggestions", List.of("Tente novamente em alguns instantes"),
                    "processingTimeMs", 0L
                );
                return objectMapper.writeValueAsString(errorResponse);
            } catch (Exception jsonError) {
                return "{\"error\": true, \"message\": \"Erro ao processar recomendações\"}";
            }
        }
    }
    
    /**
     * Extrai UserState do prompt rico em detalhes
     * Analisa o texto do prompt procurando por padrões e palavras-chave
     */
    private UserState extractUserStateFromPrompt(String prompt) {
        UserState state = new UserState();
        
        // Physical profile
        state.setAge(extractInteger(prompt, "idade|age", 30));
        state.setWeight(extractDouble(prompt, "peso|weight", 70.0));
        state.setHeight(extractDouble(prompt, "altura|height", 170.0));
        state.setGender(extractGender(prompt));
        state.setActivityLevel(extractActivityLevel(prompt));
        state.setHealthGoal(extractHealthGoal(prompt));
        
        // Extrair estado emocional
        state.setCurrentMood(extractMood(prompt));
        state.setMoodTrend(extractMoodTrend(prompt));
        state.setStressLevel(extractInteger(prompt, "stress|estresse", 5));
        state.setAnxietyLevel(extractInteger(prompt, "ansiedade|anxiety|ansiedad", 5));
        state.setEnergyLevel(extractInteger(prompt, "energia|energy", 5));
        
        // Extrair gamificação
        state.setCurrentLevel(extractInteger(prompt, "nivel|level|nível", 1));
        state.setCurrentStreak(extractInteger(prompt, "streak|sequencia|sequência", 0));
        state.setTotalXP(extractLong(prompt, "xp|experiencia|experiência", 0L));
        
        // Extrair nutrição
        state.setAverageCalories(extractInteger(prompt, "calorias|calories", null));
        state.setWaterIntake(extractInteger(prompt, "agua|water|água|hidratação", null));
        state.setMealsPerDay(extractInteger(prompt, "refeições|meals|refei", 3));
        
        // Extrair atividade física
        state.setPhysicalActivityMinutes(extractInteger(prompt, "atividade física|exercise|exercicio|exercício", 0));
        
        // Extrair sono
        state.setAverageSleepHours(extractDouble(prompt, "sono|sleep|dormir", 7.0));
        state.setSleepQuality(extractSleepQuality(prompt));
        
        log.debug("📋 UserState extraído: {}", state);
        return state;
    }
    
    /**
     * Extrai categoria do prompt (nature, meditation, music, breathing)
     */
    private String extractCategory(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        
        if (lowerPrompt.contains("natureza") || lowerPrompt.contains("nature") || 
            lowerPrompt.contains("floresta") || lowerPrompt.contains("oceano")) {
            return "nature";
        }
        if (lowerPrompt.contains("meditação") || lowerPrompt.contains("meditation") ||
            lowerPrompt.contains("mindfulness")) {
            return "meditation";
        }
        if (lowerPrompt.contains("música") || lowerPrompt.contains("music") ||
            lowerPrompt.contains("musica")) {
            return "music";
        }
        if (lowerPrompt.contains("respiração") || lowerPrompt.contains("breathing") ||
            lowerPrompt.contains("pranayama")) {
            return "breathing";
        }
        
        return null; // Deixa o sistema escolher
    }
    
    /**
     * Extrai duração preferida (short, medium, long)
     */
    private String extractPreferredDuration(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        
        if (lowerPrompt.contains("curto") || lowerPrompt.contains("short") ||
            lowerPrompt.contains("rápido") || lowerPrompt.contains("quick")) {
            return "short";
        }
        if (lowerPrompt.contains("longo") || lowerPrompt.contains("long") ||
            lowerPrompt.contains("extenso") || lowerPrompt.contains("profundo")) {
            return "long";
        }
        if (lowerPrompt.contains("médio") || lowerPrompt.contains("medium") ||
            lowerPrompt.contains("medio")) {
            return "medium";
        }
        
        return "medium"; // Default
    }
    
    /**
     * Extrai idioma (pt, en, es)
     */
    private String extractLanguage(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        
        if (lowerPrompt.contains("português") || lowerPrompt.contains("portugues") ||
            lowerPrompt.contains("pt-br") || lowerPrompt.contains("brasil")) {
            return "pt";
        }
        if (lowerPrompt.contains("english") || lowerPrompt.contains("inglês") ||
            lowerPrompt.contains("ingles")) {
            return "en";
        }
        if (lowerPrompt.contains("español") || lowerPrompt.contains("espanhol") ||
            lowerPrompt.contains("spanish")) {
            return "es";
        }
        
        return "pt"; // Default
    }
    
    /**
     * Extrai número máximo de resultados
     */
    private Integer extractMaxResults(String prompt) {
        Integer maxResults = extractInteger(prompt, "máximo|maximo|max|limite", null);
        if (maxResults == null || maxResults < 1 || maxResults > 50) {
            return 10; // Default
        }
        return maxResults;
    }
    
    // Métodos auxiliares de extração
    
    private Integer extractInteger(String prompt, String pattern, Integer defaultValue) {
        try {
            Pattern p = Pattern.compile("(" + pattern + ")[:\\s]*(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(prompt);
            if (m.find()) {
                return Integer.parseInt(m.group(2));
            }
        } catch (Exception e) {
            log.debug("Não foi possível extrair integer para pattern: {}", pattern);
        }
        return defaultValue;
    }
    
    private Long extractLong(String prompt, String pattern, Long defaultValue) {
        try {
            Pattern p = Pattern.compile("(" + pattern + ")[:\\s]*(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(prompt);
            if (m.find()) {
                return Long.parseLong(m.group(2));
            }
        } catch (Exception e) {
            log.debug("Não foi possível extrair long para pattern: {}", pattern);
        }
        return defaultValue;
    }
    
    private Double extractDouble(String prompt, String pattern, Double defaultValue) {
        try {
            Pattern p = Pattern.compile("(" + pattern + ")[:\\s]*(\\d+\\.?\\d*)", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(prompt);
            if (m.find()) {
                return Double.parseDouble(m.group(2));
            }
        } catch (Exception e) {
            log.debug("Não foi possível extrair double para pattern: {}", pattern);
        }
        return defaultValue;
    }
    
    private String extractGender(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        if (lowerPrompt.contains("masculino") || lowerPrompt.contains("male") || lowerPrompt.contains("homem")) {
            return "male";
        }
        if (lowerPrompt.contains("feminino") || lowerPrompt.contains("female") || lowerPrompt.contains("mulher")) {
            return "female";
        }
        return "other";
    }
    
    private String extractActivityLevel(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        if (lowerPrompt.contains("sedentário") || lowerPrompt.contains("sedentary")) {
            return "sedentary";
        }
        if (lowerPrompt.contains("levemente ativo") || lowerPrompt.contains("light")) {
            return "light";
        }
        if (lowerPrompt.contains("muito ativo") || lowerPrompt.contains("very active") || lowerPrompt.contains("very_active")) {
            return "very_active";
        }
        if (lowerPrompt.contains("ativo") || lowerPrompt.contains("active")) {
            return "active";
        }
        if (lowerPrompt.contains("moderado") || lowerPrompt.contains("moderate")) {
            return "moderate";
        }
        return "moderate";
    }
    
    private String extractHealthGoal(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        if (lowerPrompt.contains("perder peso") || lowerPrompt.contains("lose weight") || lowerPrompt.contains("emagrecer")) {
            return "lose";
        }
        if (lowerPrompt.contains("ganhar peso") || lowerPrompt.contains("gain weight") || lowerPrompt.contains("ganhar massa")) {
            return "gain";
        }
        if (lowerPrompt.contains("bem-estar") || lowerPrompt.contains("wellness") || lowerPrompt.contains("saúde")) {
            return "wellness";
        }
        if (lowerPrompt.contains("manter") || lowerPrompt.contains("maintain")) {
            return "maintain";
        }
        return "wellness";
    }
    
    private String extractMood(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        if (lowerPrompt.contains("ótimo") || lowerPrompt.contains("excelente") || lowerPrompt.contains("great")) {
            return "great";
        }
        if (lowerPrompt.contains("bom") || lowerPrompt.contains("good") || lowerPrompt.contains("bem")) {
            return "good";
        }
        if (lowerPrompt.contains("péssimo") || lowerPrompt.contains("terrível") || lowerPrompt.contains("terrible")) {
            return "terrible";
        }
        if (lowerPrompt.contains("ruim") || lowerPrompt.contains("bad") || lowerPrompt.contains("mal")) {
            return "bad";
        }
        return "ok";
    }
    
    private String extractMoodTrend(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        if (lowerPrompt.contains("melhorando") || lowerPrompt.contains("improving") || lowerPrompt.contains("melhor")) {
            return "improving";
        }
        if (lowerPrompt.contains("piorando") || lowerPrompt.contains("declining") || lowerPrompt.contains("pior")) {
            return "declining";
        }
        return "stable";
    }
    
    private String extractSleepQuality(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        if (lowerPrompt.contains("excelente") || lowerPrompt.contains("excellent")) {
            return "excellent";
        }
        if (lowerPrompt.contains("bom") || lowerPrompt.contains("good")) {
            return "good";
        }
        if (lowerPrompt.contains("ruim") || lowerPrompt.contains("poor")) {
            return "poor";
        }
        if (lowerPrompt.contains("razoável") || lowerPrompt.contains("fair")) {
            return "fair";
        }
        return "good";
    }

   

}
