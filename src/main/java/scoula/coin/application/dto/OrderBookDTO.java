package scoula.coin.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class OrderBookDTO {
    @JsonProperty("bid_fee")
    private BigDecimal bidFee;        // 매수 수수료 비율

    @JsonProperty("ask_fee")
    private BigDecimal askFee;        // 매도 수수료 비율

    @JsonProperty("maker_bid_fee")
    private BigDecimal makerBidFee;   // 마켓 매수 수수료 비율

    @JsonProperty("maker_ask_fee")
    private BigDecimal makerAskFee;   // 마켓 매도 수수료 비율

    private Market market;            // 마켓 정보

    @JsonProperty("bid_account")
    private Account bidAccount;       // 매수 계좌

    @JsonProperty("ask_account")
    private Account askAccount;       // 매도 계좌

    @Data
    @Builder
    public static class Market {
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

    @Data
    @Builder
    public static class OrderConstraint {
        private String currency;              // 화폐를 의미하는 영문 대문자 코드

        @JsonProperty("price_unit")
        private BigDecimal priceUnit;         // 주문금액 단위

        @JsonProperty("min_total")
        private BigDecimal minTotal;          // 최소 매도/매수 금액
    }

    @Data
    @Builder
    public static class Account {
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

}
