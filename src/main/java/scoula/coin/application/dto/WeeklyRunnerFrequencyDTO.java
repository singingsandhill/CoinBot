package scoula.coin.application.dto;

public record WeeklyRunnerFrequencyDTO(
    String name,
    long runCount,
    int weekNumber,
    int year
) {}