package com.equilibrium.mcp_video.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Estado completo do usuário para recomendações personalizadas
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserState {
    
    // Dados de perfil físico
    private Integer age;
    private String gender; // male, female, other
    private Double weight; // em kg
    private Double height; // em cm
    private String activityLevel; // sedentary, light, moderate, active, very_active
    private String healthGoal; // maintain, lose, gain, wellness
    
    // Dados de humor
    private String currentMood; // great, good, ok, bad, terrible
    private String moodTrend; // improving, stable, declining
    private Integer stressLevel; // 1-10
    private Integer anxietyLevel; // 1-10
    private Integer energyLevel; // 1-10
    
    // Dados de gamificação
    private Integer currentLevel;
    private Integer currentStreak;
    private Long totalXP;
    
    // Dados de nutrição
    private Integer averageCalories;
    private Map<String, Double> macronutrients; // proteins, carbs, fats
    private Integer waterIntake; // em ml
    private Integer mealsPerDay;
    
    // Dados de atividade física
    private Integer physicalActivityMinutes; // minutos por dia
    
    // Dados de sono (se disponível)
    private Double averageSleepHours;
    private String sleepQuality; // poor, fair, good, excellent
}
