package scoula.coin.application.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(catalog = "coin",name = "trading_signal_history")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradingSignalHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String market;         // 마켓 ID (예: KRW-BTC)

    private BigDecimal price;      // 신호 발생 시점의 가격
    private Double rsi;            // RSI 값
    private Integer signalType;    // 1: 매수, -1: 매도, 0: 중립

    private Boolean orderExecuted; // 주문 실행 여부
    private String orderUuid;      // 실제 주문이 발생한 경우 주문 UUID

    @Column(length = 1000)
    private String failureReason;  // 실패한 경우 실패 사유

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
