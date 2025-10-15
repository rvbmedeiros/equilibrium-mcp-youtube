# Backend Equilibrium - Análise de Alimentos com IA

Backend Spring Boot para análise nutricional de alimentos usando OpenAI GPT-4 Vision API.

## 📋 Requisitos

- Java 17 ou superior
- Maven 3.6+
- Chave de API OpenAI configurada

## 🚀 Como Executar

### Método 1: Maven Wrapper (Recomendado)

```bash
cd backend
./mvnw spring-boot:run
```

### Método 2: Maven Instalado

```bash
cd backend
mvn spring-boot:run
```

O servidor iniciará em: `http://localhost:8080`

## 🔑 Configuração

A chave da API OpenAI está configurada em `src/main/resources/application.yml`:

```yaml
openai:
  api-key: sk-proj-hBmBeYiovZ5zAIoPkFLOTLC_8dXhvCSvq6MvRzVbw-...
  model: gpt-4-vision-preview
  max-tokens: 1000
```

## 📡 Endpoints

### Análise Nutricional

#### POST /api/nutrition/analyze

Analisa uma imagem de alimento e retorna informações nutricionais.

**Request:**
```json
{
  "imageBase64": "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
  "audioTranscription": "Opcional: transcrição de áudio para contexto"
}
```

**Response:**
```json
{
  "foods": [
    {
      "name": "Arroz branco",
      "quantity": "1 xícara (150g)",
      "confidence": 0.95,
      "nutritionalInfo": {
        "calories": 205,
        "protein": 4.2,
        "carbs": 45.0,
        "fat": 0.4,
        "fiber": 0.6
      }
    }
  ],
  "totalCalories": 205,
  "totalProtein": 4.2,
  "totalCarbs": 45.0,
  "totalFat": 0.4,
  "analysisNotes": "Prato brasileiro típico...",
  "processingTimeMs": 2500
}
```

### GET /api/nutrition/health

Health check do serviço.

**Response:**
```json
{
  "status": "UP",
  "timestamp": "2025-01-29T12:00:00"
}
```

---

### Lista de Mercado

#### POST /api/grocery/analyze

Analisa uma imagem de receita ou lista de ingredientes e retorna lista de compras estruturada.

**Request:**
```json
{
  "imageBase64": "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
  "userContext": "Opcional: contexto adicional (ex: 'Lista para o almoço')",
  "audioTranscription": "Opcional: transcrição de áudio"
}
```

**Response:**
```json
{
  "items": [
    {
      "name": "Farinha de trigo",
      "quantity": "500",
      "unit": "g",
      "category": "Mercearia",
      "confidence": 0.95,
      "checked": false,
      "notes": "Tipo 1 ou integral"
    }
  ],
  "listTitle": "Ingredientes para Bolo de Chocolate",
  "listType": "receita",
  "totalItems": 10,
  "analysisNotes": "Receita identificada: Bolo de Chocolate. Rendimento: 12 porções.",
  "suggestions": "Considere comprar cacau em pó 100% para um sabor mais intenso.",
  "processingTimeMs": 2500,
  "recipe": {
    "name": "Bolo de Chocolate",
    "description": "Bolo fofinho e úmido",
    "servings": 12,
    "prepTime": "50 minutos",
    "instructions": ["Passo 1...", "Passo 2..."]
  }
}
```

#### GET /api/grocery/health

Health check do serviço de lista de mercado.

**Response:**
```json
{
  "status": "UP",
  "service": "Grocery List Analysis",
  "timestamp": "2025-10-05T14:30:00"
}
```

## 💰 Custos OpenAI

- **GPT-4 Vision**: ~$0.01 - $0.03 por análise
- Depende do tamanho da imagem e comprimento da resposta
- Max tokens configurado: 1000

## 🛠️ Tecnologias

- Spring Boot 3.2.0
- Spring WebFlux (para chamadas HTTP assíncronas)
- Lombok (para reduzir boilerplate)
- Jackson (para JSON parsing)

## 📝 Estrutura do Projeto

```
backend/
├── src/main/java/com/equilibrium/
│   ├── EquilibriumApplication.java    # Classe principal
│   ├── controller/
│   │   └── NutritionController.java   # REST endpoints
│   ├── service/
│   │   └── OpenAIService.java         # Integração OpenAI
│   ├── dto/
│   │   ├── FoodAnalysisRequest.java
│   │   ├── FoodAnalysisResponse.java
│   │   ├── FoodItem.java
│   │   └── NutritionalInfo.java
│   └── config/
│       └── CorsConfig.java            # CORS configuration
└── src/main/resources/
    └── application.yml                # Configuração
```

## 🧪 Testando

1. Inicie o backend:
```bash
cd backend
mvn spring-boot:run
```

2. Teste o health check:
```bash
curl http://localhost:8080/api/nutrition/health
```

3. Teste a análise (com imagem base64):
```bash
curl -X POST http://localhost:8080/api/nutrition/analyze \
  -H "Content-Type: application/json" \
  -d '{"imageBase64": "data:image/jpeg;base64,..."}'
```

## 🐛 Troubleshooting

### Erro de compilação do Maven

```bash
cd backend
mvn clean install
```

### Porta 8080 já em uso

Altere a porta em `application.yml`:
```yaml
server:
  port: 8081
```

E atualize `frontend/.env`:
```
VITE_API_URL=http://localhost:8081
```

### Erro de API Key OpenAI

Verifique se a chave está correta em `application.yml` e tem saldo suficiente na conta OpenAI.

## 📚 Recursos

- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [OpenAI API Docs](https://platform.openai.com/docs)
- [GPT-4 Vision Guide](https://platform.openai.com/docs/guides/vision)
