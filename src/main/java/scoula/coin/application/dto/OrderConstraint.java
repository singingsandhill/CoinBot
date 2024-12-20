package scoula.coin.application.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderConstraint {
    private String currency;
    private BigDecimal price_unit;
    private BigDecimal min_total;
}
