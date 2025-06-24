package scoula.coin.application.dto;

public record WeeklyRunnerDistanceDTO(
    String name,
    double totalDistance,
    int weekNumber,
    int year
) {}