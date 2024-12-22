package scoula.coin.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class OrderConstraint {
    private String currency;              // 화폐를 의미하는 영문 대문자 코드

    @JsonProperty("price_unit")
    private BigDecimal priceUnit;         // 주문금액 단위

    @JsonProperty("min_total")
    private BigDecimal minTotal;          // 최소 매도/매수 금액
}
