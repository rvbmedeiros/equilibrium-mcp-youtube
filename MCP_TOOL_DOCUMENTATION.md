# 🎬 YouTube MCP Tool - Spring AI Implementation

## 📋 Visão Geral

Este é um **MCP Tool** (Model Context Protocol) para recomendações personalizadas de vídeos do YouTube, implementado como **@Tool** da Spring AI. Não expõe RestController - é uma tool pura para ser invocada por LLMs e agentes de IA.

## 🎯 Características

✅ **@Tool Spring AI**: Integração nativa com Spring AI MCP Server  
✅ **YouTube Data API v3**: Busca real de vídeos do YouTube  
✅ **Prompt Rico**: Extrai dados detalhados do usuário do prompt  
✅ **Análise Contextual**: Considera perfil, humor, nutrição, sono, gamificação  
✅ **Sem RestController**: Tool pura, não expõe endpoints REST  

## 🏗️ Arquitetura

```
┌─────────────────────────────────────────────────────────────┐
│                    LLM / AI Agent                            │
│              (Claude, GPT-4, etc.)                           │
└────────────────────┬────────────────────────────────────────┘
                     │
                     │ Invoca @Tool
                     │ com prompt rico
                     │
┌────────────────────▼────────────────────────────────────────┐
│          YouTubeMCPToolController (@Service)                 │
│                                                              │
│  @Tool recommendYouTubeVideos(String prompt)                │
│  • Extrai UserState do prompt                               │
│  • Analisa humor, stress, energia                           │
│  • Extrai preferências (categoria, duração)                 │
│  • Chama YouTubeRecommendationService                       │
│  • Retorna JSON string                                      │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│       YouTubeRecommendationService (@Service)                │
│                                                              │
│  • Cria queries personalizadas (prompts ricos)              │
│  • Busca vídeos reais no YouTube API v3                     │
│  • Ranqueia por relevância ao perfil                        │
│  • Categoriza vídeos (nature/meditation/music/breathing)    │
│  • Gera insights e sugestões                                │
└────────────────────┬────────────────────────────────────────┘
                     │
                     │ HTTP GET
                     │
┌────────────────────▼────────────────────────────────────────┐
│              Google YouTube Data API v3                      │
└──────────────────────────────────────────────────────────────┘
```

## 🚀 Como Funciona

### 1. LLM/Agent Chama a Tool

O LLM ou agente de IA chama a tool com um prompt rico em detalhes:

```
"Usuário com idade: 30, peso: 75kg, altura: 175cm, gênero: masculino, 
stress: 8, ansiedade: 7, energia: 3, humor: bad, nível: 5, streak: 10,
calorias: 2000, sono: 6 horas, qualidade do sono: poor.
Precisa de vídeos de meditação para reduzir stress."
```

### 2. Tool Extrai Dados do Prompt

O método `extractUserStateFromPrompt` usa regex e análise de texto para extrair:

- **Perfil Físico**: idade, peso, altura, gênero, nível de atividade
- **Estado Emocional**: humor, stress (1-10), ansiedade (1-10), energia (1-10)
- **Gamificação**: nível, streak, XP
- **Nutrição**: calorias, hidratação, refeições
- **Sono**: horas de sono, qualidade
- **Preferências**: categoria, duração, idioma

### 3. Serviço Cria Prompts para YouTube

O `YouTubeRecommendationService` analisa o UserState e cria **queries ricas** para YouTube:

**Exemplo para stress alto (8/10):**
```java
"meditação guiada stress ansiedade reduzir português"
"música relaxante dormir profundo ondas cerebrais"
"sons da natureza chuva floresta relaxamento 4K"
"yoga nidra relaxamento profundo guiado"
```

**Exemplo para energia baixa (3/10):**
```java
"yoga energizante manhã despertar"
"música motivacional energia positiva"
"meditação energia vital chakra"
```

### 4. Busca no YouTube API v3

