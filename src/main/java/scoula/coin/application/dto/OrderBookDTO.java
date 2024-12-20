package scoula.coin.application.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderBookDTO {
    private BigDecimal bid_fee;
    private BigDecimal ask_fee;
    private BigDecimal maker_bid_fee;
    private BigDecimal maker_ask_fee;
    private Market market;
    private Account bid_account;
    private Account ask_account;
}
