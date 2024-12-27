package scoula.coin.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import scoula.coin.application.entity.TradingSignalHistory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingSignalHistoryDTO {
    private Long id;
    private String market;
    private BigDecimal price;
    private Double rsi;
    private Integer signalType;
    private Boolean orderExecuted;
    private String orderUuid;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TradingSignalHistoryDTO fromEntity(TradingSignalHistory entity) {
        return TradingSignalHistoryDTO.builder()
                .id(entity.getId())
                .market(entity.getMarket())
                .price(entity.getPrice())
                .rsi(entity.getRsi())
                .signalType(entity.getSignalType())
                .orderExecuted(entity.getOrderExecuted())
                .orderUuid(entity.getOrderUuid())
                .failureReason(entity.getFailureReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