```java
YouTube.Search.List search = youtube.search().list("id,snippet");
search.setQ(query); // Query rica
search.setMaxResults(5);
search.setVideoDuration("medium");
search.setRelevanceLanguage("pt");
search.setVideoEmbeddable("true");
```

### 5. Ranking Inteligente

Cada vídeo recebe score baseado em:
- ✅ Match com duração preferida (+20 pontos)
- ✅ Palavras-chave relevantes (+15 pontos)
- ✅ Alta qualidade (4K/HD) (+5 pontos)
- ✅ Vídeo guiado (+8 pontos)

### 6. Retorna JSON String

```json
{
  "recommendations": [
    {
      "category": "meditation",
      "videos": [
        {
          "videoId": "abc123",
          "title": "Meditação Guiada para Reduzir Stress",
          "thumbnailUrl": "https://...",
          "contentUrl": "https://youtube.com/watch?v=abc123",
          "durationSeconds": 600,
          "matchScore": 95,
          "reason": "Recomendado porque ajuda a reduzir o stress elevado, promove calma..."
        }
      ]
    }
  ],
  "insights": "Detectamos níveis elevados de stress (8/10)...",
  "suggestions": [
    "💧 Lembre-se de se hidratar adequadamente",
    "🧘 Reserve 10-15 minutos diários para meditação guiada"
  ],
  "processingTimeMs": 1234
}
```

## 📝 Exemplos de Prompts Ricos

### Exemplo 1: Usuário Estressado

```
Usuário masculino, 35 anos, 80kg, 180cm, moderadamente ativo.
Humor atual: ruim, stress: 9, ansiedade: 8, energia: 4.
Objetivo: wellness. Nível 8, streak 15 dias, 3500 XP.
Nutrição: 2200 calorias/dia, 3 refeições, 1.5L água.
Sono: 5 horas, qualidade poor.
Precisa relaxar urgentemente, vídeos de meditação curtos.
```

**Resultado**: Vídeos de meditação guiada focados em redução de stress, curta duração.

### Exemplo 2: Usuária com Energia Baixa

```
Usuária feminina, 28 anos, 65kg, 165cm, levemente ativa.
Humor: ok, stress: 5, ansiedade: 4, energia: 2.
Objetivo: maintain. Nível 3, streak 5 dias.
Sono: 7 horas, qualidade good.
Quer vídeos energizantes para começar o dia, yoga ou música.
```

**Resultado**: Vídeos de yoga energizante, música motivacional, meditação energia vital.

### Exemplo 3: Usuário Buscando Sono

```
Usuário, 42 anos, stress: 6, energia: 5.
Sono: 4 horas, qualidade terrible.
Precisa vídeos longos para dormir profundamente.
Sons da natureza ou música relaxante.
```

**Resultado**: Vídeos longos (>30min) de sons da natureza, música para dormir, meditação guiada sono.

## 🔧 Configuração

### application.yml

```yaml
youtube:
  api:
    key: ${YOUTUBE_API_KEY}

server:
  port: 8084

spring:
  application:
    name: equilibrium-mcp-youtube
  ai:
    mcp:
      server:
        enabled: true
```

### Variável de Ambiente

```bash
export YOUTUBE_API_KEY=your_youtube_api_key_here
```

## 🎨 Análise Contextual Detalhada

### Stress Alto (>7)
- **Queries**: meditação stress, música dormir, sons natureza
- **Boost**: palavras "relaxa", "calma", "profundo" (+15 pts)
- **Sugestões**: "Reserve 10-15min para meditação"

### Ansiedade Alta (>6)
- **Queries**: respiração ansiedade, mindfulness presente
- **Boost**: "ansiedade", "respira" (+15 pts)
- **Sugestões**: Técnicas de grounding

### Energia Baixa (<4)
- **Queries**: yoga energizante, música motivacional
- **Boost**: "energia", "motivação" (+15 pts)
- **Sugestões**: Atividade física leve

