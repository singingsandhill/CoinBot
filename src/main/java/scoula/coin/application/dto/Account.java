package scoula.coin.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class Account {
    private String currency;              // 화폐를 의미하는 영문 대문자 코드
    private BigDecimal balance;           // 주문가능 금액/수량
    private BigDecimal locked;            // 주문 중 묶여있는 금액/수량

    @JsonProperty("avg_buy_price")
    private BigDecimal avgBuyPrice;       // 매수평균가

    @JsonProperty("avg_buy_price_modified")
    private Boolean avgBuyPriceModified;  // 매수평균가 수정 여부

    @JsonProperty("unit_currency")
    private String unitCurrency;          // 평단가 기준 화폐
}
