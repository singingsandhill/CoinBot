package scoula.coin.application.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(catalog = "coin", name = "order_history")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderHistory {
    @Id
    private String uuid;

    @Column(nullable = false)
    private String market;

    @Column(nullable = false)
    private String side;

    @Column(name = "ord_type")
    private String ordType;

    private BigDecimal price;

    private BigDecimal volume;

    @Column(name="remaining_volume")
    private BigDecimal remainingVolume;

    @Column(name = "reserved_fee")
    private BigDecimal reservedFee;

    @Column(name = "remaining_fee")
    private BigDecimal remainingFee;

    @Column(name = "paid_fee")
    private BigDecimal paidFee;

    private BigDecimal locked;

    @Column(name = "executed_volume")
    private BigDecimal executedVolume;

    @Column(name = "trades_count")
    private Integer tradesCount;

    private String state;

    @Column(name="created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