### Sono Ruim (<6h ou "poor")
- **Queries**: música dormir, meditação sono
- **Boost**: vídeos longos (+20 pts)
- **Sugestões**: "Priorize 7-9h de sono"

## 📊 Dados Extraídos do Prompt

| Campo | Padrão Regex | Exemplo | Default |
|-------|--------------|---------|---------|
| idade | `idade\|age:\s*(\d+)` | "idade: 30" | 30 |
| peso | `peso\|weight:\s*(\d+\.?\d*)` | "peso: 75.5" | 70.0 |
| stress | `stress\|estresse:\s*(\d+)` | "stress: 8" | 5 |
| energia | `energia\|energy:\s*(\d+)` | "energia: 3" | 5 |
| humor | palavras-chave | "humor: ruim" | "ok" |
| categoria | palavras-chave | "meditação" | null |
| duração | palavras-chave | "vídeos curtos" | "medium" |

## 🔍 Extração Inteligente

```java
// Extrai idade do prompt
extractInteger(prompt, "idade|age", 30)

// Resultado para "Usuário com idade: 35 anos"
→ 35

// Extrai humor
extractMood(prompt)

// Resultado para "humor: péssimo"
→ "terrible"

// Extrai categoria
extractCategory(prompt)

// Resultado para "precisa de meditação"
→ "meditation"
```

## 🚀 Como Usar na Spring AI

### 1. Configure o MCP Server

```java
@Configuration
public class MCPConfig {
    
    @Bean
    public MCPServer mcpServer(YouTubeMCPToolController toolController) {
        return MCPServer.builder()
            .tool(toolController)
            .build();
    }
}
```

### 2. LLM Invoca a Tool

```java
String prompt = """
    Usuário com stress: 9, energia: 2, humor: bad.
    Precisa de vídeos relaxantes para meditar.
    """;

String result = toolController.recommendYouTubeVideos(prompt);
// Retorna JSON com vídeos recomendados
```

### 3. Frontend Consome

```typescript
const response = await mcpClient.invokeTool(
  'recommend_youtube_videos',
  promptRicoComDadosDoUsuario
);

const videos = JSON.parse(response);
// Exibe vídeos na UI
```

## 📈 Métricas e Logging

```
🎬 [MCP Tool] recommend_youtube_videos invocado com prompt
📊 UserState extraído - Stress: 8, Energia: 3, Humor: bad
🎯 Request criado - Categoria: meditation, Duração: medium, Max: 10
📝 Queries de busca geradas: [
  "meditação guiada stress ansiedade...",
  "música relaxante dormir profundo...",
  ...
]
✅ [MCP Tool] 12 vídeos recomendados em 1234ms
```

## 🎯 Vantagens do Approach

1. **✅ Prompt Rico**: Extrai máximo de informação do texto natural
2. **✅ Análise Contextual**: Considera TODOS os dados do usuário
3. **✅ Queries Personalizadas**: Cria buscas específicas para cada perfil
4. **✅ Ranking Inteligente**: Score baseado em múltiplos fatores
5. **✅ Insights Personalizados**: Feedback específico para o usuário
6. **✅ Pure Tool**: Sem RestController, integração direta com LLMs

## 🔒 Segurança

- ✅ API Key não exposta no código
- ✅ Apenas vídeos embedáveis
- ✅ Safe search ativado
- ✅ Dados do usuário não salvos
- ✅ Sem PII (informações pessoais identificáveis)

## 🎉 Conclusão

Este MCP Tool implementa recomendações **contextuais e personalizadas** de vídeos do YouTube, usando:

- **Prompts ricos** com dados completos do usuário
- **YouTube Data API v3** para busca real
- **Spring AI @Tool** para integração com LLMs
- **Análise profunda** do perfil, humor, nutrição, sono

O resultado são **vídeos relevantes** que realmente ajudam o usuário no momento certo! 🎬✨
