package scoula.coin.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class Market {
    private String id;                    // 마켓의 고유 키
    private String name;                  // 마켓 이름

    @JsonProperty("order_types")
    private List<String> orderTypes;      // 지원 주문 방식

    @JsonProperty("ask_types")
    private List<String> askTypes;        // 매도 주문 지원 방식

    @JsonProperty("bid_types")
    private List<String> bidTypes;        // 매수 주문 지원 방식

    @JsonProperty("order_sides")
    private List<String> orderSides;      // 지원 주문 종류

    private OrderConstraint bid;          // 매수 제약사항
    private OrderConstraint ask;          // 매도 제약사항

    @JsonProperty("max_total")
    private BigDecimal maxTotal;          // 최대 매도/매수 금액

    private String state;                 // 마켓 운영 상태
}
