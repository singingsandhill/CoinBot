package scoula.coin.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import scoula.coin.application.entity.OrderHistory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderHistoryDTO {
    private String uuid;
    private String market;
    private String side;
    private String ordType;
    private BigDecimal price;
    private BigDecimal volume;
    private BigDecimal remainingVolume;
    private BigDecimal reservedFee;
    private BigDecimal remainingFee;
    private BigDecimal paidFee;
    private BigDecimal locked;
    private BigDecimal executedVolume;
    private Integer tradesCount;
    private String state;
    private LocalDateTime createdAt;

    public static OrderHistoryDTO fromEntity(OrderHistory entity) {
        return OrderHistoryDTO.builder()
                .uuid(entity.getUuid())
                .market(entity.getMarket())
                .side(entity.getSide())
                .ordType(entity.getOrdType())
                .price(entity.getPrice())
                .volume(entity.getVolume())
                .remainingVolume(entity.getRemainingVolume())
                .reservedFee(entity.getReservedFee())
                .remainingFee(entity.getRemainingFee())
                .paidFee(entity.getPaidFee())
                .locked(entity.getLocked())
                .executedVolume(entity.getExecutedVolume())
                .tradesCount(entity.getTradesCount())
                .state(entity.getState())
                .createdAt(entity.getCreatedAt())
                .build();
    }
    public OrderHistory toEntity() {
        return OrderHistory.builder()
                .uuid(this.uuid)
                .market(this.market)
                .side(this.side)
                .ordType(this.ordType)
                .price(this.price)
                .volume(this.volume)
                .remainingVolume(this.remainingVolume)
                .reservedFee(this.reservedFee)
                .remainingFee(this.remainingFee)
                .paidFee(this.paidFee)
                .locked(this.locked)
                .executedVolume(this.executedVolume)
                .tradesCount(this.tradesCount)
                .state(this.state)
                .createdAt(this.createdAt)
                .build();
    }
}
